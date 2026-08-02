package com.aicompliance.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateIndustryTypeRequest(@NotBlank String name, String description) {
}
