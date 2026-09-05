package com.beezhub.student_accommodation.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** EventSource cannot send an Authorization header, so this one path may carry ?token=. */
    static final String SSE_STREAM_PATH = "/api/notifications/stream";

    private final JwtUtil jwtUtil;
    private final com.beezhub.student_accommodation.service.TokenBlacklistService tokenBlacklistService;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {
        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                jwt = authorizationHeader.substring(7);
            } else if (isEventStreamRequest(request) && request.getParameter("token") != null) {
                // EventSource cannot set headers, so the notification stream is the one endpoint
                // allowed to carry the token in the query string. Accepting it everywhere put JWTs
                // into proxy access logs and browser history (GH-123).
                jwt = request.getParameter("token");
            }

            if (jwt != null) {
                // Check blacklist first
                String jti = jwtUtil.extractJti(jwt);
                if (jti != null && tokenBlacklistService.isBlacklisted(jti)) {
                    throw new BadCredentialsException("Token is blacklisted");
                }
                username = jwtUtil.extractUsername(jwt);

                // Deactivating an account only sets account_locked_until, which an already-issued
                // token never consults, so a deactivated admin kept working until expiry. A
                // revocation cutoff recorded per user retires those tokens (GH-122).
                if (tokenBlacklistService.isRevoked(username, jwtUtil.extractIssuedAt(jwt))) {
                    throw new BadCredentialsException("Token has been revoked");
                }
            }

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Stateless: validate token and populate authorities from JWT 'roles' claim
                if (jwtUtil.validateToken(jwt, username)) {
                    var roles = jwtUtil.extractRoles(jwt);
                    var authorities = roles.stream()
                            .map(role -> {
                                // Ensure roles have ROLE_ prefix for Spring Security
                                String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                                return new SimpleGrantedAuthority(roleWithPrefix);
                            })
                            .collect(Collectors.toList());
                    var authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            username, jwt, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    Long tenantId = jwtUtil.extractTenantId(jwt);
                    if (tenantId != null) {
                        com.beezhub.student_accommodation.service.tenant.TenantContext.setTenantId(tenantId);
                    }
                }
            }

            chain.doFilter(request, response);

        } catch (Exception e) {
            // Delegate exception handling to the global exception resolver
            // (RestResponseExceptionHandler)
            handlerExceptionResolver.resolveException(request, response, null, e);
        } finally {
            com.beezhub.student_accommodation.service.tenant.TenantContext.clear();
        }
    }

    /** The notification stream is the only endpoint permitted to carry a token in the query string. */
    private static boolean isEventStreamRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return false;
        }
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return SSE_STREAM_PATH.equals(path);
    }
}
