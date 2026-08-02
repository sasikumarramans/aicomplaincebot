package com.aicompliance.domain.certificate;

public enum ProcessingStatus {
    PENDING,
    EXTRACTING,
    COMPLETED,
    FAILED,
    /** No AI extraction was requested - all fields were supplied manually at upload. */
    NOT_APPLICABLE
}
