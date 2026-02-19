package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.UserNotificationResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.UserNotification;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.UserNotificationRepository;
import com.beezhub.student_accommodation.service.notification.SseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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

    @Test
    void createNotification_shouldTriggerSse() {
        Long userId = 1L;
        String type = "TEST_TYPE";
        String title = "Test Title";
        String message = "Test Message";
        Map<String, Object> data = new HashMap<>();

        AppUser user = new AppUser();
        user.setId(userId);

        UserNotification notification = UserNotification.builder()
                .id(100L)
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .isRead(false)
                .build();

        given(appUserRepository.findById(userId)).willReturn(Optional.of(user));
        given(userNotificationRepository.save(any(UserNotification.class))).willReturn(notification);

        userNotificationService.createNotification(userId, type, title, message, data);

        verify(sseService).sendNotification(eq(userId), any(UserNotificationResponse.class));
    }
}
