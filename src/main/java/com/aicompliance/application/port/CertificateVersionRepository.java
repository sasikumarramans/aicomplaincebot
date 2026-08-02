package com.aicompliance.application.port;

import com.aicompliance.domain.certificate.CertificateVersion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateVersionRepository extends JpaRepository<CertificateVersion, UUID> {

    List<CertificateVersion> findAllByCertificateIdOrderByVersionNumberDesc(UUID certificateId);
}
