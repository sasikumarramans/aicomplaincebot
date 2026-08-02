package com.aicompliance.presentation.dto.response;

import com.aicompliance.domain.compliance.RiskAssessment;
import java.time.Instant;
import java.util.UUID;

public record RiskAssessmentResponse(
        UUID id,
        String riskLevel,
        double riskScore,
        String reasoningText,
        Instant assessedAt) {

    public static RiskAssessmentResponse from(RiskAssessment assessment) {
        return new RiskAssessmentResponse(
                assessment.getId(),
                assessment.getRiskLevel().name(),
                assessment.getRiskScore().doubleValue(),
                assessment.getReasoningText(),
                assessment.getAssessedAt());
    }
}
