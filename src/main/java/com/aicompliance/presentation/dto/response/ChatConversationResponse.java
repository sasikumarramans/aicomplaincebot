package com.aicompliance.presentation.dto.response;

import com.aicompliance.domain.chat.ChatConversation;
import java.time.Instant;
import java.util.UUID;

public record ChatConversationResponse(UUID id, String title, Instant lastMessageAt) {

    public static ChatConversationResponse from(ChatConversation conversation) {
        return new ChatConversationResponse(conversation.getId(), conversation.getTitle(),
                conversation.getLastMessageAt());
    }
}
