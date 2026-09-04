package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.model.event.DocumentVerificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentEventServiceTest {

    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private UserNotificationService userNotificationService;

    @InjectMocks
    private DocumentEventService service;

    private Document document;

    @BeforeEach
    void setUp() {
        AppUser user = new AppUser();
        user.setId(1L);

        Student student = new Student();
        student.setAppUser(user);

        DocumentType docType = new DocumentType();
        docType.setTypeName("Proof of Enrollment");

        document = new Document();
        document.setId(100L);
        document.setStudent(student);
        document.setDocumentType(docType);
    }

    @Test
    void publishDocumentStatusEvent_approved_publishesEventAndCreatesNotification() {
        service.publishDocumentStatusEvent(document, DocumentStatus.PENDING, DocumentStatus.APPROVED, null);

        verify(eventPublisher).publishEvent(any(DocumentVerificationEvent.class));
        verify(userNotificationService).createDocumentStatusNotification(1L, "Proof of Enrollment", true, null);
    }

    @Test
    void publishDocumentStatusEvent_rejected_notificationWithComment() {
        service.publishDocumentStatusEvent(document, DocumentStatus.PENDING, DocumentStatus.REJECTED, "Blurry image");

        verify(eventPublisher).publishEvent(any(DocumentVerificationEvent.class));
        verify(userNotificationService).createDocumentStatusNotification(1L, "Proof of Enrollment", false, "Blurry image");
    }

    @Test
    void publishDocumentStatusEvent_publisherThrows_exceptionSwallowed() {
        doThrow(new RuntimeException("Publish failed")).when(eventPublisher).publishEvent(any());

        // Should not throw — exception is caught and logged
        service.publishDocumentStatusEvent(document, DocumentStatus.PENDING, DocumentStatus.APPROVED, null);

        verify(userNotificationService, never()).createDocumentStatusNotification(anyLong(), anyString(), anyBoolean(), any());
    }
}
