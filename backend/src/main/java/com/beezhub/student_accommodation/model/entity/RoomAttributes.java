package com.beezhub.student_accommodation.model.entity;

import com.beezhub.student_accommodation.model.enums.GenderRestriction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "room_attributes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomAttributes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false, unique = true)
    private Room room;

    @Column(name = "accessibility_compliant", nullable = false)
    @Builder.Default
    private Boolean accessibilityCompliant = false;

    @Column(name = "gender_restriction")
    @Enumerated(EnumType.STRING)
    private GenderRestriction genderRestriction;

    @Column(name = "min_year_of_study")
    private Integer minYearOfStudy;

    @Column(name = "max_year_of_study")
    private Integer maxYearOfStudy;

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
