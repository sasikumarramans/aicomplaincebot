package com.aicompliance.presentation.dto.response;

import com.aicompliance.domain.vendor.VendorDocument;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record VendorDocumentResponse(
        UUID id,
        UUID vendorId,
        UUID categoryId,
        String originalFileName,
        LocalDate issueDate,
        LocalDate expiryDate,
        String status,
        UUID reviewedByUserId,
        Instant reviewedAt,
        String reviewNotes) {

    public static VendorDocumentResponse from(VendorDocument document) {
        return new VendorDocumentResponse(
                document.getId(),
                document.getVendorId(),
                document.getCategoryId(),
                document.getOriginalFileName(),
                document.getIssueDate(),
                document.getExpiryDate(),
                document.getStatus().name(),
                document.getReviewedByUserId(),
                document.getReviewedAt(),
                document.getReviewNotes());
    }
}
