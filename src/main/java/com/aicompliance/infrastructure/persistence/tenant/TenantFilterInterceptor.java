package com.aicompliance.infrastructure.persistence.tenant;

import com.aicompliance.application.port.CompanyContextProvider;
import com.aicompliance.domain.shared.TenantOwnedEntity;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Enables the {@link TenantOwnedEntity#TENANT_FILTER} Hibernate filter for the current
 * persistence context at the start of every {@code @Transactional} application service method,
 * scoped to the authenticated user's company. SUPER_ADMIN requests skip the filter entirely so
 * they can operate across companies.
 */
@Aspect
@Component
@Order(0)
public class TenantFilterInterceptor {

    private final EntityManager entityManager;
    private final CompanyContextProvider companyContextProvider;

    public TenantFilterInterceptor(EntityManager entityManager, CompanyContextProvider companyContextProvider) {
        this.entityManager = entityManager;
        this.companyContextProvider = companyContextProvider;
    }

    @Around("execution(public * com.aicompliance.application..*Service.*(..))")
    public Object enableTenantFilter(ProceedingJoinPoint joinPoint) throws Throwable {
        if (companyContextProvider.isAuthenticated() && !companyContextProvider.isSuperAdmin()) {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter(TenantOwnedEntity.TENANT_FILTER)
                    .setParameter("companyId", companyContextProvider.getCurrentCompanyId());
        }
        return joinPoint.proceed();
    }
}
