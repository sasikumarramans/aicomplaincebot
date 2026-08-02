package com.aicompliance.application.notification;

import com.aicompliance.application.port.CompanyContextProvider;
import com.aicompliance.application.port.NotificationChannel;
import com.aicompliance.application.port.NotificationRepository;
import com.aicompliance.application.port.UserRepository;
import com.aicompliance.domain.notification.EscalationLevel;
import com.aicompliance.domain.notification.Notification;
import com.aicompliance.domain.notification.NotificationChannelType;
import com.aicompliance.domain.notification.NotificationStatus;
import com.aicompliance.domain.notification.NotificationTriggerType;
import com.aicompliance.domain.shared.EntityNotFoundException;
import com.aicompliance.domain.shared.InvalidStateException;
import com.aicompliance.domain.user.User;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dispatches a notification through the appropriate channel(s) - a strategy map keyed by
 * {@link NotificationChannelType}, so real (Email) and stub (SMS/WhatsApp/Push/Teams/Slack)
 * implementations are selected identically. Callers pass companyId/recipientUserId explicitly
 * rather than reading them from CompanyContextProvider, since this also runs from the
 * unauthenticated ExpiryScanJob context.
 */
@Service
public class NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);

    private final Map<NotificationChannelType, NotificationChannel> channelsByType;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final CompanyContextProvider companyContextProvider;

    public NotificationDispatchService(List<NotificationChannel> channels,
            NotificationRepository notificationRepository, UserRepository userRepository,
            CompanyContextProvider companyContextProvider) {
        this.channelsByType = channels.stream()
                .collect(Collectors.toMap(NotificationChannel::getType, c -> c));
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.companyContextProvider = companyContextProvider;
    }

    public record DispatchCommand(
            UUID companyId,
            UUID recipientUserId,
            NotificationChannelType channel,
            NotificationTriggerType triggerType,
            String relatedEntityType,
            UUID relatedEntityId,
            EscalationLevel escalationLevel,
            String subject,
            String body) {
    }

    @Transactional
    public Notification dispatch(DispatchCommand command) {
        User recipient = userRepository.findById(command.recipientUserId())
                .orElseThrow(() -> new EntityNotFoundException("User", command.recipientUserId()));

        Notification notification = new Notification();
        notification.setCompanyId(command.companyId());
        notification.setRecipientUserId(command.recipientUserId());
        notification.setChannel(command.channel());
        notification.setTriggerType(command.triggerType());
        notification.setRelatedEntityType(command.relatedEntityType());
        notification.setRelatedEntityId(command.relatedEntityId());
        notification.setEscalationLevel(command.escalationLevel());
        notification.setSubject(command.subject());
        notification.setBody(command.body());
        notification = notificationRepository.save(notification);

        NotificationChannel channelImpl = channelsByType.get(command.channel());
        try {
            channelImpl.send(new NotificationChannel.NotificationMessage(
                    recipientAddress(recipient, command.channel()), command.subject(), command.body()));
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
        } catch (Exception e) {
            log.error("Failed to send {} notification {} to user {}", command.channel(), notification.getId(),
                    command.recipientUserId(), e);
            notification.setStatus(NotificationStatus.FAILED);
        }
        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification acknowledge(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification", notificationId));
        notification = companyContextProvider.requireOwnership(notification, "Notification", notificationId);

        if (!notification.getRecipientUserId().equals(companyContextProvider.getCurrentUserId())) {
            throw new InvalidStateException("Only the recipient can acknowledge this notification");
        }
        notification.setAcknowledgedAt(Instant.now());
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> listForCurrentUser() {
        return notificationRepository.findAllByCompanyIdAndRecipientUserId(
                companyContextProvider.getCurrentCompanyId(), companyContextProvider.getCurrentUserId());
    }

    /**
     * Only Email has a real recipient address today (the user's email). The stub channels have
     * no configured SMS number/Slack ID/etc. yet, so they log the user's email as a stand-in
     * identifier - sufficient for a stub, not for a real integration.
     */
    private String recipientAddress(User recipient, NotificationChannelType channel) {
        return recipient.getEmail();
    }
}
