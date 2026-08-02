package com.aicompliance.presentation.controller;

import com.aicompliance.application.vendor.VendorDocumentReviewService;
import com.aicompliance.application.vendor.VendorService;
import com.aicompliance.domain.vendor.Vendor;
import com.aicompliance.domain.vendor.VendorDocument;
import com.aicompliance.domain.vendor.VendorDocumentStatus;
import com.aicompliance.presentation.dto.request.CreateVendorRequest;
import com.aicompliance.presentation.dto.request.ReviewVendorDocumentRequest;
import com.aicompliance.presentation.dto.response.DownloadUrlResponse;
import com.aicompliance.presentation.dto.response.VendorDocumentResponse;
import com.aicompliance.presentation.dto.response.VendorResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/vendors")
public class VendorController {

    private final VendorService vendorService;
    private final VendorDocumentReviewService documentReviewService;

    public VendorController(VendorService vendorService, VendorDocumentReviewService documentReviewService) {
        this.vendorService = vendorService;
        this.documentReviewService = documentReviewService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'COMPLIANCE_MANAGER')")
    public ResponseEntity<VendorResponse> create(@Valid @RequestBody CreateVendorRequest request) {
        Vendor vendor = vendorService.create(new VendorService.CreateVendorCommand(
                request.name(), request.gstNumber(), request.panNumber(), request.contactEmail(),
                request.contactPhone()));
        return ResponseEntity.status(HttpStatus.CREATED).body(VendorResponse.from(vendor));
    }

    @GetMapping
    public ResponseEntity<List<VendorResponse>> list() {
        return ResponseEntity.ok(vendorService.listForCurrentCompany().stream()
                .map(VendorResponse::from)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendorResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(VendorResponse.from(vendorService.getById(id)));
    }

    @PutMapping("/{id}/active")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'COMPLIANCE_MANAGER')")
    public ResponseEntity<VendorResponse> setActive(@PathVariable UUID id, @RequestBody Boolean active) {
        return ResponseEntity.ok(VendorResponse.from(vendorService.setActive(id, Boolean.TRUE.equals(active))));
    }

    @PostMapping(value = "/{vendorId}/documents", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'COMPLIANCE_MANAGER')")
    public ResponseEntity<VendorDocumentResponse> uploadDocument(
            @PathVariable UUID vendorId,
            @RequestPart("file") MultipartFile file,
            @RequestParam UUID categoryId,
            @RequestParam(required = false) LocalDate issueDate,
            @RequestParam(required = false) LocalDate expiryDate) {
        try {
            VendorDocument document = documentReviewService.uploadDocument(
                    new VendorDocumentReviewService.UploadDocumentCommand(
                            vendorId, categoryId, issueDate, expiryDate, file.getInputStream(), file.getSize(),
                            file.getOriginalFilename(), file.getContentType()));
            return ResponseEntity.status(HttpStatus.CREATED).body(VendorDocumentResponse.from(document));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }
    }

    @GetMapping("/{vendorId}/documents")
    public ResponseEntity<List<VendorDocumentResponse>> listDocuments(@PathVariable UUID vendorId) {
        return ResponseEntity.ok(documentReviewService.listForVendor(vendorId).stream()
                .map(VendorDocumentResponse::from)
                .toList());
    }

    @GetMapping("/documents/{documentId}/download-url")
    public ResponseEntity<DownloadUrlResponse> documentDownloadUrl(@PathVariable UUID documentId) {
        return ResponseEntity.ok(new DownloadUrlResponse(
                documentReviewService.generateDownloadUrl(documentId).toString()));
    }

    @PostMapping("/documents/{documentId}/review")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'COMPLIANCE_MANAGER')")
    public ResponseEntity<VendorDocumentResponse> reviewDocument(@PathVariable UUID documentId,
            @Valid @RequestBody ReviewVendorDocumentRequest request) {
        VendorDocumentStatus decision = request.decision() == ReviewVendorDocumentRequest.Decision.APPROVE
                ? VendorDocumentStatus.APPROVED
                : VendorDocumentStatus.REJECTED;
        VendorDocument document = documentReviewService.reviewDocument(documentId, decision, request.notes());
        return ResponseEntity.ok(VendorDocumentResponse.from(document));
    }
}
