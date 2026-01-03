package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.NotificationsResponse;
import com.beezhub.student_accommodation.model.dto.UserNotificationResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.UserNotification;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.UserNotificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserNotificationService {

    private final UserNotificationRepository userNotificationRepository;
    private final AppUserRepository appUserRepository;
    private final ObjectMapper objectMapper;

    /**
     * Get all notifications for the current user
     */
    @Transactional(readOnly = true)
    public NotificationsResponse getNotificationsForUser(Long userId) {
        log.debug("Fetching notifications for user {}", userId);

        List<UserNotification> notifications = userNotificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId);

        long unreadCount = userNotificationRepository.countByUserIdAndIsReadFalse(userId);

        List<UserNotificationResponse> notificationResponses = notifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return NotificationsResponse.builder()
                .notifications(notificationResponses)
                .unreadCount(unreadCount)
                .totalCount(notifications.size())
                .build();
    }

    /**
     * Mark a notification as read
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        log.debug("Marking notification {} as read for user {}", notificationId, userId);

        UserNotification notification = userNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // Security check: ensure notification belongs to the user
        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to notification");
        }

        if (!notification.getIsRead()) {
            notification.markAsRead();
            userNotificationRepository.save(notification);
        }
    }

    /**
     * Mark all notifications as read for a user
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        log.debug("Marking all notifications as read for user {}", userId);

        int updatedCount = userNotificationRepository.markAllAsReadForUser(userId);
        log.info("Marked {} notifications as read for user {}", updatedCount, userId);
    }

    /**
     * Delete a notification
     */
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        log.debug("Deleting notification {} for user {}", notificationId, userId);

        UserNotification notification = userNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // Security check: ensure notification belongs to the user
        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to notification");
        }

        userNotificationRepository.delete(notification);
        log.info("Deleted notification {} for user {}", notificationId, userId);
    }

    /**
     * Create a new notification for a user
     */
    @Transactional
    public UserNotification createNotification(Long userId, String type, String title,
                                              String message, Map<String, Object> data) {
        log.debug("Creating notification for user {}: type={}, title={}", userId, type, title);

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String dataJson = null;
        if (data != null && !data.isEmpty()) {
            try {
                dataJson = objectMapper.writeValueAsString(data);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize notification data", e);
            }
        }

        UserNotification notification = UserNotification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .data(dataJson)
                .isRead(false)
                .build();

        UserNotification saved = userNotificationRepository.save(notification);
        log.info("Created notification {} for user {}", saved.getId(), userId);

        return saved;
    }

    /**
     * Create a room allocation notification
     */
    public void createRoomAllocationNotification(Long userId, String roomNumber, String buildingName) {
        Map<String, Object> data = new HashMap<>();
        data.put("roomNumber", roomNumber);
        data.put("buildingName", buildingName);

        createNotification(
                userId,
                "ROOM_ALLOCATION",
                "Room Allocated",
                String.format("Your room %s in %s has been allocated!", roomNumber, buildingName),
                data
        );
    }

    /**
     * Create a document status notification
     */
    public void createDocumentStatusNotification(Long userId, String documentType,
                                                 boolean approved, String comment) {
        String type = approved ? "DOCUMENT_APPROVED" : "DOCUMENT_REJECTED";
        String title = approved ? "Document Approved" : "Document Rejected";
        String message = approved
                ? String.format("Your %s has been approved!", documentType)
                : String.format("Your %s requires attention. %s", documentType,
                comment != null ? comment : "Please review and resubmit.");

        Map<String, Object> data = new HashMap<>();
        data.put("documentType", documentType);
        if (comment != null) {
            data.put("comment", comment);
        }

        createNotification(userId, type, title, message, data);
    }

    /**
     * Create an application status notification
     */
    public void createApplicationStatusNotification(Long userId, Long applicationId,
                                                    boolean approved, String reason) {
        String type = approved ? "APPLICATION_APPROVED" : "APPLICATION_REJECTED";
        String title = approved ? "Application Approved" : "Application Status Update";
        String message = approved
                ? "Your accommodation application has been approved! Please upload your documents."
                : String.format("Your application requires attention. %s",
                reason != null ? reason : "Please review the requirements.");

        Map<String, Object> data = new HashMap<>();
        data.put("applicationId", applicationId);
        if (reason != null) {
            data.put("reason", reason);
        }

        createNotification(userId, type, title, message, data);
    }

    /**
     * Create a payment due notification
     */
    public void createPaymentDueNotification(Long userId, double amount, String dueDate) {
        Map<String, Object> data = new HashMap<>();
        data.put("amount", amount);
        data.put("dueDate", dueDate);

        createNotification(
                userId,
                "PAYMENT_DUE",
                "Payment Due",
                String.format("Payment of R%.2f is due on %s", amount, dueDate),
                data
        );
    }

    /**
     * Create a system announcement notification
     */
    public void createSystemAnnouncementNotification(Long userId, String title, String message) {
        createNotification(userId, "SYSTEM_ANNOUNCEMENT", title, message, null);
    }

    /**
     * Map entity to response DTO
     */
    private UserNotificationResponse mapToResponse(UserNotification notification) {
        Map<String, Object> dataMap = null;
        if (notification.getData() != null) {
            try {
                dataMap = objectMapper.readValue(
                        notification.getData(),
                        new TypeReference<Map<String, Object>>() {}
                );
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize notification data", e);
            }
        }

        return UserNotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUser().getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .data(dataMap)
                .build();
    }
}
