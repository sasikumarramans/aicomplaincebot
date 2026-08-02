package com.aicompliance.infrastructure.notification;

import com.aicompliance.application.port.NotificationChannel;
import com.aicompliance.domain.notification.NotificationChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Logging stub - ready to swap in FCM/APNs later. */
@Component
public class PushNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationChannel.class);

    @Override
    public NotificationChannelType getType() {
        return NotificationChannelType.PUSH;
    }

    @Override
    public void send(NotificationMessage message) {
        log.info("[STUB PUSH] to={} subject={} body={}", message.recipientAddress(), message.subject(),
                message.body());
    }
}
