package com.aicompliance.application.certificate;

import com.aicompliance.application.category.CategoryService;
import com.aicompliance.application.port.CertificateRepository;
import com.aicompliance.application.port.CertificateVersionRepository;
import com.aicompliance.application.port.CompanyContextProvider;
import com.aicompliance.application.port.FileStorageService;
import com.aicompliance.domain.certificate.Certificate;
import com.aicompliance.domain.certificate.CertificateStatus;
import com.aicompliance.domain.certificate.CertificateVersion;
import com.aicompliance.domain.certificate.ProcessingStatus;
import com.aicompliance.domain.shared.EntityNotFoundException;
import com.aicompliance.domain.shared.ExpiryBucket;
import com.aicompliance.domain.shared.InvalidStateException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final CertificateVersionRepository versionRepository;
    private final CategoryService categoryService;
    private final FileStorageService fileStorageService;
    private final CompanyContextProvider companyContextProvider;
    private final CertificateExtractionService extractionService;

    public CertificateService(CertificateRepository certificateRepository,
            CertificateVersionRepository versionRepository, CategoryService categoryService,
            FileStorageService fileStorageService, CompanyContextProvider companyContextProvider,
            CertificateExtractionService extractionService) {
        this.certificateRepository = certificateRepository;
        this.versionRepository = versionRepository;
        this.categoryService = categoryService;
        this.fileStorageService = fileStorageService;
        this.companyContextProvider = companyContextProvider;
        this.extractionService = extractionService;
    }

    /**
     * {@code categoryId}/{@code certificateName} are required unless {@code autoExtract} is
     * true, in which case they're left null and filled in asynchronously by the AI pipeline
     * (the PRD's "no manual data entry required" path) - matched only against this company's
     * own categories, never a fixed/hardcoded list.
     */
    public record UploadCertificateCommand(
            UUID plantId,
            UUID categoryId,
            String certificateName,
            String certificateNumber,
            String issuingAuthority,
            LocalDate issueDate,
            LocalDate expiryDate,
            String licenseNumber,
            boolean hasQrCode,
            boolean hasDigitalSignature,
            boolean autoExtract,
            InputStream fileContent,
            long fileSizeBytes,
            String originalFileName,
            String mimeType) {
    }

    @Transactional
    public Certificate upload(UploadCertificateCommand command) {
        if (!command.autoExtract()) {
            if (command.categoryId() == null || command.certificateName() == null
                    || command.certificateName().isBlank()) {
                throw new InvalidStateException(
                        "categoryId and certificateName are required unless autoExtract is set");
            }
            // Validates the category belongs to this company (or 404s), independent of industry.
            categoryService.getById(command.categoryId());
        }

        UUID companyId = companyContextProvider.getCurrentCompanyId();

        Certificate certificate = new Certificate();
        certificate.setCompanyId(companyId);
        certificate.setPlantId(command.plantId());
        certificate.setCategoryId(command.categoryId());
        certificate.setCertificateName(command.certificateName());
        certificate.setCertificateNumber(command.certificateNumber());
        certificate.setIssuingAuthority(command.issuingAuthority());
        certificate.setIssueDate(command.issueDate());
        certificate.setExpiryDate(command.expiryDate());
        certificate.setLicenseNumber(command.licenseNumber());
        certificate.setHasQrCode(command.hasQrCode());
        certificate.setHasDigitalSignature(command.hasDigitalSignature());
        certificate.setStatus(CertificateStatus.PENDING_REVIEW);
        certificate.setExpiryBucket(ExpiryBucket.fromExpiryDate(command.expiryDate()));
        certificate.setUploadedByUserId(companyContextProvider.getCurrentUserId());
        certificate = certificateRepository.save(certificate);

        String storageKey = "companies/%s/certificates/%s/v1/%s".formatted(
                companyId, certificate.getId(), command.originalFileName());
        fileStorageService.upload(storageKey, command.fileContent(), command.fileSizeBytes(), command.mimeType());

        CertificateVersion version = new CertificateVersion();
        version.setCompanyId(companyId);
        version.setCertificateId(certificate.getId());
        version.setVersionNumber(1);
        version.setFileStorageKey(storageKey);
        version.setOriginalFileName(command.originalFileName());
        version.setMimeType(command.mimeType());
        version.setFileSizeBytes(command.fileSizeBytes());
        version.setUploadedByUserId(companyContextProvider.getCurrentUserId());
        version.setProcessingStatus(command.autoExtract() ? ProcessingStatus.PENDING : ProcessingStatus.NOT_APPLICABLE);
        version = versionRepository.save(version);

        certificate.setCurrentVersionId(version.getId());
        certificate = certificateRepository.save(certificate);

        if (command.autoExtract()) {
            // Must not fire until the surrounding transaction commits - otherwise the async
            // worker thread races the commit and can't see the just-inserted rows yet.
            UUID certificateId = certificate.getId();
            UUID versionId = version.getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    extractionService.processAsync(certificateId, versionId);
                }
            });
        }

        return certificate;
    }

    @Transactional(readOnly = true)
    public List<Certificate> listForCurrentCompany() {
        return certificateRepository.findAllByCompanyId(companyContextProvider.getCurrentCompanyId());
    }

    @Transactional(readOnly = true)
    public List<Certificate> listByCategory(UUID categoryId) {
        return certificateRepository.findAllByCompanyIdAndCategoryId(
                companyContextProvider.getCurrentCompanyId(), categoryId);
    }

    @Transactional(readOnly = true)
    public List<Certificate> listByPlant(UUID plantId) {
        return certificateRepository.findAllByCompanyIdAndPlantId(
                companyContextProvider.getCurrentCompanyId(), plantId);
    }

    @Transactional(readOnly = true)
    public Certificate getById(UUID id) {
        Certificate certificate = certificateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Certificate", id));
        return companyContextProvider.requireOwnership(certificate, "Certificate", id);
    }

    @Transactional(readOnly = true)
    public URL generateDownloadUrl(UUID certificateId) {
        Certificate certificate = getById(certificateId);
        CertificateVersion version = getCurrentVersion(certificate);
        return fileStorageService.generatePresignedDownloadUrl(version.getFileStorageKey(), Duration.ofMinutes(15));
    }

    @Transactional(readOnly = true)
    public CertificateVersion getExtractionStatus(UUID certificateId) {
        Certificate certificate = getById(certificateId);
        return getCurrentVersion(certificate);
    }

    private CertificateVersion getCurrentVersion(Certificate certificate) {
        return versionRepository.findById(certificate.getCurrentVersionId())
                .orElseThrow(() -> new EntityNotFoundException("CertificateVersion", certificate.getCurrentVersionId()));
    }

}
