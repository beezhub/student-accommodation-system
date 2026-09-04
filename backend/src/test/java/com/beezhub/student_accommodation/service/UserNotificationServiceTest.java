package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.NotificationsResponse;
import com.beezhub.student_accommodation.model.dto.UserNotificationResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.UserNotification;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.UserNotificationRepository;
import com.beezhub.student_accommodation.service.notification.SseService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserNotificationServiceTest {

    @Mock
    private UserNotificationRepository userNotificationRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private SseService sseService;

    @InjectMocks
    private UserNotificationService userNotificationService;

    @Captor
    private ArgumentCaptor<UserNotification> notificationCaptor;

    private AppUser user;
    private UserNotification notification;

    @BeforeEach
    void setUp() {
        user = AppUser.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        notification = UserNotification.builder()
                .id(100L)
                .user(user)
                .type("TEST_TYPE")
                .title("Test Title")
                .message("Test Message")
                .data("{\"key\":\"value\"}")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getNotificationsForUser_ReturnsNotifications() throws Exception {
        when(userNotificationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(notification));
        when(userNotificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(1L);
        when(objectMapper.readValue(eq("{\"key\":\"value\"}"), any(TypeReference.class))).thenReturn(Map.of("key", "value"));

        NotificationsResponse response = userNotificationService.getNotificationsForUser(1L);

        assertThat(response).isNotNull();
        assertThat(response.getNotifications()).hasSize(1);
        assertThat(response.getUnreadCount()).isEqualTo(1L);
        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getNotifications().get(0).getData()).containsEntry("key", "value");
    }

    @Test
    void getNotificationsForUser_JsonProcessingException_ReturnsNullData() throws Exception {
        when(userNotificationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(notification));
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenThrow(new JsonProcessingException("Error"){});

        NotificationsResponse response = userNotificationService.getNotificationsForUser(1L);

        assertThat(response.getNotifications().get(0).getData()).isNull();
    }

    @Test
    void markAsRead_ValidNotification_UpdatesReadStatus() {
        when(userNotificationRepository.findById(100L)).thenReturn(Optional.of(notification));

        userNotificationService.markAsRead(100L, 1L);

        assertThat(notification.getIsRead()).isTrue();
        verify(userNotificationRepository).save(notification);
    }

    @Test
    void markAsRead_NotificationNotFound_ThrowsException() {
        when(userNotificationRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userNotificationService.markAsRead(100L, 1L));
    }

    @Test
    void markAsRead_UnauthorizedUser_ThrowsException() {
        AppUser otherUser = AppUser.builder().id(2L).build();
        UserNotification otherNotification = UserNotification.builder().id(100L).user(otherUser).build();
        when(userNotificationRepository.findById(100L)).thenReturn(Optional.of(otherNotification));

        assertThrows(RuntimeException.class, () -> userNotificationService.markAsRead(100L, 1L));
    }

    @Test
    void markAllAsRead_CallsRepository() {
        userNotificationService.markAllAsRead(1L);
        verify(userNotificationRepository).markAllAsReadForUser(1L);
    }

    @Test
    void deleteNotification_ValidNotification_Deletes() {
        when(userNotificationRepository.findById(100L)).thenReturn(Optional.of(notification));

        userNotificationService.deleteNotification(100L, 1L);

        verify(userNotificationRepository).delete(notification);
    }
    
    @Test
    void deleteNotification_Unauthorized_ThrowsException() {
        AppUser otherUser = AppUser.builder().id(2L).build();
        UserNotification otherNotification = UserNotification.builder().id(100L).user(otherUser).build();
        when(userNotificationRepository.findById(100L)).thenReturn(Optional.of(otherNotification));

        assertThrows(RuntimeException.class, () -> userNotificationService.deleteNotification(100L, 1L));
    }

    @Test
    void createNotification_ShouldTriggerSse() throws Exception {
        Map<String, Object> data = Map.of("key", "value");
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(objectMapper.writeValueAsString(data)).thenReturn("{\"key\":\"value\"}");
        when(userNotificationRepository.save(any())).thenReturn(notification);

        userNotificationService.createNotification(1L, "TEST_TYPE", "Test Title", "Test Message", data);

        verify(sseService).sendNotification(eq(1L), any(UserNotificationResponse.class));
    }

    @Test
    void createNotification_JsonProcessingException_SavesWithoutData() throws Exception {
        Map<String, Object> data = Map.of("key", "value");
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(objectMapper.writeValueAsString(data)).thenThrow(new JsonProcessingException("Error"){});
        when(userNotificationRepository.save(any())).thenReturn(notification);

        userNotificationService.createNotification(1L, "TEST_TYPE", "Test", "Msg", data);

        verify(userNotificationRepository).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getData()).isNull();
    }

    @Test
    void createNotification_SseFails_LogsErrorAndContinues() {
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userNotificationRepository.save(any())).thenReturn(notification);
        doThrow(new RuntimeException("SSE Error")).when(sseService).sendNotification(anyLong(), any());

        userNotificationService.createNotification(1L, "TEST", "Title", "Msg", null);

        // Verification implies it didn't throw an exception up
        verify(userNotificationRepository).save(any());
    }

    @Test
    void helperMethods_CreateNotificationsWithCorrectData() {
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userNotificationRepository.save(any())).thenReturn(notification);

        userNotificationService.createRoomAllocationNotification(1L, "101", "Bld A");
        userNotificationService.createDocumentStatusNotification(1L, "ID", true, null);
        userNotificationService.createDocumentStatusNotification(1L, "ID", false, "Blurry");
        userNotificationService.createApplicationStatusNotification(1L, 10L, true, null);
        userNotificationService.createApplicationStatusNotification(1L, 10L, false, "Missing docs");
        userNotificationService.createPaymentDueNotification(1L, 500.0, "2023-12-01");
        userNotificationService.createSystemAnnouncementNotification(1L, "Alert", "Msg");
        userNotificationService.createAllocationProposalNotification(1L, "101", "Bld");
        userNotificationService.createAllocationConfirmationNotification(1L, "101", "Bld");
        userNotificationService.createAllocationRejectionNotification(1L, "101", "Full");
        userNotificationService.createAllocationRejectionNotification(1L, "101", null);
        userNotificationService.createRoomMoveNotification(1L, "101", "102", "Request");
        userNotificationService.createRoomMoveNotification(1L, "101", "102", null);
        userNotificationService.createWaitingListNotification(1L, 10L);
        userNotificationService.createOfferExpiredNotification(1L, "101", "Bld");
        userNotificationService.createCheckInExpiredNotification(1L, "101", "Bld");
        userNotificationService.createOfferExpiryReminderNotification(1L, "101", "Bld");
        userNotificationService.createCheckInReminderNotification(1L, "101", "Bld");
        userNotificationService.createCheckInConfirmedNotification(1L, "101", "Bld");

        verify(userNotificationRepository, times(19)).save(any(UserNotification.class));
    }
}