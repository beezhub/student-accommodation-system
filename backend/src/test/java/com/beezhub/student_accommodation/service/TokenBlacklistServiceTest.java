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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    @Test
    void blacklistToken_validJti_storedInRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        tokenBlacklistService.blacklistToken("test-jti", 3600);
        
        verify(valueOperations).set(eq("test-jti"), eq("blacklisted"), eq(3600L), eq(TimeUnit.SECONDS));
    }

    @Test
    void isBlacklisted_blacklistedToken_returnsTrue() {
        when(redisTemplate.hasKey("test-jti")).thenReturn(true);
        
        assertTrue(tokenBlacklistService.isBlacklisted("test-jti"));
    }

    @Test
    void isBlacklisted_nonBlacklistedToken_returnsFalse() {
        when(redisTemplate.hasKey("test-jti")).thenReturn(false);
        
        assertFalse(tokenBlacklistService.isBlacklisted("test-jti"));
    }

    @Test
    void isBlacklisted_redisUnavailable_returnsFalse() {
        when(redisTemplate.hasKey("test-jti")).thenThrow(new RuntimeException("Redis down"));
        
        // Graceful degradation
        assertFalse(tokenBlacklistService.isBlacklisted("test-jti"));
    }

    @Test
    void blacklistToken_ttlExpired_autoRemoved() {
        // This test simulates TTL <= 0 logic in service
        tokenBlacklistService.blacklistToken("test-jti", 0);
        verifyNoInteractions(redisTemplate);
        
        tokenBlacklistService.blacklistToken("test-jti", -10);
        verifyNoInteractions(redisTemplate);
    }
}
