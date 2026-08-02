package com.aicompliance.presentation.dto.response;

import java.time.Instant;

public record TokenResponse(String accessToken, String refreshToken, Instant refreshExpiresAt) {
}
