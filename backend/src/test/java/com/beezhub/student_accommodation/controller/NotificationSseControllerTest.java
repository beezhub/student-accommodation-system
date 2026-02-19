package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.service.notification.SseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
}
