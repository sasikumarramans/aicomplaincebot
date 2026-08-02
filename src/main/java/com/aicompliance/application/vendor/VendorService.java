package com.aicompliance.application.vendor;

import com.aicompliance.application.audit.AuditLogRecorder;
import com.aicompliance.application.port.CompanyContextProvider;
import com.aicompliance.application.port.VendorRepository;
import com.aicompliance.domain.shared.EntityNotFoundException;
import com.aicompliance.domain.vendor.Vendor;
import com.aicompliance.domain.vendor.VendorComplianceStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;
    private final CompanyContextProvider companyContextProvider;
    private final AuditLogRecorder auditLogRecorder;

    public VendorService(VendorRepository vendorRepository, CompanyContextProvider companyContextProvider,
            AuditLogRecorder auditLogRecorder) {
        this.vendorRepository = vendorRepository;
        this.companyContextProvider = companyContextProvider;
        this.auditLogRecorder = auditLogRecorder;
    }

    public record CreateVendorCommand(String name, String gstNumber, String panNumber, String contactEmail,
            String contactPhone) {
    }

    @Transactional
    public Vendor create(CreateVendorCommand command) {
        Vendor vendor = new Vendor();
        vendor.setCompanyId(companyContextProvider.getCurrentCompanyId());
        vendor.setName(command.name());
        vendor.setGstNumber(command.gstNumber());
        vendor.setPanNumber(command.panNumber());
        vendor.setContactEmail(command.contactEmail());
        vendor.setContactPhone(command.contactPhone());
        vendor.setComplianceStatus(VendorComplianceStatus.PENDING);
        vendor = vendorRepository.save(vendor);
        auditLogRecorder.record("VENDOR_CREATED", "Vendor", vendor.getId());
        return vendor;
    }

    @Transactional(readOnly = true)
    public List<Vendor> listForCurrentCompany() {
        return vendorRepository.findAllByCompanyId(companyContextProvider.getCurrentCompanyId());
    }

    @Transactional(readOnly = true)
    public Vendor getById(UUID id) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vendor", id));
        return companyContextProvider.requireOwnership(vendor, "Vendor", id);
    }

    @Transactional
    public Vendor setActive(UUID id, boolean active) {
        Vendor vendor = getById(id);
        vendor.setActive(active);
        return vendorRepository.save(vendor);
    }
}
