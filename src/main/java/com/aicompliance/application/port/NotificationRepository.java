package com.aicompliance.application.port;

import com.aicompliance.domain.notification.Notification;
import com.aicompliance.domain.notification.NotificationTriggerType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findAllByCompanyIdAndRecipientUserId(UUID companyId, UUID recipientUserId);

    List<Notification> findAllByAcknowledgedAtIsNull();

    List<Notification> findAllByRelatedEntityTypeAndRelatedEntityIdAndTriggerType(
            String relatedEntityType, UUID relatedEntityId, NotificationTriggerType triggerType);
}
