package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.exceptions.UserNotFoundException;
import com.beezhub.student_accommodation.model.dto.NotificationsResponse;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.service.UserNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Notifications", description = "In-app user notification APIs")
public class UserNotificationController {

    private final UserNotificationService userNotificationService;
    private final AppUserRepository appUserRepository;

    @Operation(
            summary = "Get all notifications for current user",
            description = "Retrieves all in-app notifications for the authenticated user"
    )
    @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<NotificationsResponse> getNotifications(Authentication authentication) {
        log.debug("GET /api/notifications - User: {}", authentication.getName());

        Long userId = getUserIdFromAuth(authentication);
        NotificationsResponse response = userNotificationService.getNotificationsForUser(userId);

        log.info("Retrieved {} notifications for user {}", response.getTotalCount(), userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Mark notification as read",
            description = "Marks a specific notification as read"
    )
    @ApiResponse(responseCode = "200", description = "Notification marked as read")
    @ApiResponse(responseCode = "404", description = "Notification not found")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            Authentication authentication
    ) {
        log.debug("PUT /api/notifications/{}/read - User: {}", id, authentication.getName());

        Long userId = getUserIdFromAuth(authentication);
        userNotificationService.markAsRead(id, userId);

        log.info("Marked notification {} as read for user {}", id, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Mark all notifications as read",
            description = "Marks all notifications as read for the authenticated user"
    )
    @ApiResponse(responseCode = "200", description = "All notifications marked as read")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        log.debug("PUT /api/notifications/read-all - User: {}", authentication.getName());

        Long userId = getUserIdFromAuth(authentication);
        userNotificationService.markAllAsRead(userId);

        log.info("Marked all notifications as read for user {}", userId);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Delete notification",
            description = "Deletes a specific notification"
    )
    @ApiResponse(responseCode = "204", description = "Notification deleted successfully")
    @ApiResponse(responseCode = "404", description = "Notification not found")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long id,
            Authentication authentication
    ) {
        log.debug("DELETE /api/notifications/{} - User: {}", id, authentication.getName());

        Long userId = getUserIdFromAuth(authentication);
        userNotificationService.deleteNotification(id, userId);

        log.info("Deleted notification {} for user {}", id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Helper method to get user ID from authentication
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"))
                .getId();
    }
}
