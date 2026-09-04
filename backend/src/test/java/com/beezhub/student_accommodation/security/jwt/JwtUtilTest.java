package com.beezhub.student_accommodation.security.jwt;

import com.beezhub.student_accommodation.config.JwtConfig;
import com.beezhub.student_accommodation.security.user.AppUserDetails;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET =
            "01234567890123456789012345678901234567890123456789012345678901234";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        JwtConfig cfg = new JwtConfig(SECRET, "3600000", "14400000");
        jwtUtil = new JwtUtil(cfg);
    }

    // --- subject round-trip ---

    @Test
    void generateAndValidateToken_subjectRoundTrip() {
        UserDetails user = User.withUsername("alice@example.com").password("pw").roles("USER").build();
        String token = jwtUtil.generateToken(user);

        assertEquals("alice@example.com", jwtUtil.extractUsername(token));
        assertTrue(jwtUtil.validateToken(token, user));
    }

    // --- roles claim round-trip ---

    @Test
    void generateToken_rolesClaimRoundTrip() {
        UserDetails user = new User("bob@example.com", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        String token = jwtUtil.generateToken(user);

        List<String> roles = jwtUtil.extractRoles(token);
        assertNotNull(roles);
        assertEquals(1, roles.size());
        assertEquals("ROLE_ADMIN", roles.get(0));
    }

    // --- tenantId claim round-trip ---

    @Test
    void generateToken_tenantIdClaimRoundTrip() {
        AppUserDetails user = new AppUserDetails("carol@example.com", "pw", "ROLE_USER", 42L);
        String token = jwtUtil.generateToken(user);

        Long tenantId = jwtUtil.extractTenantId(token);
        assertEquals(42L, tenantId);
    }

    @Test
    void generateToken_nullTenantId_extractsNull() {
        UserDetails user = User.withUsername("dave@example.com").password("pw").roles("USER").build();
        String token = jwtUtil.generateToken(user);

        assertNull(jwtUtil.extractTenantId(token));
    }

    // --- JTI uniqueness ---

    @Test
    void generateToken_jtiIsUniquePerToken() {
        UserDetails user = User.withUsername("eve@example.com").password("pw").roles("USER").build();
        String token1 = jwtUtil.generateToken(user);
        String token2 = jwtUtil.generateToken(user);

        String jti1 = jwtUtil.extractJti(token1);
        String jti2 = jwtUtil.extractJti(token2);

        assertNotNull(jti1);
        assertNotNull(jti2);
        assertNotEquals(jti1, jti2);
    }

    // --- expiration ---

    @Test
    void generateToken_isNotExpiredImmediately() {
        UserDetails user = User.withUsername("frank@example.com").password("pw").roles("USER").build();
        String token = jwtUtil.generateToken(user);

        assertFalse(jwtUtil.isTokenExpired(token));
        assertTrue(jwtUtil.extractExpiration(token).after(new Date()));
    }

    @Test
    void generateToken_expiresAfterTtl() throws InterruptedException {
        JwtConfig shortConfig = new JwtConfig(SECRET, "1", "10");
        JwtUtil shortUtil = new JwtUtil(shortConfig);
        UserDetails user = User.withUsername("grace@example.com").password("pw").roles("USER").build();
        String token = shortUtil.generateToken(user);

        Thread.sleep(20);

        assertThrows(ExpiredJwtException.class, () -> shortUtil.validateToken(token, user));
    }

    // --- admin role uses admin expiry ---

    @Test
    void generateToken_adminRole_usesAdminExpiry() {
        JwtConfig cfg = new JwtConfig(SECRET, "1000", "100000");
        JwtUtil util = new JwtUtil(cfg);
        UserDetails admin = new User("admin@example.com", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN")));
        String token = util.generateToken(admin);

        Date expiry = util.extractExpiration(token);
        long ttlMs = expiry.getTime() - System.currentTimeMillis();

        // Admin expiry is 100000ms; regular is 1000ms — admin token should expire much later
        assertTrue(ttlMs > 50000, "Admin token should use admin expiry (100000ms), got ttl=" + ttlMs);
    }

    // --- remaining TTL ---

    @Test
    void getRemainingTtlSeconds_returnsPositiveValue() {
        UserDetails user = User.withUsername("henry@example.com").password("pw").roles("USER").build();
        String token = jwtUtil.generateToken(user);

        long ttl = jwtUtil.getRemainingTtlSeconds(token);
        assertTrue(ttl > 0 && ttl <= 3600);
    }
}
