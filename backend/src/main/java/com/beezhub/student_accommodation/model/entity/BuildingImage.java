package com.beezhub.student_accommodation.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * A photograph attached to a building.
 *
 * <p>Images belong to the building's tenant. The table always carried {@code tenant_id} but the
 * entity had no filter, so {@code findById} and list queries returned other tenants' images
 * (GH-126). It now uses the standard dual mapping: a writable {@code tenantId} column that
 * {@code TenantListener} stamps, plus the read-only {@code tenant} association.
 *
 * <p>Public building browsing is unaffected: anonymous visitors and students carry no tenant or the
 * platform tenant, and the filter is inactive for both.
 */
@Entity
@Table(name = "building_image")
@Data
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@EntityListeners(com.beezhub.student_accommodation.model.listener.TenantListener.class)
public class BuildingImage implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "object_path", nullable = false, length = 500)
    private String objectPath;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "is_primary", nullable = false)
    private Boolean primary = false;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, insertable = false, updatable = false)
    private Tenant tenant;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
