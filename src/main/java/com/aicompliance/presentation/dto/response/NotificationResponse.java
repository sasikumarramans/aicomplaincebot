package com.aicompliance.presentation.dto.response;

import com.aicompliance.domain.notification.Notification;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String channel,
        String triggerType,
        String relatedEntityType,
        UUID relatedEntityId,
        String escalationLevel,
        String status,
        Instant sentAt,
        Instant acknowledgedAt,
        String subject,
        String body) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getChannel().name(),
                notification.getTriggerType().name(),
                notification.getRelatedEntityType(),
                notification.getRelatedEntityId(),
                notification.getEscalationLevel() != null ? notification.getEscalationLevel().name() : null,
                notification.getStatus().name(),
                notification.getSentAt(),
                notification.getAcknowledgedAt(),
                notification.getSubject(),
                notification.getBody());
    }
}
