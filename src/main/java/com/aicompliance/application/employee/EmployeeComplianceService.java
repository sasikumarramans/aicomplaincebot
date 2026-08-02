package com.aicompliance.application.employee;

import com.aicompliance.application.audit.AuditLogRecorder;
import com.aicompliance.application.port.CompanyContextProvider;
import com.aicompliance.application.port.EmployeeCertificationRepository;
import com.aicompliance.application.port.EmployeeRepository;
import com.aicompliance.application.port.FileStorageService;
import com.aicompliance.domain.employee.CertificationType;
import com.aicompliance.domain.employee.Employee;
import com.aicompliance.domain.employee.EmployeeCertification;
import com.aicompliance.domain.shared.EntityNotFoundException;
import com.aicompliance.domain.shared.ExpiryBucket;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeComplianceService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeCertificationRepository certificationRepository;
    private final FileStorageService fileStorageService;
    private final CompanyContextProvider companyContextProvider;
    private final AuditLogRecorder auditLogRecorder;

    public EmployeeComplianceService(EmployeeRepository employeeRepository,
            EmployeeCertificationRepository certificationRepository, FileStorageService fileStorageService,
            CompanyContextProvider companyContextProvider, AuditLogRecorder auditLogRecorder) {
        this.employeeRepository = employeeRepository;
        this.certificationRepository = certificationRepository;
        this.fileStorageService = fileStorageService;
        this.companyContextProvider = companyContextProvider;
        this.auditLogRecorder = auditLogRecorder;
    }

    public record UploadCertificationCommand(
            UUID employeeId,
            CertificationType certificationType,
            LocalDate issueDate,
            LocalDate expiryDate,
            InputStream fileContent,
            long fileSizeBytes,
            String originalFileName,
            String mimeType) {
    }

    @Transactional
    public EmployeeCertification uploadCertification(UploadCertificationCommand command) {
        Employee employee = employeeRepository.findById(command.employeeId())
                .orElseThrow(() -> new EntityNotFoundException("Employee", command.employeeId()));
        employee = companyContextProvider.requireOwnership(employee, "Employee", command.employeeId());

        UUID companyId = companyContextProvider.getCurrentCompanyId();
        String storageKey = "companies/%s/employees/%s/certifications/%s/%s".formatted(
                companyId, employee.getId(), command.certificationType(), command.originalFileName());
        fileStorageService.upload(storageKey, command.fileContent(), command.fileSizeBytes(), command.mimeType());

        EmployeeCertification certification = new EmployeeCertification();
        certification.setCompanyId(companyId);
        certification.setEmployeeId(employee.getId());
        certification.setCertificationType(command.certificationType());
        certification.setFileStorageKey(storageKey);
        certification.setOriginalFileName(command.originalFileName());
        certification.setMimeType(command.mimeType());
        certification.setIssueDate(command.issueDate());
        certification.setExpiryDate(command.expiryDate());
        certification.setExpiryBucket(ExpiryBucket.fromExpiryDate(command.expiryDate()));
        certification.setUploadedByUserId(companyContextProvider.getCurrentUserId());
        certification = certificationRepository.save(certification);

        auditLogRecorder.record("EMPLOYEE_CERTIFICATION_UPLOADED", "EmployeeCertification", certification.getId());
        return certification;
    }

    @Transactional(readOnly = true)
    public List<EmployeeCertification> listForEmployee(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employee", employeeId));
        companyContextProvider.requireOwnership(employee, "Employee", employeeId);
        return certificationRepository.findAllByEmployeeId(employeeId);
    }

    @Transactional(readOnly = true)
    public EmployeeCertification getById(UUID id) {
        EmployeeCertification certification = certificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("EmployeeCertification", id));
        return companyContextProvider.requireOwnership(certification, "EmployeeCertification", id);
    }

    @Transactional(readOnly = true)
    public URL generateDownloadUrl(UUID certificationId) {
        EmployeeCertification certification = getById(certificationId);
        return fileStorageService.generatePresignedDownloadUrl(certification.getFileStorageKey(),
                Duration.ofMinutes(15));
    }
}
