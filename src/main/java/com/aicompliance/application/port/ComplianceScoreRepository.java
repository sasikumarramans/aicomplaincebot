package com.aicompliance.application.port;

import com.aicompliance.domain.compliance.ComplianceScore;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplianceScoreRepository extends JpaRepository<ComplianceScore, UUID> {
}
