package com.beezhub.student_accommodation.model.entity;

import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "application")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    private Long version;
    @Column(unique = true, nullable = false, name = "application_code")
    private String applicationCode;
    @Column(name = "applicant_date")
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

    @Column(name = "preferred_accommodation_type", length = 50)
    private String preferredAccommodationType;

    @Column(name = "preferred_move_in_date")
    private LocalDate preferredMoveInDate;

    @Column(name = "preferred_duration_months")
    private Integer preferredDurationMonths;

    @Column(name = "preferred_room_type", length = 50)
    private String preferredRoomType;

    @Column(name = "special_requirements", length = 500)
    private String specialRequirements;

    @Column(name = "institution_name", length = 150)
    private String institutionName;

    @Column(name = "course_name", length = 150)
    private String courseName;

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
