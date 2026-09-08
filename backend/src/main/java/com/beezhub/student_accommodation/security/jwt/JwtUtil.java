package com.beezhub.student_accommodation.security.jwt;

import com.beezhub.student_accommodation.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.List;
import java.util.Collections;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.beezhub.student_accommodation.security.user.AppUserDetails;

@Component
public class JwtUtil {

    private final JwtConfig jwtConfig;
    private final SecretKey key;

    public JwtUtil(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        this.key = Keys.hmacShaKeyFor(jwtConfig.getSecretKeyString().getBytes(StandardCharsets.UTF_8));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    public Long extractTenantId(String token) {
        Claims claims = extractAllClaims(token);
        Object tenantId = claims.get("tenantId");
        if (tenantId instanceof Number) {
            return ((Number) tenantId).longValue();
        }
        return null;
    }

    /** Administrative roles share the shorter token lifetime. */
    private static boolean isAdminRole(String authority) {
        String role = authority.startsWith("ROLE_") ? authority.substring(5) : authority;
        try {
            return com.beezhub.student_accommodation.model.enums.UserRole.valueOf(role).isAdmin();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** When the token was issued; used to honour per-user revocation cutoffs. */
    public Instant extractIssuedAt(String token) {
        Date issuedAt = extractClaim(token, Claims::getIssuedAt);
        return issuedAt == null ? null : issuedAt.toInstant();
    }

    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Claim carrying the forced-password-change flag; read by {@code MustChangePasswordFilter}. */
    public static final String CLAIM_MUST_CHANGE_PASSWORD = "mustChangePassword";

    /**
     * Whether the token was issued to a user who still has to change their password. Tokens issued
     * before this claim existed report {@code false}.
     */
    public boolean extractMustChangePassword(String token) {
        Object value = extractAllClaims(token).get(CLAIM_MUST_CHANGE_PASSWORD);
        return Boolean.TRUE.equals(value) || "true".equals(String.valueOf(value));
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails, false);
    }

    public String generateToken(UserDetails userDetails, boolean mustChangePassword) {
        var roles = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());

        // Determine expiration based on role
        long expirationMs;
        // Every administrative role gets the shorter admin lifetime; only PLATFORM_ADMIN did before,
        // so tenant admins and building staff held 10-hour tokens (GH-122).
        if (roles.stream().anyMatch(JwtUtil::isAdminRole)) {
            expirationMs = Long.parseLong(jwtConfig.getAdminExpirationTime());
        } else {
            expirationMs = Long.parseLong(jwtConfig.getExpirationTime());
        }
        
        Long tenantId = null;
        if (userDetails instanceof AppUserDetails appUserDetails) {
            tenantId = appUserDetails.getTenantId();
        }

        return Jwts.builder()
            .id(java.util.UUID.randomUUID().toString())
            .subject(userDetails.getUsername())
            .claim("roles", roles)
            .claim("tenantId", tenantId)
            .claim(CLAIM_MUST_CHANGE_PASSWORD, mustChangePassword)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(key)
            .compact();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public boolean validateToken(String token, String username) {
        final String tokenUsername = extractUsername(token);
        return tokenUsername != null && tokenUsername.equals(username) && !isTokenExpired(token);
    }

    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List<?>) {
            return ((List<?>) rolesObj).stream().map(Object::toString).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    public long getRemainingTtlSeconds(String token) {
        Date expiration = extractExpiration(token);
        long remaining = (expiration.getTime() - System.currentTimeMillis()) / 1000;
        return remaining > 0 ? remaining : 0;
    }
}
