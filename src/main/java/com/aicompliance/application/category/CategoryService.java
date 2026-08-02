package com.aicompliance.application.category;

import com.aicompliance.application.port.CertificateCategoryRepository;
import com.aicompliance.application.port.CompanyContextProvider;
import com.aicompliance.domain.certificate.CertificateCategory;
import com.aicompliance.domain.shared.EntityNotFoundException;
import com.aicompliance.domain.shared.InvalidStateException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CertificateCategoryRepository categoryRepository;
    private final CompanyContextProvider companyContextProvider;

    public CategoryService(CertificateCategoryRepository categoryRepository,
            CompanyContextProvider companyContextProvider) {
        this.categoryRepository = categoryRepository;
        this.companyContextProvider = companyContextProvider;
    }

    public record CreateCategoryCommand(String name, String groupLabel, String description) {
    }

    @Transactional
    public CertificateCategory create(CreateCategoryCommand command) {
        UUID companyId = companyContextProvider.getCurrentCompanyId();
        if (categoryRepository.existsByCompanyIdAndNameIgnoreCase(companyId, command.name())) {
            throw new InvalidStateException("Category already exists: " + command.name());
        }

        CertificateCategory category = new CertificateCategory();
        category.setCompanyId(companyId);
        category.setName(command.name());
        category.setGroupLabel(command.groupLabel());
        category.setDescription(command.description());
        return categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public List<CertificateCategory> listForCurrentCompany() {
        return categoryRepository.findAllByCompanyId(companyContextProvider.getCurrentCompanyId());
    }

    @Transactional(readOnly = true)
    public CertificateCategory getById(UUID id) {
        CertificateCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("CertificateCategory", id));
        return companyContextProvider.requireOwnership(category, "CertificateCategory", id);
    }

    @Transactional
    public CertificateCategory setActive(UUID id, boolean active) {
        CertificateCategory category = getById(id);
        category.setActive(active);
        return categoryRepository.save(category);
    }
}
