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
import java.util.Date;
import java.util.function.Function;

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

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String generateToken(UserDetails userDetails) {
        var roles = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();

        // Determine expiration based on role
        long expirationMs;
        if (roles.contains("ROLE_ADMIN")) {
            expirationMs = Long.parseLong(jwtConfig.getAdminExpirationTime());
        } else {
            expirationMs = Long.parseLong(jwtConfig.getExpirationTime());
        }

        return Jwts.builder()
            .setSubject(userDetails.getUsername())
            .claim("roles", roles)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
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
            return ((List<?>) rolesObj).stream().map(Object::toString).toList();
        }
        return Collections.emptyList();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
