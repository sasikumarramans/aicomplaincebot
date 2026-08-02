package com.aicompliance.presentation.controller;

import com.aicompliance.application.compliance.ComplianceScoreService;
import com.aicompliance.presentation.dto.response.ComplianceScoreResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/compliance-score")
public class ComplianceScoreController {

    private final ComplianceScoreService complianceScoreService;

    public ComplianceScoreController(ComplianceScoreService complianceScoreService) {
        this.complianceScoreService = complianceScoreService;
    }

    @GetMapping
    public ResponseEntity<ComplianceScoreResponse> getScore(
            @RequestParam(defaultValue = "false") boolean explain) {
        ComplianceScoreService.ScoreBreakdown breakdown = complianceScoreService.computeScore();
        String explanation = explain ? complianceScoreService.explainScore(breakdown) : null;
        return ResponseEntity.ok(ComplianceScoreResponse.from(breakdown, explanation));
    }
}
