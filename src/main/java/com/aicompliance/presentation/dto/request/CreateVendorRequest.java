package com.aicompliance.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateVendorRequest(
        @NotBlank String name,
        String gstNumber,
        String panNumber,
        String contactEmail,
        String contactPhone) {
}
