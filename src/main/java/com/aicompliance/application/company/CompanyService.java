package com.aicompliance.application.company;

import com.aicompliance.application.audit.AuditLogRecorder;
import com.aicompliance.application.port.CertificateRepository;
import com.aicompliance.application.port.CompanyRepository;
import com.aicompliance.application.port.EmployeeRepository;
import com.aicompliance.domain.company.Company;
import com.aicompliance.domain.company.SubscriptionStatus;
import com.aicompliance.domain.shared.EntityNotFoundException;
import com.aicompliance.presentation.dto.response.PlatformSummaryResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final IndustryTypeService industryTypeService;
    private final AuditLogRecorder auditLogRecorder;
    private final EmployeeRepository employeeRepository;
    private final CertificateRepository certificateRepository;

    public CompanyService(CompanyRepository companyRepository, IndustryTypeService industryTypeService,
            AuditLogRecorder auditLogRecorder, EmployeeRepository employeeRepository,
            CertificateRepository certificateRepository) {
        this.companyRepository = companyRepository;
        this.industryTypeService = industryTypeService;
        this.auditLogRecorder = auditLogRecorder;
        this.employeeRepository = employeeRepository;
        this.certificateRepository = certificateRepository;
    }

    public record CreateCompanyCommand(String name, String gstNumber, String panNumber, String cinNumber,
            String registeredAddress, UUID industryId) {
    }

    @Transactional
    public Company createCompany(CreateCompanyCommand command) {
        if (command.industryId() != null) {
            industryTypeService.getById(command.industryId());
        }

        Company company = new Company();
        company.setName(command.name());
        company.setGstNumber(command.gstNumber());
        company.setPanNumber(command.panNumber());
        company.setCinNumber(command.cinNumber());
        company.setRegisteredAddress(command.registeredAddress());
        company.setIndustryId(command.industryId());
        company.setSubscriptionStatus(SubscriptionStatus.TRIAL);
        company = companyRepository.save(company);
        auditLogRecorder.record("COMPANY_CREATED", "Company", company.getId());
        return company;
    }

    @Transactional(readOnly = true)
    public Company getById(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Company", id));
    }

    @Transactional(readOnly = true)
    public List<Company> listAll() {
        return companyRepository.findAll();
    }

    @Transactional
    public Company updateStatus(UUID id, SubscriptionStatus status, boolean active) {
        Company company = getById(id);
        company.setSubscriptionStatus(status);
        company.setActive(active);
        return companyRepository.save(company);
    }

    @Transactional(readOnly = true)
    public PlatformSummaryResponse getPlatformSummary() {
        List<Company> companies = companyRepository.findAll();
        long trial = companies.stream().filter(c -> c.getSubscriptionStatus() == SubscriptionStatus.TRIAL).count();
        long active = companies.stream().filter(c -> c.getSubscriptionStatus() == SubscriptionStatus.ACTIVE).count();
        long suspended = companies.stream().filter(c -> c.getSubscriptionStatus() == SubscriptionStatus.SUSPENDED).count();
        long cancelled = companies.stream().filter(c -> c.getSubscriptionStatus() == SubscriptionStatus.CANCELLED).count();

        return new PlatformSummaryResponse(
                companies.size(),
                trial,
                active,
                suspended,
                cancelled,
                employeeRepository.count(),
                certificateRepository.count());
    }
}
