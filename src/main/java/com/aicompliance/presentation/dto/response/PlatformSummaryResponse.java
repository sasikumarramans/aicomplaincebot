package com.aicompliance.presentation.dto.response;

public record PlatformSummaryResponse(
        long totalCompanies,
        long trialCompanies,
        long activeCompanies,
        long suspendedCompanies,
        long cancelledCompanies,
        long totalEmployees,
        long totalCertificates) {
}
