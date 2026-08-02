package com.aicompliance.domain.certificate;

import com.aicompliance.domain.shared.Expirable;
import com.aicompliance.domain.shared.ExpiryBucket;
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
@Table(name = "certificates")
public class Certificate extends TenantOwnedEntity implements Expirable {

    @Column(name = "plant_id")
    private UUID plantId;

    /**
     * Nullable: a certificate uploaded via the AI-extraction path starts uncategorized until
     * the pipeline finishes (or fails to find a confident match), per the "no manual data entry
     * required" workflow - never auto-created, only matched against the company's own categories.
     */
    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "certificate_name")
    private String certificateName;

    @Column(name = "certificate_number")
    private String certificateNumber;

    @Column(name = "issuing_authority")
    private String issuingAuthority;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "license_number")
    private String licenseNumber;

    @Column(name = "has_qr_code", nullable = false)
    private boolean hasQrCode = false;

    @Column(name = "has_digital_signature", nullable = false)
    private boolean hasDigitalSignature = false;

    @Column(name = "current_version_id")
    private UUID currentVersionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CertificateStatus status = CertificateStatus.PENDING_REVIEW;

    @Enumerated(EnumType.STRING)
    @Column(name = "expiry_bucket", nullable = false)
    private ExpiryBucket expiryBucket = ExpiryBucket.NOT_TRACKED;

    @Column(name = "uploaded_by_user_id", nullable = false)
    private UUID uploadedByUserId;

    @Column(name = "reviewed_by_user_id")
    private UUID reviewedByUserId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "ai_confidence_score")
    private Double aiConfidenceScore;

    public UUID getPlantId() {
        return plantId;
    }

    public void setPlantId(UUID plantId) {
        this.plantId = plantId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public String getCertificateName() {
        return certificateName;
    }

    public void setCertificateName(String certificateName) {
        this.certificateName = certificateName;
    }

    public String getCertificateNumber() {
        return certificateNumber;
    }

    public void setCertificateNumber(String certificateNumber) {
        this.certificateNumber = certificateNumber;
    }

    public String getIssuingAuthority() {
        return issuingAuthority;
    }

    public void setIssuingAuthority(String issuingAuthority) {
        this.issuingAuthority = issuingAuthority;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public boolean isHasQrCode() {
        return hasQrCode;
    }

    public void setHasQrCode(boolean hasQrCode) {
        this.hasQrCode = hasQrCode;
    }

    public boolean isHasDigitalSignature() {
        return hasDigitalSignature;
    }

    public void setHasDigitalSignature(boolean hasDigitalSignature) {
        this.hasDigitalSignature = hasDigitalSignature;
    }

    public UUID getCurrentVersionId() {
        return currentVersionId;
    }

    public void setCurrentVersionId(UUID currentVersionId) {
        this.currentVersionId = currentVersionId;
    }

    public CertificateStatus getStatus() {
        return status;
    }

    public void setStatus(CertificateStatus status) {
        this.status = status;
    }

    public ExpiryBucket getExpiryBucket() {
        return expiryBucket;
    }

    public void setExpiryBucket(ExpiryBucket expiryBucket) {
        this.expiryBucket = expiryBucket;
    }

    public UUID getUploadedByUserId() {
        return uploadedByUserId;
    }

    public void setUploadedByUserId(UUID uploadedByUserId) {
        this.uploadedByUserId = uploadedByUserId;
    }

    public UUID getReviewedByUserId() {
        return reviewedByUserId;
    }

    public void setReviewedByUserId(UUID reviewedByUserId) {
        this.reviewedByUserId = reviewedByUserId;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Double getAiConfidenceScore() {
        return aiConfidenceScore;
    }

    public void setAiConfidenceScore(Double aiConfidenceScore) {
        this.aiConfidenceScore = aiConfidenceScore;
    }
}
