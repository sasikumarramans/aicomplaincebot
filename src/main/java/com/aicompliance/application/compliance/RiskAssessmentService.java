package com.aicompliance.application.compliance;

import com.aicompliance.application.port.AiService;
import com.aicompliance.application.port.CertificateRepository;
import com.aicompliance.application.port.CompanyContextProvider;
import com.aicompliance.application.port.RiskAssessmentRepository;
import com.aicompliance.domain.certificate.Certificate;
import com.aicompliance.domain.compliance.RiskAssessment;
import com.aicompliance.domain.compliance.RiskLevel;
import com.aicompliance.domain.shared.ExpiryBucket;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rule-based signal collection (overlapping expiries, expired/overdue certificates) with an AI
 * narrative layer on top - the risk LEVEL and SCORE are always computed deterministically in
 * Java; AiService only writes the human-readable explanation, never decides the score itself.
 */
@Service
public class RiskAssessmentService {

    private static final Logger log = LoggerFactory.getLogger(RiskAssessmentService.class);
    private static final int OVERLAP_WINDOW_DAYS = 14;
    private static final int OVERLAP_THRESHOLD_COUNT = 2;

    private final CertificateRepository certificateRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final AiService aiService;
    private final CompanyContextProvider companyContextProvider;
    private final ObjectMapper objectMapper;

    public RiskAssessmentService(CertificateRepository certificateRepository,
            RiskAssessmentRepository riskAssessmentRepository, AiService aiService,
            CompanyContextProvider companyContextProvider, ObjectMapper objectMapper) {
        this.certificateRepository = certificateRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.aiService = aiService;
        this.companyContextProvider = companyContextProvider;
        this.objectMapper = objectMapper;
    }

    public record Signal(String type, String description) {
    }

    @Transactional
    public RiskAssessment assess() {
        return assessForCompany(companyContextProvider.getCurrentCompanyId());
    }

    /**
     * Used both by the authenticated {@link #assess()} entry point and the unauthenticated
     * scheduled job (which iterates every company explicitly, since there is no per-request
     * tenant context to read from there).
     */
    @Transactional
    public RiskAssessment assessForCompany(UUID companyId) {
        List<Certificate> certificates = certificateRepository.findAllByCompanyId(companyId);

        List<Signal> signals = new ArrayList<>();
        signals.addAll(detectExpiredSignals(certificates));
        signals.addAll(detectOverlappingExpirySignals(certificates));

        int score = computeScore(signals, certificates.size());
        RiskLevel level = levelFor(score);

        String signalsJson = toJson(signals);
        String reasoning;
        if (signals.isEmpty()) {
            reasoning = "No significant risk signals detected.";
        } else {
            try {
                reasoning = aiService.explainRiskSignals(signalsJson);
            } catch (Exception e) {
                log.error("AI risk explanation failed for company {}", companyId, e);
                reasoning = "AI-generated explanation unavailable. Raw signals: " + signalsJson;
            }
        }

        RiskAssessment assessment = new RiskAssessment();
        assessment.setCompanyId(companyId);
        assessment.setRiskLevel(level);
        assessment.setRiskScore(BigDecimal.valueOf(score));
        assessment.setReasoningText(reasoning);
        assessment.setContributingSignalsJson(signalsJson);
        return riskAssessmentRepository.save(assessment);
    }

    @Transactional(readOnly = true)
    public List<RiskAssessment> history() {
        return riskAssessmentRepository.findAllByCompanyIdOrderByAssessedAtDesc(
                companyContextProvider.getCurrentCompanyId());
    }

    private List<Signal> detectExpiredSignals(List<Certificate> certificates) {
        return certificates.stream()
                .filter(c -> c.getExpiryBucket() == ExpiryBucket.EXPIRED)
                .map(c -> new Signal("EXPIRED_CERTIFICATE",
                        (c.getCertificateName() != null ? c.getCertificateName() : "A certificate")
                                + " expired on " + c.getExpiryDate()))
                .toList();
    }

    /**
     * Flags clusters of certificates whose expiry dates fall within OVERLAP_WINDOW_DAYS of each
     * other - a true sliding-window scan over dates sorted ascending, not fixed calendar buckets
     * (which would miss e.g. two dates one day apart that happen to straddle a bucket boundary).
     */
    private List<Signal> detectOverlappingExpirySignals(List<Certificate> certificates) {
        List<Certificate> sorted = certificates.stream()
                .filter(c -> c.getExpiryDate() != null)
                .filter(c -> c.getExpiryBucket() != ExpiryBucket.EXPIRED)
                .sorted(Comparator.comparing(Certificate::getExpiryDate))
                .toList();

        List<Signal> signals = new ArrayList<>();
        List<Certificate> cluster = new ArrayList<>();
        for (Certificate certificate : sorted) {
            if (cluster.isEmpty()
                    || ChronoUnit.DAYS.between(
                            cluster.get(cluster.size() - 1).getExpiryDate(), certificate.getExpiryDate())
                            <= OVERLAP_WINDOW_DAYS) {
                cluster.add(certificate);
            } else {
                signals.addAll(clusterSignal(cluster));
                cluster = new ArrayList<>(List.of(certificate));
            }
        }
        signals.addAll(clusterSignal(cluster));
        return signals;
    }

    private List<Signal> clusterSignal(List<Certificate> cluster) {
        if (cluster.size() < OVERLAP_THRESHOLD_COUNT) {
            return List.of();
        }
        String names = cluster.stream()
                .map(c -> c.getCertificateName() != null ? c.getCertificateName() : "Unnamed certificate")
                .collect(Collectors.joining(", "));
        return List.of(new Signal("OVERLAPPING_EXPIRY",
                cluster.size() + " certificates expire within " + OVERLAP_WINDOW_DAYS + " days of each other: "
                        + names));
    }

    private int computeScore(List<Signal> signals, int totalCertificates) {
        if (totalCertificates == 0) {
            return 0;
        }
        long expiredCount = signals.stream().filter(s -> s.type().equals("EXPIRED_CERTIFICATE")).count();
        long overlapCount = signals.stream().filter(s -> s.type().equals("OVERLAPPING_EXPIRY")).count();
        int score = (int) Math.min(100, expiredCount * 25 + overlapCount * 15);
        return score;
    }

    private RiskLevel levelFor(int score) {
        if (score >= 50) {
            return RiskLevel.HIGH;
        }
        if (score >= 20) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }
}
