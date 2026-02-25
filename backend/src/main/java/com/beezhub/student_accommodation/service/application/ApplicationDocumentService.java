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
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.ApplicationDocumentRepository;
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
 * Extracted from ApplicationAdminService.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ApplicationDocumentService {

        private final ApplicationRepository applicationRepository;
        private final DocumentRepository documentRepository;
        private final ApplicationDocumentRepository applicationDocumentRepository;
        private final DocumentTypeRepository documentTypeRepository;
        private final DocumentCommentRepository documentCommentRepository;
        private final AppUserRepository appUserRepository;

        /**
         * Gets the required documents checklist for an application.
         * Cross-checks required document types against student's uploaded documents.
         *
         * @param applicationId The application ID
         * @return RequiredDocumentsChecklistResponse with document status breakdown
         */
        public RequiredDocumentsChecklistResponse getRequiredDocumentsChecklist(Long applicationId) {
                log.info("Getting required documents checklist for application {}", applicationId);

                if (!applicationRepository.existsById(applicationId)) {
                        throw new ApplicationNotFoundException(
                                        "Application not found with ID: " + applicationId);
                }

                // Get all required document types
                List<DocumentType> requiredTypes = documentTypeRepository.findByIsRequiredTrue();

                // Get all documents for this application via junction table
                List<Document> applicationDocuments = applicationDocumentRepository
                                .findByApplication_Id(applicationId)
                                .stream()
                                .map(ad -> ad.getDocument())
                                .collect(Collectors.toList());

                // Build checklist items
                List<DocumentChecklistItem> checklistItems = new java.util.ArrayList<>();
                int approved = 0;
                int pending = 0;
                int rejected = 0;
                int notUploaded = 0;

                for (DocumentType docType : requiredTypes) {
                        // Find the latest document for this type (in case of resubmission)
                        Document matchingDoc = applicationDocuments.stream()
                                        .filter(doc -> doc.getDocumentType().getId().equals(docType.getId()))
                                        .reduce((first, second) -> second) // Get the latest (last) one
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

                // Verify application exists
                if (!applicationRepository.existsById(applicationId)) {
                        throw new ApplicationNotFoundException(
                                        "Application not found with ID: " + applicationId);
                }

                // Verify admin access
                AppUser admin = appUserRepository.findByEmail(username)
                                .orElseThrow(() -> new RuntimeException("User not found: " + username));

                if (!admin.getUserRole().isAdmin()) {
                        throw new UnauthorizedAccessException("Only admins can save review progress");
                }

                int documentsUpdated = 0;
                int commentsAdded = 0;

                for (DocumentUpdate update : request.getDocumentUpdates()) {
                        // Find the document
                        Document document = documentRepository.findById(update.getDocumentId())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Document not found with ID: " + update.getDocumentId()));

                        // Verify document belongs to this application via junction table
                        if (!applicationDocumentRepository.existsByApplication_IdAndDocument_Id(
                                        applicationId, update.getDocumentId())) {
                                throw new UnauthorizedAccessException(
                                                "Document " + update.getDocumentId()
                                                                + " does not belong to this application");
                        }

                        // Update document status if provided
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

                        // Add comment if provided
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
         * Checks if all required documents for an application are approved.
         *
         * @param applicationId The application ID
         * @return true if all required documents are approved
         */
        public boolean areAllRequiredDocumentsApproved(Long applicationId) {
                RequiredDocumentsChecklistResponse checklist = getRequiredDocumentsChecklist(applicationId);
                return Boolean.TRUE.equals(checklist.getAllApproved());
        }

        /**
         * Gets all documents for an application with their comments.
         *
         * @param applicationId The application ID
         * @return List of DocumentWithCommentsResponse
         */
        public List<DocumentWithCommentsResponse> getDocumentsWithComments(Long applicationId) {
                List<Document> applicationDocuments = applicationDocumentRepository
                                .findByApplication_Id(applicationId)
                                .stream()
                                .map(ad -> ad.getDocument())
                                .collect(Collectors.toList());
                return applicationDocuments.stream()
                                .map(this::mapDocumentWithComments)
                                .collect(Collectors.toList());
        }

        /**
         * Maps a Document entity to DocumentWithCommentsResponse including nested
         * comments.
         */
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
