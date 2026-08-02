package com.aicompliance.application.certificate;

import com.aicompliance.application.category.CategoryService;
import com.aicompliance.application.port.AiService;
import com.aicompliance.application.port.CertificateRepository;
import com.aicompliance.application.port.CompanyContextProvider;
import com.aicompliance.domain.certificate.Certificate;
import com.aicompliance.domain.certificate.CertificateCategory;
import com.aicompliance.domain.shared.EntityNotFoundException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.OptionalDouble;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Predicts a realistic renewal lead time for a certificate's category - distinct from the raw
 * days-until-expiry countdown, since government/regulatory renewals often need to start earlier
 * than the expiry date alone suggests.
 */
@Service
public class RenewalPredictionService {

    private static final Logger log = LoggerFactory.getLogger(RenewalPredictionService.class);

    /** Fallback lead time used only if the AI call itself fails (not a compliance judgment). */
    private static final int FALLBACK_LEAD_TIME_DAYS = 30;

    private final CertificateRepository certificateRepository;
    private final CategoryService categoryService;
    private final AiService aiService;
    private final CompanyContextProvider companyContextProvider;

    public RenewalPredictionService(CertificateRepository certificateRepository, CategoryService categoryService,
            AiService aiService, CompanyContextProvider companyContextProvider) {
        this.certificateRepository = certificateRepository;
        this.categoryService = categoryService;
        this.aiService = aiService;
        this.companyContextProvider = companyContextProvider;
    }

    public record Prediction(int recommendedLeadTimeDays, String explanation, LocalDate recommendedStartDate) {
    }

    @Transactional(readOnly = true)
    public Prediction predictForCertificate(UUID certificateId) {
        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new EntityNotFoundException("Certificate", certificateId));
        certificate = companyContextProvider.requireOwnership(certificate, "Certificate", certificateId);

        CertificateCategory category = certificate.getCategoryId() != null
                ? categoryService.getById(certificate.getCategoryId())
                : null;
        String categoryName = category != null ? category.getName() : "Uncategorized";

        Integer historicalAverage = computeHistoricalAverageLeadTime(certificate.getCategoryId());

        AiService.RenewalPrediction aiPrediction;
        try {
            aiPrediction = aiService.predictRenewalLeadTime(categoryName, historicalAverage);
        } catch (Exception e) {
            log.error("AI renewal prediction failed for certificate {}", certificateId, e);
            int fallback = historicalAverage != null ? historicalAverage : FALLBACK_LEAD_TIME_DAYS;
            aiPrediction = new AiService.RenewalPrediction(fallback,
                    "AI prediction unavailable; using a generic estimate.");
        }

        LocalDate recommendedStart = certificate.getExpiryDate() != null
                ? certificate.getExpiryDate().minusDays(aiPrediction.recommendedLeadTimeDays())
                : null;

        return new Prediction(aiPrediction.recommendedLeadTimeDays(), aiPrediction.explanation(), recommendedStart);
    }

    /**
     * Average number of days between issueDate and expiryDate across the company's past
     * certificates in the same category - a rough proxy for "how long a renewal cycle usually
     * takes", used only as extra context for the AI prediction, not as the prediction itself.
     */
    private Integer computeHistoricalAverageLeadTime(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }
        List<Certificate> categoryCertificates = certificateRepository.findAllByCompanyIdAndCategoryId(
                companyContextProvider.getCurrentCompanyId(), categoryId);

        OptionalDouble average = categoryCertificates.stream()
                .filter(c -> c.getIssueDate() != null && c.getExpiryDate() != null)
                .mapToLong(c -> ChronoUnit.DAYS.between(c.getIssueDate(), c.getExpiryDate()))
                .average();

        return average.isPresent() ? (int) Math.round(average.getAsDouble()) : null;
    }
}
