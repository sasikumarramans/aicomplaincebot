package com.aicompliance.application.port;

import com.aicompliance.domain.vendor.VendorDocument;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorDocumentRepository extends JpaRepository<VendorDocument, UUID> {

    List<VendorDocument> findAllByCompanyId(UUID companyId);

    List<VendorDocument> findAllByVendorId(UUID vendorId);
}
