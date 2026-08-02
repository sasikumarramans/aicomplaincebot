package com.aicompliance.domain.compliance;

import com.aicompliance.domain.shared.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "compliance_scores")
public class ComplianceScore extends TenantOwnedEntity {

    /** Null = overall/company-wide score; non-null = one category group's score. */
    @Column(name = "category_group")
    private String categoryGroup;

    @Column(name = "score_value", nullable = false)
    private BigDecimal scoreValue;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt = Instant.now();

    @Column(name = "contributing_factors_json", columnDefinition = "text")
    private String contributingFactorsJson;

    public String getCategoryGroup() {
        return categoryGroup;
    }

    public void setCategoryGroup(String categoryGroup) {
        this.categoryGroup = categoryGroup;
    }

    public BigDecimal getScoreValue() {
        return scoreValue;
    }

    public void setScoreValue(BigDecimal scoreValue) {
        this.scoreValue = scoreValue;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public String getContributingFactorsJson() {
        return contributingFactorsJson;
    }

    public void setContributingFactorsJson(String contributingFactorsJson) {
        this.contributingFactorsJson = contributingFactorsJson;
    }
}
