package com.aicompliance.application.expiry;

import com.aicompliance.application.notification.NotificationDispatchService;
import com.aicompliance.application.port.CertificateRepository;
import com.aicompliance.application.port.EmployeeCertificationRepository;
import com.aicompliance.application.port.UserRepository;
import com.aicompliance.domain.certificate.Certificate;
import com.aicompliance.domain.notification.EscalationLevel;
import com.aicompliance.domain.notification.NotificationChannelType;
import com.aicompliance.domain.notification.NotificationTriggerType;
import com.aicompliance.domain.shared.Expirable;
import com.aicompliance.domain.shared.ExpiryBucket;
import com.aicompliance.domain.user.Role;
import com.aicompliance.domain.user.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recomputes every {@link Expirable} entity's expiry bucket, across all companies and entity
 * types (Certificate, EmployeeCertification), and dispatches a notification whenever a
 * Certificate's bucket actually changes (crosses a 30/15/7/3/0-day threshold or becomes
 * Expired). Runs outside any authenticated request context (scheduled job), so the tenant
 * filter is never enabled here by design - this is the one place that legitimately needs to see
 * every company's data at once.
 */
@Service
public class ExpiryBucketingService {

    private static final Logger log = LoggerFactory.getLogger(ExpiryBucketingService.class);

    private final CertificateRepository certificateRepository;
    private final EmployeeCertificationRepository employeeCertificationRepository;
    private final UserRepository userRepository;
    private final NotificationDispatchService notificationDispatchService;

    public ExpiryBucketingService(CertificateRepository certificateRepository,
            EmployeeCertificationRepository employeeCertificationRepository, UserRepository userRepository,
            NotificationDispatchService notificationDispatchService) {
        this.certificateRepository = certificateRepository;
        this.employeeCertificationRepository = employeeCertificationRepository;
        this.userRepository = userRepository;
        this.notificationDispatchService = notificationDispatchService;
    }

    @Transactional
    public int recomputeAllBuckets() {
        int changed = 0;
        changed += recomputeCertificates();
        changed += recompute(employeeCertificationRepository.findAll());
        return changed;
    }

    private int recomputeCertificates() {
        List<Certificate> certificates = certificateRepository.findAll();
        int changed = 0;
        for (Certificate certificate : certificates) {
            ExpiryBucket newBucket = ExpiryBucket.fromExpiryDate(certificate.getExpiryDate());
            if (newBucket != certificate.getExpiryBucket()) {
                certificate.setExpiryBucket(newBucket);
                changed++;
                notifyOfBucketChange(certificate, newBucket);
            }
        }
        log.info("Expiry bucket recompute: {} of {} Certificates changed bucket", changed, certificates.size());
        return changed;
    }

    private <T extends Expirable> int recompute(List<T> entities) {
        int changed = 0;
        for (T entity : entities) {
            ExpiryBucket newBucket = ExpiryBucket.fromExpiryDate(entity.getExpiryDate());
            if (newBucket != entity.getExpiryBucket()) {
                entity.setExpiryBucket(newBucket);
                changed++;
            }
        }
        log.info("Expiry bucket recompute: {} of {} {} changed bucket", changed, entities.size(),
                entities.isEmpty() ? "entities" : entities.get(0).getClass().getSimpleName());
        return changed;
    }

    private void notifyOfBucketChange(Certificate certificate, ExpiryBucket newBucket) {
        NotificationTriggerType triggerType = triggerTypeFor(newBucket);
        if (triggerType == null) {
            return; // NOT_TRACKED or a bucket with no corresponding notification.
        }

        Optional<User> recipient = resolveRecipient(certificate.getCompanyId());
        if (recipient.isEmpty()) {
            log.warn("No COMPLIANCE_MANAGER or COMPANY_ADMIN found to notify for company {}",
                    certificate.getCompanyId());
            return;
        }

        String certLabel = certificate.getCertificateName() != null
                ? certificate.getCertificateName()
                : "Certificate " + certificate.getId();

        notificationDispatchService.dispatch(new NotificationDispatchService.DispatchCommand(
                certificate.getCompanyId(), recipient.get().getId(), NotificationChannelType.EMAIL, triggerType,
                "Certificate", certificate.getId(), EscalationLevel.MANAGER,
                certLabel + " - " + describeBucket(newBucket),
                certLabel + " has crossed the " + describeBucket(newBucket).toLowerCase()
                        + " threshold. Please review and take action."));
    }

    private NotificationTriggerType triggerTypeFor(ExpiryBucket bucket) {
        return switch (bucket) {
            case DAYS_30 -> NotificationTriggerType.CERT_EXPIRY_30;
            case DAYS_15 -> NotificationTriggerType.CERT_EXPIRY_15;
            case DAYS_7 -> NotificationTriggerType.CERT_EXPIRY_7;
            case DAYS_3 -> NotificationTriggerType.CERT_EXPIRY_3;
            case DAYS_0 -> NotificationTriggerType.CERT_EXPIRY_0;
            case EXPIRED -> NotificationTriggerType.CERT_EXPIRED;
            case NOT_TRACKED -> null;
        };
    }

    private String describeBucket(ExpiryBucket bucket) {
        return switch (bucket) {
            case DAYS_30 -> "30-day";
            case DAYS_15 -> "15-day";
            case DAYS_7 -> "7-day";
            case DAYS_3 -> "3-day";
            case DAYS_0 -> "Expiring today";
            case EXPIRED -> "Expired";
            case NOT_TRACKED -> "";
        };
    }

    private Optional<User> resolveRecipient(UUID companyId) {
        List<User> companyUsers = userRepository.findAllByCompanyId(companyId);
        return companyUsers.stream()
                .filter(u -> u.getRole() == Role.COMPLIANCE_MANAGER && u.isActive())
                .findFirst()
                .or(() -> companyUsers.stream()
                        .filter(u -> u.getRole() == Role.COMPANY_ADMIN && u.isActive())
                        .findFirst());
    }
}
