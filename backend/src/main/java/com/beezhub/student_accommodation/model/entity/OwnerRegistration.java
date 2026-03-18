package com.beezhub.student_accommodation.model.entity;

import com.beezhub.student_accommodation.model.enums.OwnerRegistrationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Pre-approval registration request from a prospective building owner.
 * No app_user exists until admin approves this record.
 */
@Entity
@Table(name = "owner_registration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "organisation_name", nullable = false, length = 200)
    private String organisationName;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OwnerRegistrationStatus status = OwnerRegistrationStatus.PENDING;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;

    @Column(name = "email_verification_token", length = 255)
    private String emailVerificationToken;

    @Column(name = "email_verification_expires_at")
    private LocalDateTime emailVerificationExpiresAt;

    @Column(name = "password_setup_token", length = 255)
    private String passwordSetupToken;

    @Column(name = "password_setup_expires_at")
    private LocalDateTime passwordSetupExpiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
