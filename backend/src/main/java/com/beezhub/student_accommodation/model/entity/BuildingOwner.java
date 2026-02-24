package com.beezhub.student_accommodation.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;

/**
 * Profile entity for a building owner (v2.0 BUILDING_OWNER role, replacing LANDLORD).
 * Tracks onboarding status and business details for the owner.
 *
 * <p>Per [ARCH 5.1.1 rule 9]: tenant-scoped via Hibernate tenantFilter.</p>
 */
@Entity
@Table(name = "building_owner",
        uniqueConstraints = @UniqueConstraint(name = "uq_building_owner_user",
                columnNames = {"user_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@jakarta.persistence.EntityListeners(com.beezhub.student_accommodation.model.listener.TenantListener.class)
public class BuildingOwner implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "region", length = 100)
    private String region;

    /**
     * Onboarding lifecycle: PENDING → APPROVED | REJECTED → SUSPENDED
     */
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private AppUser approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
