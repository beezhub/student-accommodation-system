package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.model.enums.NotificationType;
import com.beezhub.student_accommodation.model.event.*;
import com.beezhub.student_accommodation.service.EmailService;
import com.beezhub.student_accommodation.service.allocation.WaitingListService;
import com.beezhub.student_accommodation.service.application.CompanyIntakePolicy;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManager;
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The retry job sends only what the notification row stored, so a variable missing from the row is
 * content missing from the retried email (GH-141 made the rows sendable; this makes them complete).
 *
 * <p>Each email type runs through the real listener and the real dispatch service, and the stored
 * map is compared with the variables the template actually reads.
 */
class NotificationTemplateDataTest {

    private static final String BASE_URL = "http://test-url";
    private static final String COMMENT = "Please upload a clearer copy";

    private static final Pattern EXPRESSION = Pattern.compile("\\$\\{([^}]*)}");
    private static final Pattern LOCAL_VARIABLE = Pattern.compile("th:each=\"(\\w+)");
    private static final Pattern ROOT_NAME = Pattern.compile("(?<![.\\w])[a-zA-Z_]\\w*");
    private static final Set<String> NOT_VARIABLES = Set.of("null", "true", "false", "and", "or", "not", "eq", "ne");

    /** EmailService adds these to every email, on a retry as well. */
    private static final Set<String> LAYOUT_VARIABLES =
            Set.of("universityName", "supportEmail", "contactPhone", "currentYear");

    /**
     * Sent with no retryable row. The password reset URL holds a live token that must not be stored
     * (GH-141). The reminder jobs write no row: an unsent reminder is simply picked up on the next run.
     */
    private static final Set<String> NOT_RETRIED_FROM_A_ROW =
            Set.of("password-reset", "offer-expiry-reminder", "check-in-reminder",
                    DocumentExpiryNotificationListener.TEMPLATE);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private NotificationPersistenceService persistence;
    private EmailDispatchService dispatch;
    private NotificationEventListener listener;
    private TemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        persistence = mock(NotificationPersistenceService.class);
        when(persistence.createNotification(any(), any(), any(NotificationType.class), any(), any(), any(), any()))
                .thenReturn(Notification.builder().id(1L).subject("Subject").build());

        dispatch = new EmailDispatchService(
                mock(EmailService.class), new EmailContentBuilder(), new SimpleMeterRegistry());
        ReflectionTestUtils.setField(dispatch, "baseUrl", BASE_URL);

        CompanyIntakePolicy intakePolicy = mock(CompanyIntakePolicy.class);
        when(intakePolicy.openIntakes(any(), any())).thenReturn(List.of(intake("Semester 2",
                LocalDate.of(2027, 7, 15), LocalDate.of(2027, 11, 30))));
        ApplicationExpiryEmailService expiryEmails = new ApplicationExpiryEmailService(
                mock(EmailService.class), new EmailContentBuilder(), intakePolicy, new SimpleMeterRegistry());
        ReflectionTestUtils.setField(expiryEmails, "baseUrl", BASE_URL);

        AdminWelcomeEmailService adminWelcomeEmails = new AdminWelcomeEmailService(
                mock(EmailService.class), new SimpleMeterRegistry());
        ReflectionTestUtils.setField(adminWelcomeEmails, "baseUrl", BASE_URL);

