package com.aicompliance.infrastructure.notification;

import com.aicompliance.application.port.NotificationChannel;
import com.aicompliance.domain.notification.NotificationChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Logging stub - ready to swap in the WhatsApp Business API later. */
@Component
public class WhatsAppNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppNotificationChannel.class);

    @Override
    public NotificationChannelType getType() {
        return NotificationChannelType.WHATSAPP;
    }

    @Override
    public void send(NotificationMessage message) {
        log.info("[STUB WHATSAPP] to={} subject={} body={}", message.recipientAddress(), message.subject(),
                message.body());
    }
}
