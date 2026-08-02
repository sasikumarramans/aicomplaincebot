package com.aicompliance.presentation.dto.response;

import com.aicompliance.domain.certificate.CertificateVersion;
import java.time.Instant;
import java.util.UUID;

public record CertificateVersionResponse(
        UUID id,
        int versionNumber,
        String originalFileName,
        String mimeType,
        long fileSizeBytes,
        boolean archived,
        String processingStatus,
        Instant uploadedAt) {

    public static CertificateVersionResponse from(CertificateVersion version) {
        return new CertificateVersionResponse(
                version.getId(),
                version.getVersionNumber(),
                version.getOriginalFileName(),
                version.getMimeType(),
                version.getFileSizeBytes(),
                version.isArchived(),
                version.getProcessingStatus().name(),
                version.getUploadedAt());
    }
}
