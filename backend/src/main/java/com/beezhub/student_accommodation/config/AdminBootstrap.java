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
 * creates that account on startup, flagged to force a password change on first use.
 *
 * <p>This is idempotent by design: once that account has changed its password, a later startup
 * with the same variables still present leaves it alone rather than resetting it, so a routine
 * redeploy cannot silently undo the administrator's chosen password. Reapplying to an account that
 * has never completed its first change is harmless and still happens, since no chosen password is
 * being destroyed. Set {@code ADMIN_BOOTSTRAP_FORCE=true} to reset a lost password on purpose; that
 * is a deliberate recovery action, not a side effect of deploying.
 *
 * <p>Nothing happens when email or password is blank, so normal restarts are unaffected.
 */
@Component
@Slf4j
public class AdminBootstrap implements ApplicationRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapEmail;
    private final String bootstrapPassword;
    private final boolean bootstrapForce;

    public AdminBootstrap(AppUserRepository appUserRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.admin-bootstrap.email:}") String bootstrapEmail,
                          @Value("${app.admin-bootstrap.password:}") String bootstrapPassword,
                          @Value("${app.admin-bootstrap.force:false}") boolean bootstrapForce) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapEmail = bootstrapEmail;
        this.bootstrapPassword = bootstrapPassword;
        this.bootstrapForce = bootstrapForce;
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

        if (!isNew && !bootstrapForce && Boolean.FALSE.equals(admin.getMustChangePassword())) {
            log.info("action=adminBootstrap skipped=true userId={} reason=passwordAlreadyChanged", admin.getId());
            return;
        }

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
        log.warn("action=adminBootstrap created={} forced={} userId={}: this account must change its password on"
                + " first login, then clear ADMIN_BOOTSTRAP_EMAIL and ADMIN_BOOTSTRAP_PASSWORD",
                isNew, bootstrapForce, admin.getId());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
