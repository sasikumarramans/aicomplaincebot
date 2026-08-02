package com.aicompliance.application.notification;

import com.aicompliance.application.port.NotificationRepository;
import com.aicompliance.application.port.UserRepository;
import com.aicompliance.domain.notification.EscalationLevel;
import com.aicompliance.domain.notification.Notification;
import com.aicompliance.domain.notification.NotificationChannelType;
import com.aicompliance.domain.notification.NotificationTriggerType;
import com.aicompliance.domain.user.Role;
import com.aicompliance.domain.user.User;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escalates unacknowledged critical notifications through Manager -> Department Head ->
 * Compliance Officer -> Director (the last two both resolve to COMPANY_ADMIN - see
 * {@link EscalationLevel}). Runs unauthenticated (scheduled job), same pattern as
 * ExpiryBucketingService: reads across all companies, since there is no per-request tenant
 * context to scope from.
 */
@Service
public class EscalationService {

    private static final Logger log = LoggerFactory.getLogger(EscalationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationDispatchService dispatchService;
    private final Duration escalationDelay;

    public EscalationService(NotificationRepository notificationRepository, UserRepository userRepository,
            NotificationDispatchService dispatchService,
            @Value("${app.escalation.delay-hours:48}") long escalationDelayHours) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.dispatchService = dispatchService;
        this.escalationDelay = Duration.ofHours(escalationDelayHours);
    }

    @Transactional
    public int escalateOverdue() {
        List<Notification> unacknowledged = notificationRepository.findAllByAcknowledgedAtIsNull();
        Instant cutoff = Instant.now().minus(escalationDelay);

        int escalated = 0;
        for (Notification notification : unacknowledged) {
            if (notification.getSentAt() == null || notification.getSentAt().isAfter(cutoff)) {
                continue;
            }
            // Only escalate genuinely escalatable triggers, not routine reminders.
            if (notification.getTriggerType() != NotificationTriggerType.CERT_EXPIRED
                    && notification.getTriggerType() != NotificationTriggerType.APPROVAL_NEEDED) {
                continue;
            }

            EscalationLevel currentLevel = notification.getEscalationLevel();
            Optional<EscalationLevel> nextLevel = nextLevel(currentLevel);
            if (nextLevel.isEmpty()) {
                continue; // already at DIRECTOR, nowhere further to escalate.
            }

            Optional<User> nextRecipient = resolveRecipient(notification.getCompanyId(), nextLevel.get());
            if (nextRecipient.isEmpty()) {
                log.warn("No recipient found for escalation level {} in company {}", nextLevel.get(),
                        notification.getCompanyId());
                continue;
            }

            dispatchService.dispatch(new NotificationDispatchService.DispatchCommand(
                    notification.getCompanyId(), nextRecipient.get().getId(), NotificationChannelType.EMAIL,
                    NotificationTriggerType.ESCALATION, notification.getRelatedEntityType(),
                    notification.getRelatedEntityId(), nextLevel.get(),
                    "[Escalated] " + notification.getSubject(),
                    "This notification was not acknowledged within " + escalationDelay.toHours()
                            + " hours and has been escalated.\n\n" + notification.getBody()));

            // Advance the ORIGINAL notification's level so the next sweep escalates further
            // instead of re-escalating to the same tier again (it stays unacknowledged/overdue
            // indefinitely otherwise, since acknowledging happens on whichever copy the
            // recipient actually sees, not necessarily this row).
            notification.setEscalationLevel(nextLevel.get());
            notificationRepository.save(notification);
            escalated++;
        }
        log.info("Escalation sweep: {} of {} unacknowledged notifications escalated", escalated,
                unacknowledged.size());
        return escalated;
    }

    private Optional<EscalationLevel> nextLevel(EscalationLevel current) {
        if (current == null) {
            return Optional.of(EscalationLevel.MANAGER);
        }
        return switch (current) {
            case MANAGER -> Optional.of(EscalationLevel.DEPARTMENT_HEAD);
            case DEPARTMENT_HEAD -> Optional.of(EscalationLevel.COMPLIANCE_OFFICER);
            case COMPLIANCE_OFFICER -> Optional.of(EscalationLevel.DIRECTOR);
            case DIRECTOR -> Optional.empty();
        };
    }

    private Optional<User> resolveRecipient(UUID companyId, EscalationLevel level) {
        Role role = switch (level) {
            case MANAGER -> Role.COMPLIANCE_MANAGER;
            case DEPARTMENT_HEAD -> Role.DEPARTMENT_HEAD;
            case COMPLIANCE_OFFICER, DIRECTOR -> Role.COMPANY_ADMIN;
        };
        return userRepository.findAllByCompanyId(companyId).stream()
                .filter(u -> u.getRole() == role && u.isActive())
                .findFirst();
    }
}
