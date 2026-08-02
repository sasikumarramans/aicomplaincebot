package com.aicompliance.application.report;

import com.aicompliance.application.audit.AuditLogRecorder;
import com.aicompliance.application.dashboard.DashboardAggregationService;
import com.aicompliance.application.port.AiService;
import com.aicompliance.application.port.ArchiveExportService;
import com.aicompliance.application.port.AuditFolderRepository;
import com.aicompliance.application.port.CertificateRepository;
import com.aicompliance.application.port.CertificateVersionRepository;
import com.aicompliance.application.port.CompanyContextProvider;
import com.aicompliance.application.port.FileStorageService;
import com.aicompliance.domain.audit.AuditFolder;
import com.aicompliance.domain.audit.AuditFolderStatus;
import com.aicompliance.domain.certificate.Certificate;
import com.aicompliance.domain.certificate.CertificateVersion;
import com.aicompliance.domain.shared.EntityNotFoundException;
import com.aicompliance.domain.shared.InvalidStateException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * One-click "Audit Mode" export: bundles every certificate's current file, all 7 generated
 * reports, and an AI summary into a single downloadable zip. Runs async (same reasoning as
 * CertificateExtractionService - potentially many files to fetch/render) and reuses
 * ReportGenerationService/FileStorageService rather than duplicating any of that logic.
 */
@Service
public class AuditFolderExportService {

    private static final Logger log = LoggerFactory.getLogger(AuditFolderExportService.class);
    private static final Duration DOWNLOAD_URL_EXPIRY = Duration.ofDays(7);

    private final AuditFolderRepository auditFolderRepository;
    private final CertificateRepository certificateRepository;
    private final CertificateVersionRepository versionRepository;
    private final FileStorageService fileStorageService;
    private final ArchiveExportService archiveExportService;
    private final ReportGenerationService reportGenerationService;
    private final DashboardAggregationService dashboardAggregationService;
    private final AiService aiService;
    private final CompanyContextProvider companyContextProvider;
    private final AuditLogRecorder auditLogRecorder;
    private final ObjectMapper objectMapper;

