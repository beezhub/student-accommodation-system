package com.beezhub.student_accommodation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {

    private final StringRedisTemplate redisTemplate;

    private static final String LOGIN_ATTEMPT_KEY_PREFIX = "login:failed:";
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 15;

    public void loginSucceeded(Long userId) {
        String key = LOGIN_ATTEMPT_KEY_PREFIX + userId;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Failed to clear login attempts from Redis for user: {}", userId, e);
        }
    }

    public void loginFailed(Long userId) {
        String key = LOGIN_ATTEMPT_KEY_PREFIX + userId;
        try {
            Long attempts = redisTemplate.opsForValue().increment(key);
            if (attempts != null && attempts == 1) {
                redisTemplate.expire(key, LOCKOUT_DURATION_MINUTES, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.warn("Failed to increment login attempts in Redis for user: {}", userId, e);
        }
    }

    public boolean isBlocked(Long userId) {
        String key = LOGIN_ATTEMPT_KEY_PREFIX + userId;
        try {
            String attemptsStr = redisTemplate.opsForValue().get(key);
            if (attemptsStr != null) {
                int attempts = Integer.parseInt(attemptsStr);
                return attempts >= MAX_ATTEMPTS;
            }
        } catch (Exception e) {
            log.warn("Failed to check login attempts in Redis for user: {}", userId, e);
        }
        return false;
    }
}
