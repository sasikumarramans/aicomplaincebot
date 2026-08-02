package com.aicompliance.presentation.controller;

import com.aicompliance.application.company.PlantService;
import com.aicompliance.domain.company.Plant;
import com.aicompliance.presentation.dto.request.CreatePlantRequest;
import com.aicompliance.presentation.dto.response.PlantResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plants")
public class PlantController {

    private final PlantService plantService;

    public PlantController(PlantService plantService) {
        this.plantService = plantService;
    }

    @PostMapping
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<PlantResponse> create(@Valid @RequestBody CreatePlantRequest request) {
        Plant plant = plantService.createPlant(new PlantService.CreatePlantCommand(
                request.name(), request.address(), request.gstNumber(), request.plantType(),
                request.contactPerson()));
        return ResponseEntity.status(HttpStatus.CREATED).body(PlantResponse.from(plant));
    }

    @GetMapping
    public ResponseEntity<List<PlantResponse>> list() {
        List<PlantResponse> plants = plantService.listForCurrentCompany().stream()
                .map(PlantResponse::from)
                .toList();
        return ResponseEntity.ok(plants);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlantResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(PlantResponse.from(plantService.getById(id)));
    }
}
