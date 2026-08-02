package com.aicompliance.application.dashboard;

import com.aicompliance.application.certificate.MissingDocumentDetectionService;
import com.aicompliance.application.compliance.ComplianceScoreService;
import com.aicompliance.application.port.CertificateCategoryRepository;
import com.aicompliance.application.port.CertificateRepository;
import com.aicompliance.application.port.CompanyContextProvider;
import com.aicompliance.application.port.EmployeeCertificationRepository;
import com.aicompliance.application.port.PlantRepository;
import com.aicompliance.application.port.VendorRepository;
import com.aicompliance.domain.certificate.Certificate;
import com.aicompliance.domain.certificate.CertificateCategory;
import com.aicompliance.domain.certificate.CertificateStatus;
import com.aicompliance.domain.company.Plant;
import com.aicompliance.domain.employee.EmployeeCertification;
import com.aicompliance.domain.shared.ExpiryBucket;
import com.aicompliance.domain.vendor.Vendor;
import com.aicompliance.domain.vendor.VendorComplianceStatus;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardAggregationService {

    private final CertificateRepository certificateRepository;
    private final CertificateCategoryRepository categoryRepository;
    private final PlantRepository plantRepository;
    private final VendorRepository vendorRepository;
    private final EmployeeCertificationRepository employeeCertificationRepository;
    private final ComplianceScoreService complianceScoreService;
    private final MissingDocumentDetectionService missingDocumentDetectionService;
    private final CompanyContextProvider companyContextProvider;

    public DashboardAggregationService(CertificateRepository certificateRepository,
            CertificateCategoryRepository categoryRepository, PlantRepository plantRepository,
            VendorRepository vendorRepository, EmployeeCertificationRepository employeeCertificationRepository,
            ComplianceScoreService complianceScoreService,
            MissingDocumentDetectionService missingDocumentDetectionService,
            CompanyContextProvider companyContextProvider) {
        this.certificateRepository = certificateRepository;
        this.categoryRepository = categoryRepository;
        this.plantRepository = plantRepository;
        this.vendorRepository = vendorRepository;
        this.employeeCertificationRepository = employeeCertificationRepository;
        this.complianceScoreService = complianceScoreService;
        this.missingDocumentDetectionService = missingDocumentDetectionService;
        this.companyContextProvider = companyContextProvider;
    }

    public record Summary(
            int totalCertificates,
            int expiringToday,
            int expiringIn30Days,
            int expired,
            int pendingApprovals,
            int missingDocuments,
            Double complianceScore,
            Double vendorCompliancePercent,
            EmployeeCertificationSummary employeeCertifications) {
    }

    public record EmployeeCertificationSummary(int total, int expiringIn30Days, int expired, int validPercent) {
    }

    public record ChartPoint(String label, long value) {
    }

    @Transactional(readOnly = true)
    public Summary getSummary() {
        List<Certificate> certificates = certificatesForCurrentCompany();

        int expiringToday = (int) certificates.stream()
                .filter(c -> c.getExpiryBucket() == ExpiryBucket.DAYS_0)
                .count();
        int expiringIn30Days = (int) certificates.stream()
                .filter(c -> isWithin30Days(c.getExpiryBucket()))
                .count();
        int expired = (int) certificates.stream()
                .filter(c -> c.getExpiryBucket() == ExpiryBucket.EXPIRED)
                .count();
        int pendingApprovals = (int) certificates.stream()
                .filter(c -> c.getStatus() == CertificateStatus.PENDING_REVIEW)
                .count();

        return new Summary(
                certificates.size(),
                expiringToday,
                expiringIn30Days,
                expired,
                pendingApprovals,
                missingDocumentDetectionService.findMissingDocuments().size(),
                complianceScoreService.computeScore().overallPercent(),
                computeVendorCompliancePercent(),
                computeEmployeeCertificationSummary()
        );
    }

    /** Percentage of this company's vendors currently APPROVED. Null if there are no vendors. */
    private Double computeVendorCompliancePercent() {
        List<Vendor> vendors = vendorRepository.findAllByCompanyId(companyContextProvider.getCurrentCompanyId());
        if (vendors.isEmpty()) {
            return null;
        }
        long approved = vendors.stream()
                .filter(v -> v.getComplianceStatus() == VendorComplianceStatus.APPROVED)
                .count();
        return (approved * 100.0) / vendors.size();
    }

    private EmployeeCertificationSummary computeEmployeeCertificationSummary() {
        List<EmployeeCertification> certifications = employeeCertificationRepository
                .findAllByCompanyId(companyContextProvider.getCurrentCompanyId());
        if (certifications.isEmpty()) {
            return new EmployeeCertificationSummary(0, 0, 0, 100);
        }

        int expiringIn30Days = (int) certifications.stream()
                .filter(c -> isWithin30Days(c.getExpiryBucket()))
                .count();
        int expired = (int) certifications.stream()
                .filter(c -> c.getExpiryBucket() == ExpiryBucket.EXPIRED)
                .count();
        int validPercent = (int) Math.round(
                ((certifications.size() - expired) * 100.0) / certifications.size());

        return new EmployeeCertificationSummary(certifications.size(), expiringIn30Days, expired, validPercent);
    }

    /** Certificates expiring per month over the trailing 6 months + next 6 months. */
    @Transactional(readOnly = true)
    public List<ChartPoint> getExpiryTrend() {
        List<Certificate> certificates = certificatesForCurrentCompany();
        DateTimeFormatter labelFormat = DateTimeFormatter.ofPattern("yyyy-MM");

        Map<YearMonth, Long> byMonth = certificates.stream()
                .filter(c -> c.getExpiryDate() != null)
                .collect(Collectors.groupingBy(c -> YearMonth.from(c.getExpiryDate()), Collectors.counting()));

        YearMonth start = YearMonth.now().minusMonths(6);
        return java.util.stream.IntStream.rangeClosed(0, 12)
                .mapToObj(start::plusMonths)
                .map(month -> new ChartPoint(month.format(labelFormat), byMonth.getOrDefault(month, 0L)))
                .toList();
    }

    /**
     * Grouped by Plant rather than Department: certificates don't carry a department field
     * (only Plant/Category), so this is the closest real grouping available until a dedicated
     * department concept is introduced (e.g. alongside Employee Compliance).
     */
    @Transactional(readOnly = true)
    public List<ChartPoint> getPlantWiseCompliance() {
        List<Certificate> certificates = certificatesForCurrentCompany();
        Map<UUID, Plant> plantsById = plantRepository.findAllByCompanyId(companyContextProvider.getCurrentCompanyId())
                .stream()
                .collect(Collectors.toMap(Plant::getId, p -> p));

        Map<String, Long> counts = new LinkedHashMap<>();
        for (Certificate certificate : certificates) {
            String label = plantLabel(certificate.getPlantId(), plantsById);
            counts.merge(label, 1L, Long::sum);
        }

        return counts.entrySet().stream()
                .map(e -> new ChartPoint(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(ChartPoint::label))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChartPoint> getCategoryDistribution() {
        List<Certificate> certificates = certificatesForCurrentCompany();
        Map<UUID, CertificateCategory> categoriesById = categoryRepository
                .findAllByCompanyId(companyContextProvider.getCurrentCompanyId()).stream()
                .collect(Collectors.toMap(CertificateCategory::getId, c -> c));

        Map<String, Long> counts = new LinkedHashMap<>();
        for (Certificate certificate : certificates) {
            String label = categoryLabel(certificate.getCategoryId(), categoriesById);
            counts.merge(label, 1L, Long::sum);
        }

        return counts.entrySet().stream()
                .map(e -> new ChartPoint(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(ChartPoint::value).reversed())
                .toList();
    }

    private String plantLabel(UUID plantId, Map<UUID, Plant> plantsById) {
        if (plantId == null) {
            return "Unassigned";
        }
        Plant plant = plantsById.get(plantId);
        return plant != null ? plant.getName() : "Unknown Plant";
    }

    private String categoryLabel(UUID categoryId, Map<UUID, CertificateCategory> categoriesById) {
        if (categoryId == null) {
            return "Uncategorized";
        }
        CertificateCategory category = categoriesById.get(categoryId);
        return category != null ? category.getName() : "Unknown Category";
    }

    private List<Certificate> certificatesForCurrentCompany() {
        return certificateRepository.findAllByCompanyId(companyContextProvider.getCurrentCompanyId());
    }

    private boolean isWithin30Days(ExpiryBucket bucket) {
        return bucket == ExpiryBucket.DAYS_30 || bucket == ExpiryBucket.DAYS_15
                || bucket == ExpiryBucket.DAYS_7 || bucket == ExpiryBucket.DAYS_3
                || bucket == ExpiryBucket.DAYS_0;
    }
}
