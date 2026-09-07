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

    /** Browser origins allowed to call the API. Required: an empty list blocks every browser call. */
    private final java.util.List<String> allowedOrigins;

    /** Object-storage origin the browser may load images from and connect to, for the CSP. */
    private final String storagePublicOrigin;

    public SecurityConfig(
            AppUserDetailsService userDetailsService,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RateLimitingFilter rateLimitingFilter,
            MustChangePasswordFilter mustChangePasswordFilter,
            @org.springframework.beans.factory.annotation.Value("${app.security.swagger-public:true}") boolean swaggerPublic,
            @org.springframework.beans.factory.annotation.Value("${app.security.allowed-origins}") java.util.List<String> allowedOrigins,
            @org.springframework.beans.factory.annotation.Value("${app.security.storage-public-origin}") String storagePublicOrigin) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitingFilter = rateLimitingFilter;
        this.mustChangePasswordFilter = mustChangePasswordFilter;
        this.swaggerPublic = swaggerPublic;
        this.allowedOrigins = allowedOrigins;
        this.storagePublicOrigin = storagePublicOrigin;
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
                                        // The storage origin used to be pinned to a developer's port, so
                                        // presigned document and image URLs were blocked in every
                                        // environment but a developer's machine (GH-135).
                                        "img-src 'self' data: blob: https://tile.openstreetmap.org "
                                                + storagePublicOrigin + "; "
                                                +
                                                "connect-src 'self' https://nominatim.openstreetmap.org "
                                                + storagePublicOrigin + "; "
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
                        // PLATFORM_STAFF is admitted to the /api/admin URL space, and every
                        // governance endpoint inside it is held shut by its own
                        // @PreAuthorize("hasRole('PLATFORM_ADMIN')") rather than by this line. The
                        // matcher and the annotations do different jobs: this lets you into the
                        // space, the annotation stops you at the endpoint. A governance endpoint
                        // added here without one becomes staff-reachable — which is what
                        // PlatformStaffAccessIntegrationTest enumerates (GH-238).
                        .requestMatchers("/api/admin/**")
                        .hasAnyRole("PLATFORM_ADMIN", "PLATFORM_STAFF", "TENANT_ADMIN")
                        // FF4j's bundled REST API and web console are not used (see FeatureController) and
                        // the web-api jar is excluded from the build; keep the rule so a future dependency
                        // change cannot expose flag mutation to non-platform users.
                        .requestMatchers("/api/ff4j/**", "/ff4j-web-console/**").hasRole("PLATFORM_ADMIN")
                        // The two probe paths are listed explicitly rather than matched with a
                        // wildcard: /actuator/health/** would also expose every component
                        // health path by name to an anonymous caller.
                        .requestMatchers("/actuator/health", "/actuator/health/liveness",
                                "/actuator/health/readiness").permitAll()
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
     *
     * <p>PLATFORM_STAFF sits between PLATFORM_ADMIN and TENANT_ADMIN (GH-238). That one edge is
     * what grants platform employees the whole shared admin surface — everything currently open to
     * TENANT_ADMIN — without any governance surface, because a hierarchy only ever grants
     * downwards: the endpoints guarded {@code hasRole('PLATFORM_ADMIN')} stay out of reach.
     *
     * <p>What makes it cross-company is not the hierarchy but the tenant: platform roles are
     * stamped {@code tenant_id = 0}, and {@code TenantFilterAspect} skips the Hibernate filter for
     * that sentinel.
     *
     * <p>STUDENT is an independent role and is NOT part of this hierarchy. Endpoints accessible to
     * both students and platform admins must use hasAnyRole('STUDENT', 'PLATFORM_ADMIN').
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy(
                "ROLE_PLATFORM_ADMIN > ROLE_PLATFORM_STAFF\n"
                        + "ROLE_PLATFORM_STAFF > ROLE_TENANT_ADMIN\n"
                        + "ROLE_PLATFORM_ADMIN > ROLE_BUILDING_STAFF");
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

        // Read through the Spring environment rather than the raw process environment, so the value
        // can come from application.yml, a profile file or a container secret, and tests can set it.
        configuration.setAllowedOrigins(allowedOrigins);

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
