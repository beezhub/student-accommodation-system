package com.beezhub.student_accommodation.model.entity;

import com.beezhub.student_accommodation.model.enums.AllocationMethod;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.DeclineReason;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_allocation")
@Data
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@EntityListeners(com.beezhub.student_accommodation.model.listener.TenantListener.class)
public class RoomAllocation implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    /**
     * GH-196: accept and the hourly offer-expiry sweep could interleave, leaving an ACCEPTED
     * allocation on a room whose occupancy had already been released — an undercount that lets the
     * room be over-allocated. Enforcing the deadline on the accept path (GH-139) shrank that window
     * to milliseconds but did not close it, because the accept path reads through a plain JOIN
     * FETCH with no lock mode.
     *
     * <p>With a version, the second writer's UPDATE matches no row and fails instead of overwriting.
     * Preferred to a pessimistic read: the accept path is student-facing and should be told to
     * reload rather than made to wait on a lock held by a batch job.
     */
    @Version
    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long version = 0L;

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

    /** When the stay actually ended: check-out, or the offer or check-in lapsing. */
    @Column(name = "end_date")
    private LocalDateTime endDate;

    /**
     * Planned move-in, copied from the intake when the allocation is made (GH-496). Capacity is
     * counted by overlapping planned stays, so a 2027 offer does not take a 2026 bed.
     */
    @Column(name = "stay_start_date")
    private LocalDate stayStartDate;

    /** Planned check-out: the bed is free again from this date. */
    @Column(name = "stay_end_date")
    private LocalDate stayEndDate;

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

    @Column(name = "reminder_24h_sent", nullable = false)
    private boolean reminder24hSent = false;

    @Column(name = "check_in_expiry_date")
    private LocalDateTime checkInExpiryDate;

    @Column(name = "checkin_reminder_24h_sent", nullable = false)
    private boolean checkInReminder24hSent = false;

    /** When the offer was accepted (GH-496). Empty for places accepted before it was recorded. */
    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    /** When the student was first asked "Still coming?" before move-in. */
    @Column(name = "reconfirm_requested_at")
    private LocalDateTime reconfirmRequestedAt;

    /** When the last reconfirmation reminder was sent. */
    @Column(name = "reconfirm_reminded_at")
    private LocalDateTime reconfirmRemindedAt;

    /** When the student confirmed they are still coming. A confirmed place keeps its bed. */
    @Column(name = "reconfirmed_at")
    private LocalDateTime reconfirmedAt;

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
        if (allocationDate == null) {
            allocationDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
