package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.*;
import com.beezhub.student_accommodation.service.EmailService;
import com.beezhub.student_accommodation.service.allocation.WaitingListService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Answers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailDispatchServiceTest {

    @Mock
    private EmailService emailService;

    @Mock
    private EmailContentBuilder contentBuilder;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MeterRegistry meterRegistry;

    @InjectMocks
    private EmailDispatchService emailDispatchService;

    private Application application;
    private Student student;
    private AppUser user;
    private Document document;
    private RoomAllocation allocation;
    private Room room;
    private Building building;
    private StagedAllocation stagedAllocation;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailDispatchService, "baseUrl", "http://test-url");
        ReflectionTestUtils.setField(emailDispatchService, "supportEmail", "support@test.edu");
        ReflectionTestUtils.setField(emailDispatchService, "contactPhone", "+27 (0) 11 000 0000");

        user = new AppUser();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@example.com");

        student = new Student();
        student.setAppUser(user);

        application = new Application();
        application.setId(1L);
        application.setStudent(student);
        application.setApplicationDate(java.time.LocalDate.now());

        DocumentType docType = new DocumentType();
        docType.setId(1L);
        docType.setTypeName("ID Document");
        docType.setIsRequired(true);
        docType.setDescription("Test description");

        document = new Document();
        document.setId(1L);
        document.setStudent(student);
        document.setDocumentType(docType);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());

        building = new Building();
        building.setName("Test Building");
        building.setAddress("123 Test St");

        room = new Room();
        room.setRoomNumber("101");
        room.setBuilding(building);
        room.setCapacity(1);

        allocation = new RoomAllocation();
        allocation.setId(1L);
        allocation.setStudent(student);
        allocation.setRoom(room);
        allocation.setApplication(application);
        allocation.setAllocationDate(LocalDateTime.now());

        stagedAllocation = new StagedAllocation();
        stagedAllocation.setId(1L);
        stagedAllocation.setStudent(student);
        stagedAllocation.setRoom(room);
        stagedAllocation.setScore(BigDecimal.TEN);
    }

    @Test
    void sendApplicationApprovedEmail_validData_sendsEmail() {
        when(contentBuilder.formatDate(any(java.time.LocalDate.class))).thenReturn("01 January 2026");
        when(contentBuilder.academicYearOf(application)).thenReturn("2027");
        when(contentBuilder.intakeOf(application)).thenReturn("Semester 2");

        emailDispatchService.sendApplicationApprovedEmail(application, "John Doe", "john@example.com");

        verify(emailService).sendTemplateEmail(
                eq("john@example.com"),
                eq(EmailSubjects.APPLICATION_APPROVED),
                eq("application-approved"),
                any(ApplicationApprovedContext.class)
        );
    }

    /** GH-495: the email names the application's own year and intake, not the calendar's. */
    @Test
    void sendApplicationApprovedEmail_namesTheApplicationsYearAndIntake() {
        when(contentBuilder.formatDate(any(java.time.LocalDate.class))).thenReturn("01 January 2026");
        when(contentBuilder.academicYearOf(application)).thenReturn("2027");
        when(contentBuilder.intakeOf(application)).thenReturn("Semester 2");

        emailDispatchService.sendApplicationApprovedEmail(application, "John Doe", "john@example.com");

        ArgumentCaptor<ApplicationApprovedContext> ctx = ArgumentCaptor.forClass(ApplicationApprovedContext.class);
        verify(emailService).sendTemplateEmail(eq("john@example.com"), eq(EmailSubjects.APPLICATION_APPROVED),
                eq("application-approved"), ctx.capture());
        assertThat(ctx.getValue().academicYear()).isEqualTo("2027");
        assertThat(ctx.getValue().intake()).isEqualTo("Semester 2");
    }

    @Test
    void sendApplicationRejectedEmail_validData_sendsEmail() {
        when(contentBuilder.formatDate(any(java.time.LocalDate.class))).thenReturn("01 January 2026");
        when(contentBuilder.formatDate(any(LocalDateTime.class))).thenReturn("01 January 2026");
        when(contentBuilder.getNextApplicationPeriod()).thenReturn("June 2026");

        emailDispatchService.sendApplicationRejectedEmail(application, "John Doe", "john@example.com");

        verify(emailService).sendTemplateEmail(
                eq("john@example.com"),
                eq(EmailSubjects.APPLICATION_REJECTED),
                eq("application-rejected"),
                any(ApplicationRejectedContext.class)
        );
    }

    @Test
    void sendApplicationChangesRequestedEmail_validData_sendsReasonAndLinkToTheApplication() {
        application.setRemarks("Proof of registration is for the wrong year");
        application.setApplicationCode("APP-2026-001");

        emailDispatchService.sendApplicationChangesRequestedEmail(application, "John Doe", "john@example.com");

        verify(emailService).sendTemplateEmail(
                eq("john@example.com"),
                eq(EmailSubjects.APPLICATION_CHANGES_REQUESTED),
                eq("application-changes-requested"),
                eq(new ApplicationChangesRequestedContext(
                        "John Doe",
                        1L,
                        "APP-2026-001",
                        "Proof of registration is for the wrong year",
                        "http://test-url/applications/1")));
    }

    @Test
    void applicationPageUrl_returnsTheStudentApplicationRoute() {
        assertThat(emailDispatchService.applicationPageUrl(9L)).isEqualTo("http://test-url/applications/9");
    }

    @Test
    void sendDocumentVerifiedEmail_validData_sendsEmail() {
        when(contentBuilder.formatDate(any(LocalDateTime.class))).thenReturn("01 January 2026");

        emailDispatchService.sendDocumentVerifiedEmail(document, "John Doe", "john@example.com", "ID Document");

        verify(emailService).sendTemplateEmail(
                eq("john@example.com"),
                eq(EmailSubjects.DOCUMENT_VERIFIED),
                eq("document-verified"),
                any(DocumentVerifiedContext.class)
        );
    }

    @Test
    void sendDocumentRejectedEmail_validData_sendsEmail() {
        when(contentBuilder.formatDate(any(LocalDateTime.class))).thenReturn("01 January 2026");
        when(contentBuilder.parseFeedbackPoints(any())).thenReturn(List.of("Fix this"));

        emailDispatchService.sendDocumentRejectedEmail(document, "John Doe", "john@example.com", "ID Document", "Invalid file");

        verify(emailService).sendTemplateEmail(
                eq("john@example.com"),
                eq(EmailSubjects.DOCUMENT_REJECTED),
                eq("document-rejected"),
                any(DocumentRejectedContext.class)
        );
    }

    @Test
    void sendDocumentCommentEmail_validData_sendsEmail() {
        when(contentBuilder.formatDate(any(LocalDateTime.class))).thenReturn("01 January 2026");

        emailDispatchService.sendDocumentCommentEmail(document, "Comment", "John Doe", "john@example.com", "MyDoc", "ID Document", "Subject");

        verify(emailService).sendTemplateEmail(
                eq("john@example.com"),
                eq(EmailSubjects.DOCUMENT_COMMENT),
                eq("document-comment"),
                any(DocumentCommentContext.class)
        );
    }

    @Test
    void sendRoomAllocatedEmail_validData_sendsEmail() {
        when(contentBuilder.determineRoomType(any(Room.class))).thenReturn("Single");

        emailDispatchService.sendRoomAllocatedEmail(allocation, room, building, "John Doe", "john@example.com");

        verify(emailService).sendTemplateEmail(
                eq("john@example.com"),
                eq(EmailSubjects.ROOM_ALLOCATED),
                eq("room-allocated"),
                any(RoomAllocatedContext.class)
        );
    }

    /** The email has to name the moment the offer lapses and link to where the student accepts. */
    @Test
    void sendRoomAllocatedEmail_offer_carriesTheDeadlineAndTheAcceptLink() {
        LocalDateTime deadline = LocalDateTime.of(2026, 11, 11, 14, 30);
        allocation.setOfferExpiryDate(deadline);
        when(contentBuilder.formatDeadline(deadline)).thenReturn("11 November 2026 at 14:30 UTC");

        emailDispatchService.sendRoomAllocatedEmail(allocation, room, building, "John Doe", "john@example.com");

        ArgumentCaptor<RoomAllocatedContext> ctx = ArgumentCaptor.forClass(RoomAllocatedContext.class);
        verify(emailService).sendTemplateEmail(
                eq("john@example.com"), eq(EmailSubjects.ROOM_ALLOCATED), eq("room-allocated"), ctx.capture());
        assertThat(ctx.getValue().offerExpiryDate()).isEqualTo("11 November 2026 at 14:30 UTC");
        assertThat(ctx.getValue().portalUrl()).isEqualTo("http://test-url/dashboard");
    }

    /**
     * GH-529: the tenancy is the stay the student applied for, not the day the offer went out
     * plus ten months. On NONPROD an offer made on 11 Nov 2026 for a 15 Jan to 15 Dec 2027 stay
     * said 11 Nov 2026 to 11 Sep 2027.
     */
    @Test
    void sendRoomAllocatedEmail_tenancy_isTheStayNotTheOfferDate() {
        allocation.setAllocationDate(LocalDateTime.of(2026, 11, 11, 17, 34));
        allocation.setStayStartDate(java.time.LocalDate.of(2027, 1, 15));
        allocation.setStayEndDate(java.time.LocalDate.of(2027, 12, 15));
        when(contentBuilder.formatDate(java.time.LocalDate.of(2027, 1, 15))).thenReturn("15 January 2027");
        when(contentBuilder.formatDate(java.time.LocalDate.of(2027, 12, 15))).thenReturn("15 December 2027");
        when(contentBuilder.calculateDuration(java.time.LocalDate.of(2027, 1, 15), java.time.LocalDate.of(2027, 12, 15)))
                .thenReturn("11 months");

        RoomAllocatedContext ctx = sentRoomAllocatedContext();

        assertThat(ctx.startDate()).isEqualTo("15 January 2027");
        assertThat(ctx.endDate()).isEqualTo("15 December 2027");
        assertThat(ctx.duration()).isEqualTo("11 months");
    }

    @Test
    void sendRoomAllocatedEmail_noStayDates_inventsNoTenancy() {
        allocation.setStayStartDate(null);
        allocation.setStayEndDate(null);

        RoomAllocatedContext ctx = sentRoomAllocatedContext();

        assertThat(ctx.startDate()).isNull();
        assertThat(ctx.endDate()).isNull();
        assertThat(ctx.duration()).isNull();
    }

    /** The student portal has no map; the email links to the building on Google Maps instead. */
    @Test
    void sendRoomAllocatedEmail_buildingWithCoordinates_linksToTheMapAndUsesTheFullAddress() {
        building.setFormattedAddress("1 Jan Smuts Ave, Braamfontein, Johannesburg, 2000");
        building.setLatitude(new BigDecimal("-26.1929"));
        building.setLongitude(new BigDecimal("28.0305"));

        RoomAllocatedContext ctx = sentRoomAllocatedContext();

        assertThat(ctx.buildingAddress()).isEqualTo("1 Jan Smuts Ave, Braamfontein, Johannesburg, 2000");
        assertThat(ctx.mapsUrl()).isEqualTo("https://www.google.com/maps/search/?api=1&query=-26.1929,28.0305");
    }

    @Test
    void sendRoomAllocatedEmail_buildingWithoutCoordinates_hasNoMapLink() {
        RoomAllocatedContext ctx = sentRoomAllocatedContext();

        assertThat(ctx.buildingAddress()).isEqualTo("123 Test St");
        assertThat(ctx.mapsUrl()).isNull();
    }

    /**
     * GH-530: the confirmed email showed the offer date as both move-in and tenancy start, and
     * never mentioned the check-in deadline, the one date the student still has to act on.
     */
    @Test
    void sendAllocationConfirmedEmail_showsTheStayAndTheCheckInDeadline() {
        allocation.setAllocationDate(LocalDateTime.of(2026, 11, 11, 17, 34));
        allocation.setStayStartDate(java.time.LocalDate.of(2027, 1, 15));
        allocation.setStayEndDate(java.time.LocalDate.of(2027, 12, 15));
        allocation.setCheckInExpiryDate(LocalDateTime.of(2027, 1, 22, 0, 0));
        when(contentBuilder.formatDate(java.time.LocalDate.of(2027, 1, 15))).thenReturn("15 January 2027");
        when(contentBuilder.formatDate(java.time.LocalDate.of(2027, 12, 15))).thenReturn("15 December 2027");
        when(contentBuilder.formatDeadline(LocalDateTime.of(2027, 1, 22, 0, 0)))
                .thenReturn("22 January 2027 at 00:00 SAST");

        emailDispatchService.sendAllocationConfirmedEmail(allocation, room, building, "John Doe", "john@example.com");

        ArgumentCaptor<AllocationConfirmedContext> ctx = ArgumentCaptor.forClass(AllocationConfirmedContext.class);
        verify(emailService).sendTemplateEmail(eq("john@example.com"), eq(EmailSubjects.ALLOCATION_CONFIRMED),
                eq("allocation-confirmed"), ctx.capture());
        assertThat(ctx.getValue().startDate()).isEqualTo("15 January 2027");
        assertThat(ctx.getValue().endDate()).isEqualTo("15 December 2027");
        assertThat(ctx.getValue().checkInDeadline()).isEqualTo("22 January 2027 at 00:00 SAST");
    }

    private RoomAllocatedContext sentRoomAllocatedContext() {
        emailDispatchService.sendRoomAllocatedEmail(allocation, room, building, "John Doe", "john@example.com");
        ArgumentCaptor<RoomAllocatedContext> ctx = ArgumentCaptor.forClass(RoomAllocatedContext.class);
        verify(emailService).sendTemplateEmail(
                eq("john@example.com"), eq(EmailSubjects.ROOM_ALLOCATED), eq("room-allocated"), ctx.capture());
        return ctx.getValue();
    }

    @Test
    void sendAllocationProposalEmail_validData_sendsEmail() {

        emailDispatchService.sendAllocationProposalEmail(stagedAllocation, room, building, "John Doe", "john@example.com");

        verify(emailService).sendTemplateEmail(
                eq("john@example.com"),
                eq(EmailSubjects.ALLOCATION_PROPOSAL),
                eq("allocation-proposal"),
                any(AllocationProposalContext.class)
        );
    }

    @Test
    void sendOfferDeclinedRequeueEmail_validData_sendsEmail() {
        emailDispatchService.sendOfferDeclinedRequeueEmail(allocation, room, "John Doe", "john@example.com", 1, "Reason");

        verify(emailService).sendTemplateEmail(
                eq("john@example.com"),
                eq(EmailSubjects.OFFER_DECLINED_REQUEUE),
                eq("offer-declined-requeue"),
                any(OfferDeclinedRequeueContext.class)
        );
    }

    /** Declines one and two stay on the list, and the third closes the application (GH-459). */
    @Test
    void sendOfferDeclinedRequeueEmail_firstDecline_showsTheTrueCountAndLimit() {
        OfferDeclinedRequeueContext ctx = requeueContextFor(1);

        assertThat(ctx.totalDeclineCount()).isEqualTo(1);
        assertThat(ctx.declinesRemaining()).isEqualTo(1);
        assertThat(ctx.maxDeclines()).isEqualTo(WaitingListService.MAX_DECLINES);
    }

    @Test
    void sendOfferDeclinedRequeueEmail_secondDecline_showsNoneLeft() {
        OfferDeclinedRequeueContext ctx = requeueContextFor(2);

        assertThat(ctx.totalDeclineCount()).isEqualTo(2);
        assertThat(ctx.declinesRemaining()).isZero();
        assertThat(ctx.maxDeclines()).isEqualTo(WaitingListService.MAX_DECLINES);
    }

    private OfferDeclinedRequeueContext requeueContextFor(int totalDeclineCount) {
        emailDispatchService.sendOfferDeclinedRequeueEmail(
                allocation, room, "John Doe", "john@example.com", totalDeclineCount, "Reason");

        ArgumentCaptor<OfferDeclinedRequeueContext> ctx = ArgumentCaptor.forClass(OfferDeclinedRequeueContext.class);
        verify(emailService).sendTemplateEmail(
                eq("john@example.com"), eq(EmailSubjects.OFFER_DECLINED_REQUEUE), eq("offer-declined-requeue"),
                ctx.capture());
        return ctx.getValue();
    }

    /**
     * GH-141: the welcome path was the only live producer of a notification with null template
     * data, so the retry job could not rebuild the email and the row was stranded. This is the
     * only place a content assertion means anything, because this class is the one under test
     * here rather than a mock.
     */
    @Test
    void welcomeTemplateData_carriesEveryVariableTheTemplateReads() {
        user.setCreatedAt(LocalDateTime.of(2026, 1, 15, 9, 0));
        when(contentBuilder.formatDate(any(LocalDateTime.class))).thenReturn("15 Jan 2026");

        Map<String, Object> data = emailDispatchService.welcomeTemplateData(
                user, "Alice Smith", "alice@example.com");

        assertThat(data).containsOnlyKeys("templateName", "studentName", "email", "studentId",
                "registrationDate", "loginUrl");
        assertThat(data).containsEntry("templateName", "welcome");
        assertThat(data).containsEntry("studentName", "Alice Smith");
        assertThat(data).containsEntry("email", "alice@example.com");
        assertThat(data).containsEntry("registrationDate", "15 Jan 2026");
        assertThat(data).containsEntry("loginUrl", "http://test-url/login");
    }

    /** What is sent and what is stored come from the same call, so they cannot drift apart. */
    @Test
    void sendWelcomeEmail_sendsTheSameDataItWouldPersist() {
        user.setCreatedAt(LocalDateTime.of(2026, 1, 15, 9, 0));
        when(contentBuilder.formatDate(any(LocalDateTime.class))).thenReturn("15 Jan 2026");
        Map<String, Object> expected = emailDispatchService.welcomeTemplateData(
                user, "Alice Smith", "alice@example.com");

        emailDispatchService.sendWelcomeEmail(user, "Alice Smith", "alice@example.com");

        verify(emailService).sendTemplateEmail(
                eq("alice@example.com"), anyString(), eq("welcome"), eq(expected));
    }
}
