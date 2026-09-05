package com.beezhub.student_accommodation.config;

import com.beezhub.student_accommodation.security.MustChangePasswordFilter;
import com.beezhub.student_accommodation.security.RateLimitingFilter;
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
    private final RateLimitingFilter rateLimitingFilter;
    private final MustChangePasswordFilter mustChangePasswordFilter;

    /** False in production, so the springdoc paths require PLATFORM_ADMIN instead of being public. */
    private final boolean swaggerPublic;

    public SecurityConfig(
            AppUserDetailsService userDetailsService,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RateLimitingFilter rateLimitingFilter,
            MustChangePasswordFilter mustChangePasswordFilter,
            @org.springframework.beans.factory.annotation.Value("${app.security.swagger-public:true}") boolean swaggerPublic) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitingFilter = rateLimitingFilter;
        this.mustChangePasswordFilter = mustChangePasswordFilter;
        this.swaggerPublic = swaggerPublic;
    }

    /**
     * Lets Prometheus scrape metrics with HTTP basic credentials.
     *
     * <p>Everything under {@code /actuator/**} except health required {@code PLATFORM_ADMIN}, and
     * the scrape job carries no credentials, so every scrape was rejected and the Grafana
     * dashboards and alerts stayed empty (GH-134). This chain is matched first and only accepts the
     * single scrape account; a platform admin can still reach the endpoints through the main chain
     * with a JWT.
     *
     * <p>The chain is only registered when a password is configured, so an unconfigured deployment
     * keeps the previous admin-only behaviour rather than gaining an account with a blank password.
     */
    @Bean
    @org.springframework.core.annotation.Order(1)
    @org.springframework.boot.autoconfigure.condition.ConditionalOnExpression(
            "!'${management.metrics.scrape.password:}'.isEmpty()")
    public SecurityFilterChain metricsScrapeFilterChain(
            HttpSecurity http,
            @org.springframework.beans.factory.annotation.Value("${management.metrics.scrape.username:prometheus}")
            String scrapeUsername,
            @org.springframework.beans.factory.annotation.Value("${management.metrics.scrape.password}")
            String scrapePassword) throws Exception {
        var scrapeUser = org.springframework.security.core.userdetails.User
                .withUsername(scrapeUsername)
                .password(passwordEncoder().encode(scrapePassword))
                .roles("METRICS_SCRAPE")
                .build();
        var scrapeUserDetails =
                new org.springframework.security.provisioning.InMemoryUserDetailsManager(scrapeUser);

        return http
                .securityMatcher("/actuator/prometheus", "/actuator/health", "/actuator/info")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().hasRole("METRICS_SCRAPE"))
                .userDetailsService(scrapeUserDetails)
                .httpBasic(org.springframework.security.config.Customizer.withDefaults())
                .build();
    }

    @Bean
    @org.springframework.core.annotation.Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; " +
                                                "script-src 'self' 'unsafe-inline'; " +
                                                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                                                "font-src 'self' https://fonts.gstatic.com; " +
                                                "img-src 'self' data: blob: https://tile.openstreetmap.org http://localhost:9000; "
                                                +
                                                "connect-src 'self' https://nominatim.openstreetmap.org http://localhost:9000; "
                                                +
                                                "frame-src 'none'; " +
                                                "object-src 'none'")))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll()
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/signup",
                                "/api/auth/forgot-password",
                                "/api/auth/verify-reset-token",
                                "/api/auth/reset-password",
                                "/api/auth/verify-email",
                                "/api/auth/resend-verification")
                        .permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                "/api/building",
                                "/api/building/all",
                                "/api/building/browse",
                                "/api/building/search",
                                "/api/building/{id}",
                                "/api/building/by-institution/{institutionId}",
                                "/api/building/{buildingId}/images",
                                "/api/institution/all")
                        .permitAll()
                        .requestMatchers("/api/admin/**").hasAnyRole("PLATFORM_ADMIN", "TENANT_ADMIN")
                        // FF4j's bundled REST API and web console are not used (see FeatureController) and
                        // the web-api jar is excluded from the build; keep the rule so a future dependency
                        // change cannot expose flag mutation to non-platform users.
                        .requestMatchers("/api/ff4j/**", "/ff4j-web-console/**").hasRole("PLATFORM_ADMIN")
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/**").hasRole("PLATFORM_ADMIN")
                        // Public only outside production. application-prod.yml sets this false, so a
                        // production deployment does not serve its own API documentation anonymously
                        // even if springdoc were re-enabled (GH-133).
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .access((authentication, context) -> new org.springframework.security.authorization.AuthorizationDecision(
                                swaggerPublic
                                        || authentication.get().getAuthorities().stream()
                                                .anyMatch(a -> "ROLE_PLATFORM_ADMIN".equals(a.getAuthority()))))
                        .anyRequest().authenticated())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // Runs after the JWT filter: blocks everything but the change-password flow while
                // the caller still has must_change_password set.
                .addFilterAfter(mustChangePasswordFilter, JwtAuthenticationFilter.class)
                .build();
    }

    /**
     * Configure role hierarchy for the application.
     * PLATFORM_ADMIN has all privileges of BUILDING_STAFF.
     * STUDENT is an independent role and is NOT part of the management hierarchy.
     * Endpoints accessible to both students and platform admins must use hasAnyRole('STUDENT', 'PLATFORM_ADMIN').
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy(
                "ROLE_PLATFORM_ADMIN > ROLE_TENANT_ADMIN\nROLE_PLATFORM_ADMIN > ROLE_BUILDING_STAFF");
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

        // Explicitly list allowed headers — wildcard (*) is forbidden with allowCredentials=true per CORS spec
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "X-Silent-Error"
        ));

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
