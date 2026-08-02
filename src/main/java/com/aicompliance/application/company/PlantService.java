package com.aicompliance.application.company;

import com.aicompliance.application.port.CompanyContextProvider;
import com.aicompliance.application.port.PlantRepository;
import com.aicompliance.domain.company.Plant;
import com.aicompliance.domain.shared.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlantService {

    private final PlantRepository plantRepository;
    private final CompanyContextProvider companyContextProvider;

    public PlantService(PlantRepository plantRepository, CompanyContextProvider companyContextProvider) {
        this.plantRepository = plantRepository;
        this.companyContextProvider = companyContextProvider;
    }

    public record CreatePlantCommand(String name, String address, String gstNumber, String plantType,
            String contactPerson) {
    }

    @Transactional
    public Plant createPlant(CreatePlantCommand command) {
        Plant plant = new Plant();
        plant.setCompanyId(companyContextProvider.getCurrentCompanyId());
        plant.setName(command.name());
        plant.setAddress(command.address());
        plant.setGstNumber(command.gstNumber());
        plant.setPlantType(command.plantType());
        plant.setContactPerson(command.contactPerson());
        return plantRepository.save(plant);
    }

    @Transactional(readOnly = true)
    public List<Plant> listForCurrentCompany() {
        return plantRepository.findAllByCompanyId(companyContextProvider.getCurrentCompanyId());
    }

    @Transactional(readOnly = true)
    public Plant getById(UUID id) {
        Plant plant = plantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plant", id));
        return companyContextProvider.requireOwnership(plant, "Plant", id);
    }
}
