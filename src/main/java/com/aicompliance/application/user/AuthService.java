package com.aicompliance.application.user;

import com.aicompliance.application.port.PasswordResetTokenRepository;
import com.aicompliance.application.port.RefreshTokenRepository;
import com.aicompliance.application.port.UserRepository;
import com.aicompliance.domain.shared.InvalidStateException;
import com.aicompliance.domain.user.PasswordResetToken;
import com.aicompliance.domain.user.RefreshToken;
import com.aicompliance.domain.user.User;
import com.aicompliance.infrastructure.config.AppProperties;
import com.aicompliance.infrastructure.security.JwtTokenProvider;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AppProperties appProperties;
    private final JavaMailSender mailSender;

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository, PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider, AppProperties appProperties, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.appProperties = appProperties;
        this.mailSender = mailSender;
    }

    public record TokenPair(String accessToken, String refreshToken, Instant refreshExpiresAt) {
    }

    @Transactional
    public TokenPair login(String email, String rawPassword) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!user.isActive() || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        if (user.isAccessExpired()) {
            throw new InvalidStateException("Temporary access has expired");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return issueTokenPair(user);
    }

    @Transactional
    public TokenPair refresh(String rawRefreshToken) {
        String hash = hash(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (!stored.isValid()) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (!user.isActive() || user.isAccessExpired()) {
            throw new InvalidStateException("User access is no longer valid");
        }

        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

        return issueTokenPair(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .ifPresent(token -> {
                    token.setRevokedAt(Instant.now());
                    refreshTokenRepository.save(token);
                });
    }

    /**
     * Always succeeds from the caller's perspective regardless of whether the email exists, to
     * avoid leaking which addresses are registered. If a matching active user is found, emails
     * them a time-limited reset link; otherwise this is a silent no-op.
     */
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmailIgnoreCase(email)
                .filter(User::isActive)
                .ifPresent(user -> {
                    String rawToken = UUID.randomUUID() + "." + UUID.randomUUID();
                    Instant expiresAt = Instant.now()
                            .plus(appProperties.getJwt().getPasswordResetTokenTtlMinutes(), ChronoUnit.MINUTES);

                    PasswordResetToken resetToken = new PasswordResetToken();
                    resetToken.setUserId(user.getId());
                    resetToken.setTokenHash(hash(rawToken));
                    resetToken.setExpiresAt(expiresAt);
                    passwordResetTokenRepository.save(resetToken);

                    sendResetEmail(user.getEmail(), rawToken);
                });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidStateException("Invalid or expired reset link"));

        if (!resetToken.isValid()) {
            throw new InvalidStateException("Invalid or expired reset link");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new InvalidStateException("Invalid or expired reset link"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);

        // Force re-authentication everywhere after a password reset.
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(user.getId());
        activeTokens.forEach(token -> token.setRevokedAt(Instant.now()));
        refreshTokenRepository.saveAll(activeTokens);
    }

    private void sendResetEmail(String toEmail, String rawToken) {
        String resetUrl = appProperties.getFrontendUrl() + "/reset-password?token=" + rawToken;
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(toEmail);
            mail.setSubject("Reset your AI Compliance Copilot password");
            mail.setText("""
                    We received a request to reset your password.

                    Reset your password using the link below (expires in %d minutes):
                    %s

                    If you didn't request this, you can safely ignore this email.
                    """.formatted(appProperties.getJwt().getPasswordResetTokenTtlMinutes(), resetUrl));
            mailSender.send(mail);
        } catch (Exception e) {
            // Password reset must not fail the request or leak whether the email send succeeded;
            // the token is already persisted, so log and let the user retry "forgot password" if
            // the email never arrives (e.g. dev environment with no SMTP configured).
            log.warn("Failed to send password reset email to {}", toEmail, e);
        }
    }

    private TokenPair issueTokenPair(User user) {
        Instant hardExpiryLimit = user.isTemporary() ? user.getAccessExpiresAt() : null;
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getCompanyId(), user.getRole(),
                hardExpiryLimit);

        String rawRefreshToken = UUID.randomUUID() + "." + UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(appProperties.getJwt().getRefreshTokenTtlDays(), ChronoUnit.DAYS);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setTokenHash(hash(rawRefreshToken));
        refreshToken.setExpiresAt(expiresAt);
        refreshTokenRepository.save(refreshToken);

        return new TokenPair(accessToken, rawRefreshToken, expiresAt);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(value.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
