package com.aicompliance.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddFacilityRequirementRequest(@NotBlank String facilityType, @NotNull UUID categoryId) {
}
