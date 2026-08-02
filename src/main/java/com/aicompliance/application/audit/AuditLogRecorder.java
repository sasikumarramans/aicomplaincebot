package com.aicompliance.application.audit;

import com.aicompliance.application.port.AuditLogRepository;
import com.aicompliance.application.port.CompanyContextProvider;
import com.aicompliance.domain.audit.AuditLog;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lightweight write hook for the general activity audit trail. Called directly by application
 * services at the point of a meaningful action (not retrofitted via AOP), per the plan's decision
 * to thread audit logging in from Phase 1 rather than bolt it on when the Audit Mode module lands.
 */
@Component
public class AuditLogRecorder {

    private final AuditLogRepository auditLogRepository;
    private final CompanyContextProvider companyContextProvider;

    public AuditLogRecorder(AuditLogRepository auditLogRepository, CompanyContextProvider companyContextProvider) {
        this.auditLogRepository = auditLogRepository;
        this.companyContextProvider = companyContextProvider;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String action, String entityType, UUID entityId) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);

        if (companyContextProvider.isAuthenticated()) {
            log.setUserId(companyContextProvider.getCurrentUserId());
            if (!companyContextProvider.isSuperAdmin()) {
                log.setCompanyId(companyContextProvider.getCurrentCompanyId());
            }
        }

        auditLogRepository.save(log);
    }
}
