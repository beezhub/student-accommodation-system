package com.beezhub.student_accommodation.security.jwt;

import com.beezhub.student_accommodation.config.JwtConfig;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        JwtConfig cfg = new JwtConfig(
                "01234567890123456789012345678901234567890123456789012345678901234",
                "3600000",
                "14400000"
        );
        jwtUtil = new JwtUtil(cfg);
    }

    @Test
    void generateToken_containsRolesClaim_and_extractRoles() {
        var user = new User("alice@example.com", "pw", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        String token = jwtUtil.generateToken(user);
        assertNotNull(token);

        var roles = jwtUtil.extractRoles(token);
        assertNotNull(roles);
        assertTrue(roles.contains("ROLE_ADMIN"));
    }

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
        JwtConfig shortConfig = new JwtConfig("01234567890123456789012345678901234567890123456789012345678901234", "1", "10");
        JwtUtil shortUtil = new JwtUtil(shortConfig);
        UserDetails user = User.withUsername("bob@example.com").password("pw").roles("USER").build();
        String token = shortUtil.generateToken(user);
        assertNotNull(token);
        // wait to ensure expiration
        Thread.sleep(10);
        assertThrows(ExpiredJwtException.class, () -> shortUtil.validateToken(token, user));
    }
}
