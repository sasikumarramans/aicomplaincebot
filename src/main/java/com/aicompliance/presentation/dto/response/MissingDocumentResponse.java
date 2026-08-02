package com.aicompliance.presentation.dto.response;

import com.aicompliance.application.certificate.MissingDocumentDetectionService;
import java.util.UUID;

public record MissingDocumentResponse(UUID plantId, String plantName, UUID categoryId, String categoryName) {

    public static MissingDocumentResponse from(MissingDocumentDetectionService.MissingDocument missing) {
        return new MissingDocumentResponse(missing.plantId(), missing.plantName(), missing.categoryId(),
                missing.categoryName());
    }
}
