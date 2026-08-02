package com.aicompliance.application.port;

import com.aicompliance.domain.certificate.FacilityTypeRequiredCategory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityTypeRequiredCategoryRepository extends JpaRepository<FacilityTypeRequiredCategory, UUID> {

    List<FacilityTypeRequiredCategory> findAllByCompanyId(UUID companyId);

    List<FacilityTypeRequiredCategory> findAllByCompanyIdAndFacilityType(UUID companyId, String facilityType);
}
