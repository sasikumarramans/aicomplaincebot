package com.aicompliance.application.port;

import com.aicompliance.domain.certificate.CertificateCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateCategoryRepository extends JpaRepository<CertificateCategory, UUID> {

    List<CertificateCategory> findAllByCompanyId(UUID companyId);

    boolean existsByCompanyIdAndNameIgnoreCase(UUID companyId, String name);

    Optional<CertificateCategory> findByCompanyIdAndNameIgnoreCase(UUID companyId, String name);
}
