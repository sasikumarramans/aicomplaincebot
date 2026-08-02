package com.aicompliance.presentation.controller;

import com.aicompliance.application.company.SignupRequestService;
import com.aicompliance.domain.company.CompanySignupRequest;
import com.aicompliance.presentation.dto.request.CreateSignupRequestRequest;
import com.aicompliance.presentation.dto.request.RejectSignupRequestRequest;
import com.aicompliance.presentation.dto.response.SignupRequestResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST is public (a prospective company has no account yet); GET/approve/reject are
 * SUPER_ADMIN-only via method-level checks, since this path is not covered by the
 * /api/v1/companies/** URL-pattern rule in SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/signup-requests")
public class SignupRequestController {

    private final SignupRequestService signupRequestService;

    public SignupRequestController(SignupRequestService signupRequestService) {
        this.signupRequestService = signupRequestService;
    }

    @PostMapping
    public ResponseEntity<SignupRequestResponse> submit(@Valid @RequestBody CreateSignupRequestRequest request) {
        CompanySignupRequest created = signupRequestService.submit(new SignupRequestService.CreateSignupRequestCommand(
                request.companyName(), request.industryId(), request.registeredAddress(), request.gstNumber(),
                request.adminFullName(), request.adminEmail(), request.phoneNumber(), request.message()));
        return ResponseEntity.status(HttpStatus.CREATED).body(SignupRequestResponse.from(created));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<SignupRequestResponse>> list() {
        List<SignupRequestResponse> requests = signupRequestService.listAll().stream()
                .map(SignupRequestResponse::from)
                .toList();
        return ResponseEntity.ok(requests);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SignupRequestResponse> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(SignupRequestResponse.from(signupRequestService.approve(id)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SignupRequestResponse> reject(@PathVariable UUID id,
            @RequestBody(required = false) RejectSignupRequestRequest request) {
        String reason = request != null ? request.reason() : null;
        return ResponseEntity.ok(SignupRequestResponse.from(signupRequestService.reject(id, reason)));
    }
}
