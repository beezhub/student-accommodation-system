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
 * A staff member invited by a {@link BuildingOwner} to manage their building(s).
 * Invitation expires after 48 hours per [REQ BO-002].
 *
 * <p>Per [ARCH 5.1.1 rule 9]: tenant-scoped via Hibernate tenantFilter.</p>
 */
@Entity
@Table(name = "building_staff",
        uniqueConstraints = @UniqueConstraint(name = "uq_staff_user_owner",
                columnNames = {"user_id", "owner_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@jakarta.persistence.EntityListeners(com.beezhub.student_accommodation.model.listener.TenantListener.class)
public class BuildingStaff implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private BuildingOwner owner;

    /**
     * FULL = all management operations.
     * RESTRICTED = view and limited operations (per-permission granularity is a future enhancement).
     */
    @Column(name = "permission_set", nullable = false, length = 20)
    private String permissionSet;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "invited_at", nullable = false, updatable = false)
    private LocalDateTime invitedAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    /**
     * Staff invitation expires after 48 hours per [REQ BO-002].
     */
    @Column(name = "invitation_expires_at")
    private LocalDateTime invitationExpiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    @PrePersist
    protected void onCreate() {
        invitedAt = LocalDateTime.now();
    }
}
