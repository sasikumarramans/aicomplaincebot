package com.aicompliance.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(@NotBlank String name, String groupLabel, String description) {
}
