package com.aicompliance.presentation.dto.request;

import com.aicompliance.domain.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotBlank String fullName,
        @NotNull Role role,
        UUID plantId,
        String department,
        Boolean temporary,
        Instant accessExpiresAt) {

    public boolean isTemporary() {
        return Boolean.TRUE.equals(temporary);
    }
}
