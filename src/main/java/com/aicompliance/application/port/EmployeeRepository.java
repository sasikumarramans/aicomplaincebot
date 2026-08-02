package com.aicompliance.application.port;

import com.aicompliance.domain.employee.Employee;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    List<Employee> findAllByCompanyId(UUID companyId);

    long countByCompanyId(UUID companyId);
}
