package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.UserNotificationResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.service.notification.SseService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationSseController.class)
class NotificationSseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SseService sseService;

    @MockBean
    private AppUserRepository appUserRepository;

    @MockBean
    private com.beezhub.student_accommodation.security.user.AppUserDetailsService appUserDetailsService;

    @MockBean
    private com.beezhub.student_accommodation.security.jwt.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private com.beezhub.student_accommodation.security.RateLimitingFilter rateLimitingFilter;

    // Filter beans are part of the web slice; mocking it keeps JwtUtil out of this context.
    @MockBean
    private com.beezhub.student_accommodation.security.MustChangePasswordFilter mustChangePasswordFilter;

    @org.junit.jupiter.api.BeforeEach
    void passThroughMockedFilters() throws Exception {
        Mockito.doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(mustChangePasswordFilter).doFilter(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void subscribe_shouldReturnSseEmitter() throws Exception {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setEmail("test@example.com");

        given(appUserRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
        given(sseService.subscribe(anyLong())).willReturn(new SseEmitter());

        mockMvc.perform(get("/api/notifications/stream"))
                .andExpect(status().isOk());
    }

    /**
     * Unit tests for SseService.sendNotification() push behaviour.
     * SseEmitter.send() requires an HTTP response writer, so a mock emitter is
     * injected directly into the service's internal emitters map via reflection
     * to isolate the push logic from the HTTP layer.
     */
    @Nested
    class SseServicePushTests {

        @Test
        @SuppressWarnings("unchecked")
        void pushNotification_subscribedUser_receivesEvent() throws Exception {
            SseService service = new SseService(Mockito.mock(com.beezhub.student_accommodation.repository.AppUserRepository.class));

            // Mock the emitter — avoids the HTTP response writer requirement
            SseEmitter mockEmitter = Mockito.mock(SseEmitter.class);

            // Inject directly into the service's emitters map
            Field emittersField = SseService.class.getDeclaredField("emitters");
            emittersField.setAccessible(true);
            Map<Long, SseEmitter> emitters = (Map<Long, SseEmitter>) emittersField.get(service);
            emitters.put(42L, mockEmitter);

            UserNotificationResponse notification = UserNotificationResponse.builder()
                    .id(1L)
                    .message("Your offer has been accepted")
                    .build();

            // Act
            service.sendNotification(42L, notification);

            // Assert — send() was invoked once with the SSE event builder
            Mockito.verify(mockEmitter, Mockito.times(1))
                    .send(Mockito.any(SseEmitter.SseEventBuilder.class));
        }
    }
}
