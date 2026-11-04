package com.beezhub.student_accommodation.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.beezhub.student_accommodation.AbstractJpaTest;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.BuildingAttributes;
import com.beezhub.student_accommodation.model.entity.Tenant;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** accessibility_features is a JSONB column; a save has to reach it on a real PostgreSQL. */
class BuildingAttributesPersistenceTest extends AbstractJpaTest {

    @Autowired private BuildingAttributesRepository attributesRepository;
    @PersistenceContext private EntityManager entityManager;

    private Building building() {
        Tenant tenant = new Tenant();
        tenant.setName("Company A");
        entityManager.persist(tenant);
        entityManager.flush();
        Building building = new Building();
        building.setName("Hall A");
        building.setTenantId(tenant.getId());
        entityManager.persist(building);
        entityManager.flush();
        return building;
    }

    @Test
    void save_accessibilityFeaturesList_reachesTheJsonbColumn() {
        BuildingAttributes saved = attributesRepository.saveAndFlush(BuildingAttributes.builder()
                .building(building())
                .accessibilityFeatures("[\"Ramp access\",\"Lift\"]")
                .build());
        entityManager.clear();

        assertThat(attributesRepository.findById(saved.getId()).orElseThrow().getAccessibilityFeatures())
                .contains("Ramp access");
    }

    @Test
    void save_noAccessibilityFeatures_reachesTheJsonbColumn() {
        BuildingAttributes saved = attributesRepository.saveAndFlush(BuildingAttributes.builder()
                .building(building())
                .build());

        assertThat(saved.getId()).isNotNull();
    }
}
