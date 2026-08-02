package com.aicompliance.domain.company;

import com.aicompliance.domain.shared.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A company-independent, admin-managed lookup (e.g. Manufacturing, Software/IT,
 * Healthcare, Retail, ...). Not a Java enum: new industries are added as data, with no
 * code change, so the platform is never hardcoded to any one vertical.
 */
@Entity
@Table(name = "industry_types")
public class IndustryType extends AuditableEntity {

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Column(nullable = false)
    private boolean active = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
