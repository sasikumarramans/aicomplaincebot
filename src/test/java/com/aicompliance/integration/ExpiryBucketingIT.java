package com.aicompliance.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.aicompliance.application.category.CategoryService;
import com.aicompliance.application.certificate.CertificateService;
import com.aicompliance.application.company.CompanyService;
import com.aicompliance.application.employee.EmployeeComplianceService;
import com.aicompliance.application.employee.EmployeeService;
import com.aicompliance.application.expiry.ExpiryBucketingService;
import com.aicompliance.application.port.CertificateRepository;
import com.aicompliance.application.port.EmployeeCertificationRepository;
import com.aicompliance.application.port.FileStorageService;
import com.aicompliance.application.port.UserRepository;
import com.aicompliance.domain.certificate.CertificateCategory;
import com.aicompliance.domain.company.Company;
import com.aicompliance.domain.employee.CertificationType;
import com.aicompliance.domain.employee.Employee;
import com.aicompliance.domain.shared.ExpiryBucket;
import com.aicompliance.domain.user.Role;
import com.aicompliance.domain.user.User;
import com.aicompliance.infrastructure.security.AuthenticatedPrincipal;
import com.aicompliance.infrastructure.security.JwtAuthenticationToken;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Proves the generalized ExpiryBucketingService recomputes both Certificate and
 * EmployeeCertification buckets in a single run (the Phase 6 Expirable-abstraction refactor),
 * running as it does in production with no authenticated context.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ExpiryBucketingIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private CompanyService companyService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeComplianceService employeeComplianceService;

    @Autowired
    private ExpiryBucketingService expiryBucketingService;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private EmployeeCertificationRepository employeeCertificationRepository;

    @Autowired
    private UserRepository userRepository;

    // Real S3/MinIO isn't available on CI runners, and this test only exercises expiry
    // bucketing, not file persistence - a mock stands in so certificateService.upload(...)
    // and employeeComplianceService.uploadCertification(...) don't need real storage.
    @MockitoBean
    private FileStorageService fileStorageService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recomputesBothCertificateAndEmployeeCertificationBuckets() throws Exception {
        setPrincipal(new AuthenticatedPrincipal(UUID.randomUUID(), null, Role.SUPER_ADMIN));
        Company company = companyService.createCompany(new CompanyService.CreateCompanyCommand(
                "Expiry Test Co", null, null, null, null, null));

        User admin = new User();
        admin.setCompanyId(company.getId());
        admin.setEmail("admin@expirytest.test");
        admin.setPasswordHash("unused");
        admin.setFullName("Expiry Test Admin");
        admin.setRole(Role.COMPANY_ADMIN);
        admin = userRepository.save(admin);

        setPrincipal(new AuthenticatedPrincipal(admin.getId(), company.getId(), Role.COMPANY_ADMIN));

        CertificateCategory category = categoryService.create(
                new CategoryService.CreateCategoryCommand("Test Category", null, null));

        var certificate = certificateService.upload(new CertificateService.UploadCertificateCommand(
                null, category.getId(), "Test Cert", null, null, null,
                LocalDate.now().plusDays(2), // stale bucket set to something wrong on purpose below
                null, false, false, false,
                new ByteArrayInputStream("x".getBytes()), 1, "f.txt", "text/plain"));

        Employee employee = employeeService.create(new EmployeeService.CreateEmployeeCommand(
                null, "E1", "Test Employee", null, null, null));
        var employeeCert = employeeComplianceService.uploadCertification(
                new EmployeeComplianceService.UploadCertificationCommand(
                        employee.getId(), CertificationType.SAFETY_TRAINING, null,
                        LocalDate.now().plusDays(10),
                        new ByteArrayInputStream("x".getBytes()), 1, "f.txt", "text/plain"));

        // Force both into a deliberately wrong bucket to prove the job actually corrects them,
        // rather than trivially passing because upload-time computation already got it right.
        certificateRepository.findById(certificate.getId()).ifPresent(c -> {
            c.setExpiryBucket(ExpiryBucket.NOT_TRACKED);
            certificateRepository.save(c);
        });
        employeeCertificationRepository.findById(employeeCert.getId()).ifPresent(ec -> {
            ec.setExpiryBucket(ExpiryBucket.NOT_TRACKED);
            employeeCertificationRepository.save(ec);
        });

        SecurityContextHolder.clearContext(); // the job itself runs unauthenticated
        int changed = expiryBucketingService.recomputeAllBuckets();

        assertThat(changed).isEqualTo(2);
        assertThat(certificateRepository.findById(certificate.getId()).orElseThrow().getExpiryBucket())
                .isEqualTo(ExpiryBucket.DAYS_3);
        assertThat(employeeCertificationRepository.findById(employeeCert.getId()).orElseThrow().getExpiryBucket())
                .isEqualTo(ExpiryBucket.DAYS_15);
    }

    private void setPrincipal(AuthenticatedPrincipal principal) {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(principal));
    }
}
