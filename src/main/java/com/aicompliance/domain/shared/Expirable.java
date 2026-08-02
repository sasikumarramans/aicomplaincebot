package com.aicompliance.domain.shared;

import java.time.LocalDate;

/**
 * Implemented by any tenant-scoped entity that carries an expiry date and a cached
 * {@link ExpiryBucket}, so a single scheduled job can recompute buckets across entity types
 * (Certificate, EmployeeCertification, ...) without type-specific logic.
 */
public interface Expirable {

    LocalDate getExpiryDate();

    ExpiryBucket getExpiryBucket();

    void setExpiryBucket(ExpiryBucket expiryBucket);
}
