package com.aicompliance.infrastructure.notification;

import com.aicompliance.application.port.NotificationChannel;
import com.aicompliance.domain.notification.NotificationChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Logging stub - ready to swap in the Slack SDK later. */
@Component
public class SlackNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SlackNotificationChannel.class);

    @Override
    public NotificationChannelType getType() {
        return NotificationChannelType.SLACK;
    }

    @Override
    public void send(NotificationMessage message) {
        log.info("[STUB SLACK] to={} subject={} body={}", message.recipientAddress(), message.subject(),
                message.body());
    }
}
