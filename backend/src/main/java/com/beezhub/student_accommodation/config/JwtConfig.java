package com.beezhub.student_accommodation.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class JwtConfig {

    private final String secretKeyString;
    private final String expirationTime;
    private final String adminExpirationTime;

    public JwtConfig(
        @Value("${jwt.secret}") String secretKeyString,
        @Value("${jwt.expiration-time-ms}") String expirationTime,
        @Value("${jwt.admin-expiration-time-ms}") String adminExpirationTime
    ) {
        this.secretKeyString = secretKeyString;
        this.expirationTime = expirationTime;
        this.adminExpirationTime = adminExpirationTime;
    }
}
