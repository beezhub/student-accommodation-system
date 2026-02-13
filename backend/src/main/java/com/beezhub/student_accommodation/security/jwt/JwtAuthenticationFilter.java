package com.beezhub.student_accommodation.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final com.beezhub.student_accommodation.service.TokenBlacklistService tokenBlacklistService;


    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {
        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            
            // Check blacklist first
            try {
                String jti = jwtUtil.extractJti(jwt);
                if (jti != null && tokenBlacklistService.isBlacklisted(jti)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                username = jwtUtil.extractUsername(jwt);
            } catch (Exception e) {
                // Token parsing failed or expired
                // Let the chain continue, it will likely fail later or be treated as anonymous
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
            }
        }

        chain.doFilter(request, response);
    }
}
