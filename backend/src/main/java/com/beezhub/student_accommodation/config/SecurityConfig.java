package com.beezhub.student_accommodation.config;

import com.beezhub.student_accommodation.security.jwt.JwtAuthenticationFilter;
import com.beezhub.student_accommodation.security.user.AppUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final AppUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(AppUserDetailsService userDetailsService, JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/signup",
                                "/api/auth/forgot-password",
                                "/api/auth/verify-reset-token",
                                "/api/auth/reset-password",
                                "/api/auth/verify-email",
                                "/api/auth/resend-verification",
                                "/api/building/maps-config")
                        .permitAll()
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;

// ...

                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
                        .requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("ADMIN")
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Configure role hierarchy for the application.
     * ADMIN has all privileges of LANDLORD and STUDENT
     * LANDLORD has all privileges of STUDENT
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        // Use the static factory method instead of deprecated constructor
        return RoleHierarchyImpl.fromHierarchy("ROLE_ADMIN > ROLE_LANDLORD \n ROLE_LANDLORD > ROLE_STUDENT");
    }

    /**
     * Configure method security expression handler with role hierarchy.
     * This ensures that @PreAuthorize and @PostAuthorize annotations respect the
     * role hierarchy.
     */
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy());
        return expressionHandler;
    }

    /**
     * Configure CORS for the application.
     * Allows requests from specified origins with credentials.
     * Admin endpoints require specific CORS configuration.
     * Uses environment variable ALLOWED_ORIGINS for production configuration.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Get allowed origins from environment variable or use defaults for development
        String allowedOrigins = System.getenv("ALLOWED_ORIGINS");
        if (allowedOrigins != null && !allowedOrigins.trim().isEmpty()) {
            configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        } else {
            // Default to localhost origins for development
            configuration.setAllowedOrigins(Arrays.asList(
                    "http://localhost:3000",
                    "http://localhost:4200",
                    "http://localhost:8080"));
        }

        // Allow all standard HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Allow all headers
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Expose authorization header
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        // Max age for preflight requests
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Apply CORS configuration to all endpoints
        source.registerCorsConfiguration("/**", configuration);

        // Specific configuration for admin endpoints can be added if needed
        // CorsConfiguration adminConfig = new CorsConfiguration(configuration);
        // adminConfig.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        // source.registerCorsConfiguration("/api/admin/**", adminConfig);

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(authProvider);
    }
}
