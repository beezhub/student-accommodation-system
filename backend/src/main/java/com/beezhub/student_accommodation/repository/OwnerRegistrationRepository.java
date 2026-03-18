package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.OwnerRegistration;
import com.beezhub.student_accommodation.model.enums.OwnerRegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OwnerRegistrationRepository extends JpaRepository<OwnerRegistration, Long> {

    Optional<OwnerRegistration> findByEmailVerificationToken(String token);

    @Modifying
    @Query("""
            UPDATE OwnerRegistration r
               SET r.status = :status,
                   r.verifiedAt = :now,
                   r.emailVerificationToken = NULL,
                   r.emailVerificationExpiresAt = NULL
             WHERE r.id = :id
            """)
    int markEmailVerified(@org.springframework.data.repository.query.Param("id") Long id,
                          @org.springframework.data.repository.query.Param("now") LocalDateTime now,
                          @org.springframework.data.repository.query.Param("status") OwnerRegistrationStatus status);

    Optional<OwnerRegistration> findByPasswordSetupToken(String token);

    boolean existsByEmailAndStatusIn(String email, List<OwnerRegistrationStatus> statuses);

    List<OwnerRegistration> findByStatus(OwnerRegistrationStatus status);

    List<OwnerRegistration> findByStatusIn(List<OwnerRegistrationStatus> statuses);

    List<OwnerRegistration> findByStatusOrderByCreatedAtAsc(OwnerRegistrationStatus status);

    List<OwnerRegistration> findByEmailAndStatusIn(String email, List<OwnerRegistrationStatus> statuses);

    /** Pending registrations not yet reminded, older than the given cutoff. */
    @Query("""
            SELECT r FROM OwnerRegistration r
            WHERE r.status = :pending
              AND r.reminderSentAt IS NULL
              AND r.createdAt < :cutoff
            """)
    List<OwnerRegistration> findUnremindedPendingBefore(
            @org.springframework.data.repository.query.Param("pending") OwnerRegistrationStatus pending,
            @org.springframework.data.repository.query.Param("cutoff") LocalDateTime cutoff);
}
