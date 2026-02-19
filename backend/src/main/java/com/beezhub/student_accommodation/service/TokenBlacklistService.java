package com.beezhub.student_accommodation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    public void blacklistToken(String jti, long remainingTtlSeconds) {
        if (remainingTtlSeconds <= 0) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(jti, "blacklisted", remainingTtlSeconds, TimeUnit.SECONDS);
            log.info("Token blacklisted: jti={}", jti);
        } catch (Exception e) {
            log.warn("Redis unavailable, could not blacklist token: jti={}", jti, e);
        }
    }

    public boolean isBlacklisted(String jti) {
        try {
            Boolean hasKey = redisTemplate.hasKey(jti);
            return Boolean.TRUE.equals(hasKey);
        } catch (Exception e) {
            log.warn("Redis unavailable, token blacklist check skipped for jti={}", jti);
            // Fail open: if Redis is down, allow the request (graceful degradation)
            // returning false means "not blacklisted"
            return false;
        }
    }
}
