package com.aicompliance.application.chat;

import com.aicompliance.application.port.AiService;
import com.aicompliance.application.port.ChatConversationRepository;
import com.aicompliance.application.port.ChatMessageRepository;
import com.aicompliance.application.port.CompanyContextProvider;
import com.aicompliance.domain.chat.ChatConversation;
import com.aicompliance.domain.chat.ChatMessage;
import com.aicompliance.domain.chat.ChatMessageRole;
import com.aicompliance.domain.shared.EntityNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatAssistantService {

    private static final Logger log = LoggerFactory.getLogger(ChatAssistantService.class);
    private static final int MAX_TITLE_LENGTH = 60;
    private static final String AI_UNAVAILABLE_MESSAGE =
            "Sorry, the compliance assistant is temporarily unavailable. Please try again shortly.";

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final ComplianceContextRetriever contextRetriever;
    private final AiService aiService;
    private final CompanyContextProvider companyContextProvider;
    private final ObjectMapper objectMapper;

    public ChatAssistantService(ChatConversationRepository conversationRepository,
            ChatMessageRepository messageRepository, ComplianceContextRetriever contextRetriever,
            AiService aiService, CompanyContextProvider companyContextProvider, ObjectMapper objectMapper) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.contextRetriever = contextRetriever;
        this.aiService = aiService;
        this.companyContextProvider = companyContextProvider;
        this.objectMapper = objectMapper;
    }

    public record AskResult(UUID conversationId, String answer) {
    }

    @Transactional
    public AskResult ask(UUID existingConversationId, String question) {
        UUID companyId = companyContextProvider.getCurrentCompanyId();
        UUID userId = companyContextProvider.getCurrentUserId();

        ChatConversation conversation = existingConversationId != null
                ? getOwnedConversation(existingConversationId)
                : createConversation(companyId, userId, question);

        String contextJson = toJson(contextRetriever.retrieveCertificateSummaries());

        ChatMessage userMessage = new ChatMessage();
        userMessage.setCompanyId(companyId);
        userMessage.setConversationId(conversation.getId());
        userMessage.setRole(ChatMessageRole.USER);
        userMessage.setContent(question);
        userMessage.setContextJson(contextJson);
        messageRepository.save(userMessage);

        String answer;
        try {
            answer = aiService.answerComplianceQuestion(question, contextJson);
        } catch (Exception e) {
            log.error("AI chat call failed for conversation {}", conversation.getId(), e);
            answer = AI_UNAVAILABLE_MESSAGE;
        }

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setCompanyId(companyId);
        assistantMessage.setConversationId(conversation.getId());
        assistantMessage.setRole(ChatMessageRole.ASSISTANT);
        assistantMessage.setContent(answer);
        messageRepository.save(assistantMessage);

        conversation.setLastMessageAt(Instant.now());
        conversationRepository.save(conversation);

        return new AskResult(conversation.getId(), answer);
    }

    @Transactional(readOnly = true)
    public List<ChatConversation> listConversationsForCurrentUser() {
        return conversationRepository.findAllByCompanyIdAndUserId(
                companyContextProvider.getCurrentCompanyId(), companyContextProvider.getCurrentUserId());
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> listMessages(UUID conversationId) {
        getOwnedConversation(conversationId);
        return messageRepository.findAllByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    private ChatConversation getOwnedConversation(UUID conversationId) {
        ChatConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("ChatConversation", conversationId));
        return companyContextProvider.requireOwnership(conversation, "ChatConversation", conversationId);
    }

    private ChatConversation createConversation(UUID companyId, UUID userId, String firstQuestion) {
        ChatConversation conversation = new ChatConversation();
        conversation.setCompanyId(companyId);
        conversation.setUserId(userId);
        conversation.setTitle(firstQuestion.length() > MAX_TITLE_LENGTH
                ? firstQuestion.substring(0, MAX_TITLE_LENGTH) + "..."
                : firstQuestion);
        return conversationRepository.save(conversation);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }
}
