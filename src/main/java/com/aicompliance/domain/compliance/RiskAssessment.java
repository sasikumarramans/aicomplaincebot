package com.aicompliance.domain.compliance;

import com.aicompliance.domain.shared.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "risk_assessments")
public class RiskAssessment extends TenantOwnedEntity {

    @Column(name = "plant_id")
    private UUID plantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private RiskLevel riskLevel;

    @Column(name = "risk_score", nullable = false)
    private BigDecimal riskScore;

    @Column(name = "reasoning_text", columnDefinition = "text")
    private String reasoningText;

    @Column(name = "contributing_signals_json", columnDefinition = "text")
    private String contributingSignalsJson;

    @Column(name = "assessed_at", nullable = false)
    private Instant assessedAt = Instant.now();

    public UUID getPlantId() {
        return plantId;
    }

    public void setPlantId(UUID plantId) {
        this.plantId = plantId;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(BigDecimal riskScore) {
        this.riskScore = riskScore;
    }

    public String getReasoningText() {
        return reasoningText;
    }

    public void setReasoningText(String reasoningText) {
        this.reasoningText = reasoningText;
    }

    public String getContributingSignalsJson() {
        return contributingSignalsJson;
    }

    public void setContributingSignalsJson(String contributingSignalsJson) {
        this.contributingSignalsJson = contributingSignalsJson;
    }

    public Instant getAssessedAt() {
        return assessedAt;
    }

    public void setAssessedAt(Instant assessedAt) {
        this.assessedAt = assessedAt;
    }
}
