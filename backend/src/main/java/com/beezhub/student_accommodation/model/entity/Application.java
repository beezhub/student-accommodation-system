package com.beezhub.student_accommodation.model.entity;

import com.beezhub.student_accommodation.model.enums.AllocationMethod;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "application")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@EntityListeners(com.beezhub.student_accommodation.model.listener.TenantListener.class)
public class Application implements TenantAware {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Version
    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    @Builder.Default
    private Long version = 0L;
    @Column(unique = true, nullable = false, name = "application_code")
    private String applicationCode;
    @Column(name = "application_date")
    private LocalDate applicationDate;
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;
    @Column(name = "remarks")
    private String remarks;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "student_id", referencedColumnName = "id", nullable = false)
    private Student student;

    @Column(name = "requires_accessibility")
    private Boolean requiresAccessibility = false;

    @Column(name = "special_requirements", length = 500)
    private String specialRequirements;

    @Column(name = "institution_name", length = 150)
    private String institutionName;

    @Column(name = "course_name", length = 150)
    private String courseName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_building_id")
    private Building preferredBuilding;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("preferenceOrder ASC")
    @Builder.Default
    private List<ApplicationBuildingPreference> buildingPreferences = new ArrayList<>();

    /**
     * NOT NULL in the database since GH-496 (V2026_11_10_1710), unless that migration found rows it
     * could not repair. Not marked {@code optional = false} here, so a left-over empty row still
     * loads.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id")
    private AcademicYear academicYear;

    /**
     * The intake applied for, inside {@link #academicYear} (GH-495). NOT NULL in the database since
     * GH-496, as {@link #academicYear}; new applications always carry it.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intake_id")
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Intake intake;

    /**
     * The student's institution when they applied, from their profile (GH-495). A later change of
     * institution on the profile does not move existing applications.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id")
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Institution institution;

    @Column(name = "allocation_method")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AllocationMethod allocationMethod = AllocationMethod.MANUAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, insertable = false, updatable = false)
    private Tenant tenant;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
