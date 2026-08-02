package com.aicompliance.presentation.dto.response;

import com.aicompliance.domain.company.CompanySignupRequest;
import java.time.Instant;
import java.util.UUID;

public record SignupRequestResponse(
        UUID id,
        String companyName,
        UUID industryId,
        String registeredAddress,
        String gstNumber,
        String adminFullName,
        String adminEmail,
        String phoneNumber,
        String message,
        String status,
        UUID reviewedByUserId,
        Instant reviewedAt,
        String rejectionReason,
        UUID createdCompanyId,
        Instant createdAt) {

    public static SignupRequestResponse from(CompanySignupRequest request) {
        return new SignupRequestResponse(
                request.getId(),
                request.getCompanyName(),
                request.getIndustryId(),
                request.getRegisteredAddress(),
                request.getGstNumber(),
                request.getAdminFullName(),
                request.getAdminEmail(),
                request.getPhoneNumber(),
                request.getMessage(),
                request.getStatus().name(),
                request.getReviewedByUserId(),
                request.getReviewedAt(),
                request.getRejectionReason(),
                request.getCreatedCompanyId(),
                request.getCreatedAt());
    }
}
