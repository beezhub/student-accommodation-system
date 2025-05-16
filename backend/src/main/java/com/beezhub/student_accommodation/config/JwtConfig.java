package com.beezhub.student_accommodation.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class JwtConfig {

    private final String secretKeyString;
    private final long expirationTime;

    public JwtConfig(@Value("${jwt.secret}") String secretKeyString
    , @Value("${jwt.expiration-time-ms}") long expirationTime) {
        this.secretKeyString = secretKeyString;
        this.expirationTime = expirationTime;
    }
}
