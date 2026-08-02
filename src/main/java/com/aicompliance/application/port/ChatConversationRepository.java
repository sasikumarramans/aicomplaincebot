package com.aicompliance.application.port;

import com.aicompliance.domain.chat.ChatConversation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, UUID> {

    List<ChatConversation> findAllByCompanyIdAndUserId(UUID companyId, UUID userId);
}
