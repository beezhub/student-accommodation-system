package com.beezhub.student_accommodation.config;

import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Creates the first platform administrator from environment variables.
 *
 * <p>The seeded demo administrator is disabled outside demo environments (see
 * {@code V2026_09_05_1000__disable_seeded_admin.sql}), so a fresh NONPROD or PROD database needs
 * another way in. Setting {@code ADMIN_BOOTSTRAP_EMAIL} and {@code ADMIN_BOOTSTRAP_PASSWORD}
 * creates that account on startup, flagged to force a password change on first use. An existing
 * account with the same email has its password reset and that flag set again, which makes the
 * variables a recovery path as well.
 *
 * <p>Nothing happens when either variable is blank, so normal restarts are unaffected. Remove the
 * variables once the administrator has signed in.
 */
@Component
@Slf4j
public class AdminBootstrap implements ApplicationRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapEmail;
    private final String bootstrapPassword;

    public AdminBootstrap(AppUserRepository appUserRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.admin-bootstrap.email:}") String bootstrapEmail,
                          @Value("${app.admin-bootstrap.password:}") String bootstrapPassword) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapEmail = bootstrapEmail;
        this.bootstrapPassword = bootstrapPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (isBlank(bootstrapEmail) || isBlank(bootstrapPassword)) {
            return;
        }
        String email = bootstrapEmail.trim().toLowerCase();

        AppUser admin = appUserRepository.findByEmail(email).orElseGet(AppUser::new);
        boolean isNew = admin.getId() == null;

        if (isNew) {
            admin.setEmail(email);
            admin.setFirstName("Platform");
            admin.setLastName("Administrator");
            admin.setTenantId(TenantContext.PLATFORM_TENANT_ID);
            admin.setCreatedAt(LocalDateTime.now());
        }
        admin.setUserRole(UserRole.PLATFORM_ADMIN);
        admin.setUserPassword(passwordEncoder.encode(bootstrapPassword));
        admin.setEmailVerified(true);
        admin.setMustChangePassword(true);
        admin.setAccountLockedUntil(null);
        admin.setFailedLoginAttempts(0);
        admin.setUpdatedAt(LocalDateTime.now());

        appUserRepository.save(admin);
        log.warn("action=adminBootstrap created={} userId={} — this account must change its password on first login,"
                + " then clear ADMIN_BOOTSTRAP_EMAIL and ADMIN_BOOTSTRAP_PASSWORD", isNew, admin.getId());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
