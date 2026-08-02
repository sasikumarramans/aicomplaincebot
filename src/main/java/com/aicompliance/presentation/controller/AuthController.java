package com.aicompliance.presentation.controller;

import com.aicompliance.application.port.CompanyContextProvider;
import com.aicompliance.application.port.UserRepository;
import com.aicompliance.application.user.AuthService;
import com.aicompliance.domain.shared.EntityNotFoundException;
import com.aicompliance.presentation.dto.request.ForgotPasswordRequest;
import com.aicompliance.presentation.dto.request.LoginRequest;
import com.aicompliance.presentation.dto.request.RefreshRequest;
import com.aicompliance.presentation.dto.request.ResetPasswordRequest;
import com.aicompliance.presentation.dto.response.TokenResponse;
import com.aicompliance.presentation.dto.response.UserResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final CompanyContextProvider companyContextProvider;

    public AuthController(AuthService authService, UserRepository userRepository,
            CompanyContextProvider companyContextProvider) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.companyContextProvider = companyContextProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.TokenPair tokens = authService.login(request.email(), request.password());
        return ResponseEntity.ok(toResponse(tokens));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthService.TokenPair tokens = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(toResponse(tokens));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    /**
     * Sits under the permitAll() /api/v1/auth/** URL pattern, so it relies on this method-level
     * check (not the URL-pattern rule) to require a valid JWT - the JWT itself carries no name/
     * email, only userId/role/companyId, so the UI needs this to display the current user's name.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> me() {
        UUID currentUserId = companyContextProvider.getCurrentUserId();
        return userRepository.findById(currentUserId)
                .map(user -> ResponseEntity.ok(UserResponse.from(user)))
                .orElseThrow(() -> new EntityNotFoundException("User", currentUserId));
    }

    private TokenResponse toResponse(AuthService.TokenPair tokens) {
        return new TokenResponse(tokens.accessToken(), tokens.refreshToken(), tokens.refreshExpiresAt());
    }
}
