package com.aicompliance.domain.certificate;

import com.aicompliance.domain.shared.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Fully custom, per-company - never pre-seeded. A manufacturer, a hospital, and a software
 * company each define their own categories through the same generic CRUD; nothing
 * industry-specific is hardcoded here. {@code groupLabel} is a free-text grouping the company
 * chooses for itself (e.g. "Safety", "Clinical", "Security") purely for dashboard/report
 * grouping - it carries no special logic.
 */
@Entity
@Table(name = "certificate_categories")
public class CertificateCategory extends TenantOwnedEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "group_label")
    private String groupLabel;

    private String description;

    @Column(nullable = false)
    private boolean active = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroupLabel() {
        return groupLabel;
    }

    public void setGroupLabel(String groupLabel) {
        this.groupLabel = groupLabel;
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
