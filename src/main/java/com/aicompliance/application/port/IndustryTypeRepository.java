package com.aicompliance.application.port;

import com.aicompliance.domain.company.IndustryType;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IndustryTypeRepository extends JpaRepository<IndustryType, UUID> {

    boolean existsByNameIgnoreCase(String name);
}
