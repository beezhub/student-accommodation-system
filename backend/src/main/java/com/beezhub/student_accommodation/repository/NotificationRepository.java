package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.Notification;
import com.beezhub.student_accommodation.model.enums.NotificationStatus;
import com.beezhub.student_accommodation.model.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByStatus(NotificationStatus status);

    List<Notification> findByRecipient(String recipient);

    List<Notification> findByUser_Id(Long userId);

    List<Notification> findByType(NotificationType type);

    /**
     * Rows the retry job should look at.
     *
     * <p>Three things here are deliberate and each fixes part of GH-141.
     *
     * <p>RETRYING is in scope because the job used to commit that status before discovering it
     * could not rebuild the email, and the old query looked only at FAILED, so those rows were
     * never scanned again and sat there forever.
     *
     * <p>There is no {@code retryCount < maxRetries} predicate. That is exactly what
     * {@code canRetry()} tests, so every row the loop saw already passed it and the branch that
     * marks a notification permanently failed was unreachable in production. Exhausted rows are
     * now fetched once more so the loop can terminate them.
     *
     * <p>The null check on the last attempt matters because SQL three-valued logic would
     * otherwise drop a row that has never been attempted.
     *
     * <p>Paged because dropping the retry-count predicate means the first pass after deploy sees
     * the whole historical backlog, and the caller updates every row it returns in one
     * transaction.
     */
    @Query("SELECT n FROM Notification n WHERE n.status IN (:statuses) "
            + "AND (n.lastAttemptAt IS NULL OR n.lastAttemptAt < :threshold) ORDER BY n.id")
    List<Notification> findRetryableNotifications(Collection<NotificationStatus> statuses,
            LocalDateTime threshold, Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.status = :status")
    long countByStatus(NotificationStatus status);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.status = :status AND n.sentAt BETWEEN :startDate AND :endDate")
    long countByStatusAndDateRange(NotificationStatus status, LocalDateTime startDate, LocalDateTime endDate);
}
