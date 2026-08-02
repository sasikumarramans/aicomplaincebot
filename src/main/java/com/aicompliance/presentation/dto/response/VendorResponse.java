package com.aicompliance.presentation.dto.response;

import com.aicompliance.domain.vendor.Vendor;
import java.util.UUID;

public record VendorResponse(
        UUID id,
        String name,
        String gstNumber,
        String panNumber,
        String contactEmail,
        String contactPhone,
        String complianceStatus,
        boolean active) {

    public static VendorResponse from(Vendor vendor) {
        return new VendorResponse(vendor.getId(), vendor.getName(), vendor.getGstNumber(), vendor.getPanNumber(),
                vendor.getContactEmail(), vendor.getContactPhone(), vendor.getComplianceStatus().name(),
                vendor.isActive());
    }
}
