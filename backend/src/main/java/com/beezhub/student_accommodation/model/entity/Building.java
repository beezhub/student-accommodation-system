package com.beezhub.student_accommodation.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "building")
@Data
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@EntityListeners(com.beezhub.student_accommodation.model.listener.TenantListener.class)
public class Building implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String address;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "place_id", length = 255)
    private String placeId;

    @Column(name = "formatted_address", length = 500)
    private String formattedAddress;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "address_components", columnDefinition = "jsonb")
    private Map<String, String> addressComponents = new HashMap<>();

    @Column(name = "total_floors")
    private Integer totalFloors;

    @OneToMany(mappedBy = "building", fetch = FetchType.LAZY)
    private List<Room> rooms = new ArrayList<>();

    @OneToMany(mappedBy = "building", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BuildingImage> images = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "building_institution",
        joinColumns = @JoinColumn(name = "building_id"),
        inverseJoinColumns = @JoinColumn(name = "institution_id")
    )
    private Set<Institution> institutions = new HashSet<>();

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