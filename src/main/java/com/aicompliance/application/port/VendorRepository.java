package com.aicompliance.application.port;

import com.aicompliance.domain.vendor.Vendor;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    List<Vendor> findAllByCompanyId(UUID companyId);
}
