package com.aicompliance.presentation.dto.response;

import com.aicompliance.domain.employee.EmployeeCertification;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeCertificationResponse(
        UUID id,
        UUID employeeId,
        String certificationType,
        LocalDate issueDate,
        LocalDate expiryDate,
        String expiryBucket,
        String expiryColorCode) {

    public static EmployeeCertificationResponse from(EmployeeCertification certification) {
        return new EmployeeCertificationResponse(
                certification.getId(),
                certification.getEmployeeId(),
                certification.getCertificationType().name(),
                certification.getIssueDate(),
                certification.getExpiryDate(),
                certification.getExpiryBucket().name(),
                certification.getExpiryBucket().getColorCode());
    }
}
