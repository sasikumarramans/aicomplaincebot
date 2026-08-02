package com.aicompliance.presentation.controller;

import com.aicompliance.application.user.UserService;
import com.aicompliance.domain.user.User;
import com.aicompliance.presentation.dto.request.CreateUserRequest;
import com.aicompliance.presentation.dto.response.UserResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(new UserService.CreateUserCommand(
                request.email(), request.password(), request.fullName(), request.role(), request.plantId(),
                request.department(), request.isTemporary(), request.accessExpiresAt()));
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'COMPLIANCE_MANAGER', 'DEPARTMENT_HEAD')")
    public ResponseEntity<List<UserResponse>> list() {
        List<UserResponse> users = userService.listForCurrentCompany().stream()
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}/active")
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<UserResponse> setActive(@PathVariable UUID id, @RequestBody Boolean active) {
        return ResponseEntity.ok(UserResponse.from(userService.setActive(id, Boolean.TRUE.equals(active))));
    }
}
