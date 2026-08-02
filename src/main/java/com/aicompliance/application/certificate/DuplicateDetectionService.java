package com.aicompliance.application.certificate;

import com.aicompliance.application.port.AiService;
import com.aicompliance.application.port.CertificateRepository;
import com.aicompliance.application.port.CompanyContextProvider;
import com.aicompliance.domain.certificate.Certificate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Duplicate detection is primarily rule-based/relational (same category + exact or near-exact
 * certificate number), not AI-driven - AI is only consulted for genuinely ambiguous near-matches,
 * to control token spend. Exact matches (same category, same normalized certificate number) are
 * flagged without any AI call at all.
 */
@Service
public class DuplicateDetectionService {

    private final CertificateRepository certificateRepository;
    private final AiService aiService;
    private final CompanyContextProvider companyContextProvider;
    private final ObjectMapper objectMapper;

    public DuplicateDetectionService(CertificateRepository certificateRepository, AiService aiService,
            CompanyContextProvider companyContextProvider, ObjectMapper objectMapper) {
        this.certificateRepository = certificateRepository;
        this.aiService = aiService;
        this.companyContextProvider = companyContextProvider;
        this.objectMapper = objectMapper;
    }

    public record DuplicatePair(UUID certificateAId, String certificateAName, UUID certificateBId,
            String certificateBName, String matchType) {
    }

    @Transactional(readOnly = true)
    public List<DuplicatePair> findDuplicates() {
        UUID companyId = companyContextProvider.getCurrentCompanyId();
        List<Certificate> certificates = certificateRepository.findAllByCompanyId(companyId).stream()
                .filter(c -> c.getCertificateNumber() != null && !c.getCertificateNumber().isBlank())
                .filter(c -> c.getCategoryId() != null)
                .toList();

        Map<UUID, List<Certificate>> byCategory = certificates.stream()
                .collect(Collectors.groupingBy(Certificate::getCategoryId));

        List<DuplicatePair> exactMatches = new ArrayList<>();
        List<CandidatePair> ambiguousCandidates = new ArrayList<>();

        for (List<Certificate> group : byCategory.values()) {
            for (int i = 0; i < group.size(); i++) {
                for (int j = i + 1; j < group.size(); j++) {
                    Certificate a = group.get(i);
                    Certificate b = group.get(j);
                    String normA = normalize(a.getCertificateNumber());
                    String normB = normalize(b.getCertificateNumber());

                    if (normA.equals(normB)) {
                        exactMatches.add(new DuplicatePair(a.getId(), a.getCertificateName(), b.getId(),
                                b.getCertificateName(), "EXACT"));
                    } else if (isNearMatch(normA, normB)) {
                        ambiguousCandidates.add(new CandidatePair(a, b));
                    }
                }
            }
        }

        List<DuplicatePair> confirmed = new ArrayList<>(exactMatches);
        confirmed.addAll(confirmAmbiguousCandidates(ambiguousCandidates));
        return confirmed;
    }

    private record CandidatePair(Certificate a, Certificate b) {
    }

    private List<DuplicatePair> confirmAmbiguousCandidates(List<CandidatePair> candidates) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<Map<String, String>> payload = candidates.stream()
                .map(c -> Map.of(
                        "certA", c.a().getCertificateNumber(),
                        "certB", c.b().getCertificateNumber()))
                .toList();

        List<Integer> confirmedIndices = aiService.confirmDuplicateIndices(toJson(payload));

        List<DuplicatePair> result = new ArrayList<>();
        for (Integer index : confirmedIndices) {
            if (index >= 0 && index < candidates.size()) {
                CandidatePair pair = candidates.get(index);
                result.add(new DuplicatePair(pair.a().getId(), pair.a().getCertificateName(), pair.b().getId(),
                        pair.b().getCertificateName(), "AI_CONFIRMED_NEAR_MATCH"));
            }
        }
        return result;
    }

    private String normalize(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    /** Near-match: normalized strings share the same length and differ in at most 2 characters. */
    private boolean isNearMatch(String a, String b) {
        if (a.isEmpty() || b.isEmpty() || Math.abs(a.length() - b.length()) > 2) {
            return false;
        }
        int maxLen = Math.max(a.length(), b.length());
        int minLen = Math.min(a.length(), b.length());
        if (maxLen - minLen > 2) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < minLen; i++) {
            if (a.charAt(i) != b.charAt(i)) {
                diff++;
            }
        }
        diff += (maxLen - minLen);
        return diff > 0 && diff <= 2;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }
}
