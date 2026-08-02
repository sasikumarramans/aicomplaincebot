package com.aicompliance.application.port;

import com.aicompliance.domain.shared.EntityNotFoundException;
import com.aicompliance.domain.shared.TenantOwnedEntity;
import com.aicompliance.domain.user.Role;
import java.util.UUID;

public interface CompanyContextProvider {

    boolean isAuthenticated();

    UUID getCurrentUserId();

    UUID getCurrentCompanyId();

    Role getCurrentRole();

    boolean isSuperAdmin();

    /**
     * Guards direct entity-by-id loads (JPA {@code EntityManager.find} / Spring Data
     * {@code findById}), which - unlike derived/JPQL queries - do not honor Hibernate's enabled
     * {@code @Filter}. Call this on every {@code TenantOwnedEntity} fetched by id before
     * returning it to a caller; treats a cross-tenant match the same as not-found so callers
     * cannot distinguish "doesn't exist" from "belongs to another company".
     */
    default <T extends TenantOwnedEntity> T requireOwnership(T entity, String entityName, UUID id) {
        if (!isSuperAdmin() && !entity.getCompanyId().equals(getCurrentCompanyId())) {
            throw new EntityNotFoundException(entityName, id);
        }
        return entity;
    }
}
