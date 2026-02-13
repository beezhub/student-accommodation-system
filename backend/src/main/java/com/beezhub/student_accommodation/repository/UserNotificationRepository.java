package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    /**
     * Find all notifications for a specific user, ordered by most recent first
     */
    List<UserNotification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find unread notifications for a specific user
     */
    List<UserNotification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    /**
     * Count unread notifications for a specific user
     */
    long countByUserIdAndIsReadFalse(Long userId);

    /**
     * Mark all notifications as read for a specific user
     */
    @Modifying
    @Query("UPDATE UserNotification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsReadForUser(@Param("userId") Long userId);

    /**
     * Delete old read notifications (older than specified cutoff)
     */
    @Modifying
    @Query("DELETE FROM UserNotification n WHERE n.user.id = :userId AND n.isRead = true AND n.readAt IS NOT NULL AND n.readAt < :cutoff")
    int deleteOldReadNotifications(@Param("userId") Long userId, @Param("cutoff") LocalDateTime cutoff);

    /**
     * Delete all notifications for a specific user
     */
    void deleteByUser(com.beezhub.student_accommodation.model.entity.AppUser user);
}
