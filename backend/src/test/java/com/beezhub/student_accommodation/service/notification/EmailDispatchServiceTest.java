package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.*;
import com.beezhub.student_accommodation.service.EmailService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Answers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
        when(contentBuilder.getCurrentAcademicYear()).thenReturn("2026/2027");
        when(contentBuilder.getCurrentSemester()).thenReturn("First Semester");

        emailDispatchService.sendApplicationApprovedEmail(application, "John Doe", "john@example.com");

        verify(emailService).sendTemplateEmail(
                eq("john@example.com"),
                eq(EmailSubjects.APPLICATION_APPROVED),
                eq("application-approved"),
                any(ApplicationApprovedContext.class)
        );
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
        when(contentBuilder.extractFloorNumber(anyString())).thenReturn(1);
        when(contentBuilder.determineRoomType(any(Room.class))).thenReturn("Single");
        when(contentBuilder.formatDate(any(LocalDateTime.class))).thenReturn("01 January 2026");
        when(contentBuilder.calculateDuration(any(), any())).thenReturn("1 year");

        emailDispatchService.sendRoomAllocatedEmail(allocation, room, building, "John Doe", "john@example.com");

        verify(emailService).sendTemplateEmail(
                eq("john@example.com"),
                eq(EmailSubjects.ROOM_ALLOCATED),
                eq("room-allocated"),
                any(RoomAllocatedContext.class)
        );
    }

    @Test
    void sendAllocationProposalEmail_validData_sendsEmail() {
        when(contentBuilder.extractFloorNumber(anyString())).thenReturn(1);
        when(contentBuilder.parseWarnings(any())).thenReturn(List.of());

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
}
