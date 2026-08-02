package com.aicompliance.application.certificate;

import com.aicompliance.application.category.CategoryService;
import com.aicompliance.application.port.AiService;
import com.aicompliance.application.port.CertificateCategoryRepository;
import com.aicompliance.application.port.CertificateRepository;
import com.aicompliance.application.port.CertificateVersionRepository;
import com.aicompliance.application.port.FileStorageService;
import com.aicompliance.application.port.OcrTextExtractor;
import com.aicompliance.domain.certificate.Certificate;
import com.aicompliance.domain.certificate.CertificateCategory;
import com.aicompliance.domain.certificate.CertificateVersion;
import com.aicompliance.domain.certificate.ProcessingStatus;
import com.aicompliance.domain.shared.EntityNotFoundException;
import com.aicompliance.domain.shared.ExpiryBucket;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the "no manual data entry" upload path: file bytes -> OCR text -> AI field
 * extraction -> populate Certificate/CertificateVersion. Runs on an {@code @Async} worker thread
 * with no authenticated request context, so it does NOT go through CompanyContextProvider-backed
 * services (which require an authenticated principal) - it works directly against repositories,
 * scoped explicitly by the companyId/certificateId passed in at trigger time, and the Hibernate
 * tenant filter is correctly inactive here since there is no SecurityContext to enable it from.
 */
@Service
public class CertificateExtractionService {

    private static final Logger log = LoggerFactory.getLogger(CertificateExtractionService.class);

    private final CertificateRepository certificateRepository;
    private final CertificateVersionRepository versionRepository;
    private final CertificateCategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;
    private final OcrTextExtractor ocrTextExtractor;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    public CertificateExtractionService(CertificateRepository certificateRepository,
            CertificateVersionRepository versionRepository, CertificateCategoryRepository categoryRepository,
            FileStorageService fileStorageService, OcrTextExtractor ocrTextExtractor, AiService aiService,
            ObjectMapper objectMapper) {
        this.certificateRepository = certificateRepository;
        this.versionRepository = versionRepository;
        this.categoryRepository = categoryRepository;
        this.fileStorageService = fileStorageService;
        this.ocrTextExtractor = ocrTextExtractor;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    @Async
    public void processAsync(UUID certificateId, UUID versionId) {
        try {
            String ocrText = runOcr(certificateId, versionId);
            runAiExtraction(certificateId, versionId, ocrText);
        } catch (Exception e) {
            log.error("Certificate extraction failed for certificate {} version {}", certificateId, versionId, e);
            markFailed(versionId, e.getMessage());
        }
    }

    /**
     * Persisted as its own transaction so OCR text survives even if the subsequent AI call
     * fails (e.g. transient API error) - the extraction can be retried from the AI step alone
     * without re-running OCR.
     */
    @Transactional
    String runOcr(UUID certificateId, UUID versionId) {
        CertificateVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new EntityNotFoundException("CertificateVersion", versionId));
        version.setProcessingStatus(ProcessingStatus.EXTRACTING);
        versionRepository.save(version);

        byte[] fileBytes = readAllBytes(version.getFileStorageKey());
        OcrTextExtractor.ExtractedText extracted = ocrTextExtractor.extractText(fileBytes, version.getMimeType());

        version.setOcrRawText(extracted.text());
        versionRepository.save(version);
        return extracted.text();
    }

    @Transactional
    void runAiExtraction(UUID certificateId, UUID versionId, String ocrText) {
        CertificateVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new EntityNotFoundException("CertificateVersion", versionId));

        List<String> existingCategoryNames = categoryRepository.findAllByCompanyId(version.getCompanyId()).stream()
                .map(CertificateCategory::getName)
                .toList();

        AiService.CertificateFieldExtraction fields = aiService.extractCertificateFields(
                ocrText, existingCategoryNames);

        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new EntityNotFoundException("Certificate", certificateId));

        applyExtractedFields(certificate, fields, version.getCompanyId());
        certificateRepository.save(certificate);

        version.setAiExtractedFieldsJson(toJson(fields));
        version.setProcessingStatus(ProcessingStatus.COMPLETED);
        versionRepository.save(version);
    }

    private void applyExtractedFields(Certificate certificate, AiService.CertificateFieldExtraction fields,
            UUID companyId) {
        if (fields.certificateName() != null) {
            certificate.setCertificateName(fields.certificateName());
        }
        certificate.setCertificateNumber(fields.certificateNumber());
        certificate.setIssuingAuthority(fields.issuingAuthority());
        certificate.setIssueDate(fields.issueDate());
        certificate.setExpiryDate(fields.expiryDate());
        certificate.setLicenseNumber(fields.licenseNumber());
        certificate.setHasQrCode(fields.hasQrCode());
        certificate.setHasDigitalSignature(fields.hasDigitalSignature());
        certificate.setExpiryBucket(ExpiryBucket.fromExpiryDate(fields.expiryDate()));
        certificate.setAiConfidenceScore(fields.confidenceScore());

        matchCategory(fields.suggestedCategoryName(), companyId)
                .ifPresent(category -> certificate.setCategoryId(category.getId()));
    }

    private Optional<CertificateCategory> matchCategory(String suggestedName, UUID companyId) {
        if (suggestedName == null || suggestedName.isBlank()) {
            return Optional.empty();
        }
        return categoryRepository.findByCompanyIdAndNameIgnoreCase(companyId, suggestedName.trim());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void markFailed(UUID versionId, String errorMessage) {
        versionRepository.findById(versionId).ifPresent(version -> {
            version.setProcessingStatus(ProcessingStatus.FAILED);
            version.setProcessingError(errorMessage);
            versionRepository.save(version);
        });
    }

    private byte[] readAllBytes(String storageKey) {
        try {
            return fileStorageService.download(storageKey).readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read file from storage: " + storageKey, e);
        }
    }

    private String toJson(AiService.CertificateFieldExtraction fields) {
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (Exception e) {
            log.warn("Failed to serialize extracted fields to JSON", e);
            return null;
        }
    }
}
