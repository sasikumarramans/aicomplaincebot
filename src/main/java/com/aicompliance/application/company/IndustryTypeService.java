package com.aicompliance.application.company;

import com.aicompliance.application.port.IndustryTypeRepository;
import com.aicompliance.domain.company.IndustryType;
import com.aicompliance.domain.shared.EntityNotFoundException;
import com.aicompliance.domain.shared.InvalidStateException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Platform-wide lookup, not tenant-scoped: any company, in any vertical (manufacturing,
 * software, healthcare, ...), picks from - or requests additions to - the same list.
 */
@Service
public class IndustryTypeService {

    private final IndustryTypeRepository industryTypeRepository;

    public IndustryTypeService(IndustryTypeRepository industryTypeRepository) {
        this.industryTypeRepository = industryTypeRepository;
    }

    @Transactional
    public IndustryType create(String name, String description) {
        if (industryTypeRepository.existsByNameIgnoreCase(name)) {
            throw new InvalidStateException("Industry type already exists: " + name);
        }
        IndustryType industryType = new IndustryType();
        industryType.setName(name);
        industryType.setDescription(description);
        return industryTypeRepository.save(industryType);
    }

    @Transactional(readOnly = true)
    public List<IndustryType> listActive() {
        return industryTypeRepository.findAll().stream()
                .filter(IndustryType::isActive)
                .toList();
    }

    @Transactional(readOnly = true)
    public IndustryType getById(UUID id) {
        return industryTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("IndustryType", id));
    }
}
