package com.aicompliance.presentation.dto.response;

import com.aicompliance.application.dashboard.DashboardAggregationService;

public record DashboardSummaryResponse(
        int totalCertificates,
        int expiringToday,
        int expiringIn30Days,
        int expired,
        int pendingApprovals,
        int missingDocuments,
        Double complianceScore,
        Double vendorCompliancePercent,
        EmployeeCertificationSummaryResponse employeeCertifications) {

    public record EmployeeCertificationSummaryResponse(
            int total,
            int expiringIn30Days,
            int expired,
            int validPercent) {

        public static EmployeeCertificationSummaryResponse from(
                DashboardAggregationService.EmployeeCertificationSummary summary) {
            return new EmployeeCertificationSummaryResponse(
                    summary.total(), summary.expiringIn30Days(), summary.expired(), summary.validPercent());
        }
    }

    public static DashboardSummaryResponse from(DashboardAggregationService.Summary summary) {
        return new DashboardSummaryResponse(
                summary.totalCertificates(),
                summary.expiringToday(),
                summary.expiringIn30Days(),
                summary.expired(),
                summary.pendingApprovals(),
                summary.missingDocuments(),
                summary.complianceScore(),
                summary.vendorCompliancePercent(),
                EmployeeCertificationSummaryResponse.from(summary.employeeCertifications()));
    }
}
