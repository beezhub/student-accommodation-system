package com.beezhub.student_accommodation.security.jwt;

import com.beezhub.student_accommodation.config.JwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import io.jsonwebtoken.ExpiredJwtException;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtConfig jwtConfig = new JwtConfig("test-secret-key-test-secret-key-test-secret-key-12345", "3600000");
    private JwtUtil jwtUtil = new JwtUtil(jwtConfig);

    @Test
    void generateAndValidateToken() {
        UserDetails user = User.withUsername("alice@example.com").password("pw").roles("USER").build();
        String token = jwtUtil.generateToken(user);
        assertNotNull(token);

        String username = jwtUtil.extractUsername(token);
        assertEquals("alice@example.com", username);

        assertTrue(jwtUtil.validateToken(token, user));
    }

    @Test
    void tokenExpires() throws InterruptedException {
        JwtConfig shortConfig = new JwtConfig("test-secret-key-test-secret-key-test-secret-key-12345", "1");
        JwtUtil shortUtil = new JwtUtil(shortConfig);
        UserDetails user = User.withUsername("bob@example.com").password("pw").roles("USER").build();
        String token = shortUtil.generateToken(user);
        assertNotNull(token);
        // wait to ensure expiration
        Thread.sleep(10);
        assertThrows(ExpiredJwtException.class, () -> shortUtil.validateToken(token, user));
    }
}
