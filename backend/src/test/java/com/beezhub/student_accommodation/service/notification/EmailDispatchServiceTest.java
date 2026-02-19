package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.*;
import com.beezhub.student_accommodation.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailDispatchServiceTest {

    @Mock
    private EmailService emailService;

    @Mock
    private EmailContentBuilder contentBuilder;

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
        // Set @Value fields
        ReflectionTestUtils.setField(emailDispatchService, "baseUrl", "http://test-url");
        ReflectionTestUtils.setField(emailDispatchService, "supportEmail", "support@test.edu");

        // Setup common test data
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

        verify(emailService).sendApplicationApprovedEmail(
                eq("john@example.com"),
                eq("John Doe"),
                eq(1L),
                eq("01 January 2026"),
                eq("2026/2027"),
                eq("First Semester"),
                anyString()
        );
    }

    @Test
    void sendApplicationRejectedEmail_validData_sendsEmail() {
        when(contentBuilder.formatDate(any(java.time.LocalDate.class))).thenReturn("01 January 2026");
        when(contentBuilder.formatDate(any(LocalDateTime.class))).thenReturn("01 January 2026");
        when(contentBuilder.getNextApplicationPeriod()).thenReturn("June 2026");

        emailDispatchService.sendApplicationRejectedEmail(application, "John Doe", "john@example.com");

        verify(emailService).sendApplicationRejectedEmail(
                eq("john@example.com"),
                eq("John Doe"),
                eq(1L),
                anyString(),
                eq("01 January 2026"),
                eq("01 January 2026"),
                eq("June 2026"),
                anyString()
        );
    }

    @Test
    void sendDocumentVerifiedEmail_validData_sendsEmail() {
        when(contentBuilder.formatDate(any(LocalDateTime.class))).thenReturn("01 January 2026");

        emailDispatchService.sendDocumentVerifiedEmail(document, "John Doe", "john@example.com", "ID Document");

        verify(emailService).sendDocumentVerifiedEmail(
                eq("john@example.com"),
                eq("John Doe"),
                eq("ID Document"),
                anyString(),
                eq("01 January 2026"),
                anyString(),
                anyString()
        );
    }

    @Test
    void sendDocumentRejectedEmail_validData_sendsEmail() {
        when(contentBuilder.formatDate(any(LocalDateTime.class))).thenReturn("01 January 2026");

        emailDispatchService.sendDocumentRejectedEmail(document, "John Doe", "john@example.com", "ID Document", "Invalid file");

        verify(emailService).sendDocumentRejectedEmail(
                eq("john@example.com"),
                eq("John Doe"),
                eq("ID Document"),
                anyString(),
                eq("01 January 2026"),
                eq("01 January 2026"),
                anyString(),
                eq("01 January 2026"),
                anyString(),
                anyMap()
        );
    }

    @Test
    void sendDocumentCommentEmail_validData_sendsEmail() {
        when(contentBuilder.buildDocumentCommentEmailContent(any(), any(), any(), any(), any(), any())).thenReturn("<html></html>");

        emailDispatchService.sendDocumentCommentEmail(document, "Comment", "John Doe", "john@example.com", "MyDoc", "ID Document", "Subject");

        verify(emailService).sendHtmlEmail(
                eq("john@example.com"),
                eq("Subject"),
                eq("<html></html>")
        );
    }

    @Test
    void sendRoomAllocatedEmail_validData_sendsEmail() {
        when(contentBuilder.extractFloorNumber(anyString())).thenReturn(1);
        when(contentBuilder.determineRoomType(any(Room.class))).thenReturn("Single");
        when(contentBuilder.formatDate(any(LocalDateTime.class))).thenReturn("01 January 2026");
        when(contentBuilder.calculateDuration(any(), any())).thenReturn("1 year");

        emailDispatchService.sendRoomAllocatedEmail(allocation, room, building, "John Doe", "john@example.com");

        verify(emailService).sendRoomAllocatedEmail(
                eq("john@example.com"),
                eq("John Doe"),
                eq("101"),
                eq("Test Building"),
                eq(1),
                eq("Single"),
                anyString(),
                anyString(),
                eq("01 January 2026"),
                anyString(),
                eq("1 year"),
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    void sendAllocationProposalEmail_validData_sendsEmail() {
        when(contentBuilder.extractFloorNumber(anyString())).thenReturn(1);
        when(contentBuilder.parseWarnings(any())).thenReturn(List.of());

        emailDispatchService.sendAllocationProposalEmail(stagedAllocation, room, building, "John Doe", "john@example.com");

        verify(emailService).sendAllocationProposalEmail(
                eq("john@example.com"),
                eq("John Doe"),
                eq("101"),
                eq("Test Building"),
                eq(1),
                anyInt(),
                anyString(),
                any(),
                anyList(),
                isNull(),
                anyString(),
                anyString()
        );
    }

    @Test
    void sendOfferDeclinedRequeueEmail_validData_sendsEmail() {
        when(contentBuilder.buildOfferDeclinedRequeueEmailContent(any(), any(), any(), anyInt(), anyInt(), any(), any())).thenReturn("<html></html>");

        emailDispatchService.sendOfferDeclinedRequeueEmail(allocation, room, "John Doe", "john@example.com", 1, 2, "Subject");

        verify(emailService).sendHtmlEmail(
                eq("john@example.com"),
                eq("Subject"),
                eq("<html></html>")
        );
    }
}
