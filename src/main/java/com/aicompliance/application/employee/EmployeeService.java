package com.aicompliance.application.employee;

import com.aicompliance.application.audit.AuditLogRecorder;
import com.aicompliance.application.port.CompanyContextProvider;
import com.aicompliance.application.port.EmployeeRepository;
import com.aicompliance.domain.employee.Employee;
import com.aicompliance.domain.shared.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final CompanyContextProvider companyContextProvider;
    private final AuditLogRecorder auditLogRecorder;

    public EmployeeService(EmployeeRepository employeeRepository, CompanyContextProvider companyContextProvider,
            AuditLogRecorder auditLogRecorder) {
        this.employeeRepository = employeeRepository;
        this.companyContextProvider = companyContextProvider;
        this.auditLogRecorder = auditLogRecorder;
    }

    public record CreateEmployeeCommand(UUID plantId, String employeeCode, String fullName, String department,
            String designation, String email) {
    }

    @Transactional
    public Employee create(CreateEmployeeCommand command) {
        Employee employee = new Employee();
        employee.setCompanyId(companyContextProvider.getCurrentCompanyId());
        employee.setPlantId(command.plantId());
        employee.setEmployeeCode(command.employeeCode());
        employee.setFullName(command.fullName());
        employee.setDepartment(command.department());
        employee.setDesignation(command.designation());
        employee.setEmail(command.email());
        employee = employeeRepository.save(employee);
        auditLogRecorder.record("EMPLOYEE_CREATED", "Employee", employee.getId());
        return employee;
    }

    @Transactional(readOnly = true)
    public List<Employee> listForCurrentCompany() {
        return employeeRepository.findAllByCompanyId(companyContextProvider.getCurrentCompanyId());
    }

    @Transactional(readOnly = true)
    public Employee getById(UUID id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee", id));
        return companyContextProvider.requireOwnership(employee, "Employee", id);
    }

    @Transactional
    public Employee setActive(UUID id, boolean active) {
        Employee employee = getById(id);
        employee.setActive(active);
        return employeeRepository.save(employee);
    }
}
