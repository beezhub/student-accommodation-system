package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.aspect.LogActivity;
import com.beezhub.student_accommodation.exceptions.ApplicationNotApprovedException;
import com.beezhub.student_accommodation.exceptions.DocumentNotFoundException;
import com.beezhub.student_accommodation.exceptions.InvalidStatusException;
import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.exceptions.UserNotFoundException;
import com.beezhub.student_accommodation.mapper.DocumentMapper;
import com.beezhub.student_accommodation.model.dto.DocumentResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.beezhub.student_accommodation.model.enums.DocumentStatus.APPROVED;
import static com.beezhub.student_accommodation.model.enums.DocumentStatus.REJECTED;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentStatusService {

    private final DocumentRepository documentRepository;
    private final DocumentCommentService documentCommentService;
    private final DocumentEventService documentEventService;
    private final ApplicationDocumentCoordinator applicationDocumentCoordinator;
    private final AppUserRepository appUserRepository;
    private final DocumentMapper documentMapper;
    private final com.beezhub.student_accommodation.service.document.DocumentAccessGuard documentAccessGuard;

    @Transactional
    @LogActivity(action = "UPDATE", entityType = "DOCUMENT", description = "Updated document {0} status to {1}", entityIdParamIndex = 0)
    public DocumentResponse updateDocumentStatus(Long documentId, String status, String comment, String username) {
        log.info("Updating document {} to status: {}", documentId, status);

        var user = getUserByEmail(username);
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        documentAccessGuard.assertCanAccessDocument(document, user);

        DocumentStatus oldStatus = document.getDocumentStatus();
        DocumentStatus newStatus = parseDocumentStatus(status);

        Application application = applicationDocumentCoordinator.findApprovedApplication(document.getStudent().getId());

        document.setDocumentStatus(newStatus);
        Document updatedDocument;
        try {
            updatedDocument = documentRepository.save(document);
        } catch (OptimisticLockingFailureException e) {
            throw e; // surface for handler; caller can retry
        }

        log.info("Document {} status updated from {} to: {}", documentId, oldStatus, newStatus);

        // Auto-create rejection comment if status is REJECTED and comment is provided
        if (newStatus == REJECTED && comment != null && !comment.trim().isEmpty()) {
            documentCommentService.createRejectionComment(updatedDocument, user, comment);
        }

        if (newStatus != oldStatus && (newStatus == APPROVED || newStatus == REJECTED)) {
            documentEventService.publishDocumentStatusEvent(updatedDocument, oldStatus, newStatus, comment);
        }

        // If approved, check all required docs and allocate room or enqueue waiting list
        if (newStatus == APPROVED) {
            applicationDocumentCoordinator.tryAllocateOrQueue(document.getStudent(), application);
        }

        DocumentResponse response = documentMapper.toDocumentResponse(updatedDocument);
        documentMapper.enrichWithApplication(response, application);
        documentMapper.enrichWithRoom(response, applicationDocumentCoordinator.findActiveAllocation(document.getStudent().getId()));
        return response;
    }

    private DocumentStatus parseDocumentStatus(String status) {
        try {
            return DocumentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidStatusException("Invalid document status: " + status);
        }
    }

    private AppUser getUserByEmail(String email) {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Student not found"));
    }
}
