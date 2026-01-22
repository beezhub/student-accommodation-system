package com.beezhub.student_accommodation.model.entity;

import com.beezhub.student_accommodation.model.enums.GenderRestriction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "building_attributes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildingAttributes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false, unique = true)
    private Building building;

    @Column(name = "gender_restriction")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private GenderRestriction genderRestriction = GenderRestriction.ANY;

    @Column(name = "accessibility_features", columnDefinition = "JSONB")
    private String accessibilityFeatures;

    @Column(name = "desirability_score")
    private Integer desirabilityScore;

    @Column(name = "allow_mixed_gender", nullable = false)
    @Builder.Default
    private Boolean allowMixedGender = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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
