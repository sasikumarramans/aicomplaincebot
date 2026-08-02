package com.aicompliance.presentation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateCompanyRequest(
        @NotBlank String name,
        String gstNumber,
        String panNumber,
        String cinNumber,
        String registeredAddress,
        UUID industryId,
        @NotBlank @Email String adminEmail,
        @NotBlank String adminPassword,
        @NotBlank String adminFullName) {
}
