package com.aicompliance.presentation.controller;

import com.aicompliance.application.employee.EmployeeComplianceService;
import com.aicompliance.application.employee.EmployeeService;
import com.aicompliance.domain.employee.CertificationType;
import com.aicompliance.domain.employee.Employee;
import com.aicompliance.domain.employee.EmployeeCertification;
import com.aicompliance.presentation.dto.request.CreateEmployeeRequest;
import com.aicompliance.presentation.dto.response.DownloadUrlResponse;
import com.aicompliance.presentation.dto.response.EmployeeCertificationResponse;
import com.aicompliance.presentation.dto.response.EmployeeResponse;
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
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeComplianceService complianceService;

    public EmployeeController(EmployeeService employeeService, EmployeeComplianceService complianceService) {
        this.employeeService = employeeService;
        this.complianceService = complianceService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'COMPLIANCE_MANAGER', 'DEPARTMENT_HEAD')")
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody CreateEmployeeRequest request) {
        Employee employee = employeeService.create(new EmployeeService.CreateEmployeeCommand(
                request.plantId(), request.employeeCode(), request.fullName(), request.department(),
                request.designation(), request.email()));
        return ResponseEntity.status(HttpStatus.CREATED).body(EmployeeResponse.from(employee));
    }

    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> list() {
        return ResponseEntity.ok(employeeService.listForCurrentCompany().stream()
                .map(EmployeeResponse::from)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(EmployeeResponse.from(employeeService.getById(id)));
    }

    @PutMapping("/{id}/active")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'COMPLIANCE_MANAGER')")
    public ResponseEntity<EmployeeResponse> setActive(@PathVariable UUID id, @RequestBody Boolean active) {
        return ResponseEntity.ok(EmployeeResponse.from(employeeService.setActive(id, Boolean.TRUE.equals(active))));
    }

    @PostMapping(value = "/{employeeId}/certifications", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'COMPLIANCE_MANAGER', 'DEPARTMENT_HEAD')")
    public ResponseEntity<EmployeeCertificationResponse> uploadCertification(
            @PathVariable UUID employeeId,
            @RequestPart("file") MultipartFile file,
            @RequestParam CertificationType certificationType,
            @RequestParam(required = false) LocalDate issueDate,
            @RequestParam(required = false) LocalDate expiryDate) {
        try {
            EmployeeCertification certification = complianceService.uploadCertification(
                    new EmployeeComplianceService.UploadCertificationCommand(
                            employeeId, certificationType, issueDate, expiryDate, file.getInputStream(),
                            file.getSize(), file.getOriginalFilename(), file.getContentType()));
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(EmployeeCertificationResponse.from(certification));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }
    }

    @GetMapping("/{employeeId}/certifications")
    public ResponseEntity<List<EmployeeCertificationResponse>> listCertifications(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(complianceService.listForEmployee(employeeId).stream()
                .map(EmployeeCertificationResponse::from)
                .toList());
    }

    @GetMapping("/certifications/{certificationId}/download-url")
    public ResponseEntity<DownloadUrlResponse> certificationDownloadUrl(@PathVariable UUID certificationId) {
        return ResponseEntity.ok(new DownloadUrlResponse(
                complianceService.generateDownloadUrl(certificationId).toString()));
    }
}
