package com.aicompliance.presentation.dto.response;

import com.aicompliance.domain.certificate.Certificate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CertificateResponse(
        UUID id,
        UUID plantId,
        UUID categoryId,
        String certificateName,
        String certificateNumber,
        String issuingAuthority,
        LocalDate issueDate,
        LocalDate expiryDate,
        String licenseNumber,
        boolean hasQrCode,
        boolean hasDigitalSignature,
        String status,
        String expiryBucket,
        String expiryColorCode,
        Double aiConfidenceScore,
        UUID reviewedByUserId,
        Instant reviewedAt) {

    public static CertificateResponse from(Certificate certificate) {
        return new CertificateResponse(
                certificate.getId(),
                certificate.getPlantId(),
                certificate.getCategoryId(),
                certificate.getCertificateName(),
                certificate.getCertificateNumber(),
                certificate.getIssuingAuthority(),
                certificate.getIssueDate(),
                certificate.getExpiryDate(),
                certificate.getLicenseNumber(),
                certificate.isHasQrCode(),
                certificate.isHasDigitalSignature(),
                certificate.getStatus().name(),
                certificate.getExpiryBucket().name(),
                certificate.getExpiryBucket().getColorCode(),
                certificate.getAiConfidenceScore(),
                certificate.getReviewedByUserId(),
                certificate.getReviewedAt());
    }
}
