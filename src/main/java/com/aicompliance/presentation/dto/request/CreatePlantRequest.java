package com.aicompliance.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreatePlantRequest(
        @NotBlank String name,
        String address,
        String gstNumber,
        String plantType,
        String contactPerson) {
}
