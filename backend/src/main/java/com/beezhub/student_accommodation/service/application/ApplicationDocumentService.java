package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.aspect.LogActivity;
import com.beezhub.student_accommodation.exceptions.ApplicationNotFoundException;
import com.beezhub.student_accommodation.exceptions.InvalidStatusException;
import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.model.dto.RequiredDocumentsChecklistResponse;
import com.beezhub.student_accommodation.model.dto.RequiredDocumentsChecklistResponse.DocumentChecklistItem;
import com.beezhub.student_accommodation.model.dto.ReviewProgressRequest;
import com.beezhub.student_accommodation.model.dto.ReviewProgressRequest.DocumentUpdate;
import com.beezhub.student_accommodation.model.dto.ReviewProgressResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentComment;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.DocumentCommentRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beezhub.student_accommodation.model.dto.DocumentWithCommentsResponse;
import com.beezhub.student_accommodation.model.dto.DocumentWithCommentsResponse.CommentResponse;
import java.util.stream.Collectors;
import java.util.List;

/**
 * Service handling document-related operations for applications.
 * Documents are owned at the student level (one per document type).
 * Checklist and review operations query documents directly via student_id.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ApplicationDocumentService {

        private final ApplicationRepository applicationRepository;
        private final DocumentRepository documentRepository;
        private final DocumentTypeRepository documentTypeRepository;
        private final DocumentCommentRepository documentCommentRepository;
        private final AppUserRepository appUserRepository;
        private final com.beezhub.student_accommodation.service.DocumentTypeService documentTypeService;

        /**
         * Gets the required documents checklist for an application.
         * Cross-checks required document types against the student's document pack.
         *
         * @param applicationId The application ID
         * @return RequiredDocumentsChecklistResponse with document status breakdown
         */
        public RequiredDocumentsChecklistResponse getRequiredDocumentsChecklist(Long applicationId) {
                log.info("Getting required documents checklist for application {}", applicationId);

                Application application = applicationRepository.findById(applicationId)
                                .orElseThrow(() -> new ApplicationNotFoundException(
                                                "Application not found with ID: " + applicationId));

                // Get required document types from the application's tenant effective view (name-based)
                List<DocumentType> requiredTypes = documentTypeService.getEffectiveDocumentTypes(application.getTenantId())
                                .stream().filter(DocumentType::getIsRequired).collect(java.util.stream.Collectors.toList());

                // Get all documents for this student's pack (documents live at student level)
                Long studentId = application.getStudent().getId();
                List<Document> studentDocuments = documentRepository.findAllByStudent_Id(studentId);

                // Build checklist items using name-based matching
                List<DocumentChecklistItem> checklistItems = new java.util.ArrayList<>();
                int approved = 0;
                int pending = 0;
                int rejected = 0;
                int notUploaded = 0;

                for (DocumentType docType : requiredTypes) {
                        // Match by type name (case-insensitive) — student's doc may reference a platform type ID
                        // while the effective required type may be a tenant shadow with a different ID
                        Document matchingDoc = studentDocuments.stream()
                                        .filter(doc -> doc.getDocumentType().getTypeName()
                                                .equalsIgnoreCase(docType.getTypeName()))
                                        .findFirst()
                                        .orElse(null);

                        DocumentChecklistItem item;
                        if (matchingDoc != null) {
                                String status = matchingDoc.getDocumentStatus().name();
                                item = DocumentChecklistItem.builder()
                                                .documentTypeId(docType.getId())
                                                .type(docType.getTypeName())
                                                .status(status)
                                                .uploadedAt(matchingDoc.getCreatedAt())
                                                .documentId(matchingDoc.getId())
                                                .documentName(matchingDoc.getDocumentName())
                                                .isRequired(true)
                                                .build();

                                switch (matchingDoc.getDocumentStatus()) {
                                        case APPROVED -> approved++;
                                        case PENDING -> pending++;
                                        case REJECTED -> rejected++;
                                }
                        } else {
                                item = DocumentChecklistItem.builder()
                                                .documentTypeId(docType.getId())
                                                .type(docType.getTypeName())
                                                .status("NOT_UPLOADED")
                                                .isRequired(true)
                                                .build();
                                notUploaded++;
                        }
                        checklistItems.add(item);
                }

                RequiredDocumentsChecklistResponse response = RequiredDocumentsChecklistResponse.builder()
                                .applicationId(applicationId)
                                .totalRequired(requiredTypes.size())
                                .approved(approved)
                                .pending(pending)
                                .rejected(rejected)
                                .notUploaded(notUploaded)
                                .documents(checklistItems)
                                .build();

                response.calculateAllApproved();
                log.info("Checklist for application {}: {}/{} approved", applicationId, approved, requiredTypes.size());

                return response;
        }

        /**
         * Saves document review progress atomically.
         * Updates document statuses and adds comments without completing the review.
         *
         * @param applicationId The application ID
         * @param request       The review progress request
         * @param username      The admin username
         * @return ReviewProgressResponse with operation result
         */
        @Transactional
        @LogActivity(action = "UPDATE", entityType = "APPLICATION", description = "Saved review progress for application {0}", entityIdParamIndex = 0)
        public ReviewProgressResponse saveReviewProgress(Long applicationId, ReviewProgressRequest request,
                        String username) {
                log.info("Saving review progress for application {} by {}", applicationId, username);

                Application application = applicationRepository.findById(applicationId)
                                .orElseThrow(() -> new ApplicationNotFoundException(
                                                "Application not found with ID: " + applicationId));

                AppUser admin = appUserRepository.findByEmail(username)
                                .orElseThrow(() -> new RuntimeException("User not found: " + username));

                if (!admin.getUserRole().isAdmin()) {
                        throw new UnauthorizedAccessException("Only admins can save review progress");
                }

                Long studentId = application.getStudent().getId();
                int documentsUpdated = 0;
                int commentsAdded = 0;

                for (DocumentUpdate update : request.getDocumentUpdates()) {
                        Document document = documentRepository.findById(update.getDocumentId())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Document not found with ID: " + update.getDocumentId()));

                        // Verify document belongs to this application's student
                        if (!document.getStudent().getId().equals(studentId)) {
                                throw new UnauthorizedAccessException(
                                                "Document " + update.getDocumentId()
                                                                + " does not belong to this application's student");
                        }

                        if (update.getNewStatus() != null && !update.getNewStatus().trim().isEmpty()) {
                                try {
                                        DocumentStatus newStatus = DocumentStatus
                                                        .valueOf(update.getNewStatus().toUpperCase());
                                        document.setDocumentStatus(newStatus);
                                        documentRepository.save(document);
                                        documentsUpdated++;
                                        log.info("Updated document {} status to {}", update.getDocumentId(), newStatus);
                                } catch (IllegalArgumentException e) {
                                        throw new InvalidStatusException(
                                                        "Invalid document status: " + update.getNewStatus());
                                }
                        }

                        if (update.getComment() != null && !update.getComment().trim().isEmpty()) {
                                DocumentComment comment = DocumentComment.builder()
                                                .document(document)
                                                .admin(admin)
                                                .commentText(update.getComment())
                                                .build();
                                documentCommentRepository.save(comment);
                                commentsAdded++;
                                log.info("Added comment to document {}", update.getDocumentId());
                        }
                }

                log.info("Review progress saved for application {}: {} documents updated, {} comments added",
                                applicationId, documentsUpdated, commentsAdded);

                return ReviewProgressResponse.builder()
                                .success(true)
                                .message("Review progress saved successfully")
                                .documentsUpdated(documentsUpdated)
                                .commentsAdded(commentsAdded)
                                .build();
        }

        /**
         * Checks if all required documents for an application's student are approved.
         *
         * @param applicationId The application ID
         * @return true if all required documents are approved
         */
        public boolean areAllRequiredDocumentsApproved(Long applicationId) {
                RequiredDocumentsChecklistResponse checklist = getRequiredDocumentsChecklist(applicationId);
                return Boolean.TRUE.equals(checklist.getAllApproved());
        }

        /**
         * Gets all documents for an application's student with their comments.
         *
         * @param applicationId The application ID
         * @return List of DocumentWithCommentsResponse
         */
        public List<DocumentWithCommentsResponse> getDocumentsWithComments(Long applicationId) {
                Application application = applicationRepository.findById(applicationId)
                                .orElseThrow(() -> new ApplicationNotFoundException(
                                                "Application not found with ID: " + applicationId));

                Long studentId = application.getStudent().getId();
                List<Document> studentDocuments = documentRepository.findAllByStudent_Id(studentId);

                return studentDocuments.stream()
                                .map(this::mapDocumentWithComments)
                                .collect(Collectors.toList());
        }

        private DocumentWithCommentsResponse mapDocumentWithComments(Document document) {
                List<DocumentComment> comments = documentCommentRepository
                                .findByDocument_IdOrderByCreatedAtDesc(document.getId());

                List<CommentResponse> commentResponses = comments.stream()
                                .map(comment -> CommentResponse.builder()
                                                .id(comment.getId())
                                                .adminName(comment.getAdmin().getFirstName() + " "
                                                                + comment.getAdmin().getLastName())
                                                .text(comment.getCommentText())
                                                .createdAt(comment.getCreatedAt())
                                                .build())
                                .collect(Collectors.toList());

                return DocumentWithCommentsResponse.builder()
                                .id(document.getId())
                                .fileName(document.getDocumentName())
                                .type(document.getDocumentType().getTypeName())
                                .status(document.getDocumentStatus().name())
                                .uploadedAt(document.getCreatedAt())
                                .documentTypeId(document.getDocumentType().getId())
                                .documentTypeName(document.getDocumentType().getTypeName())
                                .comments(commentResponses)
                                .build();
        }
}
