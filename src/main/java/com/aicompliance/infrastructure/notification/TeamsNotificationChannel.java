package com.aicompliance.infrastructure.notification;

import com.aicompliance.application.port.NotificationChannel;
import com.aicompliance.domain.notification.NotificationChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Logging stub - ready to swap in a Microsoft Teams incoming-webhook adapter later. */
@Component
public class TeamsNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(TeamsNotificationChannel.class);

    @Override
    public NotificationChannelType getType() {
        return NotificationChannelType.TEAMS;
    }

    @Override
    public void send(NotificationMessage message) {
        log.info("[STUB TEAMS] to={} subject={} body={}", message.recipientAddress(), message.subject(),
                message.body());
    }
}
