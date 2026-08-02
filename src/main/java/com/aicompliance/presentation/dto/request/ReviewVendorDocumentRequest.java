package com.aicompliance.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

public record ReviewVendorDocumentRequest(
        @NotNull Decision decision,
        String notes) {

    public enum Decision {
        APPROVE,
        REJECT
    }
}
