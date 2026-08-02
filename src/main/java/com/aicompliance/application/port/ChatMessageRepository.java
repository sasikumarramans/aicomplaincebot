package com.aicompliance.application.port;

import com.aicompliance.domain.chat.ChatMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findAllByConversationIdOrderByCreatedAtAsc(UUID conversationId);
}
