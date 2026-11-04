package com.beezhub.student_accommodation.model.entity;

import com.beezhub.student_accommodation.model.enums.GenderRestriction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    /**
     * @deprecated Gender restrictions are now managed at the room level only.
     *             This field will be removed in a future version.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    @Column(name = "gender_restriction")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private GenderRestriction genderRestriction = GenderRestriction.ANY;

    /**
     * A JSON list of text, such as {@code ["Ramp access"]}, shown on the public building page. Without
     * the type code Hibernate binds a varchar, which PostgreSQL refuses for a JSONB column, so every
     * save of this entity failed, even with this field empty.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "accessibility_features", columnDefinition = "JSONB")
    private String accessibilityFeatures;

    /**
     * @deprecated Desirability scoring has been removed from the allocation system.
     *             This field will be removed in a future version.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    @Column(name = "desirability_score")
    private Integer desirabilityScore;

    /**
     * @deprecated Gender restrictions are now managed at the room level only.
     *             This field will be removed in a future version.
     */
    @Deprecated(since = "2.0", forRemoval = true)
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
