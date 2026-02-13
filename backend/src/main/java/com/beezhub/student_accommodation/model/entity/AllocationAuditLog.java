package com.beezhub.student_accommodation.model.entity;

import com.beezhub.student_accommodation.model.enums.AllocationAuditActionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "allocation_audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocationAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_id")
    private RoomAllocation allocation;

    @Column(name = "action_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AllocationAuditActionType actionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id")
    private AppUser adminUser;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_room_id")
    private Room oldRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_room_id")
    private Room newRoom;

    @Column(name = "constraint_bypassed", length = 255)
    private String constraintBypassed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
