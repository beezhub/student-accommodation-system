package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.TenantRepository;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GH-419: an admin account added to a company is welcomed once the assignment commits, through the
 * after-commit listener on the notification executor, and a rolled-back assignment sends nothing.
 */
@DisplayName("Admin welcome email on assignment to a company (integration)")
class AdminWelcomeEmailIntegrationTest extends AbstractIntegrationTest {

    private static final LocalDateTime LOCKED_UNTIL_ASSIGNED = LocalDateTime.of(9999, 12, 31, 23, 59, 59);
    private static final String WELCOME_ROWS = "SELECT notification_status, template_data::text FROM notification "
            + "WHERE user_id = ? AND template_data->>'templateName' = 'admin-welcome'";

    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transactionTemplate;
    @Autowired TenantRepository tenantRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired UnassignedAdminService unassignedAdminService;

    private Tenant company;
    private AppUser admin;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        try {
            company = tenantRepository.save(Tenant.builder()
                    .name("Welcome Co " + System.nanoTime()).isActive(true).build());
            // As user management leaves a company admin created without a company (#413).
            admin = appUserRepository.save(AppUser.builder()
                    .email("welcome." + System.nanoTime() + "@test.com")
                    .firstName("Ada")
                    .lastName("Admin")
                    .userPassword("$2a$10$hashed")
                    .userRole(UserRole.TENANT_ADMIN)
                    .tenantId(TenantContext.PLATFORM_TENANT_ID)
                    .emailVerified(true)
                    .mustChangePassword(true)
                    .accountLockedUntil(LOCKED_UNTIL_ASSIGNED)
                    .build());
        } finally {
            TenantContext.clear();
        }
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        // The welcome is written by an async listener after commit.
        Thread.sleep(1500);
        TenantContext.clear();
        jdbc.update("DELETE FROM notification WHERE user_id = ?", admin.getId());
        jdbc.update("DELETE FROM user_notifications WHERE user_id = ?", admin.getId());
        jdbc.update("DELETE FROM app_user WHERE id = ?", admin.getId());
        jdbc.update("DELETE FROM activity_logs WHERE tenant_id = ?", company.getId());
        jdbc.update("DELETE FROM tenant WHERE id = ?", company.getId());
    }

    @Test
    @DisplayName("Adding an admin to a company sends one welcome after the commit")
    void assignTenant_committed_sendsOneWelcome() throws InterruptedException {
        unassignedAdminService.assignTenant(admin.getId(), company.getId());

        Map<String, Object> row = awaitSent();
        assertThat((String) row.get("template_data"))
                .contains(company.getName())
                .contains("/login")
                .doesNotContain("$2a$10$hashed");
        assertThat(jdbc.queryForList(WELCOME_ROWS, admin.getId())).hasSize(1);
    }

    @Test
    @DisplayName("A rolled-back assignment sends no welcome")
    void assignTenant_rolledBack_sendsNothing() throws InterruptedException {
        transactionTemplate.executeWithoutResult(tx -> {
            unassignedAdminService.assignTenant(admin.getId(), company.getId());
            tx.setRollbackOnly();
        });

        // Long enough for an after-commit listener to have run, had one been triggered.
        Thread.sleep(2000);
        assertThat(jdbc.queryForList(WELCOME_ROWS, admin.getId())).isEmpty();
        assertThat(jdbc.queryForObject("SELECT tenant_id FROM app_user WHERE id = ?", Long.class, admin.getId()))
                .isEqualTo(TenantContext.PLATFORM_TENANT_ID);
    }

    private Map<String, Object> awaitSent() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            List<Map<String, Object>> rows = jdbc.queryForList(WELCOME_ROWS, admin.getId());
            if (!rows.isEmpty() && "SENT".equals(rows.get(0).get("notification_status"))) {
                return rows.get(0);
            }
            Thread.sleep(100);
        }
        throw new AssertionError("no welcome sent for user " + admin.getId());
    }
}
