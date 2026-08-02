package com.aicompliance.presentation.dto.response;

import com.aicompliance.domain.certificate.FacilityTypeRequiredCategory;
import java.util.UUID;

public record FacilityRequirementResponse(UUID id, String facilityType, UUID categoryId) {

    public static FacilityRequirementResponse from(FacilityTypeRequiredCategory requirement) {
        return new FacilityRequirementResponse(requirement.getId(), requirement.getFacilityType(),
                requirement.getCategoryId());
    }
}
