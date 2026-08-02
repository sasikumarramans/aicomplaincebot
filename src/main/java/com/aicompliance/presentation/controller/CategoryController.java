package com.aicompliance.presentation.controller;

import com.aicompliance.application.category.CategoryService;
import com.aicompliance.domain.certificate.CertificateCategory;
import com.aicompliance.presentation.dto.request.CreateCategoryRequest;
import com.aicompliance.presentation.dto.response.CategoryResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'COMPLIANCE_MANAGER')")
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        CertificateCategory category = categoryService.create(new CategoryService.CreateCategoryCommand(
                request.name(), request.groupLabel(), request.description()));
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryResponse.from(category));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> list() {
        List<CategoryResponse> categories = categoryService.listForCurrentCompany().stream()
                .map(CategoryResponse::from)
                .toList();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(CategoryResponse.from(categoryService.getById(id)));
    }

    @PutMapping("/{id}/active")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'COMPLIANCE_MANAGER')")
    public ResponseEntity<CategoryResponse> setActive(@PathVariable UUID id, @RequestBody Boolean active) {
        return ResponseEntity.ok(CategoryResponse.from(categoryService.setActive(id, Boolean.TRUE.equals(active))));
    }
}