        // find() returns null, so the listener uses the entity carried on the event.
        listener = new NotificationEventListener(persistence, dispatch, expiryEmails, adminWelcomeEmails,
                mock(EntityManager.class));

        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        templateEngine.addDialect(new LayoutDialect());
    }

    static Stream<Arguments> emailTypes() {
        return Stream.of(
                emailType("application-approved", l -> l.handleApplicationStatusChanged(
                        new ApplicationStatusChangedEvent(application(), ApplicationStatus.PENDING, ApplicationStatus.APPROVED))),
                emailType("application-rejected", l -> l.handleApplicationStatusChanged(
                        new ApplicationStatusChangedEvent(application(), ApplicationStatus.PENDING, ApplicationStatus.REJECTED))),
                emailType("application-changes-requested", l -> l.handleApplicationStatusChanged(
                        new ApplicationStatusChangedEvent(application(), ApplicationStatus.UNDER_REVIEW, ApplicationStatus.CHANGES_REQUESTED))),
                emailType("application-expired", l -> l.handleApplicationStatusChanged(
                        new ApplicationStatusChangedEvent(expiredApplication(), ApplicationStatus.PENDING, ApplicationStatus.EXPIRED))),
                emailType("document-verified",l -> l.handleDocumentVerification(
                        new DocumentVerificationEvent(document(), DocumentStatus.PENDING, DocumentStatus.APPROVED, null))),
                emailType("document-rejected", l -> l.handleDocumentVerification(
                        new DocumentVerificationEvent(document(), DocumentStatus.PENDING, DocumentStatus.REJECTED, COMMENT))),
                emailType("document-comment", l -> l.handleDocumentComment(
                        new DocumentCommentEvent(document(), COMMENT, "Reviewer"))),
                emailType("room-allocated", l -> l.handleAllocationCreated(new AllocationCreatedEvent(allocation()))),
                emailType("allocation-moved", l -> l.handleAllocationMoved(
                        new AllocationMovedEvent(allocation(), room("202"), LocalDateTime.now(), "Maintenance"))),
                emailType("offer-expired", l -> l.handleAllocationExpired(new AllocationExpiredEvent(allocation(), false))),
                emailType("check-in-expired", l -> l.handleAllocationExpired(new AllocationExpiredEvent(allocation(), true))),
                emailType("allocation-confirmed", l -> l.handleAllocationAccepted(new AllocationAcceptedEvent(allocation()))),
                emailType("offer-declined-requeue", l -> l.handleAllocationDeclined(
                        new AllocationDeclinedEvent(allocation(), 1))),
                emailType("offer-declined-max-reached", l -> l.handleAllocationDeclined(
                        new AllocationDeclinedEvent(allocation(), WaitingListService.MAX_DECLINES))),
                emailType("allocation-proposal", l -> l.handleStagedAllocationProposed(
                        new StagedAllocationProposedEvent(stagedAllocation()))),
                emailType("allocation-rejected", l -> l.handleStagedAllocationRejected(
                        new StagedAllocationRejectedEvent(stagedAllocation(), "Room withdrawn"))),
                emailType(AdminWelcomeEmailService.TEMPLATE, l -> l.handleAdminTenantAssigned(
                        new AdminTenantAssignedEvent(companyAdmin(), Tenant.builder().id(1L).name("Company A").build()))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("emailTypes")
    void storedTemplateData_everyEmailType_carriesEveryVariableTheTemplateReads(
            String template, Consumer<NotificationEventListener> fire) throws IOException {
        Map<String, Object> stored = storedBy(fire);

        assertThat(stored).containsEntry("templateName", template);
        assertThat(stored).containsKeys(variablesReadBy(template).toArray(String[]::new));
    }

    /** What the retry job reads back has been through jsonb, so that is what has to render. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("emailTypes")
    void storedTemplateData_afterAJsonRoundTrip_rendersWithItsLink(
            String template, Consumer<NotificationEventListener> fire) throws IOException {
        String html = renderRetry(template, storedBy(fire));

        assertThat(html).contains("href=\"" + BASE_URL + "/");
    }

    @Test
    void storedTemplateData_documentComment_rendersTheComment() throws IOException {
        Map<String, Object> stored = storedBy(l -> l.handleDocumentComment(
                new DocumentCommentEvent(document(), COMMENT, "Reviewer")));

        assertThat(renderRetry("document-comment", stored)).contains(COMMENT);
    }

    @Test
    void storedTemplateData_welcome_carriesEveryVariableTheTemplateReads() throws IOException {
        Map<String, Object> stored = dispatch.welcomeTemplateData(student().getAppUser(), "John Doe", "john@example.com");

        assertThat(stored).containsKeys(variablesReadBy("welcome").toArray(String[]::new));
    }

    /** A new template has to be added above, or say here why a retry never reads it. */
    @Test
    void emailTypes_everyTemplate_isCoveredOrDeliberatelyExempt() throws IOException {
        Set<String> onDisk = Arrays.stream(new PathMatchingResourcePatternResolver()
                        .getResources("classpath:templates/email/*.html"))
                .map(resource -> resource.getFilename().replace(".html", ""))
                .filter(name -> !name.startsWith("_"))
                .collect(Collectors.toSet());

        Set<String> accountedFor = emailTypes().map(type -> (String) type.get()[0]).collect(Collectors.toSet());
        reconfirmationEmailTypes().map(type -> (String) type.get()[0]).forEach(accountedFor::add);
        accountedFor.add(WithdrawalNotificationListener.TEMPLATE);
        accountedFor.add(StayExtensionNotificationListener.TEMPLATE);
        accountedFor.add("welcome");
        accountedFor.addAll(NOT_RETRIED_FROM_A_ROW);

        assertThat(accountedFor).containsExactlyInAnyOrderElementsOf(onDisk);
    }

    /** GH-496: the reconfirmation emails go through their own listener. */
    static Stream<Arguments> reconfirmationEmailTypes() {
        return Stream.of(
                Arguments.of("reconfirm-request", (Consumer<ReconfirmationNotificationListener>) l ->
                        l.handleReconfirmationRequested(new ReconfirmationRequestedEvent(
                                reconfirmAllocation(), false, LocalDate.of(2027, 1, 18)))),
                Arguments.of("reconfirm-reminder", (Consumer<ReconfirmationNotificationListener>) l ->
                        l.handleReconfirmationRequested(new ReconfirmationRequestedEvent(
                                reconfirmAllocation(), true, LocalDate.of(2027, 1, 18)))),
                Arguments.of("place-released", (Consumer<ReconfirmationNotificationListener>) l ->
                        l.handlePlaceReleased(new PlaceReleasedEvent(reconfirmAllocation()))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("reconfirmationEmailTypes")
    @SuppressWarnings("unchecked")
    void storedTemplateData_reconfirmationEmails_carryEveryVariableAndRender(
            String template, Consumer<ReconfirmationNotificationListener> fire) throws IOException {
        ReconfirmationEmailService emails = new ReconfirmationEmailService(
                mock(EmailService.class), new EmailContentBuilder(), new SimpleMeterRegistry());
        ReflectionTestUtils.setField(emails, "baseUrl", BASE_URL);
        fire.accept(new ReconfirmationNotificationListener(persistence, emails, mock(EntityManager.class)));
        ArgumentCaptor<Map<String, Object>> captured = ArgumentCaptor.forClass(Map.class);
        verify(persistence).createNotification(any(), any(), any(NotificationType.class), any(), any(), any(),
                captured.capture());
        Map<String, Object> stored = captured.getValue();

        assertThat(stored).containsEntry("templateName", template);
        assertThat(stored).containsKeys(variablesReadBy(template).toArray(String[]::new));
        assertThat(renderRetry(template, stored)).contains("href=\"" + BASE_URL + "/").contains("01 February 2027");
    }

    /** GH-497: the withdrawal email goes through its own listener and lists every application. */
    @Test
    @SuppressWarnings("unchecked")
    void storedTemplateData_applicationsWithdrawn_carriesEveryVariableAndRenders() throws IOException {
        EntityManager entityManager = mock(EntityManager.class);
        when(entityManager.find(Student.class, 1L)).thenReturn(student());
        when(entityManager.find(RoomAllocation.class, 30L)).thenReturn(allocation());
        WithdrawalNotificationListener withdrawal = new WithdrawalNotificationListener(persistence,
                mock(EmailService.class), mock(com.beezhub.student_accommodation.service.UserNotificationService.class),
                mock(com.beezhub.student_accommodation.repository.AppUserRepository.class), entityManager,
                new SimpleMeterRegistry());
        ReflectionTestUtils.setField(withdrawal, "baseUrl", BASE_URL);

        withdrawal.handleApplicationsWithdrawn(new ApplicationsWithdrawnEvent(1L, 30L, List.of(
                new ApplicationsWithdrawnEvent.Withdrawn(11L, "APP-11", 8L, "Company B", "Semester 1, 2027", 0))));
        ArgumentCaptor<Map<String, Object>> captured = ArgumentCaptor.forClass(Map.class);
        verify(persistence).createNotification(any(), any(), any(NotificationType.class), any(), any(), any(),
                captured.capture());
        Map<String, Object> stored = captured.getValue();

        assertThat(stored).containsEntry("templateName", "applications-withdrawn");
        assertThat(stored).containsKeys(variablesReadBy("applications-withdrawn").toArray(String[]::new));
        assertThat(renderRetry("applications-withdrawn", stored)).contains("href=\"" + BASE_URL + "/")
                .contains("APP-11 at Company B, Semester 1, 2027");
    }

    /** GH-498: the stay-extended confirmation is stored for retry and renders from the stored row. */
    @Test
    @SuppressWarnings("unchecked")
    void storedTemplateData_stayExtended_carriesEveryVariableAndRenders() throws IOException {
        EntityManager entityManager = mock(EntityManager.class);
        when(entityManager.find(RoomAllocation.class, 30L)).thenReturn(allocation());
        StayExtensionNotificationListener stayListener = new StayExtensionNotificationListener(persistence,
                mock(EmailService.class), new EmailContentBuilder(),
                mock(com.beezhub.student_accommodation.service.UserNotificationService.class),
                mock(com.beezhub.student_accommodation.repository.AppUserRepository.class), entityManager,
                new SimpleMeterRegistry());
        ReflectionTestUtils.setField(stayListener, "baseUrl", BASE_URL);

        stayListener.handleStayExtended(new StayExtendedEvent(30L, "Semester 2",
                LocalDate.of(2027, 6, 30), LocalDate.of(2027, 11, 30)));
        ArgumentCaptor<Map<String, Object>> captured = ArgumentCaptor.forClass(Map.class);
        verify(persistence).createNotification(any(), any(), any(NotificationType.class), any(), any(), any(),
                captured.capture());
        Map<String, Object> stored = captured.getValue();

        assertThat(stored).containsEntry("templateName", "stay-extended");
        assertThat(stored).containsKeys(variablesReadBy("stay-extended").toArray(String[]::new));
        assertThat(renderRetry("stay-extended", stored)).contains("href=\"" + BASE_URL + "/")
                .contains("30 November 2027");
    }

    private static RoomAllocation reconfirmAllocation() {
        RoomAllocation allocation = allocation();
        allocation.setStayStartDate(LocalDate.of(2027, 2, 1));
        allocation.setStayEndDate(LocalDate.of(2027, 11, 30));
        return allocation;
    }

    private static Arguments emailType(String template, Consumer<NotificationEventListener> fire) {
        return Arguments.of(template, fire);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> storedBy(Consumer<NotificationEventListener> fire) {
        fire.accept(listener);
        ArgumentCaptor<Map<String, Object>> stored = ArgumentCaptor.forClass(Map.class);
        verify(persistence).createNotification(any(), any(), any(NotificationType.class), any(), any(), any(),
                stored.capture());
        return stored.getValue();
    }

    private String renderRetry(String template, Map<String, Object> stored) throws IOException {
        Map<String, Object> reloaded = objectMapper.readValue(
                objectMapper.writeValueAsString(stored), new TypeReference<>() { });
        Map<String, Object> vars = new HashMap<>(reloaded);
        LAYOUT_VARIABLES.forEach(name -> vars.put(name, name));
        Context context = new Context();
        context.setVariables(vars);
        return templateEngine.process("email/" + template, context);
    }

    private static Set<String> variablesReadBy(String template) throws IOException {
        String source = new ClassPathResource("templates/email/" + template + ".html")
                .getContentAsString(StandardCharsets.UTF_8);

        Set<String> locals = new HashSet<>(LAYOUT_VARIABLES);
        locals.addAll(NOT_VARIABLES);
        Matcher local = LOCAL_VARIABLE.matcher(source);
        while (local.find()) {
            locals.add(local.group(1));
        }

        Set<String> variables = new HashSet<>();
        Matcher expression = EXPRESSION.matcher(source);
        while (expression.find()) {
            Matcher name = ROOT_NAME.matcher(expression.group(1).replaceAll("'[^']*'", "").replaceAll("#\\w+\\.\\w+", ""));
            while (name.find()) {
                variables.add(name.group());
            }
        }
        variables.removeAll(locals);
        assertThat(variables).as("variables parsed from %s", template).isNotEmpty();
        return variables;
    }

    private static Student student() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@example.com");
        user.setCreatedAt(LocalDateTime.of(2026, 1, 15, 9, 0));
        Student student = new Student();
        student.setId(1L);
        student.setAppUser(user);
        return student;
    }

    /** GH-419: an account created in user management, just added to its company. */
    private static AppUser companyAdmin() {
        AppUser admin = new AppUser();
        admin.setId(2L);
        admin.setFirstName("Ada");
        admin.setLastName("Admin");
        admin.setEmail("ada@company-a.test");
        admin.setTenantId(1L);
        admin.setMustChangePassword(true);
        return admin;
    }

    private static Application application() {
        Application application = new Application();
        application.setId(10L);
        application.setTenantId(1L);
        application.setStudent(student());
        application.setApplicationCode("APP-10");
        application.setApplicationDate(LocalDate.of(2026, 1, 10));
        return application;
    }

    /** GH-497: an application for Semester 1, which ended on 30 June 2027. */
    private static Application expiredApplication() {
        Application application = application();
        application.setIntake(intake("Semester 1", LocalDate.of(2027, 2, 1), LocalDate.of(2027, 6, 30)));
        application.setTenant(Tenant.builder().id(1L).name("Company A").build());
        return application;
    }

    private static Intake intake(String name, LocalDate start, LocalDate end) {
        AcademicYear year = AcademicYear.builder().id(5L).name("2027").build();
        return Intake.builder().id(6L).name(name).academicYear(year).stayStartDate(start).stayEndDate(end)
                .applicationOpenDate(start.minusMonths(4)).applicationCloseDate(start.minusDays(14)).build();
    }

    private static Document document() {
        DocumentType type = new DocumentType();
        type.setId(1L);
        type.setTypeName("ID Document");
        Document document = new Document();
        document.setId(20L);
        document.setTenantId(1L);
        document.setStudent(student());
        document.setDocumentType(type);
        document.setDocumentName("id.pdf");
        document.setCreatedAt(LocalDateTime.of(2026, 1, 11, 9, 0));
        document.setUpdatedAt(LocalDateTime.of(2026, 1, 12, 9, 0));
        return document;
    }

    private static Room room(String number) {
        Building building = new Building();
        building.setName("Block A");
        building.setAddress("1 Campus Drive");
        Room room = new Room();
        room.setId(Long.valueOf(number));
        room.setRoomNumber(number);
        room.setCapacity(2);
        room.setBuilding(building);
        return room;
    }

    private static RoomAllocation allocation() {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(30L);
        allocation.setTenantId(1L);
        allocation.setStudent(student());
        allocation.setRoom(room("101"));
        allocation.setApplication(application());
        allocation.setAllocationDate(LocalDateTime.of(2026, 2, 1, 9, 0));
        allocation.setOfferExpiryDate(LocalDateTime.of(2026, 2, 8, 9, 0));
        allocation.setCheckInExpiryDate(LocalDateTime.of(2026, 2, 15, 9, 0));
        return allocation;
    }

    private static StagedAllocation stagedAllocation() {
        StagedAllocation staged = new StagedAllocation();
        staged.setId(40L);
        staged.setTenantId(1L);
        staged.setStudent(student());
        staged.setRoom(room("101"));
        staged.setScore(BigDecimal.TEN);
        staged.setWarnings("[\"Far from campus\"]");
        return staged;
    }
}
