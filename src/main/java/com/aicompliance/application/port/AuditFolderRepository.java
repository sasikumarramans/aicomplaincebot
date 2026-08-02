package com.aicompliance.application.port;

import com.aicompliance.domain.audit.AuditFolder;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditFolderRepository extends JpaRepository<AuditFolder, UUID> {

    List<AuditFolder> findAllByCompanyIdOrderByGeneratedAtDesc(UUID companyId);
}
