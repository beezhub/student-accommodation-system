package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.service.EmailService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** GH-419: the welcome an admin account gets when it is added to a company. */
class AdminWelcomeEmailServiceTest {

    private EmailService emailService;
    private SimpleMeterRegistry meterRegistry;
    private AdminWelcomeEmailService service;

    @BeforeEach
    void setUp() {
        emailService = mock(EmailService.class);
        meterRegistry = new SimpleMeterRegistry();
        service = new AdminWelcomeEmailService(emailService, meterRegistry);
        ReflectionTestUtils.setField(service, "baseUrl", "http://test-url");
    }

    @Test
    void welcome_namesTheCompanyAndLinksToSignIn() {
        TemplatedEmail email = service.welcome(admin(true), company());

        assertThat(email.templateName()).isEqualTo("admin-welcome");
        assertThat(email.subject()).isEqualTo(EmailSubjects.ADMIN_WELCOME);
        assertThat(email.context()).isEqualTo(new AdminWelcomeContext(
                "Ada Admin", "Acme Residences", "ada@acme.test", true, "http://test-url/login"));
    }

    /** An older account repaired by assignment may already have its own password. */
    @Test
    void welcome_accountWithoutAForcedChange_saysSo() {
        AdminWelcomeContext context = (AdminWelcomeContext) service.welcome(admin(false), company()).context();

        assertThat(context.mustChangePassword()).isFalse();
    }

    /** The retry job rebuilds the email from this map alone. */
    @Test
    void welcome_templateData_carriesTheTemplateNameAndNoPassword() {
        AppUser admin = admin(true);
        admin.setUserPassword("$2a$10$hashed");

        assertThat(service.welcome(admin, company()).templateData())
                .containsEntry("templateName", "admin-welcome")
                .containsOnlyKeys("templateName", "adminName", "companyName", "email", "mustChangePassword",
                        "loginUrl");
    }

    @Test
    void send_countsTheEmail() {
        TemplatedEmail email = service.welcome(admin(true), company());

        service.send("ada@acme.test", email);

        verify(emailService).sendTemplateEmail("ada@acme.test", EmailSubjects.ADMIN_WELCOME, "admin-welcome",
                email.context());
        assertThat(meterRegistry.counter("sas.emails.sent").count()).isEqualTo(1);
    }

    /** The listener marks the row failed for the retry job, so the failure has to reach it. */
    @Test
    void send_failure_isCountedAndRethrown() {
        TemplatedEmail email = service.welcome(admin(true), company());
        doThrow(new IllegalStateException("smtp down"))
                .when(emailService).sendTemplateEmail(anyString(), anyString(), anyString(), any(EmailContext.class));

        assertThatThrownBy(() -> service.send("ada@acme.test", email)).isInstanceOf(IllegalStateException.class);
        assertThat(meterRegistry.counter("sas.emails.failed").count()).isEqualTo(1);
    }

    private static AppUser admin(boolean mustChangePassword) {
        AppUser admin = new AppUser();
        admin.setId(5L);
        admin.setFirstName("Ada");
        admin.setLastName("Admin");
        admin.setEmail("ada@acme.test");
        admin.setTenantId(42L);
        admin.setMustChangePassword(mustChangePassword);
        return admin;
    }

    private static Tenant company() {
        return Tenant.builder().id(42L).name("Acme Residences").build();
    }
}
