package com.aicompliance.application.chat;

import com.aicompliance.application.port.CertificateCategoryRepository;
import com.aicompliance.application.port.CertificateRepository;
import com.aicompliance.application.port.CompanyContextProvider;
import com.aicompliance.domain.certificate.Certificate;
import com.aicompliance.domain.certificate.CertificateCategory;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Builds the structured (not embeddings/RAG) grounding context handed to the AI chat assistant:
 * a compact snapshot of the current company's real certificate data, so answers are always
 * derived from actual DB state rather than free-text document search or model recall.
 */
@Component
public class ComplianceContextRetriever {

    private final CertificateRepository certificateRepository;
    private final CertificateCategoryRepository categoryRepository;
    private final CompanyContextProvider companyContextProvider;

    public ComplianceContextRetriever(CertificateRepository certificateRepository,
            CertificateCategoryRepository categoryRepository, CompanyContextProvider companyContextProvider) {
        this.certificateRepository = certificateRepository;
        this.categoryRepository = categoryRepository;
        this.companyContextProvider = companyContextProvider;
    }

    public record CertificateSummary(
            String certificateName,
            String categoryName,
            String certificateNumber,
            String issuingAuthority,
            String issueDate,
            String expiryDate,
            String status,
            String expiryBucket) {
    }

    public List<CertificateSummary> retrieveCertificateSummaries() {
        UUID companyId = companyContextProvider.getCurrentCompanyId();
        Map<UUID, CertificateCategory> categoriesById = categoryRepository.findAllByCompanyId(companyId).stream()
                .collect(Collectors.toMap(CertificateCategory::getId, c -> c));

        return certificateRepository.findAllByCompanyId(companyId).stream()
                .map(cert -> toSummary(cert, categoriesById))
                .toList();
    }

    private CertificateSummary toSummary(Certificate cert, Map<UUID, CertificateCategory> categoriesById) {
        CertificateCategory category = cert.getCategoryId() != null ? categoriesById.get(cert.getCategoryId()) : null;
        return new CertificateSummary(
                cert.getCertificateName(),
                category != null ? category.getName() : null,
                cert.getCertificateNumber(),
                cert.getIssuingAuthority(),
                cert.getIssueDate() != null ? cert.getIssueDate().toString() : null,
                cert.getExpiryDate() != null ? cert.getExpiryDate().toString() : null,
                cert.getStatus().name(),
                cert.getExpiryBucket().name());
    }
}
