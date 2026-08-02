package com.aicompliance.presentation.dto.response;

import com.aicompliance.application.compliance.ComplianceScoreService;
import java.util.List;

public record ComplianceScoreResponse(double overallPercent, List<CategoryScoreResponse> byCategoryGroup,
        String explanation) {

    public record CategoryScoreResponse(String categoryGroup, double scorePercent, int validCount, int totalCount) {

        public static CategoryScoreResponse from(ComplianceScoreService.CategoryScore score) {
            return new CategoryScoreResponse(score.categoryGroup(), score.scorePercent(), score.validCount(),
                    score.totalCount());
        }
    }

    public static ComplianceScoreResponse from(ComplianceScoreService.ScoreBreakdown breakdown, String explanation) {
        return new ComplianceScoreResponse(
                breakdown.overallPercent(),
                breakdown.byCategoryGroup().stream().map(CategoryScoreResponse::from).toList(),
                explanation);
    }
}
