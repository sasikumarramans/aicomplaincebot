package com.aicompliance.presentation.dto.request;

import com.aicompliance.domain.company.SubscriptionStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateCompanyStatusRequest(
        @NotNull SubscriptionStatus subscriptionStatus,
        @NotNull Boolean active) {
}
