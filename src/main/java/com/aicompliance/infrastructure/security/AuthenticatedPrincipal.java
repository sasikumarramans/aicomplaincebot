package com.aicompliance.infrastructure.security;

import com.aicompliance.domain.user.Role;
import java.util.UUID;

public record AuthenticatedPrincipal(UUID userId, UUID companyId, Role role) {
}
