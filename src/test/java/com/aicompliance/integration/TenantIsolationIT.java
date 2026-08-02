package com.aicompliance.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aicompliance.application.company.CompanyService;
import com.aicompliance.application.company.PlantService;
import com.aicompliance.application.port.CompanyContextProvider;
import com.aicompliance.domain.company.Company;
import com.aicompliance.domain.company.Plant;
import com.aicompliance.domain.shared.EntityNotFoundException;
import com.aicompliance.domain.user.Role;
import com.aicompliance.infrastructure.security.AuthenticatedPrincipal;
import com.aicompliance.infrastructure.security.JwtAuthenticationToken;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Proves that a company can never read another company's tenant-scoped data, including via
 * direct-by-id lookups, which is the case most likely to leak if the Hibernate tenant filter
 * were ever accidentally bypassed.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class TenantIsolationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private CompanyService companyService;

    @Autowired
    private PlantService plantService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void companyCannotListOrFetchAnotherCompanysPlants() {
        actAsSuperAdmin();
        Company companyA = companyService.createCompany(new CompanyService.CreateCompanyCommand(
                "Tenant A", null, null, null, null, null));
        Company companyB = companyService.createCompany(new CompanyService.CreateCompanyCommand(
                "Tenant B", null, null, null, null, null));

        actAsCompanyAdmin(companyA.getId());
        Plant plantA = plantService.createPlant(new PlantService.CreatePlantCommand(
                "Plant A", "Address A", null, "Factory", null));

        actAsCompanyAdmin(companyB.getId());
        Plant plantB = plantService.createPlant(new PlantService.CreatePlantCommand(
                "Plant B", "Address B", null, "Factory", null));

        // Tenant B must not see tenant A's plant in its list.
        List<Plant> tenantBPlants = plantService.listForCurrentCompany();
        assertThat(tenantBPlants).extracting(Plant::getId).containsExactly(plantB.getId());

        // Tenant B must not be able to fetch tenant A's plant by id either.
        assertThatThrownBy(() -> plantService.getById(plantA.getId()))
                .isInstanceOf(EntityNotFoundException.class);

        // Switch back to tenant A and confirm the symmetric case.
        actAsCompanyAdmin(companyA.getId());
        List<Plant> tenantAPlants = plantService.listForCurrentCompany();
        assertThat(tenantAPlants).extracting(Plant::getId).containsExactly(plantA.getId());
        assertThatThrownBy(() -> plantService.getById(plantB.getId()))
                .isInstanceOf(EntityNotFoundException.class);

        // Tenant A can still fetch its own plant.
        assertThat(plantService.getById(plantA.getId()).getId()).isEqualTo(plantA.getId());
    }

    @Test
    void superAdminCanSeeAcrossCompanies() {
        actAsSuperAdmin();
        Company companyA = companyService.createCompany(new CompanyService.CreateCompanyCommand(
                "Tenant C", null, null, null, null, null));
        Company companyB = companyService.createCompany(new CompanyService.CreateCompanyCommand(
                "Tenant D", null, null, null, null, null));

        List<Company> all = companyService.listAll();
        assertThat(all).extracting(Company::getId).contains(companyA.getId(), companyB.getId());
    }

    private void actAsSuperAdmin() {
        setPrincipal(new AuthenticatedPrincipal(UUID.randomUUID(), null, Role.SUPER_ADMIN));
    }

    private void actAsCompanyAdmin(UUID companyId) {
        setPrincipal(new AuthenticatedPrincipal(UUID.randomUUID(), companyId, Role.COMPANY_ADMIN));
    }

    private void setPrincipal(AuthenticatedPrincipal principal) {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(principal));
    }
}
