package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.Notification;
import com.beezhub.student_accommodation.model.enums.NotificationStatus;
import com.beezhub.student_accommodation.model.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByStatus(NotificationStatus status);

    List<Notification> findByRecipient(String recipient);

    List<Notification> findByUser_Id(Long userId);

    List<Notification> findByType(NotificationType type);

    List<Notification> findByStatusAndRetryCountLessThan(NotificationStatus status, Integer maxRetries);

    @Query("SELECT n FROM notification n WHERE n.status = :status AND n.retryCount < n.maxRetries AND n.lastAttemptAt < :threshold")
    List<Notification> findFailedNotificationsForRetry(NotificationStatus status, LocalDateTime threshold);

    @Query("SELECT COUNT(n) FROM notification n WHERE n.status = :status")
    long countByStatus(NotificationStatus status);

    @Query("SELECT COUNT(n) FROM notification n WHERE n.status = :status AND n.sentAt BETWEEN :startDate AND :endDate")
    long countByStatusAndDateRange(NotificationStatus status, LocalDateTime startDate, LocalDateTime endDate);
}
