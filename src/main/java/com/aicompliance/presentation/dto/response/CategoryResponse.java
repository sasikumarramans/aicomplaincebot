package com.aicompliance.presentation.dto.response;

import com.aicompliance.domain.certificate.CertificateCategory;
import java.util.UUID;

public record CategoryResponse(UUID id, String name, String groupLabel, String description, boolean active) {

    public static CategoryResponse from(CertificateCategory category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getGroupLabel(),
                category.getDescription(), category.isActive());
    }
}
