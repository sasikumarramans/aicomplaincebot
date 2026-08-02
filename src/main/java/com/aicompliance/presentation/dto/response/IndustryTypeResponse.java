package com.aicompliance.presentation.dto.response;

import com.aicompliance.domain.company.IndustryType;
import java.util.UUID;

public record IndustryTypeResponse(UUID id, String name, String description, boolean active) {

    public static IndustryTypeResponse from(IndustryType industryType) {
        return new IndustryTypeResponse(industryType.getId(), industryType.getName(),
                industryType.getDescription(), industryType.isActive());
    }
}
