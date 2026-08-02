package com.aicompliance.presentation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateSignupRequestRequest(
        @NotBlank String companyName,
        UUID industryId,
        String registeredAddress,
        String gstNumber,
        @NotBlank String adminFullName,
        @NotBlank @Email String adminEmail,
        String phoneNumber,
        String message) {
}
