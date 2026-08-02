package com.aicompliance.presentation.dto.response;

import com.aicompliance.domain.user.User;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String role,
        UUID plantId,
        String department,
        boolean active,
        boolean temporary,
        Instant accessExpiresAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole().name(),
                user.getPlantId(), user.getDepartment(), user.isActive(), user.isTemporary(),
                user.getAccessExpiresAt());
    }
}
