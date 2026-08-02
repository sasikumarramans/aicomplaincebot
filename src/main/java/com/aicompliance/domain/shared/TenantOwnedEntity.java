package com.aicompliance.domain.shared;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@MappedSuperclass
@FilterDef(
        name = TenantOwnedEntity.TENANT_FILTER,
        parameters = @ParamDef(name = "companyId", type = UUID.class))
@Filter(name = TenantOwnedEntity.TENANT_FILTER, condition = "company_id = :companyId")
public abstract class TenantOwnedEntity extends AuditableEntity {

    public static final String TENANT_FILTER = "tenantFilter";

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }
}
