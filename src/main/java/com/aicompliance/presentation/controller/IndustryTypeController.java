package com.aicompliance.presentation.controller;

import com.aicompliance.application.company.IndustryTypeService;
import com.aicompliance.domain.company.IndustryType;
import com.aicompliance.presentation.dto.request.CreateIndustryTypeRequest;
import com.aicompliance.presentation.dto.response.IndustryTypeResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Platform-wide lookup - readable by any authenticated user (so a company admin of any
 * vertical can pick an industry at signup), writable only by SUPER_ADMIN.
 */
@RestController
@RequestMapping("/api/v1/industry-types")
public class IndustryTypeController {

    private final IndustryTypeService industryTypeService;

    public IndustryTypeController(IndustryTypeService industryTypeService) {
        this.industryTypeService = industryTypeService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<IndustryTypeResponse> create(@Valid @RequestBody CreateIndustryTypeRequest request) {
        IndustryType industryType = industryTypeService.create(request.name(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(IndustryTypeResponse.from(industryType));
    }

    @GetMapping
    public ResponseEntity<List<IndustryTypeResponse>> list() {
        List<IndustryTypeResponse> types = industryTypeService.listActive().stream()
                .map(IndustryTypeResponse::from)
                .toList();
        return ResponseEntity.ok(types);
    }
}
