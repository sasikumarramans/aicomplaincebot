package com.aicompliance.presentation.controller;

import com.aicompliance.application.dashboard.DashboardAggregationService;
import com.aicompliance.presentation.dto.response.ChartPointResponse;
import com.aicompliance.presentation.dto.response.DashboardSummaryResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardAggregationService dashboardAggregationService;

    public DashboardController(DashboardAggregationService dashboardAggregationService) {
        this.dashboardAggregationService = dashboardAggregationService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> summary() {
        return ResponseEntity.ok(DashboardSummaryResponse.from(dashboardAggregationService.getSummary()));
    }

    @GetMapping("/charts/expiry-trend")
    public ResponseEntity<List<ChartPointResponse>> expiryTrend() {
        return ResponseEntity.ok(dashboardAggregationService.getExpiryTrend().stream()
                .map(ChartPointResponse::from)
                .toList());
    }

    @GetMapping("/charts/plant-wise-compliance")
    public ResponseEntity<List<ChartPointResponse>> plantWiseCompliance() {
        return ResponseEntity.ok(dashboardAggregationService.getPlantWiseCompliance().stream()
                .map(ChartPointResponse::from)
                .toList());
    }

    @GetMapping("/charts/category-distribution")
    public ResponseEntity<List<ChartPointResponse>> categoryDistribution() {
        return ResponseEntity.ok(dashboardAggregationService.getCategoryDistribution().stream()
                .map(ChartPointResponse::from)
                .toList());
    }
}
