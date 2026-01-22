package com.beezhub.student_accommodation.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "allocation_rules",
       uniqueConstraints = @UniqueConstraint(columnNames = {"building_id", "rule_name"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private Building building;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Column(name = "preference_match_weight", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal preferenceMatchWeight = BigDecimal.ONE;

    @Column(name = "desirability_weight", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal desirabilityWeight = BigDecimal.ONE;

    @Column(name = "accessibility_weight", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal accessibilityWeight = BigDecimal.ONE;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

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
