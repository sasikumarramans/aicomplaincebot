package com.aicompliance.presentation.dto.response;

import com.aicompliance.application.certificate.RenewalPredictionService;
import java.time.LocalDate;

public record RenewalPredictionResponse(int recommendedLeadTimeDays, String explanation,
        LocalDate recommendedStartDate) {

    public static RenewalPredictionResponse from(RenewalPredictionService.Prediction prediction) {
        return new RenewalPredictionResponse(prediction.recommendedLeadTimeDays(), prediction.explanation(),
                prediction.recommendedStartDate());
    }
}
