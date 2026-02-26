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
    private final com.beezhub.student_accommodation.service.notification.SseService sseService;

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

        try {
            sseService.sendNotification(userId, mapToResponse(saved));
        } catch (Exception e) {
            log.error("Failed to send SSE notification", e);
        }

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
                data);
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
                data);
    }

    /**
     * Create a system announcement notification
     */
    public void createSystemAnnouncementNotification(Long userId, String title, String message) {
        createNotification(userId, "SYSTEM_ANNOUNCEMENT", title, message, null);
    }

    /**
     * Create an allocation proposal notification
     */
    public void createAllocationProposalNotification(Long userId, String roomNumber, String buildingName) {
        Map<String, Object> data = new HashMap<>();
        data.put("roomNumber", roomNumber);
        data.put("buildingName", buildingName);

        createNotification(
                userId,
                "ALLOCATION_PROPOSED",
                "Room Allocation Proposed",
                String.format(
                        "A room allocation has been proposed for you: Room %s in %s. Please review in your portal.",
                        roomNumber, buildingName),
                data);
    }

    /**
     * Create an allocation confirmation notification
     */
    public void createAllocationConfirmationNotification(Long userId, String roomNumber, String buildingName) {
        Map<String, Object> data = new HashMap<>();
        data.put("roomNumber", roomNumber);
        data.put("buildingName", buildingName);

        createNotification(
                userId,
                "ALLOCATION_CONFIRMED",
                "Room Allocation Confirmed",
                String.format("Your room allocation has been confirmed! You have been assigned to Room %s in %s.",
                        roomNumber, buildingName),
                data);
    }

    /**
     * Create an allocation rejection notification
     */
    public void createAllocationRejectionNotification(Long userId, String roomNumber, String reason) {
        Map<String, Object> data = new HashMap<>();
        data.put("roomNumber", roomNumber);
        if (reason != null) {
            data.put("reason", reason);
        }

        createNotification(
                userId,
                "ALLOCATION_REJECTED",
                "Proposed Allocation Not Confirmed",
                String.format("Your proposed allocation to Room %s was not confirmed. Reason: %s",
                        roomNumber, reason != null ? reason : "Please contact administration for details."),
                data);
    }

    /**
     * Create a room move notification
     */
    public void createRoomMoveNotification(Long userId, String oldRoomNumber, String newRoomNumber, String reason) {
        Map<String, Object> data = new HashMap<>();
        data.put("oldRoomNumber", oldRoomNumber);
        data.put("newRoomNumber", newRoomNumber);
        if (reason != null) {
            data.put("reason", reason);
        }

        createNotification(
                userId,
                "ROOM_MOVED",
                "Room Assignment Changed",
                String.format("You have been moved from Room %s to Room %s. Reason: %s",
                        oldRoomNumber, newRoomNumber, reason != null ? reason : "Administrative decision."),
                data);
    }

    /**
     * Creates an in-app notification when a student is added to the waiting list.
     *
     * @param userId        The app user ID
     * @param applicationId The application ID
     */
    @Transactional
    public void createWaitingListNotification(Long userId, Long applicationId) {
        Map<String, Object> data = new HashMap<>();
        data.put("applicationId", applicationId);

        createNotification(
                userId,
                "WAITING_LIST",
                "Added to Waiting List",
                "Your application has been approved! You've been added to the waiting list. " +
                        "We'll notify you as soon as a room becomes available.",
                data);

        log.info("Created waiting list notification for user {} and application {}", userId, applicationId);
    }

    /**
     * Creates an in-app notification when a room offer expires without response.
     *
     * @param userId       The app user ID
     * @param roomNumber   The room number
     * @param buildingName The building name
     */
    @Transactional
    public void createOfferExpiredNotification(Long userId, String roomNumber, String buildingName) {
        createNotification(
                userId,
                "OFFER_EXPIRED",
                "Room Offer Expired",
                String.format("Your offer for Room %s in %s has expired. Your application has been cancelled. " +
                        "Please submit a new application if you still need accommodation.", roomNumber, buildingName),
                null);

        log.info("Created offer expired notification for user {}, room {}", userId, roomNumber);
    }

    /**
     * Creates an in-app notification when check-in deadline is missed.
     *
     * @param userId       The app user ID
     * @param roomNumber   The room number
     * @param buildingName The building name
     */
    @Transactional
    public void createCheckInExpiredNotification(Long userId, String roomNumber, String buildingName) {
        createNotification(
                userId,
                "CHECK_IN_EXPIRED",
                "Check-In Deadline Expired",
                String.format("Your room allocation for Room %s in %s has expired due to failure to check in. " +
                        "Your application has been cancelled. Please submit a new application if you still need accommodation.",
                        roomNumber, buildingName),
                null);

        log.info("Created check-in expired notification for user {}, room {}", userId, roomNumber);
    }

    /**
     * Creates an in-app notification reminding the student that their room offer
     * expires within 24 hours.
     *
     * @param userId       The app user ID
     * @param roomNumber   The room number
     * @param buildingName The building name
     */
    @Transactional
    public void createOfferExpiryReminderNotification(Long userId, String roomNumber, String buildingName) {
        createNotification(
                userId,
                "OFFER_EXPIRY_REMINDER",
                "Room Offer Expiring Soon",
                String.format("Your offer for Room %s in %s expires in less than 24 hours. " +
                        "Please log in and respond before the deadline to keep your room.", roomNumber, buildingName),
                null);

        log.info("Created offer expiry reminder notification for user {}, room {}", userId, roomNumber);
    }

    @Transactional
    public void createCheckInReminderNotification(Long userId, String roomNumber, String buildingName) {
        createNotification(
                userId,
                "CHECK_IN_REMINDER",
                "Check-In Deadline in 24 Hours",
                String.format("Reminder: Your check-in deadline for Room %s in %s is within 24 hours. " +
                        "Please check in before the deadline to keep your room allocation.", roomNumber, buildingName),
                null);

        log.info("Created check-in reminder notification for user {}, room {}", userId, roomNumber);
    }

    /**
     * Creates an in-app notification when admin confirms student check-in.
     *
     * @param userId       The app user ID
     * @param roomNumber   The room number
     * @param buildingName The building name
     */
    @Transactional
    public void createCheckInConfirmedNotification(Long userId, String roomNumber, String buildingName) {
        createNotification(
                userId,
                "CHECK_IN_CONFIRMED",
                "Check-In Confirmed",
                String.format("Welcome! Your check-in to Room %s in %s has been confirmed. " +
                        "Your accommodation is now active. Enjoy your stay!",
                        roomNumber, buildingName),
                null);

        log.info("Created check-in confirmed notification for user {}, room {}", userId, roomNumber);
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
                        new TypeReference<Map<String, Object>>() {
                        });
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
