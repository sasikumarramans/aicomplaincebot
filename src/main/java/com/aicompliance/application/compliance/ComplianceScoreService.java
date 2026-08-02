package com.aicompliance.application.compliance;

import com.aicompliance.application.port.AiService;
import com.aicompliance.application.port.CertificateCategoryRepository;
import com.aicompliance.application.port.CertificateRepository;
import com.aicompliance.application.port.CompanyContextProvider;
import com.aicompliance.application.port.ComplianceScoreRepository;
import com.aicompliance.domain.certificate.Certificate;
import com.aicompliance.domain.certificate.CertificateCategory;
import com.aicompliance.domain.certificate.CertificateStatus;
import com.aicompliance.domain.compliance.ComplianceScore;
import com.aicompliance.domain.shared.ExpiryBucket;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Weighted compliance score per category group + overall. A certificate counts as "valid" if
 * APPROVED and not EXPIRED. This is still a valid/total ratio, not a valid/required ratio -
 * MissingDocumentDetectionService (Phase 9) tracks missing-but-required documents separately
 * rather than folding them into this score; revisit if they should count as failures here too.
 */
@Service
public class ComplianceScoreService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceScoreService.class);

    private final CertificateRepository certificateRepository;
    private final CertificateCategoryRepository categoryRepository;
    private final ComplianceScoreRepository scoreRepository;
    private final AiService aiService;
    private final CompanyContextProvider companyContextProvider;
    private final ObjectMapper objectMapper;

    public ComplianceScoreService(CertificateRepository certificateRepository,
            CertificateCategoryRepository categoryRepository, ComplianceScoreRepository scoreRepository,
            AiService aiService, CompanyContextProvider companyContextProvider, ObjectMapper objectMapper) {
        this.certificateRepository = certificateRepository;
        this.categoryRepository = categoryRepository;
        this.scoreRepository = scoreRepository;
        this.aiService = aiService;
        this.companyContextProvider = companyContextProvider;
        this.objectMapper = objectMapper;
    }

    public record CategoryScore(String categoryGroup, double scorePercent, int validCount, int totalCount) {
    }

    public record ScoreBreakdown(double overallPercent, List<CategoryScore> byCategoryGroup) {
    }

    @Transactional(readOnly = true)
    public ScoreBreakdown computeScore() {
        UUID companyId = companyContextProvider.getCurrentCompanyId();
        List<Certificate> certificates = certificateRepository.findAllByCompanyId(companyId);
        Map<UUID, CertificateCategory> categoriesById = categoryRepository.findAllByCompanyId(companyId).stream()
                .collect(Collectors.toMap(CertificateCategory::getId, c -> c));

        if (certificates.isEmpty()) {
            return new ScoreBreakdown(100.0, List.of());
        }

        Map<String, List<Certificate>> byGroup = certificates.stream()
                .collect(Collectors.groupingBy(c -> groupLabel(c, categoriesById), LinkedHashMap::new,
                        Collectors.toList()));

        List<CategoryScore> categoryScores = byGroup.entrySet().stream()
                .map(e -> toCategoryScore(e.getKey(), e.getValue()))
                .toList();

        double overall = categoryScores.stream()
                .mapToDouble(CategoryScore::scorePercent)
                .average()
                .orElse(100.0);

        return new ScoreBreakdown(round(overall), categoryScores);
    }

    @Transactional
    public String explainScore(ScoreBreakdown breakdown) {
        try {
            return aiService.explainComplianceScore(toJson(breakdown));
        } catch (Exception e) {
            log.error("AI compliance score explanation failed", e);
            return "AI-generated explanation unavailable.";
        }
    }

    @Transactional
    public void persistSnapshot(ScoreBreakdown breakdown) {
        UUID companyId = companyContextProvider.getCurrentCompanyId();

        ComplianceScore overall = new ComplianceScore();
        overall.setCompanyId(companyId);
        overall.setCategoryGroup(null);
        overall.setScoreValue(BigDecimal.valueOf(breakdown.overallPercent()));
        scoreRepository.save(overall);

        for (CategoryScore categoryScore : breakdown.byCategoryGroup()) {
            ComplianceScore score = new ComplianceScore();
            score.setCompanyId(companyId);
            score.setCategoryGroup(categoryScore.categoryGroup());
            score.setScoreValue(BigDecimal.valueOf(categoryScore.scorePercent()));
            scoreRepository.save(score);
        }
    }

    private CategoryScore toCategoryScore(String groupLabel, List<Certificate> certs) {
        long valid = certs.stream().filter(this::isValid).count();
        double percent = round((valid * 100.0) / certs.size());
        return new CategoryScore(groupLabel, percent, (int) valid, certs.size());
    }

    private boolean isValid(Certificate certificate) {
        return certificate.getStatus() == CertificateStatus.APPROVED
                && certificate.getExpiryBucket() != ExpiryBucket.EXPIRED;
    }

    private String groupLabel(Certificate certificate, Map<UUID, CertificateCategory> categoriesById) {
        if (certificate.getCategoryId() == null) {
            return "Uncategorized";
        }
        CertificateCategory category = categoriesById.get(certificate.getCategoryId());
        if (category == null || category.getGroupLabel() == null || category.getGroupLabel().isBlank()) {
            return "Other";
        }
        return category.getGroupLabel();
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }
}
