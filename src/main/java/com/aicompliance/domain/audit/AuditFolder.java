package com.aicompliance.domain.audit;

import com.aicompliance.domain.shared.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "audit_folders")
public class AuditFolder extends TenantOwnedEntity {

    @Column(name = "generated_by_user_id", nullable = false)
    private UUID generatedByUserId;

    @Column(name = "generated_at")
    private Instant generatedAt;

    @Column(name = "date_range_start")
    private LocalDate dateRangeStart;

    @Column(name = "date_range_end")
    private LocalDate dateRangeEnd;

    @Column(name = "file_storage_key")
    private String fileStorageKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditFolderStatus status = AuditFolderStatus.GENERATING;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public UUID getGeneratedByUserId() {
        return generatedByUserId;
    }

    public void setGeneratedByUserId(UUID generatedByUserId) {
        this.generatedByUserId = generatedByUserId;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public LocalDate getDateRangeStart() {
        return dateRangeStart;
    }

    public void setDateRangeStart(LocalDate dateRangeStart) {
        this.dateRangeStart = dateRangeStart;
    }

    public LocalDate getDateRangeEnd() {
        return dateRangeEnd;
    }

    public void setDateRangeEnd(LocalDate dateRangeEnd) {
        this.dateRangeEnd = dateRangeEnd;
    }

    public String getFileStorageKey() {
        return fileStorageKey;
    }

    public void setFileStorageKey(String fileStorageKey) {
        this.fileStorageKey = fileStorageKey;
    }

    public AuditFolderStatus getStatus() {
        return status;
    }

    public void setStatus(AuditFolderStatus status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
