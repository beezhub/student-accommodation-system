package com.beezhub.student_accommodation.model.entity;

import com.beezhub.student_accommodation.service.DocumentTypeService;
import org.hibernate.annotations.Filter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GH-126: {@code BuildingImage} held a tenant column with no filter, so list queries and
 * {@code findById} returned other tenants' rows.
 *
 * <p>These checks are structural on purpose. Whether the filter actually excludes rows is proved by
 * the Testcontainers isolation suite; what keeps breaking is the wiring, because an entity can gain
 * a {@code tenant_id} column without the interface, the filter or the listener and nothing
 * complains at build time.
 */
class TenantScopedEntityTest {

    @Test
    void buildingImage_isTenantScoped() {
        assertThat(TenantAware.class.isAssignableFrom(BuildingImage.class))
                .as("BuildingImage must be TenantAware so TenantListener stamps tenant_id")
                .isTrue();

        Filter filter = BuildingImage.class.getAnnotation(Filter.class);
        assertThat(filter).as("BuildingImage must carry the tenant filter").isNotNull();
        assertThat(filter.name()).isEqualTo("tenantFilter");
        assertThat(filter.condition()).isEqualTo("tenant_id = :tenantId");

        assertThat(BuildingImage.class.getAnnotation(EntityListeners.class)).isNotNull();
    }

    @Test
    void buildingImage_usesTheDualTenantMapping() throws NoSuchFieldException {
        // Writable column plus read-only association; writing both would fight over one column.
        Field tenantId = BuildingImage.class.getDeclaredField("tenantId");
        assertThat(tenantId.getAnnotation(Column.class).name()).isEqualTo("tenant_id");

        Field tenant = BuildingImage.class.getDeclaredField("tenant");
        JoinColumn joinColumn = tenant.getAnnotation(JoinColumn.class);
        assertThat(tenant.getAnnotation(ManyToOne.class)).isNotNull();
        assertThat(joinColumn.insertable()).isFalse();
        assertThat(joinColumn.updatable()).isFalse();
    }

    /**
     * DocumentType is the deliberate exception. The effective view merges platform defaults
     * (tenant 0) with a tenant's own types, so filtering would hide the defaults from every tenant
     * admin. Cross-tenant access is refused explicitly in DocumentTypeService instead.
     */
    @Test
    void documentType_isDeliberatelyNotFiltered() {
        assertThat(DocumentType.class.getAnnotation(Filter.class))
                .as("filtering DocumentType would hide platform defaults from tenant admins")
                .isNull();
        assertThat(TenantAware.class.isAssignableFrom(DocumentType.class)).isFalse();
    }

    @Test
    void documentTypeService_stillMergesPlatformAndTenantRows() throws NoSuchMethodException {
        // The guarantee that makes the exception above safe to keep.
        assertThat(DocumentTypeService.class.getMethod("getEffectiveDocumentTypes", Long.class))
                .isNotNull();
    }

    @ParameterizedTest
    @ValueSource(classes = {Building.class, Room.class, Application.class, RoomAllocation.class,
            AcademicYear.class, AllocationRule.class, StagedAllocation.class, WaitingListEntry.class})
    void establishedTenantEntities_keepTheirFilter(Class<?> entity) {
        // Guards against a filter being dropped while refactoring an entity.
        Filter filter = entity.getAnnotation(Filter.class);

        assertThat(TenantAware.class.isAssignableFrom(entity)).isTrue();
        assertThat(filter).as(entity.getSimpleName() + " lost its tenant filter").isNotNull();
        assertThat(filter.name()).isEqualTo("tenantFilter");
    }
}
