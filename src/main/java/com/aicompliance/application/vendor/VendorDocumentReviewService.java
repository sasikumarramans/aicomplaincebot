package com.aicompliance.application.vendor;

import com.aicompliance.application.audit.AuditLogRecorder;
import com.aicompliance.application.category.CategoryService;
import com.aicompliance.application.port.CompanyContextProvider;
import com.aicompliance.application.port.FileStorageService;
import com.aicompliance.application.port.VendorDocumentRepository;
import com.aicompliance.application.port.VendorRepository;
import com.aicompliance.domain.shared.EntityNotFoundException;
import com.aicompliance.domain.vendor.Vendor;
import com.aicompliance.domain.vendor.VendorComplianceStatus;
import com.aicompliance.domain.vendor.VendorDocument;
import com.aicompliance.domain.vendor.VendorDocumentStatus;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Upload + review for vendor documents (GST/PAN/ISO/Factory License/Insurance/Labour License, or
 * whatever a company defines), and the status rollup that computes Vendor.complianceStatus from
 * its documents' individual statuses.
 */
@Service
public class VendorDocumentReviewService {

    private final VendorRepository vendorRepository;
    private final VendorDocumentRepository documentRepository;
    private final CategoryService categoryService;
    private final FileStorageService fileStorageService;
    private final CompanyContextProvider companyContextProvider;
    private final AuditLogRecorder auditLogRecorder;

    public VendorDocumentReviewService(VendorRepository vendorRepository,
            VendorDocumentRepository documentRepository, CategoryService categoryService,
            FileStorageService fileStorageService, CompanyContextProvider companyContextProvider,
            AuditLogRecorder auditLogRecorder) {
        this.vendorRepository = vendorRepository;
        this.documentRepository = documentRepository;
        this.categoryService = categoryService;
        this.fileStorageService = fileStorageService;
        this.companyContextProvider = companyContextProvider;
        this.auditLogRecorder = auditLogRecorder;
    }

    public record UploadDocumentCommand(
            UUID vendorId,
            UUID categoryId,
            LocalDate issueDate,
            LocalDate expiryDate,
            InputStream fileContent,
            long fileSizeBytes,
            String originalFileName,
            String mimeType) {
    }

    @Transactional
    public VendorDocument uploadDocument(UploadDocumentCommand command) {
        Vendor vendor = getVendor(command.vendorId());
        // Validates the category belongs to this company (or 404s), independent of industry.
        categoryService.getById(command.categoryId());

        UUID companyId = companyContextProvider.getCurrentCompanyId();
        String storageKey = "companies/%s/vendors/%s/documents/%s".formatted(
                companyId, vendor.getId(), command.originalFileName());
        fileStorageService.upload(storageKey, command.fileContent(), command.fileSizeBytes(), command.mimeType());

        VendorDocument document = new VendorDocument();
        document.setCompanyId(companyId);
        document.setVendorId(vendor.getId());
        document.setCategoryId(command.categoryId());
        document.setFileStorageKey(storageKey);
        document.setOriginalFileName(command.originalFileName());
        document.setMimeType(command.mimeType());
        document.setIssueDate(command.issueDate());
        document.setExpiryDate(command.expiryDate());
        document.setStatus(computeInitialStatus(command.expiryDate()));
        document.setUploadedByUserId(companyContextProvider.getCurrentUserId());
        document = documentRepository.save(document);

        recomputeVendorStatus(vendor.getId());
        auditLogRecorder.record("VENDOR_DOCUMENT_UPLOADED", "VendorDocument", document.getId());
        return document;
    }

    @Transactional
    public VendorDocument reviewDocument(UUID documentId, VendorDocumentStatus decision, String notes) {
        if (decision != VendorDocumentStatus.APPROVED && decision != VendorDocumentStatus.REJECTED) {
            throw new IllegalArgumentException("Review decision must be APPROVED or REJECTED");
        }

        VendorDocument document = getDocument(documentId);
        document.setStatus(decision);
        document.setReviewedByUserId(companyContextProvider.getCurrentUserId());
        document.setReviewedAt(Instant.now());
        document.setReviewNotes(notes);
        document = documentRepository.save(document);

        recomputeVendorStatus(document.getVendorId());
        auditLogRecorder.record("VENDOR_DOCUMENT_" + decision.name(), "VendorDocument", document.getId());
        return document;
    }

    @Transactional(readOnly = true)
    public List<VendorDocument> listForVendor(UUID vendorId) {
        getVendor(vendorId);
        return documentRepository.findAllByVendorId(vendorId);
    }

    @Transactional(readOnly = true)
    public VendorDocument getDocument(UUID id) {
        VendorDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("VendorDocument", id));
        return companyContextProvider.requireOwnership(document, "VendorDocument", id);
    }

    @Transactional(readOnly = true)
    public URL generateDownloadUrl(UUID documentId) {
        VendorDocument document = getDocument(documentId);
        return fileStorageService.generatePresignedDownloadUrl(document.getFileStorageKey(), Duration.ofMinutes(15));
    }

    private Vendor getVendor(UUID vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new EntityNotFoundException("Vendor", vendorId));
        return companyContextProvider.requireOwnership(vendor, "Vendor", vendorId);
    }

    private VendorDocumentStatus computeInitialStatus(LocalDate expiryDate) {
        if (expiryDate != null && expiryDate.isBefore(LocalDate.now())) {
            return VendorDocumentStatus.EXPIRED;
        }
        return VendorDocumentStatus.PENDING;
    }

    /**
     * Vendor.complianceStatus is a rollup of its documents: any REJECTED wins (most severe),
     * else any EXPIRED, else any PENDING, else (all APPROVED, at least one document) APPROVED.
     * A vendor with zero documents stays PENDING.
     */
    private void recomputeVendorStatus(UUID vendorId) {
        List<VendorDocument> documents = documentRepository.findAllByVendorId(vendorId);
        VendorComplianceStatus status;
        if (documents.isEmpty()) {
            status = VendorComplianceStatus.PENDING;
        } else if (documents.stream().anyMatch(d -> d.getStatus() == VendorDocumentStatus.REJECTED)) {
            status = VendorComplianceStatus.REJECTED;
        } else if (documents.stream().anyMatch(d -> d.getStatus() == VendorDocumentStatus.EXPIRED)) {
            status = VendorComplianceStatus.EXPIRED;
        } else if (documents.stream().anyMatch(d -> d.getStatus() == VendorDocumentStatus.PENDING)) {
            status = VendorComplianceStatus.PENDING;
        } else {
            status = VendorComplianceStatus.APPROVED;
        }

        Vendor vendor = vendorRepository.findById(vendorId).orElseThrow();
        vendor.setComplianceStatus(status);
        vendorRepository.save(vendor);
    }
}
