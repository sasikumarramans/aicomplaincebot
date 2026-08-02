package com.aicompliance.infrastructure.security;

import com.aicompliance.application.port.CompanyContextProvider;
import com.aicompliance.domain.shared.InvalidStateException;
import com.aicompliance.domain.user.Role;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityCompanyContextProvider implements CompanyContextProvider {

    @Override
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof AuthenticatedPrincipal;
    }

    @Override
    public UUID getCurrentUserId() {
        return principal().userId();
    }

    @Override
    public UUID getCurrentCompanyId() {
        UUID companyId = principal().companyId();
        if (companyId == null) {
            throw new InvalidStateException("Current user has no owning company");
        }
        return companyId;
    }

    @Override
    public Role getCurrentRole() {
        return principal().role();
    }

    @Override
    public boolean isSuperAdmin() {
        return principal().role() == Role.SUPER_ADMIN;
    }

    private AuthenticatedPrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedPrincipal principal)) {
            throw new InvalidStateException("No authenticated user in the current security context");
        }
        return principal;
    }
}
