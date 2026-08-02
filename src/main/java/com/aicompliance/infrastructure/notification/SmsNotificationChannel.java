package com.aicompliance.infrastructure.notification;

import com.aicompliance.application.port.NotificationChannel;
import com.aicompliance.domain.notification.NotificationChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Logging stub - ready to swap in a Twilio (or similar) adapter later. */
@Component
public class SmsNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationChannel.class);

    @Override
    public NotificationChannelType getType() {
        return NotificationChannelType.SMS;
    }

    @Override
    public void send(NotificationMessage message) {
        log.info("[STUB SMS] to={} subject={} body={}", message.recipientAddress(), message.subject(),
                message.body());
    }
}
