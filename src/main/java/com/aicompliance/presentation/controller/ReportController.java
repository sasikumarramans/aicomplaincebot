package com.aicompliance.presentation.controller;

import com.aicompliance.application.report.ReportGenerationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportGenerationService reportGenerationService;

    public ReportController(ReportGenerationService reportGenerationService) {
        this.reportGenerationService = reportGenerationService;
    }

    @GetMapping("/expiry")
    public ResponseEntity<byte[]> expiryReport() {
        return pdf("expiry-report.pdf", reportGenerationService.generateExpiryReport());
    }

    @GetMapping("/department-compliance")
    public ResponseEntity<byte[]> departmentComplianceReport() {
        return pdf("department-compliance-report.pdf", reportGenerationService.generateDepartmentComplianceReport());
    }

    @GetMapping("/monthly")
    public ResponseEntity<byte[]> monthlyReport() {
        return pdf("monthly-report.pdf", reportGenerationService.generateMonthlyReport());
    }

    @GetMapping("/vendor")
    public ResponseEntity<byte[]> vendorReport() {
        return pdf("vendor-report.pdf", reportGenerationService.generateVendorReport());
    }

    @GetMapping("/employee")
    public ResponseEntity<byte[]> employeeReport() {
        return pdf("employee-report.pdf", reportGenerationService.generateEmployeeReport());
    }

    @GetMapping("/compliance-score")
    public ResponseEntity<byte[]> complianceScoreReport(@RequestParam(defaultValue = "false") boolean explain) {
        return pdf("compliance-score-report.pdf", reportGenerationService.generateComplianceScoreReport(explain));
    }

    @GetMapping("/audit")
    public ResponseEntity<byte[]> auditReport() {
        return pdf("audit-report.pdf", reportGenerationService.generateAuditReport(null));
    }

    private ResponseEntity<byte[]> pdf(String filename, byte[] content) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(content);
    }
}
