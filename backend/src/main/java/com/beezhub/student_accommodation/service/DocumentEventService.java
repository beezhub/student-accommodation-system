package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.model.event.DocumentVerificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import static com.beezhub.student_accommodation.model.enums.DocumentStatus.APPROVED;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentEventService {

    private final ApplicationEventPublisher eventPublisher;
    private final UserNotificationService userNotificationService;

    public void publishDocumentStatusEvent(Document document, DocumentStatus oldStatus, DocumentStatus newStatus, String comment) {
        try {
            log.info("Triggering notification for document {} with status {}", document.getId(), newStatus);
            eventPublisher.publishEvent(new DocumentVerificationEvent(document, oldStatus, newStatus, comment));

            Long studentUserId = document.getStudent().getAppUser().getId();
            userNotificationService.createDocumentStatusNotification(
                    studentUserId,
                    document.getDocumentType().getTypeName(),
                    newStatus == APPROVED,
                    comment);
        } catch (Exception e) {
            log.error("Failed to trigger notification for document {} - notification will be retried by scheduler",
                    document.getId(), e);
        }
    }
}
