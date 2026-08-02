package com.aicompliance.presentation.dto.response;

import java.util.UUID;

public record ChatAnswerResponse(UUID conversationId, String answer) {
}
