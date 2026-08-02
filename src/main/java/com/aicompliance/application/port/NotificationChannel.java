package com.aicompliance.application.port;

import com.aicompliance.domain.notification.NotificationChannelType;

public interface NotificationChannel {

    NotificationChannelType getType();

    /**
     * Sends the notification. Implementations should throw on failure (caught by the dispatcher,
     * which marks the Notification FAILED) rather than swallowing errors.
     */
    void send(NotificationMessage message);

    record NotificationMessage(String recipientAddress, String subject, String body) {
    }
}
