package com.aicompliance.application.certificate;

import com.aicompliance.application.audit.AuditLogRecorder;
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
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * The "Renewal" and "Archive Old Version" steps of the certificate workflow: uploading a new
 * version supersedes the current one (archived, kept for history) rather than replacing it.
 */
@Service
public class CertificateVersionService {

    private final CertificateRepository certificateRepository;
    private final CertificateVersionRepository versionRepository;
    private final FileStorageService fileStorageService;
    private final CompanyContextProvider companyContextProvider;
    private final CertificateExtractionService extractionService;
    private final AuditLogRecorder auditLogRecorder;

    public CertificateVersionService(CertificateRepository certificateRepository,
            CertificateVersionRepository versionRepository, FileStorageService fileStorageService,
            CompanyContextProvider companyContextProvider, CertificateExtractionService extractionService,
            AuditLogRecorder auditLogRecorder) {
        this.certificateRepository = certificateRepository;
        this.versionRepository = versionRepository;
        this.fileStorageService = fileStorageService;
        this.companyContextProvider = companyContextProvider;
        this.extractionService = extractionService;
        this.auditLogRecorder = auditLogRecorder;
    }

    public record RenewCommand(
            LocalDate issueDate,
            LocalDate expiryDate,
            String certificateNumber,
            boolean autoExtract,
            InputStream fileContent,
            long fileSizeBytes,
            String originalFileName,
            String mimeType) {
    }

    @Transactional
    public Certificate renew(UUID certificateId, RenewCommand command) {
        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new EntityNotFoundException("Certificate", certificateId));
        certificate = companyContextProvider.requireOwnership(certificate, "Certificate", certificateId);

        UUID previousVersionId = certificate.getCurrentVersionId();
        CertificateVersion previousVersion = versionRepository.findById(previousVersionId)
                .orElseThrow(() -> new EntityNotFoundException("CertificateVersion", previousVersionId));
        previousVersion.setArchived(true);
        versionRepository.save(previousVersion);

        UUID companyId = certificate.getCompanyId();
        int newVersionNumber = previousVersion.getVersionNumber() + 1;
        String storageKey = "companies/%s/certificates/%s/v%d/%s".formatted(
                companyId, certificate.getId(), newVersionNumber, command.originalFileName());
        fileStorageService.upload(storageKey, command.fileContent(), command.fileSizeBytes(), command.mimeType());

        CertificateVersion newVersion = new CertificateVersion();
        newVersion.setCompanyId(companyId);
        newVersion.setCertificateId(certificate.getId());
        newVersion.setVersionNumber(newVersionNumber);
        newVersion.setFileStorageKey(storageKey);
        newVersion.setOriginalFileName(command.originalFileName());
        newVersion.setMimeType(command.mimeType());
        newVersion.setFileSizeBytes(command.fileSizeBytes());
        newVersion.setUploadedByUserId(companyContextProvider.getCurrentUserId());
        newVersion.setProcessingStatus(command.autoExtract() ? ProcessingStatus.PENDING : ProcessingStatus.NOT_APPLICABLE);
        newVersion = versionRepository.save(newVersion);

        certificate.setCurrentVersionId(newVersion.getId());
        certificate.setStatus(CertificateStatus.PENDING_REVIEW);
        certificate.setReviewedByUserId(null);
        certificate.setReviewedAt(null);
        if (!command.autoExtract()) {
            certificate.setIssueDate(command.issueDate());
            certificate.setExpiryDate(command.expiryDate());
            certificate.setCertificateNumber(command.certificateNumber());
            certificate.setExpiryBucket(ExpiryBucket.fromExpiryDate(command.expiryDate()));
        }
        certificate = certificateRepository.save(certificate);
        auditLogRecorder.record("CERTIFICATE_RENEWED", "Certificate", certificate.getId());

        if (command.autoExtract()) {
            UUID certId = certificate.getId();
            UUID versionId = newVersion.getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    extractionService.processAsync(certId, versionId);
                }
            });
        }

        return certificate;
    }

    @Transactional(readOnly = true)
    public List<CertificateVersion> listVersionHistory(UUID certificateId) {
        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new EntityNotFoundException("Certificate", certificateId));
        companyContextProvider.requireOwnership(certificate, "Certificate", certificateId);
        return versionRepository.findAllByCertificateIdOrderByVersionNumberDesc(certificateId);
    }

}
