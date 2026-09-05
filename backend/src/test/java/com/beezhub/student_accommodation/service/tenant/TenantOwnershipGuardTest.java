package com.beezhub.student_accommodation.service.tenant;

import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.model.entity.Building;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantOwnershipGuardTest {

    private static final long TENANT_A = 5L;
    private static final long TENANT_B = 6L;

    private final TenantOwnershipGuard guard = new TenantOwnershipGuard();

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private static Building buildingOwnedBy(Long tenantId) {
        Building building = new Building();
        building.setId(42L);
        building.setTenantId(tenantId);
        return building;
    }

    @Test
    void assertOwned_sameTenant_returnsEntity() {
        TenantContext.setTenantId(TENANT_A);
        Building building = buildingOwnedBy(TENANT_A);

        assertThat(guard.assertOwned(building, "Building", 42L)).isSameAs(building);
    }

    @Test
    void assertOwned_otherTenant_throwsNotFoundSoIdsCannotBeProbed() {
        TenantContext.setTenantId(TENANT_A);

        assertThatThrownBy(() -> guard.assertOwned(buildingOwnedBy(TENANT_B), "Building", 42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Building not found with id: 42");
    }

    @Test
    void assertOwned_platformRow_refusedForTenantAdmin() {
        TenantContext.setTenantId(TENANT_A);

        assertThatThrownBy(() -> guard.assertOwned(buildingOwnedBy(TenantContext.PLATFORM_TENANT_ID), "Building", 42L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void assertReadable_platformRow_allowed() {
        TenantContext.setTenantId(TENANT_A);
        Building platformRow = buildingOwnedBy(TenantContext.PLATFORM_TENANT_ID);

        assertThat(guard.assertReadable(platformRow, "Building", 42L)).isSameAs(platformRow);
    }

    @Test
    void assertReadable_otherTenant_stillRefused() {
        TenantContext.setTenantId(TENANT_A);

        assertThatThrownBy(() -> guard.assertReadable(buildingOwnedBy(TENANT_B), "Building", 42L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void platformAdminContext_seesEveryTenant() {
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        Building otherTenants = buildingOwnedBy(TENANT_B);

        assertThat(guard.assertOwned(otherTenants, "Building", 42L)).isSameAs(otherTenants);
    }

    @Test
    void noTenantContext_allowsScheduledJobsAndAsyncListeners() {
        TenantContext.clear();
        Building otherTenants = buildingOwnedBy(TENANT_B);

        assertThat(guard.assertOwned(otherTenants, "Building", 42L)).isSameAs(otherTenants);
    }

    @Test
    void nullEntity_passesThroughSoCallersKeepTheirOwnNotFoundHandling() {
        TenantContext.setTenantId(TENANT_A);

        assertThatCode(() -> guard.assertOwned((Building) null, "Building", 42L)).doesNotThrowAnyException();
    }

    @Test
    void entityWithNoTenantStamp_refusedForTenantAdmin() {
        TenantContext.setTenantId(TENANT_A);

        assertThatThrownBy(() -> guard.assertOwned(buildingOwnedBy(null), "Building", 42L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
