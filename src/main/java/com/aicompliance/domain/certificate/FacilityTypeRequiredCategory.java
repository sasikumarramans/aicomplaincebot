package com.aicompliance.domain.certificate;

import com.aicompliance.domain.shared.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Company-defined checklist: "a plant of facility type X must have a valid certificate in
 * category Y". Facility type is free text chosen by the company (e.g. "Factory", "Data Center",
 * "Clinic") - never a fixed/hardcoded list, same rule as certificate categories themselves.
 */
@Entity
@Table(name = "facility_type_required_categories")
public class FacilityTypeRequiredCategory extends TenantOwnedEntity {

    @Column(name = "facility_type", nullable = false)
    private String facilityType;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    public String getFacilityType() {
        return facilityType;
    }

    public void setFacilityType(String facilityType) {
        this.facilityType = facilityType;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }
}
