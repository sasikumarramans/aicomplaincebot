package com.aicompliance.presentation.controller;

import com.aicompliance.application.compliance.RiskAssessmentService;
import com.aicompliance.domain.compliance.RiskAssessment;
import com.aicompliance.presentation.dto.response.RiskAssessmentResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/risk-assessments")
public class RiskAssessmentController {

    private final RiskAssessmentService riskAssessmentService;

    public RiskAssessmentController(RiskAssessmentService riskAssessmentService) {
        this.riskAssessmentService = riskAssessmentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'COMPLIANCE_MANAGER')")
    public ResponseEntity<RiskAssessmentResponse> assessNow() {
        RiskAssessment assessment = riskAssessmentService.assess();
        return ResponseEntity.ok(RiskAssessmentResponse.from(assessment));
    }

    @GetMapping
    public ResponseEntity<List<RiskAssessmentResponse>> history() {
        return ResponseEntity.ok(riskAssessmentService.history().stream()
                .map(RiskAssessmentResponse::from)
                .toList());
    }
}
