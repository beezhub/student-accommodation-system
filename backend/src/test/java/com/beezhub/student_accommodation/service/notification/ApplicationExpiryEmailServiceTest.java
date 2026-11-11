package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Institution;
import com.beezhub.student_accommodation.model.entity.Intake;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.service.EmailService;
import com.beezhub.student_accommodation.service.application.CompanyIntakePolicy;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** GH-497: the expiry email names what ended and offers the next open intake at that company. */
class ApplicationExpiryEmailServiceTest {

    private EmailService emailService;
    private CompanyIntakePolicy intakePolicy;
    private SimpleMeterRegistry meterRegistry;
    private ApplicationExpiryEmailService service;

    @BeforeEach
    void setUp() {
        emailService = mock(EmailService.class);
        intakePolicy = mock(CompanyIntakePolicy.class);
        meterRegistry = new SimpleMeterRegistry();
        service = new ApplicationExpiryEmailService(emailService, new EmailContentBuilder(), intakePolicy, meterRegistry);
        ReflectionTestUtils.setField(service, "baseUrl", "http://portal");
    }

    private static Intake intake(String name, LocalDate start, LocalDate end) {
        AcademicYear year = AcademicYear.builder().id(5L).name("2027").build();
        return Intake.builder().id(6L).name(name).academicYear(year).stayStartDate(start).stayEndDate(end)
                .applicationOpenDate(start.minusMonths(4)).applicationCloseDate(start.minusDays(14)).build();
    }

    private static Application application() {
        Application application = new Application();
        application.setId(10L);
        application.setTenantId(7L);
        application.setApplicationCode("APP-10");
        application.setTenant(Tenant.builder().id(7L).name("Company A").build());
        Institution institution = new Institution();
        institution.setId(3L);
        application.setInstitution(institution);
        application.setIntake(intake("Semester 1", LocalDate.of(2027, 2, 1), LocalDate.of(2027, 6, 30)));
        return application;
    }

    @Test
    void expired_nextIntakeOpen_offersIt() {
        when(intakePolicy.openIntakes(7L, 3L)).thenReturn(List.of(
                intake("Semester 2", LocalDate.of(2027, 7, 15), LocalDate.of(2027, 11, 30))));

        TemplatedEmail email = service.expired(application(), "John Doe");

        assertThat(email.templateName()).isEqualTo("application-expired");
        assertThat(email.subject()).isEqualTo(EmailSubjects.APPLICATION_EXPIRED);
        Map<String, Object> data = email.templateData();
        assertThat(data).containsEntry("templateName", "application-expired")
                .containsEntry("companyName", "Company A")
                .containsEntry("intakeName", "Semester 1, 2027")
                .containsEntry("stayEndDate", "30 June 2027")
                .containsEntry("nextIntakeName", "Semester 2, 2027")
                .containsEntry("nextMoveInDate", "15 July 2027")
                .containsEntry("nextApplyByDate", "01 July 2027")
                .containsEntry("applyUrl", "http://portal/applications/new");
    }

    @Test
    void expired_noIntakeOpen_leavesTheOfferOut() {
        when(intakePolicy.openIntakes(7L, 3L)).thenReturn(List.of());

        ApplicationExpiredContext context = (ApplicationExpiredContext) service.expired(application(), "John Doe").context();

        assertThat(context.nextIntakeName()).isNull();
        assertThat(context.nextMoveInDate()).isNull();
        assertThat(context.portalUrl()).isEqualTo("http://portal/applications");
    }

    @Test
    void expired_noIntakeOrCompany_stillBuildsTheEmail() {
        Application application = new Application();
        application.setApplicationCode("APP-11");

        ApplicationExpiredContext context = (ApplicationExpiredContext) service.expired(application, "John Doe").context();

        assertThat(context.companyName()).isEqualTo("the residence");
        assertThat(context.intakeName()).isEqualTo("N/A");
        assertThat(context.nextIntakeName()).isNull();
    }

    @Test
    void send_success_countsTheEmail() {
        TemplatedEmail email = service.expired(application(), "John Doe");

        service.send("john@example.com", email);

        verify(emailService).sendTemplateEmail(eq("john@example.com"), eq(EmailSubjects.APPLICATION_EXPIRED),
                eq("application-expired"), any(EmailContext.class));
        assertThat(meterRegistry.counter("sas.emails.sent").count()).isEqualTo(1.0);
    }

    @Test
    void send_failure_countsAndRethrows() {
        TemplatedEmail email = service.expired(application(), "John Doe");
        doThrow(new IllegalStateException("smtp")).when(emailService)
                .sendTemplateEmail(anyString(), anyString(), anyString(), any(EmailContext.class));

        assertThatThrownBy(() -> service.send("john@example.com", email)).isInstanceOf(IllegalStateException.class);
        assertThat(meterRegistry.counter("sas.emails.failed").count()).isEqualTo(1.0);
    }
}
