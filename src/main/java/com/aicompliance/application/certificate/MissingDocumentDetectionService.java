package com.aicompliance.application.certificate;

import com.aicompliance.application.port.CertificateCategoryRepository;
import com.aicompliance.application.port.CertificateRepository;
import com.aicompliance.application.port.CompanyContextProvider;
import com.aicompliance.application.port.FacilityTypeRequiredCategoryRepository;
import com.aicompliance.application.port.PlantRepository;
import com.aicompliance.domain.certificate.Certificate;
import com.aicompliance.domain.certificate.CertificateCategory;
import com.aicompliance.domain.certificate.CertificateStatus;
import com.aicompliance.domain.certificate.FacilityTypeRequiredCategory;
import com.aicompliance.domain.company.Plant;
import com.aicompliance.domain.shared.ExpiryBucket;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Purely relational - compares each plant's plantType against the company's own
 * FacilityTypeRequiredCategory checklist and its actual valid certificates. No AI involved: this
 * is a straightforward set-difference, not a judgment call.
 */
@Service
public class MissingDocumentDetectionService {

    private final PlantRepository plantRepository;
    private final CertificateRepository certificateRepository;
    private final CertificateCategoryRepository categoryRepository;
    private final FacilityTypeRequiredCategoryRepository requiredCategoryRepository;
    private final CompanyContextProvider companyContextProvider;

    public MissingDocumentDetectionService(PlantRepository plantRepository,
            CertificateRepository certificateRepository, CertificateCategoryRepository categoryRepository,
            FacilityTypeRequiredCategoryRepository requiredCategoryRepository,
            CompanyContextProvider companyContextProvider) {
        this.plantRepository = plantRepository;
        this.certificateRepository = certificateRepository;
        this.categoryRepository = categoryRepository;
        this.requiredCategoryRepository = requiredCategoryRepository;
        this.companyContextProvider = companyContextProvider;
    }

    public record MissingDocument(UUID plantId, String plantName, UUID categoryId, String categoryName) {
    }

    @Transactional(readOnly = true)
    public List<MissingDocument> findMissingDocuments() {
        UUID companyId = companyContextProvider.getCurrentCompanyId();
        List<Plant> plants = plantRepository.findAllByCompanyId(companyId);
        List<Certificate> certificates = certificateRepository.findAllByCompanyId(companyId);
        List<FacilityTypeRequiredCategory> requirements = requiredCategoryRepository.findAllByCompanyId(companyId);
        Map<UUID, CertificateCategory> categoriesById = categoryRepository.findAllByCompanyId(companyId)
                .stream()
                .collect(Collectors.toMap(CertificateCategory::getId, c -> c));

        return plants.stream()
                .filter(plant -> plant.getPlantType() != null)
                .flatMap(plant -> missingForPlant(plant, requirements, certificates, categoriesById).stream())
                .toList();
    }

    private List<MissingDocument> missingForPlant(Plant plant, List<FacilityTypeRequiredCategory> requirements,
            List<Certificate> allCertificates, Map<UUID, CertificateCategory> categoriesById) {

        Set<UUID> requiredCategoryIds = requirements.stream()
                .filter(r -> r.getFacilityType().equalsIgnoreCase(plant.getPlantType()))
                .map(FacilityTypeRequiredCategory::getCategoryId)
                .collect(Collectors.toSet());

        if (requiredCategoryIds.isEmpty()) {
            return List.of();
        }

        Set<UUID> validCategoryIdsForPlant = allCertificates.stream()
                .filter(c -> plant.getId().equals(c.getPlantId()))
                .filter(this::isValid)
                .map(Certificate::getCategoryId)
                .collect(Collectors.toSet());

        return requiredCategoryIds.stream()
                .filter(categoryId -> !validCategoryIdsForPlant.contains(categoryId))
                .map(categoryId -> new MissingDocument(plant.getId(), plant.getName(), categoryId,
                        categoryName(categoryId, categoriesById)))
                .toList();
    }

    private boolean isValid(Certificate certificate) {
        return certificate.getStatus() == CertificateStatus.APPROVED
                && certificate.getExpiryBucket() != ExpiryBucket.EXPIRED;
    }

    private String categoryName(UUID categoryId, Map<UUID, CertificateCategory> categoriesById) {
        CertificateCategory category = categoriesById.get(categoryId);
        return category != null ? category.getName() : "Unknown Category";
    }
}
