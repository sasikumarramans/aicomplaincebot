package com.aicompliance.presentation.controller;

import com.aicompliance.application.certificate.FacilityRequirementService;
import com.aicompliance.application.certificate.MissingDocumentDetectionService;
import com.aicompliance.domain.certificate.FacilityTypeRequiredCategory;
import com.aicompliance.presentation.dto.request.AddFacilityRequirementRequest;
import com.aicompliance.presentation.dto.response.FacilityRequirementResponse;
import com.aicompliance.presentation.dto.response.MissingDocumentResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/facility-requirements")
public class FacilityRequirementController {

    private final FacilityRequirementService facilityRequirementService;
    private final MissingDocumentDetectionService missingDocumentDetectionService;

    public FacilityRequirementController(FacilityRequirementService facilityRequirementService,
            MissingDocumentDetectionService missingDocumentDetectionService) {
        this.facilityRequirementService = facilityRequirementService;
        this.missingDocumentDetectionService = missingDocumentDetectionService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'COMPLIANCE_MANAGER')")
    public ResponseEntity<FacilityRequirementResponse> add(@Valid @RequestBody AddFacilityRequirementRequest request) {
        FacilityTypeRequiredCategory requirement = facilityRequirementService.addRequirement(
                request.facilityType(), request.categoryId());
        return ResponseEntity.status(HttpStatus.CREATED).body(FacilityRequirementResponse.from(requirement));
    }

    @GetMapping
    public ResponseEntity<List<FacilityRequirementResponse>> list() {
        return ResponseEntity.ok(facilityRequirementService.listForCurrentCompany().stream()
                .map(FacilityRequirementResponse::from)
                .toList());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'COMPLIANCE_MANAGER')")
    public ResponseEntity<Void> remove(@PathVariable UUID id) {
        facilityRequirementService.removeRequirement(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/missing-documents")
    public ResponseEntity<List<MissingDocumentResponse>> missingDocuments() {
        return ResponseEntity.ok(missingDocumentDetectionService.findMissingDocuments().stream()
                .map(MissingDocumentResponse::from)
                .toList());
    }
}
