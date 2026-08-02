package com.aicompliance.application.port;

import com.aicompliance.domain.employee.EmployeeCertification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeCertificationRepository extends JpaRepository<EmployeeCertification, UUID> {

    List<EmployeeCertification> findAllByCompanyId(UUID companyId);

    List<EmployeeCertification> findAllByEmployeeId(UUID employeeId);
}
