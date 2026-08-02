package com.aicompliance.presentation.dto.response;

import com.aicompliance.domain.company.Company;
import java.time.Instant;
import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String name,
        String gstNumber,
        String panNumber,
        String cinNumber,
        String registeredAddress,
        UUID industryId,
        String subscriptionStatus,
        boolean active,
        Instant createdAt,
        long employeeCount,
        long certificateCount) {

    public static CompanyResponse from(Company company, long employeeCount, long certificateCount) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getGstNumber(),
                company.getPanNumber(),
                company.getCinNumber(),
                company.getRegisteredAddress(),
                company.getIndustryId(),
                company.getSubscriptionStatus().name(),
                company.isActive(),
                company.getCreatedAt(),
                employeeCount,
                certificateCount);
    }
}
