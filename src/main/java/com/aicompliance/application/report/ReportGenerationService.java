package com.aicompliance.application.report;

import com.aicompliance.application.certificate.MissingDocumentDetectionService;
import com.aicompliance.application.compliance.ComplianceScoreService;
import com.aicompliance.application.dashboard.DashboardAggregationService;
import com.aicompliance.application.port.CertificateCategoryRepository;
import com.aicompliance.application.port.CertificateRepository;
import com.aicompliance.application.port.CompanyContextProvider;
import com.aicompliance.application.port.CompanyRepository;
import com.aicompliance.application.port.EmployeeCertificationRepository;
import com.aicompliance.application.port.EmployeeRepository;
import com.aicompliance.application.port.PdfReportGenerator;
import com.aicompliance.application.port.VendorRepository;
import com.aicompliance.domain.certificate.Certificate;
import com.aicompliance.domain.certificate.CertificateCategory;
import com.aicompliance.domain.company.Company;
import com.aicompliance.domain.employee.Employee;
import com.aicompliance.domain.employee.EmployeeCertification;
import com.aicompliance.domain.vendor.Vendor;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates each of the 7 report types as PDF, from real company data. Reports read data through
 * the same services/repositories the rest of the app uses (no report-specific data path), so
 * there is a single source of truth for what "expired" / "valid" / etc. mean.
 */
