package com.aicompliance.presentation.dto.response;

import com.aicompliance.application.dashboard.DashboardAggregationService;

public record ChartPointResponse(String label, long value) {

    public static ChartPointResponse from(DashboardAggregationService.ChartPoint point) {
        return new ChartPointResponse(point.label(), point.value());
    }
}
