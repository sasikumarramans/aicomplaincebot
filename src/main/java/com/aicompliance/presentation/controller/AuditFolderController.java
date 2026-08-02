package com.aicompliance.presentation.controller;

import com.aicompliance.application.report.AuditFolderExportService;
import com.aicompliance.domain.audit.AuditFolder;
import com.aicompliance.presentation.dto.response.AuditFolderResponse;
import com.aicompliance.presentation.dto.response.DownloadUrlResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-folders")
public class AuditFolderController {

    private final AuditFolderExportService auditFolderExportService;

    public AuditFolderController(AuditFolderExportService auditFolderExportService) {
        this.auditFolderExportService = auditFolderExportService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'COMPLIANCE_MANAGER')")
    public ResponseEntity<AuditFolderResponse> generate() {
        AuditFolder folder = auditFolderExportService.requestGeneration();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(AuditFolderResponse.from(folder));
    }

    @GetMapping
    public ResponseEntity<List<AuditFolderResponse>> list() {
        return ResponseEntity.ok(auditFolderExportService.listForCurrentCompany().stream()
                .map(AuditFolderResponse::from)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditFolderResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(AuditFolderResponse.from(auditFolderExportService.getById(id)));
    }

    @GetMapping("/{id}/download-url")
    public ResponseEntity<DownloadUrlResponse> downloadUrl(@PathVariable UUID id) {
        return ResponseEntity.ok(new DownloadUrlResponse(
                auditFolderExportService.generateDownloadUrl(id).toString()));
    }
}