@Service
public class ReportGenerationService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'");

    private final CompanyRepository companyRepository;
    private final CertificateRepository certificateRepository;
    private final CertificateCategoryRepository categoryRepository;
    private final VendorRepository vendorRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeCertificationRepository employeeCertificationRepository;
    private final DashboardAggregationService dashboardAggregationService;
    private final ComplianceScoreService complianceScoreService;
    private final MissingDocumentDetectionService missingDocumentDetectionService;
    private final PdfReportGenerator pdfReportGenerator;
    private final CompanyContextProvider companyContextProvider;

    public ReportGenerationService(CompanyRepository companyRepository, CertificateRepository certificateRepository,
            CertificateCategoryRepository categoryRepository,
            VendorRepository vendorRepository, EmployeeRepository employeeRepository,
            EmployeeCertificationRepository employeeCertificationRepository,
            DashboardAggregationService dashboardAggregationService, ComplianceScoreService complianceScoreService,
            MissingDocumentDetectionService missingDocumentDetectionService, PdfReportGenerator pdfReportGenerator,
            CompanyContextProvider companyContextProvider) {
        this.companyRepository = companyRepository;
        this.certificateRepository = certificateRepository;
        this.categoryRepository = categoryRepository;
        this.vendorRepository = vendorRepository;
        this.employeeRepository = employeeRepository;
        this.employeeCertificationRepository = employeeCertificationRepository;
        this.dashboardAggregationService = dashboardAggregationService;
        this.complianceScoreService = complianceScoreService;
        this.missingDocumentDetectionService = missingDocumentDetectionService;
        this.pdfReportGenerator = pdfReportGenerator;
        this.companyContextProvider = companyContextProvider;
    }

    @Transactional(readOnly = true)
    public byte[] generateExpiryReport() {
        UUID companyId = companyContextProvider.getCurrentCompanyId();
        Map<UUID, CertificateCategory> categoriesById = categoriesById(companyId);

        List<Map<String, Object>> rows = certificateRepository.findAllByCompanyId(companyId).stream()
                .map(c -> certificateRow(c, categoriesById))
                .toList();

        Map<String, Object> model = baseModel(companyId);
        model.put("certificates", rows);
        return pdfReportGenerator.generate("expiry-report", model);
    }

    @Transactional(readOnly = true)
    public byte[] generateDepartmentComplianceReport() {
        UUID companyId = companyContextProvider.getCurrentCompanyId();
        Map<String, Object> model = baseModel(companyId);
        model.put("plantCounts", dashboardAggregationService.getPlantWiseCompliance().stream()
                .map(p -> Map.of("label", p.label(), "value", p.value()))
                .toList());
        return pdfReportGenerator.generate("department-compliance-report", model);
    }

    @Transactional(readOnly = true)
    public byte[] generateMonthlyReport() {
        UUID companyId = companyContextProvider.getCurrentCompanyId();
        Map<String, Object> model = baseModel(companyId);
        model.put("expiryTrend", dashboardAggregationService.getExpiryTrend().stream()
                .map(p -> Map.of("label", p.label(), "value", p.value()))
                .toList());
        return pdfReportGenerator.generate("monthly-report", model);
    }

    @Transactional(readOnly = true)
    public byte[] generateVendorReport() {
        UUID companyId = companyContextProvider.getCurrentCompanyId();
        List<Map<String, Object>> vendors = vendorRepository.findAllByCompanyId(companyId).stream()
                .map(this::vendorRow)
                .toList();

        Map<String, Object> model = baseModel(companyId);
        model.put("vendors", vendors);
        return pdfReportGenerator.generate("vendor-report", model);
    }

    @Transactional(readOnly = true)
    public byte[] generateEmployeeReport() {
        UUID companyId = companyContextProvider.getCurrentCompanyId();
        Map<UUID, Employee> employeesById = employeeRepository.findAllByCompanyId(companyId).stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));

        List<Map<String, Object>> rows = employeeCertificationRepository.findAllByCompanyId(companyId).stream()
                .map(cert -> employeeCertRow(cert, employeesById))
                .toList();

        Map<String, Object> model = baseModel(companyId);
        model.put("certifications", rows);
        return pdfReportGenerator.generate("employee-report", model);
    }

    @Transactional(readOnly = true)
    public byte[] generateComplianceScoreReport(boolean includeExplanation) {
        UUID companyId = companyContextProvider.getCurrentCompanyId();
        ComplianceScoreService.ScoreBreakdown breakdown = complianceScoreService.computeScore();

        Map<String, Object> model = baseModel(companyId);
        model.put("overallPercent", breakdown.overallPercent());
        model.put("byCategoryGroup", breakdown.byCategoryGroup().stream()
                .map(c -> Map.of(
                        "categoryGroup", c.categoryGroup(),
                        "scorePercent", c.scorePercent(),
                        "validCount", c.validCount(),
                        "totalCount", c.totalCount()))
                .toList());
        model.put("explanation", includeExplanation ? complianceScoreService.explainScore(breakdown) : null);
        return pdfReportGenerator.generate("compliance-score-report", model);
    }

    @Transactional(readOnly = true)
    public byte[] generateAuditReport(String aiSummary) {
        UUID companyId = companyContextProvider.getCurrentCompanyId();
        DashboardAggregationService.Summary summary = dashboardAggregationService.getSummary();

        Map<String, Object> model = baseModel(companyId);
        model.put("totalCertificates", summary.totalCertificates());
        model.put("pendingApprovals", summary.pendingApprovals());
        model.put("expiredCount", summary.expired());
        model.put("missingDocumentCount", summary.missingDocuments());
        model.put("complianceScore", summary.complianceScore());
        model.put("aiSummary", aiSummary);
        model.put("missingDocuments", missingDocumentDetectionService.findMissingDocuments().stream()
                .map(m -> Map.of("plantName", m.plantName(), "categoryName", m.categoryName()))
                .toList());
        return pdfReportGenerator.generate("audit-report", model);
    }

    private Map<String, Object> baseModel(UUID companyId) {
        Company company = companyRepository.findById(companyId).orElse(null);
        Map<String, Object> model = new HashMap<>();
        model.put("companyName", company != null ? company.getName() : "Unknown Company");
        model.put("generatedAt", TIMESTAMP_FORMAT.format(Instant.now().atZone(ZoneOffset.UTC)));
        return model;
    }

    private Map<UUID, CertificateCategory> categoriesById(UUID companyId) {
        return categoryRepository.findAllByCompanyId(companyId).stream()
                .collect(Collectors.toMap(CertificateCategory::getId, c -> c));
    }

    private Map<String, Object> certificateRow(Certificate certificate, Map<UUID, CertificateCategory> categoriesById) {
        CertificateCategory category = certificate.getCategoryId() != null
                ? categoriesById.get(certificate.getCategoryId())
                : null;
        Map<String, Object> row = new HashMap<>();
        row.put("certificateName", certificate.getCertificateName());
        row.put("categoryName", category != null ? category.getName() : "Uncategorized");
        row.put("expiryDate", certificate.getExpiryDate() != null ? certificate.getExpiryDate().toString() : "-");
        row.put("expiryBucket", certificate.getExpiryBucket().name());
        return row;
    }

    private Map<String, Object> vendorRow(Vendor vendor) {
        Map<String, Object> row = new HashMap<>();
        row.put("name", vendor.getName());
        row.put("gstNumber", vendor.getGstNumber() != null ? vendor.getGstNumber() : "-");
        row.put("complianceStatus", vendor.getComplianceStatus().name());
        return row;
    }

    private Map<String, Object> employeeCertRow(EmployeeCertification cert, Map<UUID, Employee> employeesById) {
        Employee employee = employeesById.get(cert.getEmployeeId());
        Map<String, Object> row = new HashMap<>();
        row.put("employeeName", employee != null ? employee.getFullName() : "Unknown Employee");
        row.put("certificationType", cert.getCertificationType().name());
        row.put("expiryDate", cert.getExpiryDate() != null ? cert.getExpiryDate().toString() : "-");
        row.put("expiryBucket", cert.getExpiryBucket().name());
        return row;
    }
}
