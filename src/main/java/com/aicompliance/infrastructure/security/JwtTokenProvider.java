package com.aicompliance.infrastructure.security;

import com.aicompliance.domain.user.Role;
import com.aicompliance.infrastructure.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final String CLAIM_COMPANY_ID = "companyId";
    private static final String CLAIM_ROLE = "role";

    private final Key signingKey;
    private final AppProperties appProperties;

    public JwtTokenProvider(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.signingKey = Keys.hmacShaKeyFor(appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, UUID companyId, Role role) {
        return generateAccessToken(userId, companyId, role, null);
    }

    /**
     * @param hardExpiryLimit if non-null (a temporary user's accessExpiresAt), caps the token's
     *                        own expiration at that instant even if it's sooner than the normal
     *                        TTL - so a temporary AUDITOR's access is enforced by the JWT itself
     *                        expiring, not just by a check at login time that a still-valid
     *                        token could outlive.
     */
    public String generateAccessToken(UUID userId, UUID companyId, Role role, Instant hardExpiryLimit) {
        Instant now = Instant.now();
        Instant expiry = now.plus(appProperties.getJwt().getAccessTokenTtlMinutes(), ChronoUnit.MINUTES);
        if (hardExpiryLimit != null && hardExpiryLimit.isBefore(expiry)) {
            expiry = hardExpiryLimit;
        }

        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_ROLE, role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry));

        if (companyId != null) {
            builder.claim(CLAIM_COMPANY_ID, companyId.toString());
        }

        return builder.signWith(signingKey).compact();
    }

    public AuthenticatedPrincipal parseAndValidate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith((javax.crypto.SecretKey) signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            Role role = Role.valueOf(claims.get(CLAIM_ROLE, String.class));
            String companyIdClaim = claims.get(CLAIM_COMPANY_ID, String.class);
            UUID companyId = companyIdClaim != null ? UUID.fromString(companyIdClaim) : null;

            return new AuthenticatedPrincipal(userId, companyId, role);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Invalid or expired token", ex);
        }
    }
}