    public AuditFolderExportService(AuditFolderRepository auditFolderRepository,
            CertificateRepository certificateRepository, CertificateVersionRepository versionRepository,
            FileStorageService fileStorageService, ArchiveExportService archiveExportService,
            ReportGenerationService reportGenerationService, DashboardAggregationService dashboardAggregationService,
            AiService aiService, CompanyContextProvider companyContextProvider, AuditLogRecorder auditLogRecorder,
            ObjectMapper objectMapper) {
        this.auditFolderRepository = auditFolderRepository;
        this.certificateRepository = certificateRepository;
        this.versionRepository = versionRepository;
        this.fileStorageService = fileStorageService;
        this.archiveExportService = archiveExportService;
        this.reportGenerationService = reportGenerationService;
        this.dashboardAggregationService = dashboardAggregationService;
        this.aiService = aiService;
        this.companyContextProvider = companyContextProvider;
        this.auditLogRecorder = auditLogRecorder;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AuditFolder requestGeneration() {
        UUID companyId = companyContextProvider.getCurrentCompanyId();

        AuditFolder folder = new AuditFolder();
        folder.setCompanyId(companyId);
        folder.setGeneratedByUserId(companyContextProvider.getCurrentUserId());
        folder.setStatus(AuditFolderStatus.GENERATING);
        folder = auditFolderRepository.save(folder);

        UUID folderId = folder.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                generateAsync(folderId, companyId);
            }
        });

        auditLogRecorder.record("AUDIT_FOLDER_REQUESTED", "AuditFolder", folder.getId());
        return folder;
    }

    @Async
    public void generateAsync(UUID folderId, UUID companyId) {
        try {
            byte[] zip = buildZip(companyId);
            String storageKey = "companies/%s/audit-folders/%s.zip".formatted(companyId, folderId);
            fileStorageService.upload(storageKey, new ByteArrayInputStream(zip), zip.length,
                    "application/zip");

            markReady(folderId, storageKey);
        } catch (Exception e) {
            log.error("Audit folder generation failed for folder {}", folderId, e);
            markFailed(folderId, e.getMessage());
        }
    }

    private byte[] buildZip(UUID companyId) {
        List<ArchiveExportService.ArchiveEntry> entries = new ArrayList<>();

        for (Certificate certificate : certificateRepository.findAllByCompanyId(companyId)) {
            if (certificate.getCurrentVersionId() == null) {
                continue;
            }
            versionRepository.findById(certificate.getCurrentVersionId()).ifPresent(version -> {
                byte[] content = readFile(version);
                String safeName = (certificate.getCertificateName() != null
                        ? certificate.getCertificateName() : certificate.getId().toString())
                        .replaceAll("[^a-zA-Z0-9._-]", "_");
                entries.add(new ArchiveExportService.ArchiveEntry(
                        "certificates/" + safeName + "-" + version.getOriginalFileName(), content));
            });
        }

        entries.add(new ArchiveExportService.ArchiveEntry("reports/expiry-report.pdf",
                reportGenerationService.generateExpiryReport()));
        entries.add(new ArchiveExportService.ArchiveEntry("reports/department-compliance-report.pdf",
                reportGenerationService.generateDepartmentComplianceReport()));
        entries.add(new ArchiveExportService.ArchiveEntry("reports/monthly-report.pdf",
                reportGenerationService.generateMonthlyReport()));
        entries.add(new ArchiveExportService.ArchiveEntry("reports/vendor-report.pdf",
                reportGenerationService.generateVendorReport()));
        entries.add(new ArchiveExportService.ArchiveEntry("reports/employee-report.pdf",
                reportGenerationService.generateEmployeeReport()));
        entries.add(new ArchiveExportService.ArchiveEntry("reports/compliance-score-report.pdf",
                reportGenerationService.generateComplianceScoreReport(false)));

        String aiSummary = buildAiSummary();
        entries.add(new ArchiveExportService.ArchiveEntry("reports/audit-report.pdf",
                reportGenerationService.generateAuditReport(aiSummary)));
        entries.add(new ArchiveExportService.ArchiveEntry("ai-summary.txt", aiSummary.getBytes()));

        return archiveExportService.buildZip(entries);
    }

    private String buildAiSummary() {
        DashboardAggregationService.Summary summary = dashboardAggregationService.getSummary();
        try {
            return aiService.explainComplianceScore(objectMapper.writeValueAsString(summary));
        } catch (Exception e) {
            log.warn("AI summary generation failed for audit folder; falling back to raw summary", e);
            return "AI summary unavailable. Total certificates: " + summary.totalCertificates()
                    + ", expired: " + summary.expired() + ", pending approvals: " + summary.pendingApprovals()
                    + ", missing documents: " + summary.missingDocuments() + ".";
        }
    }

    private byte[] readFile(CertificateVersion version) {
        try {
            return fileStorageService.download(version.getFileStorageKey()).readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read certificate file: " + version.getFileStorageKey(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void markReady(UUID folderId, String storageKey) {
        auditFolderRepository.findById(folderId).ifPresent(folder -> {
            folder.setFileStorageKey(storageKey);
            folder.setStatus(AuditFolderStatus.READY);
            folder.setGeneratedAt(Instant.now());
            folder.setExpiresAt(Instant.now().plus(DOWNLOAD_URL_EXPIRY.plus(1, ChronoUnit.DAYS)));
            auditFolderRepository.save(folder);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void markFailed(UUID folderId, String reason) {
        auditFolderRepository.findById(folderId).ifPresent(folder -> {
            folder.setStatus(AuditFolderStatus.FAILED);
            folder.setFailureReason(reason);
            auditFolderRepository.save(folder);
        });
    }

    @Transactional(readOnly = true)
    public List<AuditFolder> listForCurrentCompany() {
        return auditFolderRepository.findAllByCompanyIdOrderByGeneratedAtDesc(
                companyContextProvider.getCurrentCompanyId());
    }

    @Transactional(readOnly = true)
    public AuditFolder getById(UUID id) {
        AuditFolder folder = auditFolderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AuditFolder", id));
        return companyContextProvider.requireOwnership(folder, "AuditFolder", id);
    }

    @Transactional(readOnly = true)
    public URL generateDownloadUrl(UUID id) {
        AuditFolder folder = getById(id);
        if (folder.getStatus() != AuditFolderStatus.READY || folder.getFileStorageKey() == null) {
            throw new InvalidStateException(
                    "Audit folder is not ready (status: " + folder.getStatus() + ")");
        }
        return fileStorageService.generatePresignedDownloadUrl(folder.getFileStorageKey(), DOWNLOAD_URL_EXPIRY);
    }
}
