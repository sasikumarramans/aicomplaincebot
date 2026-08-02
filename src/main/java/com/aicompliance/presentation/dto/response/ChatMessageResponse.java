package com.aicompliance.presentation.dto.response;

import com.aicompliance.domain.chat.ChatMessage;
import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(UUID id, String role, String content, Instant createdAt) {

    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(message.getId(), message.getRole().name(), message.getContent(),
                message.getCreatedAt());
    }
}
