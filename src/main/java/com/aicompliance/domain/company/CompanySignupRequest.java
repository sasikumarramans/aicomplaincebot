package com.aicompliance.domain.company;

import com.aicompliance.domain.shared.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "company_signup_requests")
public class CompanySignupRequest extends AuditableEntity {

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "industry_id")
    private UUID industryId;

    @Column(name = "registered_address")
    private String registeredAddress;

    @Column(name = "gst_number")
    private String gstNumber;

    @Column(name = "admin_full_name", nullable = false)
    private String adminFullName;

    @Column(name = "admin_email", nullable = false)
    private String adminEmail;

    @Column(name = "phone_number")
    private String phoneNumber;

    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SignupRequestStatus status = SignupRequestStatus.PENDING;

    @Column(name = "reviewed_by_user_id")
    private UUID reviewedByUserId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "created_company_id")
    private UUID createdCompanyId;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public UUID getIndustryId() {
        return industryId;
    }

    public void setIndustryId(UUID industryId) {
        this.industryId = industryId;
    }

    public String getRegisteredAddress() {
        return registeredAddress;
    }

    public void setRegisteredAddress(String registeredAddress) {
        this.registeredAddress = registeredAddress;
    }

    public String getGstNumber() {
        return gstNumber;
    }

    public void setGstNumber(String gstNumber) {
        this.gstNumber = gstNumber;
    }

    public String getAdminFullName() {
        return adminFullName;
    }

    public void setAdminFullName(String adminFullName) {
        this.adminFullName = adminFullName;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public SignupRequestStatus getStatus() {
        return status;
    }

    public void setStatus(SignupRequestStatus status) {
        this.status = status;
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

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public UUID getCreatedCompanyId() {
        return createdCompanyId;
    }

    public void setCreatedCompanyId(UUID createdCompanyId) {
        this.createdCompanyId = createdCompanyId;
    }
}
