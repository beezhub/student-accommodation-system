package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.*;
import com.beezhub.student_accommodation.model.event.*;
import com.beezhub.student_accommodation.service.allocation.WaitingListService;
import jakarta.persistence.EntityManager;
import com.beezhub.student_accommodation.service.notification.NotificationPersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationEventListenerTest {

    @Mock
    private NotificationPersistenceService notificationService;

    @Mock
    private EmailDispatchService emailDispatchService;

    /**
     * Returns null from every {@code find}, so the listener falls back to the entity carried on the
     * event. That keeps these cases asserting on the same instances they always did, and covers the
     * deleted-row fallback path added for GH-131.
     */
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private NotificationEventListener notificationEventListener;

    private AppUser appUser;
    private Student student;
    private Notification notification;

    @BeforeEach
    void setUp() {
        appUser = AppUser.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        student = new Student();
        student.setId(1L);
        student.setAppUser(appUser);

        notification = Notification.builder()
                .id(1L)
                .subject("Test Subject")
                .build();
    }

    private Application createMockApplication() {
        Application app = mock(Application.class);
        when(app.getTenantId()).thenReturn(1L);
        when(app.getStudent()).thenReturn(student);
        when(app.getId()).thenReturn(10L);
        return app;
    }

    private Document createMockDocument() {
        Document doc = mock(Document.class);
        when(doc.getTenantId()).thenReturn(1L);
        when(doc.getStudent()).thenReturn(student);
        when(doc.getId()).thenReturn(20L);
        
        DocumentType type = mock(DocumentType.class);
        when(type.getTypeName()).thenReturn("ID_DOCUMENT");
        when(doc.getDocumentType()).thenReturn(type);
        
        return doc;
    }

    private RoomAllocation createMockRoomAllocation() {
        RoomAllocation alloc = mock(RoomAllocation.class);
        when(alloc.getTenantId()).thenReturn(1L);
        when(alloc.getStudent()).thenReturn(student);
        when(alloc.getId()).thenReturn(30L);
        
        Room room = mock(Room.class);
        Building building = mock(Building.class);
        when(room.getBuilding()).thenReturn(building);
        when(alloc.getRoom()).thenReturn(room);
        
        return alloc;
    }
    
    private StagedAllocation createMockStagedAllocation() {
        StagedAllocation alloc = mock(StagedAllocation.class);
        when(alloc.getTenantId()).thenReturn(1L);
        when(alloc.getStudent()).thenReturn(student);
        when(alloc.getId()).thenReturn(40L);
        
        Room room = mock(Room.class);
        Building building = mock(Building.class);
        when(room.getBuilding()).thenReturn(building);
        when(alloc.getRoom()).thenReturn(room);
        
        return alloc;
    }

    @Test
    void handleApplicationStatusChanged_Approved() throws Exception {
        Application application = createMockApplication();
        ApplicationStatusChangedEvent event = new ApplicationStatusChangedEvent(application, ApplicationStatus.PENDING, ApplicationStatus.APPROVED);

        when(notificationService.createNotification(anyString(), anyString(), any(NotificationType.class), any(), anyLong(), anyString(), any()))
                .thenReturn(notification);

        notificationEventListener.handleApplicationStatusChanged(event);

        verify(emailDispatchService).sendApplicationApprovedEmail(eq(application), eq("John Doe"), eq("test@example.com"));
        verify(notificationService).markNotificationAsSent(notification);
    }
    
    @Test
    void handleApplicationStatusChanged_Rejected() throws Exception {
        Application application = createMockApplication();
        ApplicationStatusChangedEvent event = new ApplicationStatusChangedEvent(application, ApplicationStatus.PENDING, ApplicationStatus.REJECTED);

        when(notificationService.createNotification(anyString(), anyString(), any(NotificationType.class), any(), anyLong(), anyString(), any()))
                .thenReturn(notification);

        notificationEventListener.handleApplicationStatusChanged(event);

        verify(emailDispatchService).sendApplicationRejectedEmail(eq(application), eq("John Doe"), eq("test@example.com"));
        verify(notificationService).markNotificationAsSent(notification);
    }

    @Test
    void handleApplicationStatusChanged_ChangesRequested_sendsTheEmailBeforeMarkingSent() {
        Application application = createMockApplication();
        when(application.getRemarks()).thenReturn("Proof of registration is for the wrong year");
        when(emailDispatchService.applicationPageUrl(10L)).thenReturn("http://portal/applications/10");
        ApplicationStatusChangedEvent event = new ApplicationStatusChangedEvent(
                application, ApplicationStatus.UNDER_REVIEW, ApplicationStatus.CHANGES_REQUESTED);
        when(notificationService.createNotification(anyString(), anyString(), any(NotificationType.class), any(), anyLong(), anyString(), any()))
                .thenReturn(notification);

        notificationEventListener.handleApplicationStatusChanged(event);

        InOrder inOrder = inOrder(emailDispatchService, notificationService);
        inOrder.verify(emailDispatchService).sendApplicationChangesRequestedEmail(application, "John Doe", "test@example.com");
        inOrder.verify(notificationService).markNotificationAsSent(notification);
    }

    /**
     * The retry job rebuilds the email from the stored row alone, so the row has to name the right
     * template and carry everything that template prints. It used to be typed and templated as a
     * rejection, so a retry would have told the student they were rejected.
     */
    @Test
    void handleApplicationStatusChanged_ChangesRequested_storesWhatARetryNeeds() {
        Application application = createMockApplication();
        when(application.getRemarks()).thenReturn("Proof of registration is for the wrong year");
        when(emailDispatchService.applicationPageUrl(10L)).thenReturn("http://portal/applications/10");
        ApplicationStatusChangedEvent event = new ApplicationStatusChangedEvent(
                application, ApplicationStatus.UNDER_REVIEW, ApplicationStatus.CHANGES_REQUESTED);
        when(notificationService.createNotification(anyString(), anyString(), any(NotificationType.class), any(), anyLong(), anyString(), any()))
                .thenReturn(notification);

        notificationEventListener.handleApplicationStatusChanged(event);

        verify(notificationService).createNotification(
                eq("test@example.com"),
                eq(EmailSubjects.APPLICATION_CHANGES_REQUESTED),
                eq(NotificationType.APPLICATION_CHANGES_REQUESTED),
                eq(appUser),
                eq(10L),
                eq("Application"),
                argThat(data -> "application-changes-requested".equals(data.get("templateName"))
                        && "John Doe".equals(data.get("studentName"))
                        && Long.valueOf(10L).equals(data.get("applicationId"))
                        && "Proof of registration is for the wrong year".equals(data.get("changesReason"))
                        && "http://portal/applications/10".equals(data.get("portalUrl"))));
    }

    @Test
    void handleApplicationStatusChanged_ChangesRequestedEmailFails_isNotMarkedSent() {
        Application application = createMockApplication();
        ApplicationStatusChangedEvent event = new ApplicationStatusChangedEvent(
                application, ApplicationStatus.UNDER_REVIEW, ApplicationStatus.CHANGES_REQUESTED);
        when(notificationService.createNotification(anyString(), anyString(), any(NotificationType.class), any(), anyLong(), anyString(), any()))
                .thenReturn(notification);
        RuntimeException failure = new RuntimeException("SMTP down");
        doThrow(failure).when(emailDispatchService)
                .sendApplicationChangesRequestedEmail(application, "John Doe", "test@example.com");

        notificationEventListener.handleApplicationStatusChanged(event);

        verify(notificationService).handleNotificationFailure(notification, failure);
        verify(notificationService, never()).markNotificationAsSent(any());
    }

    @Test
    void handleApplicationStatusChanged_Pending_DoesNothing() {
        Application application = createMockApplication();
        ApplicationStatusChangedEvent event = new ApplicationStatusChangedEvent(application, null, ApplicationStatus.PENDING);

        when(notificationService.createNotification(anyString(), anyString(), any(NotificationType.class), any(), anyLong(), anyString(), any()))
                .thenReturn(notification);

        notificationEventListener.handleApplicationStatusChanged(event);

        verify(emailDispatchService, never()).sendApplicationApprovedEmail(any(), any(), any());
        verify(emailDispatchService, never()).sendApplicationRejectedEmail(any(), any(), any());
    }
    
    @Test
    void handleApplicationStatusChanged_Exception_HandlesFailure() throws Exception {
        Application application = createMockApplication();
        ApplicationStatusChangedEvent event = new ApplicationStatusChangedEvent(application, ApplicationStatus.PENDING, ApplicationStatus.APPROVED);

        when(notificationService.createNotification(anyString(), anyString(), any(NotificationType.class), any(), anyLong(), anyString(), any()))
                .thenReturn(notification);
        doThrow(new RuntimeException("Email failed")).when(emailDispatchService).sendApplicationApprovedEmail(any(), anyString(), anyString());

        notificationEventListener.handleApplicationStatusChanged(event);

        verify(notificationService).handleNotificationFailure(eq(notification), any(Exception.class));
    }

    @Test
    void handleDocumentVerification_Approved() throws Exception {
        Document document = createMockDocument();
        DocumentVerificationEvent event = new DocumentVerificationEvent(document, DocumentStatus.PENDING, DocumentStatus.APPROVED, null);

        when(notificationService.createNotification(anyString(), anyString(), any(NotificationType.class), any(), anyLong(), anyString(), any()))
                .thenReturn(notification);

        notificationEventListener.handleDocumentVerification(event);

        verify(emailDispatchService).sendDocumentVerifiedEmail(eq(document), eq("John Doe"), eq("test@example.com"), eq("ID_DOCUMENT"));
        verify(notificationService).markNotificationAsSent(notification);
    }

    @Test
    void handleDocumentVerification_Rejected() throws Exception {
        Document document = createMockDocument();
        DocumentVerificationEvent event = new DocumentVerificationEvent(document, DocumentStatus.PENDING, DocumentStatus.REJECTED, "Blurry");

        when(notificationService.createNotification(anyString(), anyString(), any(NotificationType.class), any(), anyLong(), anyString(), any()))
                .thenReturn(notification);

        notificationEventListener.handleDocumentVerification(event);

        verify(emailDispatchService).sendDocumentRejectedEmail(eq(document), eq("John Doe"), eq("test@example.com"), eq("ID_DOCUMENT"), eq("Blurry"));
        verify(notificationService).markNotificationAsSent(notification);
    }

    @Test
    void handleDocumentComment() throws Exception {
        Document document = createMockDocument();
        when(document.getDocumentName()).thenReturn("ID_FILE.pdf");
        DocumentCommentEvent event = new DocumentCommentEvent(document, "Please re-upload", "System");

        when(notificationService.createNotification(anyString(), anyString(), any(NotificationType.class), any(), anyLong(), anyString(), any()))
                .thenReturn(notification);

        notificationEventListener.handleDocumentComment(event);

        // sendDocumentCommentEmail delegates to sendDocumentCommentEmailWithTemplate (merged path)
        verify(emailDispatchService).sendDocumentCommentEmailWithTemplate(eq(document), eq("Please re-upload"),
                eq("Accommodation Office"), eq("John Doe"), eq("test@example.com"), eq("ID_FILE.pdf"), eq("ID_DOCUMENT"));
        verify(notificationService).markNotificationAsSent(notification);
    }

    @Test
    void handleAllocationCreated() throws Exception {
        RoomAllocation allocation = createMockRoomAllocation();
        AllocationCreatedEvent event = new AllocationCreatedEvent(allocation);

        when(notificationService.createNotification(anyString(), anyString(), any(NotificationType.class), any(), anyLong(), anyString(), any()))
                .thenReturn(notification);

        notificationEventListener.handleAllocationCreated(event);

        verify(emailDispatchService).sendRoomAllocatedEmail(eq(allocation), any(), any(), eq("John Doe"), eq("test@example.com"));
        verify(notificationService).markNotificationAsSent(notification);
    }

    @Test
    void handleAllocationMoved() throws Exception {
        RoomAllocation allocation = createMockRoomAllocation();
        Room oldRoom = mock(Room.class);
        Building oldBuilding = mock(Building.class);
        when(oldRoom.getBuilding()).thenReturn(oldBuilding);
        
        AllocationMovedEvent event = new AllocationMovedEvent(allocation, oldRoom, LocalDateTime.now(), "Maintenance");

        when(notificationService.createNotification(anyString(), anyString(), any(NotificationType.class), any(), anyLong(), anyString(), any()))
                .thenReturn(notification);

        notificationEventListener.handleAllocationMoved(event);

        verify(emailDispatchService).sendAllocationMovedEmail(eq(allocation), eq(oldRoom), any(), eq(oldBuilding), 
                eq("John Doe"), eq("test@example.com"), any(), eq("Maintenance"));
        verify(notificationService).markNotificationAsSent(notification);
    }

    @Test
    void handleAllocationExpired_CheckIn() throws Exception {
        RoomAllocation allocation = createMockRoomAllocation();
        Application app = mock(Application.class);
        when(allocation.getApplication()).thenReturn(app);
        AllocationExpiredEvent event = new AllocationExpiredEvent(allocation, true);

        when(notificationService.createNotification(anyString(), anyString(), any(NotificationType.class), any(), anyLong(), anyString(), any()))
                .thenReturn(notification);

        notificationEventListener.handleAllocationExpired(event);

        verify(emailDispatchService).sendCheckInExpiredEmail(eq(allocation), eq(app), any(), eq("John Doe"), eq("test@example.com"), eq("Test Subject"));
        verify(notificationService).markNotificationAsSent(notification);
    }
    
    @Test
    void handleAllocationExpired_Offer() throws Exception {
        RoomAllocation allocation = createMockRoomAllocation();
        Application app = mock(Application.class);
        when(allocation.getApplication()).thenReturn(app);
        AllocationExpiredEvent event = new AllocationExpiredEvent(allocation, false);

        when(notificationService.createNotification(anyString(), anyString(), any(NotificationType.class), any(), anyLong(), anyString(), any()))
                .thenReturn(notification);

        notificationEventListener.handleAllocationExpired(event);

        verify(emailDispatchService).sendOfferExpiredEmail(eq(allocation), eq(app), any(), eq("John Doe"), eq("test@example.com"), eq("Test Subject"));
        verify(notificationService).markNotificationAsSent(notification);
    }

    @Test
    void handleAllocationAccepted() throws Exception {
        RoomAllocation allocation = createMockRoomAllocation();
        AllocationAcceptedEvent event = new AllocationAcceptedEvent(allocation);

        when(notificationService.createNotification(anyString(), anyString(), any(NotificationType.class), any(), anyLong(), anyString(), any()))
                .thenReturn(notification);

        notificationEventListener.handleAllocationAccepted(event);

        verify(emailDispatchService).sendAllocationConfirmedEmail(eq(allocation), any(), any(), eq("John Doe"), eq("test@example.com"));
        verify(notificationService).markNotificationAsSent(notification);
    }

    @Test
    void handleAllocationDeclined_Requeue() throws Exception {
        RoomAllocation allocation = createMockRoomAllocation();
        AllocationDeclinedEvent event = new AllocationDeclinedEvent(allocation, 1);

        when(notificationService.createNotification(anyString(), anyString(), any(NotificationType.class), any(), anyLong(), anyString(), any()))
                .thenReturn(notification);

        notificationEventListener.handleAllocationDeclined(event);

        verify(emailDispatchService).sendOfferDeclinedRequeueEmail(eq(allocation), any(), eq("John Doe"), eq("test@example.com"), eq(1), eq("Test Subject"));
        verify(notificationService).markNotificationAsSent(notification);
    }

    /** The third decline closes the application, so saying "back on the waiting list" is false (GH-459). */
    @Test
    void handleAllocationDeclined_finalDecline_sendsMaxReachedEmailNotRequeue() throws Exception {
        RoomAllocation allocation = createMockRoomAllocation();
        AllocationDeclinedEvent event = new AllocationDeclinedEvent(allocation, WaitingListService.MAX_DECLINES);

        when(notificationService.createNotification(anyString(), anyString(), any(NotificationType.class), any(), anyLong(), anyString(), any()))
                .thenReturn(notification);

        notificationEventListener.handleAllocationDeclined(event);

        verify(emailDispatchService).sendOfferDeclinedMaxReachedEmail(eq(allocation), any(), eq("John Doe"),
                eq("test@example.com"), eq(WaitingListService.MAX_DECLINES), eq("Test Subject"));
        verify(emailDispatchService, never()).sendOfferDeclinedRequeueEmail(any(), any(), any(), any(), anyInt(), any());
        verify(notificationService).createNotification(anyString(), eq(EmailSubjects.OFFER_DECLINED_MAX_REACHED),
                any(NotificationType.class), any(), anyLong(), anyString(), any());
        verify(notificationService).markNotificationAsSent(notification);
    }

    @Test
    void handleStagedAllocationProposed() throws Exception {
        StagedAllocation allocation = createMockStagedAllocation();
        StagedAllocationProposedEvent event = new StagedAllocationProposedEvent(allocation);

        when(notificationService.createNotification(anyString(), anyString(), any(NotificationType.class), any(), anyLong(), anyString(), any()))
                .thenReturn(notification);

        notificationEventListener.handleStagedAllocationProposed(event);

        verify(emailDispatchService).sendAllocationProposalEmail(eq(allocation), any(), any(), eq("John Doe"), eq("test@example.com"));
        verify(notificationService).markNotificationAsSent(notification);
    }

    @Test
    void handleStagedAllocationConfirmed() throws Exception {
        StagedAllocation stagedAllocation = createMockStagedAllocation();
        RoomAllocation allocation = createMockRoomAllocation();
        StagedAllocationConfirmedEvent event = new StagedAllocationConfirmedEvent(stagedAllocation, allocation);

        when(notificationService.createNotification(anyString(), anyString(), any(NotificationType.class), any(), anyLong(), anyString(), any()))
                .thenReturn(notification);

        notificationEventListener.handleStagedAllocationConfirmed(event);

        verify(emailDispatchService).sendAllocationConfirmedEmail(eq(allocation), any(), any(), eq("John Doe"), eq("test@example.com"));
        verify(notificationService).markNotificationAsSent(notification);
    }

    @Test
    void handleStagedAllocationRejected() throws Exception {
        StagedAllocation allocation = createMockStagedAllocation();
        StagedAllocationRejectedEvent event = new StagedAllocationRejectedEvent(allocation, "Full");

        when(notificationService.createNotification(anyString(), anyString(), any(NotificationType.class), any(), anyLong(), anyString(), any()))
                .thenReturn(notification);

        notificationEventListener.handleStagedAllocationRejected(event);

        verify(emailDispatchService).sendAllocationRejectedEmail(eq(allocation), any(), any(), eq("John Doe"), eq("test@example.com"), eq("Full"), eq(0));
        verify(notificationService).markNotificationAsSent(notification);
    }
}