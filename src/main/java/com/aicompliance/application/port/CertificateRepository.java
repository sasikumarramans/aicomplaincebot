package com.aicompliance.application.port;

import com.aicompliance.domain.certificate.Certificate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

    List<Certificate> findAllByCompanyId(UUID companyId);

    List<Certificate> findAllByCompanyIdAndCategoryId(UUID companyId, UUID categoryId);

    List<Certificate> findAllByCompanyIdAndPlantId(UUID companyId, UUID plantId);

    long countByCompanyId(UUID companyId);
}
