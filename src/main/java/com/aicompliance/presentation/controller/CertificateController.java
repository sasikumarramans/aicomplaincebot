package com.aicompliance.presentation.controller;

import com.aicompliance.application.certificate.CertificateReviewService;
import com.aicompliance.application.certificate.CertificateService;
import com.aicompliance.application.certificate.CertificateVersionService;
import com.aicompliance.application.certificate.DuplicateDetectionService;
import com.aicompliance.application.certificate.RenewalPredictionService;
import com.aicompliance.domain.certificate.Certificate;
import com.aicompliance.domain.certificate.CertificateVersion;
import com.aicompliance.presentation.dto.request.RejectCertificateRequest;
import com.aicompliance.presentation.dto.response.CertificateResponse;
import com.aicompliance.presentation.dto.response.CertificateVersionResponse;
import com.aicompliance.presentation.dto.response.DownloadUrlResponse;
import com.aicompliance.presentation.dto.response.DuplicatePairResponse;
import com.aicompliance.presentation.dto.response.ExtractionStatusResponse;
import com.aicompliance.presentation.dto.response.RenewalPredictionResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/certificates")
public class CertificateController {

    private final CertificateService certificateService;
    private final CertificateReviewService certificateReviewService;
    private final CertificateVersionService certificateVersionService;
    private final DuplicateDetectionService duplicateDetectionService;
    private final RenewalPredictionService renewalPredictionService;

    public CertificateController(CertificateService certificateService,
            CertificateReviewService certificateReviewService, CertificateVersionService certificateVersionService,
            DuplicateDetectionService duplicateDetectionService, RenewalPredictionService renewalPredictionService) {
        this.certificateService = certificateService;
        this.certificateReviewService = certificateReviewService;
        this.certificateVersionService = certificateVersionService;
        this.duplicateDetectionService = duplicateDetectionService;
        this.renewalPredictionService = renewalPredictionService;
    }

    /**
     * categoryId/certificateName are required unless autoExtract=true, in which case the AI
     * pipeline fills them in asynchronously - poll GET /{id}/extraction-status for progress.
     */
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'COMPLIANCE_MANAGER', 'DEPARTMENT_HEAD')")
    public ResponseEntity<CertificateResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID plantId,
            @RequestParam(required = false) String certificateName,
            @RequestParam(required = false) String certificateNumber,
            @RequestParam(required = false) String issuingAuthority,
            @RequestParam(required = false) LocalDate issueDate,
            @RequestParam(required = false) LocalDate expiryDate,
            @RequestParam(required = false) String licenseNumber,
            @RequestParam(defaultValue = "false") boolean hasQrCode,
            @RequestParam(defaultValue = "false") boolean hasDigitalSignature,
            @RequestParam(defaultValue = "false") boolean autoExtract) {
        try {
            Certificate certificate = certificateService.upload(new CertificateService.UploadCertificateCommand(
                    plantId, categoryId, certificateName, certificateNumber, issuingAuthority, issueDate,
                    expiryDate, licenseNumber, hasQrCode, hasDigitalSignature, autoExtract, file.getInputStream(),
                    file.getSize(), file.getOriginalFilename(), file.getContentType()));
            return ResponseEntity.status(HttpStatus.CREATED).body(CertificateResponse.from(certificate));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }
    }

    @GetMapping
    public ResponseEntity<List<CertificateResponse>> list(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID plantId) {
        List<Certificate> certificates;
        if (categoryId != null) {
            certificates = certificateService.listByCategory(categoryId);
        } else if (plantId != null) {
            certificates = certificateService.listByPlant(plantId);
        } else {
            certificates = certificateService.listForCurrentCompany();
        }
        return ResponseEntity.ok(certificates.stream().map(CertificateResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CertificateResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(CertificateResponse.from(certificateService.getById(id)));
    }

    @GetMapping("/{id}/download-url")
    public ResponseEntity<DownloadUrlResponse> downloadUrl(@PathVariable UUID id) {
        return ResponseEntity.ok(new DownloadUrlResponse(certificateService.generateDownloadUrl(id).toString()));
    }

    @GetMapping("/{id}/extraction-status")
    public ResponseEntity<ExtractionStatusResponse> extractionStatus(@PathVariable UUID id) {
        CertificateVersion version = certificateService.getExtractionStatus(id);
        return ResponseEntity.ok(new ExtractionStatusResponse(
                version.getProcessingStatus().name(), version.getProcessingError()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'COMPLIANCE_MANAGER')")
    public ResponseEntity<CertificateResponse> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(CertificateResponse.from(certificateReviewService.approve(id)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'COMPLIANCE_MANAGER')")
    public ResponseEntity<CertificateResponse> reject(@PathVariable UUID id,
            @Valid @RequestBody RejectCertificateRequest request) {
        return ResponseEntity.ok(CertificateResponse.from(certificateReviewService.reject(id, request.reason())));
    }

    /**
     * Renewal: uploads a new version for an existing certificate, archiving the current one.
     * Same manual-vs-autoExtract split as the initial upload.
     */
    @PostMapping(value = "/{id}/renew", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'COMPLIANCE_MANAGER', 'DEPARTMENT_HEAD')")
    public ResponseEntity<CertificateResponse> renew(
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String certificateNumber,
            @RequestParam(required = false) LocalDate issueDate,
            @RequestParam(required = false) LocalDate expiryDate,
            @RequestParam(defaultValue = "false") boolean autoExtract) {
        try {
            Certificate certificate = certificateVersionService.renew(id, new CertificateVersionService.RenewCommand(
                    issueDate, expiryDate, certificateNumber, autoExtract, file.getInputStream(), file.getSize(),
                    file.getOriginalFilename(), file.getContentType()));
            return ResponseEntity.ok(CertificateResponse.from(certificate));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<List<CertificateVersionResponse>> versionHistory(@PathVariable UUID id) {
        List<CertificateVersionResponse> versions = certificateVersionService.listVersionHistory(id).stream()
                .map(CertificateVersionResponse::from)
                .toList();
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/analysis/duplicates")
    public ResponseEntity<List<DuplicatePairResponse>> findDuplicates() {
        return ResponseEntity.ok(duplicateDetectionService.findDuplicates().stream()
                .map(DuplicatePairResponse::from)
                .toList());
    }

    @GetMapping("/{id}/renewal-prediction")
    public ResponseEntity<RenewalPredictionResponse> renewalPrediction(@PathVariable UUID id) {
        return ResponseEntity.ok(RenewalPredictionResponse.from(renewalPredictionService.predictForCertificate(id)));
    }
}
