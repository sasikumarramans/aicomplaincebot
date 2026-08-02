package com.aicompliance.domain.notification;

/**
 * Maps the PRD's escalation chain (Manager -> Department Head -> Compliance Officer -> Director)
 * onto the platform's fixed roles: MANAGER = COMPLIANCE_MANAGER, DEPARTMENT_HEAD =
 * DEPARTMENT_HEAD. There is no dedicated "Compliance Officer" or "Director" role, so both final
 * tiers escalate to COMPANY_ADMIN (the highest company-level role) - COMPLIANCE_OFFICER and
 * DIRECTOR are kept as distinct enum values so the escalation history stays legible even though
 * they resolve to the same recipient today.
 */
public enum EscalationLevel {
    MANAGER,
    DEPARTMENT_HEAD,
    COMPLIANCE_OFFICER,
    DIRECTOR
}
