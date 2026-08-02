package com.aicompliance.presentation.controller;

import com.aicompliance.application.company.CompanyService;
import com.aicompliance.application.port.CertificateRepository;
import com.aicompliance.application.port.EmployeeRepository;
import com.aicompliance.application.user.UserService;
import com.aicompliance.domain.company.Company;
import com.aicompliance.presentation.dto.request.CreateCompanyRequest;
import com.aicompliance.presentation.dto.request.UpdateCompanyStatusRequest;
import com.aicompliance.presentation.dto.response.CompanyResponse;
import com.aicompliance.presentation.dto.response.PlatformSummaryResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SUPER_ADMIN only - enforced via URL pattern in SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final CompanyService companyService;
    private final UserService userService;
    private final EmployeeRepository employeeRepository;
    private final CertificateRepository certificateRepository;

    public CompanyController(CompanyService companyService, UserService userService,
            EmployeeRepository employeeRepository, CertificateRepository certificateRepository) {
        this.companyService = companyService;
        this.userService = userService;
        this.employeeRepository = employeeRepository;
        this.certificateRepository = certificateRepository;
    }

    private CompanyResponse toResponse(Company company) {
        long employeeCount = employeeRepository.countByCompanyId(company.getId());
        long certificateCount = certificateRepository.countByCompanyId(company.getId());
        return CompanyResponse.from(company, employeeCount, certificateCount);
    }

    @PostMapping
    public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CreateCompanyRequest request) {
        Company company = companyService.createCompany(new CompanyService.CreateCompanyCommand(
                request.name(), request.gstNumber(), request.panNumber(), request.cinNumber(),
                request.registeredAddress(), request.industryId()));

        userService.createCompanyAdmin(company.getId(), request.adminEmail(), request.adminPassword(),
                request.adminFullName());

        return ResponseEntity.status(HttpStatus.CREATED).body(CompanyResponse.from(company, 0, 0));
    }

    @GetMapping
    public ResponseEntity<List<CompanyResponse>> list() {
        List<CompanyResponse> companies = companyService.listAll().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(companies);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(companyService.getById(id)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<CompanyResponse> updateStatus(@PathVariable UUID id,
            @Valid @RequestBody UpdateCompanyStatusRequest request) {
        Company company = companyService.updateStatus(id, request.subscriptionStatus(), request.active());
        return ResponseEntity.ok(toResponse(company));
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<PlatformSummaryResponse> dashboardSummary() {
        return ResponseEntity.ok(companyService.getPlatformSummary());
    }
}
