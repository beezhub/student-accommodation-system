package com.beezhub.student_accommodation.model.entity;

import com.beezhub.student_accommodation.model.enums.AllocationMethod;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.DeclineReason;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "room_allocation")
@Data
public class RoomAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "allocation_date", nullable = false)
    private LocalDateTime allocationDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private Application application;

    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private AllocationStatus status;

    @Column(name = "check_in_date")
    private LocalDateTime checkInDate;

    @Column(name = "check_out_date")
    private LocalDateTime checkOutDate;

    @Column(name = "expected_check_in_date")
    private LocalDateTime expectedCheckInDate;

    @Column(name = "allocation_method")
    @Enumerated(EnumType.STRING)
    private AllocationMethod allocationMethod;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id")
    private AcademicYear academicYear;

    @Column(name = "decline_reason", length = 50)
    @Enumerated(EnumType.STRING)
    private DeclineReason declineReason;

    @Column(name = "decline_reason_text", length = 500)
    private String declineReasonText;

    @Column(name = "offer_expiry_date")
    private LocalDateTime offerExpiryDate;

    @Column(name = "check_in_expiry_date")
    private LocalDateTime checkInExpiryDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (allocationDate == null) {
            allocationDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
