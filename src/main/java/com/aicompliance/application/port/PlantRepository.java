package com.aicompliance.application.port;

import com.aicompliance.domain.company.Plant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlantRepository extends JpaRepository<Plant, UUID> {

    List<Plant> findAllByCompanyId(UUID companyId);
}
