package com.aicompliance.domain.shared;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Shared across every expirable entity (Certificate, EmployeeCertification, VendorDocument, ...)
 * so the 30/15/7/3/0/Expired bucketing and color-code rules live in exactly one place.
 */
public enum ExpiryBucket {
    DAYS_30("GREEN"),
    DAYS_15("ORANGE"),
    DAYS_7("ORANGE"),
    DAYS_3("RED"),
    DAYS_0("RED"),
    EXPIRED("RED"),
    NOT_TRACKED("GREEN");

    private final String colorCode;

    ExpiryBucket(String colorCode) {
        this.colorCode = colorCode;
    }

    public String getColorCode() {
        return colorCode;
    }

    public static ExpiryBucket fromExpiryDate(LocalDate expiryDate) {
        if (expiryDate == null) {
            return NOT_TRACKED;
        }
        return fromDaysRemaining(ChronoUnit.DAYS.between(LocalDate.now(), expiryDate));
    }

    public static ExpiryBucket fromDaysRemaining(long daysRemaining) {
        if (daysRemaining < 0) {
            return EXPIRED;
        }
        if (daysRemaining == 0) {
            return DAYS_0;
        }
        if (daysRemaining <= 3) {
            return DAYS_3;
        }
        if (daysRemaining <= 7) {
            return DAYS_7;
        }
        if (daysRemaining <= 15) {
            return DAYS_15;
        }
        if (daysRemaining <= 30) {
            return DAYS_30;
        }
        return NOT_TRACKED;
    }
}
