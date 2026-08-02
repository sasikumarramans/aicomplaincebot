package com.aicompliance.application.port;

import com.aicompliance.domain.compliance.RiskAssessment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID> {

    List<RiskAssessment> findAllByCompanyIdOrderByAssessedAtDesc(UUID companyId);
}
