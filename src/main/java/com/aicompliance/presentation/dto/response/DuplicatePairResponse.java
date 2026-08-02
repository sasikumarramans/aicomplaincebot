package com.aicompliance.presentation.dto.response;

import com.aicompliance.application.certificate.DuplicateDetectionService;
import java.util.UUID;

public record DuplicatePairResponse(
        UUID certificateAId,
        String certificateAName,
        UUID certificateBId,
        String certificateBName,
        String matchType) {

    public static DuplicatePairResponse from(DuplicateDetectionService.DuplicatePair pair) {
        return new DuplicatePairResponse(pair.certificateAId(), pair.certificateAName(), pair.certificateBId(),
                pair.certificateBName(), pair.matchType());
    }
}
