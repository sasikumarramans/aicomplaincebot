package com.aicompliance.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record AskChatRequest(UUID conversationId, @NotBlank String question) {
}
