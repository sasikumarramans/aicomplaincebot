package com.aicompliance.application.certificate;

import com.aicompliance.application.category.CategoryService;
import com.aicompliance.application.port.CompanyContextProvider;
import com.aicompliance.application.port.FacilityTypeRequiredCategoryRepository;
import com.aicompliance.domain.certificate.FacilityTypeRequiredCategory;
import com.aicompliance.domain.shared.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD for the company-defined "facility type X requires category Y" checklist that
 * MissingDocumentDetectionService compares plants against. Facility type is free text matched
 * against Plant.plantType - never a hardcoded/fixed list.
 */
@Service
public class FacilityRequirementService {

    private final FacilityTypeRequiredCategoryRepository repository;
    private final CategoryService categoryService;
    private final CompanyContextProvider companyContextProvider;

    public FacilityRequirementService(FacilityTypeRequiredCategoryRepository repository,
            CategoryService categoryService, CompanyContextProvider companyContextProvider) {
        this.repository = repository;
        this.categoryService = categoryService;
        this.companyContextProvider = companyContextProvider;
    }

    @Transactional
    public FacilityTypeRequiredCategory addRequirement(String facilityType, UUID categoryId) {
        categoryService.getById(categoryId); // validates ownership/existence

        FacilityTypeRequiredCategory requirement = new FacilityTypeRequiredCategory();
        requirement.setCompanyId(companyContextProvider.getCurrentCompanyId());
        requirement.setFacilityType(facilityType);
        requirement.setCategoryId(categoryId);
        return repository.save(requirement);
    }

    @Transactional(readOnly = true)
    public List<FacilityTypeRequiredCategory> listForCurrentCompany() {
        return repository.findAllByCompanyId(companyContextProvider.getCurrentCompanyId());
    }

    @Transactional
    public void removeRequirement(UUID id) {
        FacilityTypeRequiredCategory requirement = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FacilityTypeRequiredCategory", id));
        companyContextProvider.requireOwnership(requirement, "FacilityTypeRequiredCategory", id);
        repository.delete(requirement);
    }
}
