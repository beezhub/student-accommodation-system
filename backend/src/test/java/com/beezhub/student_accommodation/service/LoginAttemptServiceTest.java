package com.beezhub.student_accommodation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private LoginAttemptService loginAttemptService;

    @Test
    void loginSucceeded_clearsAttempts() {
        loginAttemptService.loginSucceeded(1L);
        verify(redisTemplate).delete("login:failed:1");
    }

    @Test
    void loginFailed_incrementsAttempts() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        loginAttemptService.loginFailed(1L);

        verify(valueOperations).increment("login:failed:1");
        verify(redisTemplate).expire(eq("login:failed:1"), eq(15L), eq(TimeUnit.MINUTES));
    }

    @Test
    void isBlocked_attemptsBelowThreshold_returnsFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("login:failed:1")).thenReturn("4");

        assertFalse(loginAttemptService.isBlocked(1L));
    }

    @Test
    void isBlocked_attemptsAtThreshold_returnsTrue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("login:failed:1")).thenReturn("5");

        assertTrue(loginAttemptService.isBlocked(1L));
    }

    @Test
    void isBlocked_redisUnavailable_returnsFalse() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis down"));

        // Graceful degradation
        assertFalse(loginAttemptService.isBlocked(1L));
    }
}
