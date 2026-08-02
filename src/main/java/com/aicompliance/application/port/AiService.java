package com.aicompliance.application.port;

import java.time.LocalDate;
import java.util.List;

public interface AiService {

    /**
     * Extracts structured certificate fields from raw OCR'd text, and suggests a category name
     * (matched by the caller against the company's own categories - never a fixed list, since
     * categories are fully custom per company/industry).
     */
    CertificateFieldExtraction extractCertificateFields(String ocrText, List<String> existingCategoryNames);

    record CertificateFieldExtraction(
            String certificateName,
            String certificateNumber,
            String issuingAuthority,
            LocalDate issueDate,
            LocalDate expiryDate,
            String licenseNumber,
            boolean hasQrCode,
            boolean hasDigitalSignature,
            String suggestedCategoryName,
            double confidenceScore) {
    }

    /**
     * Answers a natural-language compliance question grounded in {@code certificateDataJson} - a
     * structured snapshot of the company's real certificate data (not embeddings/RAG over
     * documents). The model must answer only from the provided data.
     */
    String answerComplianceQuestion(String question, String certificateDataJson);

    /** Produces a short natural-language explanation of a computed compliance score breakdown. */
    String explainComplianceScore(String scoreBreakdownJson);

    /**
     * Produces a narrative risk explanation given a pre-computed set of rule-based signals (e.g.
     * overlapping expiries, overdue inspections). The signals themselves are computed in Java,
     * not by the model - this call only reasons over them to write the explanation.
     */
    String explainRiskSignals(String riskSignalsJson);

    /**
     * Confirms or rules out ambiguous near-duplicate candidates that a rule-based pre-filter
     * (same category + fuzzy-matched certificate number) already narrowed down. Only called for
     * genuinely ambiguous cases, not as the primary detection mechanism.
     */
    List<Integer> confirmDuplicateIndices(String candidatePairsJson);

    /**
     * Predicts a realistic renewal lead time in days for a certificate category, given the
     * category name and any historical renewal timing available - distinct from the raw
     * days-until-expiry countdown.
     */
    RenewalPrediction predictRenewalLeadTime(String categoryName, Integer historicalAverageLeadTimeDays);

    record RenewalPrediction(int recommendedLeadTimeDays, String explanation) {
    }
}
