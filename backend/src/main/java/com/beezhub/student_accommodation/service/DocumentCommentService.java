package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.aspect.LogActivity;
import com.beezhub.student_accommodation.exceptions.DocumentCommentNotFoundException;
import com.beezhub.student_accommodation.exceptions.DocumentNotFoundException;
import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.exceptions.UserNotFoundException;
import com.beezhub.student_accommodation.model.dto.DocumentCommentResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentComment;
import com.beezhub.student_accommodation.model.event.DocumentCommentEvent;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.DocumentCommentRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentCommentService {

    private final DocumentCommentRepository documentCommentRepository;
    private final DocumentRepository documentRepository;
    private final AppUserRepository appUserRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Adds a comment to a document.
     * Only admins can add comments to documents.
     *
     * @param documentId  The document ID
     * @param username    The admin's email/username
     * @param commentText The comment text
     * @return The created comment response
     */
    @Transactional
    @LogActivity(action = "CREATE", entityType = "DOCUMENT_COMMENT", description = "Added comment to document {0}", entityIdParamIndex = 0)
    public DocumentCommentResponse addComment(Long documentId, String username, String commentText) {
        var admin = getUserByEmail(username);
        return addCommentByAdminId(documentId, admin.getId(), commentText);
    }

    /**
     * Adds a comment to a document.
     * Only admins can add comments to documents.
     *
     * @param documentId  The document ID
     * @param adminId     The admin user ID
     * @param commentText The comment text
     * @return The created comment response
     */
    @Transactional
    public DocumentCommentResponse addCommentByAdminId(Long documentId, Long adminId, String commentText) {
        log.info("Adding comment to document {} by admin {}", documentId, adminId);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with id: " + documentId));

        AppUser admin = appUserRepository.findById(adminId)
                .orElseThrow(() -> new UserNotFoundException("Admin not found with id: " + adminId));

        if (!admin.getUserRole().isAdmin()) {
            throw new UnauthorizedAccessException("Only admins can add comments to documents");
        }

        DocumentComment comment = DocumentComment.builder()
                .document(document)
                .admin(admin)
                .commentText(commentText)
                .build();

        DocumentComment savedComment = documentCommentRepository.save(comment);
        log.info("Comment {} added to document {} by admin {}", savedComment.getId(), documentId, adminId);

        // Trigger notification to student
        try {
            String authorName = comment.getAdmin().getFirstName() + " " + comment.getAdmin().getLastName();
            eventPublisher.publishEvent(new DocumentCommentEvent(document, commentText, authorName));
            log.info("Notification event published for document comment on document {}", documentId);
        } catch (Exception e) {
            log.error("Failed to send notification for document comment: {}", e.getMessage());
        }

        return mapToCommentResponse(savedComment);
    }

    /**
     * Gets all comments for a document ordered by creation date descending.
     * Verifies that the user is either an admin or the document owner.
     *
     * @param documentId The document ID
     * @param username   The user's email/username requesting comments
     * @return List of comment responses
     */
    @Transactional(readOnly = true)
    public List<DocumentCommentResponse> getCommentsByDocument(Long documentId, String username) {
        log.info("Fetching comments for document {} by user {}", documentId, username);

        var user = getUserByEmail(username);
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with id: " + documentId));

        // Authorization check: admin or document owner
        boolean isAdmin = user.getUserRole().isAdmin();
        boolean isOwner = document.getStudent().getAppUser().getId().equals(user.getId());

        if (!isAdmin && !isOwner) {
            throw new UnauthorizedAccessException("You are not authorized to view comments for this document");
        }

        List<DocumentComment> comments = documentCommentRepository.findByDocument_IdOrderByCreatedAtDesc(documentId);
        log.info("Found {} comments for document {}", comments.size(), documentId);

        return comments.stream()
                .map(this::mapToCommentResponse)
                .toList();
    }

    /**
     * Deletes a comment from a document.
     * Only the admin who created the comment can delete it (unless they are a
     * super-admin).
     *
     * @param commentId The comment ID
     * @param username  The user's email/username requesting deletion
     */
    @Transactional
    @LogActivity(action = "DELETE", entityType = "DOCUMENT_COMMENT", description = "Deleted comment {0}", entityIdParamIndex = 0)
    public void deleteComment(Long commentId, String username) {
        var user = getUserByEmail(username);
        deleteCommentByUserId(commentId, user.getId());
    }

    /**
     * Deletes a comment from a document.
     * Only the admin who created the comment can delete it (unless they are a
     * super-admin).
     *
     * @param commentId The comment ID
     * @param adminId   The admin user ID requesting deletion
     */
    @Transactional
    public void deleteCommentByUserId(Long commentId, Long adminId) {
        log.info("Deleting comment {} by admin {}", commentId, adminId);

        DocumentComment comment = documentCommentRepository.findById(commentId)
                .orElseThrow(() -> new DocumentCommentNotFoundException("Comment not found with id: " + commentId));

        AppUser requestingAdmin = appUserRepository.findById(adminId)
                .orElseThrow(() -> new UserNotFoundException("Admin not found with id: " + adminId));

        // Check if the admin owns the comment or is authorized to delete
        boolean isOwner = comment.getAdmin().getId().equals(adminId);
        boolean isAdmin = requestingAdmin.getUserRole().isAdmin();

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedAccessException("You are not authorized to delete this comment");
        }

        documentCommentRepository.delete(comment);
        log.info("Comment {} deleted by admin {}", commentId, adminId);
    }

    /**
     * Creates a comment when rejecting a document.
     *
     * @param document    The document being rejected
     * @param admin       The admin rejecting the document
     * @param commentText The rejection reason/comment
     */
    @Transactional
    public void createRejectionComment(Document document, AppUser admin, String commentText) {
        if (commentText == null || commentText.trim().isEmpty()) {
            return;
        }

        DocumentComment comment = DocumentComment.builder()
                .document(document)
                .admin(admin)
                .commentText(commentText)
                .build();

        documentCommentRepository.save(comment);
        log.info("Rejection comment created for document {} by admin {}", document.getId(), admin.getId());
    }

    private AppUser getUserByEmail(String email) {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Student not found"));
    }

    /**
     * Maps a DocumentComment entity to a DocumentCommentResponse DTO.
     */
    private DocumentCommentResponse mapToCommentResponse(DocumentComment comment) {
        String adminName = comment.getAdmin().getFirstName() + " " + comment.getAdmin().getLastName();

        return DocumentCommentResponse.builder()
                .id(comment.getId())
                .documentId(comment.getDocument().getId())
                .adminName(adminName)
                .commentText(comment.getCommentText())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
