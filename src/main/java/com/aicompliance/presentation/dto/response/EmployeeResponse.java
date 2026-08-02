package com.aicompliance.presentation.dto.response;

import com.aicompliance.domain.employee.Employee;
import java.util.UUID;

public record EmployeeResponse(
        UUID id,
        UUID plantId,
        String employeeCode,
        String fullName,
        String department,
        String designation,
        String email,
        boolean active) {

    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(employee.getId(), employee.getPlantId(), employee.getEmployeeCode(),
                employee.getFullName(), employee.getDepartment(), employee.getDesignation(), employee.getEmail(),
                employee.isActive());
    }
}
