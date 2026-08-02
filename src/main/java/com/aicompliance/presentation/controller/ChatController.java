package com.aicompliance.presentation.controller;

import com.aicompliance.application.chat.ChatAssistantService;
import com.aicompliance.presentation.dto.request.AskChatRequest;
import com.aicompliance.presentation.dto.response.ChatAnswerResponse;
import com.aicompliance.presentation.dto.response.ChatConversationResponse;
import com.aicompliance.presentation.dto.response.ChatMessageResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatAssistantService chatAssistantService;

    public ChatController(ChatAssistantService chatAssistantService) {
        this.chatAssistantService = chatAssistantService;
    }

    @PostMapping("/ask")
    public ResponseEntity<ChatAnswerResponse> ask(@Valid @RequestBody AskChatRequest request) {
        ChatAssistantService.AskResult result = chatAssistantService.ask(request.conversationId(),
                request.question());
        return ResponseEntity.ok(new ChatAnswerResponse(result.conversationId(), result.answer()));
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ChatConversationResponse>> listConversations() {
        return ResponseEntity.ok(chatAssistantService.listConversationsForCurrentUser().stream()
                .map(ChatConversationResponse::from)
                .toList());
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<ChatMessageResponse>> listMessages(@PathVariable UUID id) {
        return ResponseEntity.ok(chatAssistantService.listMessages(id).stream()
                .map(ChatMessageResponse::from)
                .toList());
    }
}
