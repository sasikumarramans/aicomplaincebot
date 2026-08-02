package com.aicompliance.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateEmployeeRequest(
        UUID plantId,
        String employeeCode,
        @NotBlank String fullName,
        String department,
        String designation,
        String email) {
}
