package com.aicompliance.presentation.dto.response;

import com.aicompliance.domain.company.Plant;
import java.util.UUID;

public record PlantResponse(
        UUID id,
        String name,
        String address,
        String gstNumber,
        String plantType,
        String contactPerson,
        boolean active) {

    public static PlantResponse from(Plant plant) {
        return new PlantResponse(plant.getId(), plant.getName(), plant.getAddress(), plant.getGstNumber(),
                plant.getPlantType(), plant.getContactPerson(), plant.isActive());
    }
}
