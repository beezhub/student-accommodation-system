package com.beezhub.student_accommodation.security;

import com.beezhub.student_accommodation.security.jwt.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Enforces a forced password change on the server.
 *
 * <p>Accounts created by an administrator, and the seeded bootstrap admin, carry
 * {@code must_change_password}. Until this change that flag was honoured only by an Angular route
 * guard, so anyone holding such a credential could call the API directly with full privileges.
 * While the flag is set on the caller's token, every request is rejected except the few needed to
 * clear it: change the password, read the current user, and log out.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MustChangePasswordFilter extends OncePerRequestFilter {

    /** Paths reachable while a password change is outstanding. */
    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/auth/change-password",
            "/api/auth/logout",
            "/api/auth/me",
            "/api/auth/login",
            "/api/auth/refresh-token");

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = bearerToken(request);
        if (token == null || isAllowed(request)) {
            chain.doFilter(request, response);
            return;
        }

        boolean mustChange;
        try {
            mustChange = jwtUtil.extractMustChangePassword(token);
        } catch (Exception e) {
            // Malformed or expired tokens are the JWT filter's problem, not ours.
            chain.doFilter(request, response);
            return;
        }

        if (!mustChange) {
            chain.doFilter(request, response);
            return;
        }

        log.warn("action=passwordChangeRequired method={} path={}", request.getMethod(), request.getRequestURI());
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"You must change your password before using the application.\"}");
    }

    private boolean isAllowed(HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return ALLOWED_PATHS.contains(path);
    }

    private static String bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
