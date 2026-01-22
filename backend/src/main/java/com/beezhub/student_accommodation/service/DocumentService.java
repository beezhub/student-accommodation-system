package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.ApplicationNotApprovedException;
import com.beezhub.student_accommodation.exceptions.DocumentNotFoundException;
import com.beezhub.student_accommodation.exceptions.InvalidStatusException;
import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import org.springframework.dao.OptimisticLockingFailureException;

import com.beezhub.student_accommodation.aspect.LogActivity;
import com.beezhub.student_accommodation.config.MinioConfig;
import com.beezhub.student_accommodation.mapper.DocumentMapper;
import com.beezhub.student_accommodation.model.dto.DocumentCommentResponse;
import com.beezhub.student_accommodation.model.dto.DocumentDownloadResponse;
import com.beezhub.student_accommodation.model.dto.DocumentResponse;
import com.beezhub.student_accommodation.model.dto.RoomAllocationRequest;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentComment;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.WaitingListEntry;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.DocumentCommentRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.WaitingListRepository;
import com.beezhub.student_accommodation.service.RoomAllocationService;
import com.beezhub.student_accommodation.specification.DocumentSpecification;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.beezhub.student_accommodation.model.enums.DocumentStatus.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final DocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final DocumentCommentRepository documentCommentRepository;
    private final AppUserRepository appUserRepository;
    private final StudentRepository studentRepository;
    private final DocumentMapper documentMapper;
    private final AuthService authService;
    private final NotificationService notificationService;
    private final UserNotificationService userNotificationService;
    private final ApplicationRepository applicationRepository;
    private final RoomAllocationRepository roomAllocationRepository;
    private final RoomAllocationService roomAllocationService;
    private final WaitingListRepository waitingListRepository;

    @LogActivity(action = "UPLOAD", entityType = "DOCUMENT", description = "Uploaded {0} documents", entityIdParamIndex = -1)
    public List<DocumentResponse> uploadDocuments(List<MultipartFile> files, String email, List<Long> documentTypeIds) {
        String bucketName = minioConfig.getBucketName();
        var user = getUserByEmail(email);
        List<DocumentResponse> documentResponses = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            DocumentType docType = getDocumentTypeById(documentTypeIds.get(i));
            String objectPath = buildObjectPath(user, docType, file);

            uploadFileToMinio(bucketName, objectPath, file);
            var document = saveDocumentRecord(user, docType, objectPath, file);
            documentResponses.add(documentMapper.toDocumentResponse(document));
        }
        return documentResponses;
    }

    public DocumentResponse getDocumentInfo(Long documentId, String username) {
        var user = getUserByEmail(username);
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        if (!document.getStudent().getAppUser().equals(user) && !user.getUserRole().isAdmin()) {
            throw new UnauthorizedAccessException("Unauthorized access to document");
        }

        String bucketName = minioConfig.getBucketName();
        String objectPath = document.getDocumentPath();
        var documentResponse = documentMapper.toDocumentResponse(document);
        documentResponse.setFileUrl(getPresignedUrl(bucketName, objectPath));
        return documentResponse;
    }

    public void uploadSingleDocument(MultipartFile file, String username, Long documentTypeId) {
        String bucketName = minioConfig.getBucketName();
        var user = getUserByEmail(username);
        DocumentType docType = getDocumentTypeById(documentTypeId);
        String objectPath = buildObjectPath(user, docType, file);
        uploadFileToMinio(bucketName, objectPath, file);
        saveDocumentRecord(user, docType, objectPath, file);
    }

    private String getPresignedUrl(String bucketName, String objectPath) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .method(Method.GET)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error generating presigned URL", e);
        }
    }

    private GetObjectResponse downloadFileFromMinio(String bucketName, String objectPath) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error downloading file from Minio: " + objectPath, e);
        }
    }

    private AppUser getUserByEmail(String email) {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));
    }

    private DocumentType getDocumentTypeById(Long docTypeId) {
        return documentTypeRepository.findById(docTypeId)
                .orElseThrow(() -> new RuntimeException("DocumentType not found"));
    }

    private String buildObjectPath(AppUser user, DocumentType docType, MultipartFile file) {
        return String.format("%s/%s/%s/%s",
                user.getUserRole().getRole(),
                user.getId(),
                docType.getTypeName(),
                file.getOriginalFilename()
        );
    }

    private void uploadFileToMinio(String bucketName, String objectPath, MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .stream(is, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error uploading file: " + file.getOriginalFilename(), e);
        }
    }

    private Document saveDocumentRecord(AppUser user, DocumentType docType, String objectPath, MultipartFile file) {
        var student = studentRepository.findStudentByAppUser(user)
                .orElseThrow(() -> new RuntimeException("Student not found for user ID: " + user.getId()));

        Application application = getActiveApplicationForStudent(student);

        var document = Document.builder()
                .documentName(file.getOriginalFilename())
                .documentType(docType)
                .documentPath(objectPath)
                .student(student)
                .application(application)
                .documentStatus(PENDING)
                .build();
        return documentRepository.save(document);
    }

    /**
     * Gets the active application for a student.
     * Active application is one with status PENDING, APPROVED, or AWAITING_ALLOCATION.
     *
     * @param student The student
     * @return The active application
     * @throws RuntimeException if no active application is found
     */
    private Application getActiveApplicationForStudent(Student student) {
        return applicationRepository.findByStudent_Id(student.getId())
                .stream()
                .filter(app -> app.getStatus() == ApplicationStatus.PENDING
                        || app.getStatus() == ApplicationStatus.APPROVED
                        || app.getStatus() == ApplicationStatus.AWAITING_ALLOCATION)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No active application found for student. Please create an application first."));
    }


    public DocumentDownloadResponse downloadDocument(Long documentId, String username) {
        var user = getUserByEmail(username);
        var document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!document.getStudent().getAppUser().equals(user) && !user.getUserRole().isAdmin()) {
            throw new RuntimeException("Unauthorized access to document");
        }

        String bucketName = minioConfig.getBucketName();
        String objectPath = document.getDocumentPath();

        var response = downloadFileFromMinio(bucketName, objectPath);
        var inputStreamResource = new InputStreamResource(response);
        return DocumentDownloadResponse.builder()
                .filename(document.getDocumentName())
                .resource(inputStreamResource)
                .contentType(response.headers().get("Content-Type"))
                .build();
    }

    public List<DocumentResponse> getAllDocumentsByUserId(Long userId, String username) {

        var user = getUserByEmail(username);
        if (!user.getId().equals(userId) && !user.getUserRole().isAdmin()) {
            throw new RuntimeException("Unauthorized access to documents for user ID: " + userId);
        }

        List<Document> documents = documentRepository.findAllByStudent_AppUser_Id(userId);
        return documentMapper.toDocumentResponseList(documents);
    }

    public DocumentResponse updateDocumentStatus(Long documentId, String status, String username) {
        return updateDocumentStatus(documentId, status, null, username);
    }

    @Transactional
    @LogActivity(action = "UPDATE", entityType = "DOCUMENT", description = "Updated document {0} status to {1}", entityIdParamIndex = 0)
    public DocumentResponse updateDocumentStatus(Long documentId, String status, String comment, String username) {
        log.info("Updating document {} to status: {}", documentId, status);

        var user = getUserByEmail(username);
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        if (!document.getStudent().getAppUser().equals(user) && !user.getUserRole().isAdmin()) {
            throw new UnauthorizedAccessException("Unauthorized access to document");
        }

        DocumentStatus oldStatus = document.getDocumentStatus();
        DocumentStatus newStatus;
        try {
            newStatus = DocumentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidStatusException("Invalid document status: " + status);
        }

        Application application = applicationRepository.findByStudent_Id(document.getStudent().getId())
                .stream()
                .filter(app -> app.getStatus() == ApplicationStatus.APPROVED)
                .findFirst()
                .orElseThrow(() -> new ApplicationNotApprovedException("Documents can only be reviewed for approved applications"));

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
            createRejectionComment(updatedDocument, user, comment);
            log.info("Auto-created rejection comment for document {}", documentId);
        }

        if (newStatus != oldStatus && (newStatus == APPROVED || newStatus == REJECTED)) {
            try {
                log.info("Triggering notification for document {} with status {}", documentId, newStatus);
                notificationService.sendDocumentVerificationNotification(updatedDocument, newStatus, comment);
                Long studentUserId = document.getStudent().getAppUser().getId();
                userNotificationService.createDocumentStatusNotification(
                        studentUserId,
                        document.getDocumentType().getTypeName(),
                        newStatus == APPROVED,
                        comment
                );
            } catch (Exception e) {
                log.error("Failed to trigger notification for document {} - notification will be retried by scheduler",
                        documentId, e);
            }
        }

        // If approved, check all required docs and allocate room or enqueue waiting list
        if (newStatus == APPROVED) {
            tryAllocateOrQueue(document.getStudent(), application, updatedDocument);
        }

        DocumentResponse response = documentMapper.toDocumentResponse(updatedDocument);
        documentMapper.enrichWithApplication(response, application);
        documentMapper.enrichWithRoom(response, findActiveAllocation(document.getStudent().getId()));
        return response;
    }

    private RoomAllocation findActiveAllocation(Long studentId) {
        return roomAllocationRepository.findActiveAllocationByStudentId(studentId).orElse(null);
    }

    private void tryAllocateOrQueue(Student student, Application application, Document updatedDocument) {
        // Check if all required docs are approved for this application
        List<Document> documents = documentRepository.findByApplicationId(application.getId());
        long requiredApproved = documents.stream()
                .filter(doc -> doc.getDocumentStatus() == APPROVED)
                .map(Document::getDocumentType)
                .filter(DocumentType::getIsRequired)
                .count();
        long requiredTotal = documents.stream()
                .map(Document::getDocumentType)
                .filter(DocumentType::getIsRequired)
                .count();

        if (requiredApproved < requiredTotal) {
            log.info("Student {} does not have all required documents approved yet", student.getId());
            return;
        }

        try {
            Room availableRoom = roomAllocationService.findAvailableRoom();
            if (availableRoom != null) {
                RoomAllocationRequest request = new RoomAllocationRequest();
                request.setStudentId(student.getId());
                request.setRoomId(availableRoom.getId());
                request.setApplicationId(application.getId());
                roomAllocationService.createRoomAllocation(request);
                log.info("Allocated room {} to student {} after document approvals", availableRoom.getRoomNumber(), student.getId());
            } else {
                enqueueWaitingList(student, application);
            }
        } catch (Exception e) {
            log.error("Failed to auto-allocate room for student {}: {}", student.getId(), e.getMessage(), e);
        }
    }

    private void enqueueWaitingList(Student student, Application application) {
        if (waitingListRepository.existsByStudent_IdAndStatus(student.getId(), "WAITING")) {
            log.info("Student {} is already on the waiting list", student.getId());
            return;
        }
        WaitingListEntry entry = new WaitingListEntry();
        entry.setStudent(student);
        entry.setApplication(application);
        entry.setStatus("WAITING");
        waitingListRepository.save(entry);
        log.warn("No rooms available; student {} placed on waiting list", student.getId());
    }

    @LogActivity(action = "DELETE", entityType = "DOCUMENT", description = "Deleted document {0}", entityIdParamIndex = 0)
    public void deleteDocument(Long documentId, String username) {
        var user = getUserByEmail(username);
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!document.getStudent().getAppUser().equals(user) && !user.getUserRole().isAdmin()) {
            throw new RuntimeException("Unauthorized access to document");
        }

        String bucketName = minioConfig.getBucketName();
        String objectPath = document.getDocumentPath();

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .build()
            );
            documentRepository.delete(document);
        } catch (Exception e) {
            throw new RuntimeException("Error deleting document: " + documentId, e);
        }
    }

    public Page<DocumentResponse> searchDocuments(
            String documentName,
            Long studentId,
            Long documentTypeId,
            String status,
            String applicationStatus,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore,
            Pageable pageable,
            String username
    ) {
        var user = getUserByEmail(username);
        Specification<Document> spec = DocumentSpecification.filterBy(
                documentName, studentId, documentTypeId, status, applicationStatus, createdAfter, createdBefore
        );

        // If not admin, restrict to user's own documents
        if (!user.getUserRole().isAdmin()) {
            spec = spec.and(DocumentSpecification.belongsToUser(user.getId()));
        }

        Page<Document> documentsPage = documentRepository.findAll(spec, pageable);
        return documentsPage.map(documentMapper::toDocumentResponse);
    }

    public Page<DocumentResponse> getMyDocuments(
            String documentName,
            Long documentTypeId,
            String status,
            String applicationStatus,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore,
            Pageable pageable
    ) {
        var currentUser = authService.getCurrentUser();
        Specification<Document> spec = DocumentSpecification.belongsToUser(currentUser.getId())
                .and(DocumentSpecification.filterBy(
                        documentName, null, documentTypeId, status, applicationStatus, createdAfter, createdBefore
                ));

        Page<Document> documentsPage = documentRepository.findAll(spec, pageable);
        return documentsPage.map(documentMapper::toDocumentResponse);
    }

    // ==================== Document Comment Methods ====================

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
                .orElseThrow(() -> new RuntimeException("Admin not found with id: " + adminId));

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
            String studentEmail = document.getStudent().getAppUser().getEmail();
            String documentName = document.getDocumentName();
            notificationService.sendDocumentCommentNotification(document, commentText);
            log.info("Notification sent to student {} for document comment", studentEmail);
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
     * Gets all comments for a document ordered by creation date descending.
     * Internal method without authorization check.
     *
     * @param documentId The document ID
     * @return List of comment responses
     */
    List<DocumentCommentResponse> getCommentsByDocumentInternal(Long documentId) {
        log.info("Fetching comments for document {}", documentId);

        // Verify document exists
        if (!documentRepository.existsById(documentId)) {
            throw new DocumentNotFoundException("Document not found with id: " + documentId);
        }

        List<DocumentComment> comments = documentCommentRepository.findByDocument_IdOrderByCreatedAtDesc(documentId);
        log.info("Found {} comments for document {}", comments.size(), documentId);

        return comments.stream()
                .map(this::mapToCommentResponse)
                .toList();
    }

    /**
     * Deletes a comment from a document.
     * Only the admin who created the comment can delete it (unless they are a super-admin).
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
     * Only the admin who created the comment can delete it (unless they are a super-admin).
     *
     * @param commentId The comment ID
     * @param adminId   The admin user ID requesting deletion
     */
    @Transactional
    public void deleteCommentByUserId(Long commentId, Long adminId) {
        log.info("Deleting comment {} by admin {}", commentId, adminId);

        DocumentComment comment = documentCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));

        AppUser requestingAdmin = appUserRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found with id: " + adminId));

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
     * Internal method to create a comment when rejecting a document.
     * Called from updateDocumentStatus when status changes to REJECTED.
     *
     * @param document    The document being rejected
     * @param admin       The admin rejecting the document
     * @param commentText The rejection reason/comment
     */
    private void createRejectionComment(Document document, AppUser admin, String commentText) {
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

