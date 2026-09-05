package com.beezhub.student_accommodation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
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

    /**
     * Invalidates every token already issued to a user, without needing to know their ids.
     *
     * <p>Deactivating an account only set {@code account_locked_until}, which the login path
     * checks but an already-issued token never touches, so a deactivated admin kept working until
     * the token expired. Recording a cutoff here lets the JWT filter reject any token issued
     * before it, at the cost of one Redis read per request alongside the blacklist check.
     *
     * @param username   the account's login name, which is the token subject
     * @param ttlSeconds how long to keep the marker; use the longest token lifetime in play
     */
    public void revokeTokensIssuedBefore(String username, Instant cutoff, long ttlSeconds) {
        if (username == null || cutoff == null || ttlSeconds <= 0) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(
                    revocationKey(username), String.valueOf(cutoff.toEpochMilli()), ttlSeconds, TimeUnit.SECONDS);
            log.info("action=tokensRevoked username={} cutoff={}", username, cutoff);
        } catch (Exception e) {
            log.warn("Redis unavailable, could not revoke tokens for username={}", username, e);
        }
    }

    /** True when the user's tokens were revoked at or after the moment this one was issued. */
    public boolean isRevoked(String username, Instant issuedAt) {
        if (username == null || issuedAt == null) {
            return false;
        }
        try {
            String cutoff = redisTemplate.opsForValue().get(revocationKey(username));
            if (cutoff == null) {
                return false;
            }
            return issuedAt.toEpochMilli() < Long.parseLong(cutoff);
        } catch (Exception e) {
            // Fail open, matching isBlacklisted: a Redis outage must not lock everyone out.
            log.warn("Redis unavailable, token revocation check skipped for username={}", username);
            return false;
        }
    }

    private static String revocationKey(String username) {
        return "revoked-before:" + username;
    }
}
