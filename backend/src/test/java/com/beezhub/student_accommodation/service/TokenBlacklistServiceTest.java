package com.beezhub.student_accommodation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
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

    @Test
    void blacklistToken_redisThrows_doesNotPropagate() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis down"));

        tokenBlacklistService.blacklistToken("test-jti", 60);

        verify(redisTemplate).opsForValue();
    }

    @Test
    void revokeTokensIssuedBefore_validInput_storesCutoffMillis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Instant cutoff = Instant.ofEpochMilli(1_700_000_000_000L);

        tokenBlacklistService.revokeTokensIssuedBefore("admin@example.com", cutoff, 3600);

        verify(valueOperations).set(
                eq("revoked-before:admin@example.com"), eq("1700000000000"), eq(3600L), eq(TimeUnit.SECONDS));
    }

    @Test
    void revokeTokensIssuedBefore_nullOrNonPositiveInput_doesNothing() {
        Instant cutoff = Instant.now();

        tokenBlacklistService.revokeTokensIssuedBefore(null, cutoff, 60);
        tokenBlacklistService.revokeTokensIssuedBefore("user", null, 60);
        tokenBlacklistService.revokeTokensIssuedBefore("user", cutoff, 0);

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void revokeTokensIssuedBefore_redisThrows_doesNotPropagate() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis down"));

        tokenBlacklistService.revokeTokensIssuedBefore("user", Instant.now(), 60);

        verify(redisTemplate).opsForValue();
    }

    @Test
    void isRevoked_tokenIssuedBeforeCutoff_returnsTrue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("revoked-before:user")).thenReturn("2000");

        assertTrue(tokenBlacklistService.isRevoked("user", Instant.ofEpochMilli(1000)));
    }

    @Test
    void isRevoked_tokenIssuedAfterCutoff_returnsFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("revoked-before:user")).thenReturn("2000");

        assertFalse(tokenBlacklistService.isRevoked("user", Instant.ofEpochMilli(3000)));
    }

    @Test
    void isRevoked_noMarker_returnsFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("revoked-before:user")).thenReturn(null);

        assertFalse(tokenBlacklistService.isRevoked("user", Instant.now()));
    }

    @Test
    void isRevoked_nullArguments_returnsFalseWithoutRedis() {
        assertFalse(tokenBlacklistService.isRevoked(null, Instant.now()));
        assertFalse(tokenBlacklistService.isRevoked("user", null));

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void isRevoked_redisUnavailable_returnsFalse() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis down"));

        assertFalse(tokenBlacklistService.isRevoked("user", Instant.now()));
    }
}
