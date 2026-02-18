package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.ApplicationNotApprovedException;
import com.beezhub.student_accommodation.exceptions.ApplicationNotFoundException;
import com.beezhub.student_accommodation.exceptions.DocumentCommentNotFoundException;
import com.beezhub.student_accommodation.exceptions.DocumentNotFoundException;
import com.beezhub.student_accommodation.exceptions.DocumentTypeNotFoundException;
import com.beezhub.student_accommodation.exceptions.FileStorageException;
import com.beezhub.student_accommodation.exceptions.FileValidationException;
import com.beezhub.student_accommodation.exceptions.InvalidStatusException;
import com.beezhub.student_accommodation.exceptions.StudentNotFoundException;
import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.exceptions.UserNotFoundException;
import com.beezhub.student_accommodation.service.document.FileStorageService;
import com.beezhub.student_accommodation.service.document.FileValidationService;
import com.beezhub.student_accommodation.service.document.ValidationResult;
import org.springframework.dao.OptimisticLockingFailureException;

import com.beezhub.student_accommodation.aspect.LogActivity;
import com.beezhub.student_accommodation.mapper.DocumentMapper;
import com.beezhub.student_accommodation.model.dto.DocumentCommentResponse;
import com.beezhub.student_accommodation.model.dto.DocumentDownloadResponse;
import com.beezhub.student_accommodation.model.dto.DocumentResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;
import org.springframework.context.ApplicationEventPublisher;
import com.beezhub.student_accommodation.model.event.DocumentVerificationEvent;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.specification.DocumentSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.beezhub.student_accommodation.model.enums.DocumentStatus.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final FileStorageService fileStorageService;
    private final FileValidationService fileValidationService;
    private final DocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final DocumentCommentService documentCommentService;
    private final AppUserRepository appUserRepository;
    private final StudentRepository studentRepository;
    private final DocumentMapper documentMapper;
    private final AuthService authService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserNotificationService userNotificationService;
    private final ApplicationRepository applicationRepository;
    private final ApplicationDocumentCoordinator applicationDocumentCoordinator;

    @Transactional
    @LogActivity(action = "UPLOAD", entityType = "DOCUMENT", description = "Uploaded {0} documents", entityIdParamIndex = -1)
    public List<DocumentResponse> uploadDocuments(List<MultipartFile> files, String email, List<Long> documentTypeIds) {
        var user = getUserByEmail(email);
        List<DocumentResponse> documentResponses = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            DocumentType docType = getDocumentTypeById(documentTypeIds.get(i));
            String objectPath = buildObjectPath(user, docType, file);

            try (InputStream is = file.getInputStream()) {
                ValidationResult validationResult = fileValidationService.scanFile(is, file.getOriginalFilename());
                if (!validationResult.clean()) {
                    throw new FileValidationException("File rejected: " + validationResult.reason());
                }
            } catch (FileValidationException e) {
                throw e;
            } catch (IOException e) {
                throw new FileStorageException("Cannot read file for validation: " + file.getOriginalFilename(), e);
            }

            try (InputStream is = file.getInputStream()) {
                fileStorageService.upload(objectPath, is, file.getSize(), file.getContentType());
            } catch (IOException e) {
                throw new FileStorageException("Error uploading file: " + file.getOriginalFilename(), e);
            }

            Document document;
            try {
                document = saveDocumentRecord(user, docType, objectPath, file);
            } catch (Exception e) {
                // Compensating transaction: delete file if DB save fails
                try {
                    fileStorageService.delete(objectPath);
                    log.info("Deleted orphaned file {} after DB save failure", objectPath);
                } catch (Exception deleteEx) {
                    log.error("Failed to delete orphaned file {} after DB save failure", objectPath, deleteEx);
                }
                throw e; // Rethrow original exception
            }
            documentResponses.add(documentMapper.toDocumentResponse(document));
        }
        return documentResponses;
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocumentInfo(Long documentId, String username) {
        var user = getUserByEmail(username);
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        if (!document.getStudent().getAppUser().equals(user) && !user.getUserRole().isAdmin()) {
            throw new UnauthorizedAccessException("Unauthorized access to document");
        }

        String objectPath = document.getDocumentPath();
        var documentResponse = documentMapper.toDocumentResponse(document);
        // Generate presigned URL valid for 1 hour
        documentResponse.setFileUrl(fileStorageService.generatePresignedUrl(objectPath, Duration.ofHours(1)));
        return documentResponse;
    }

    @Transactional
    public void uploadSingleDocument(MultipartFile file, String username, Long documentTypeId) {
        var user = getUserByEmail(username);
        DocumentType docType = getDocumentTypeById(documentTypeId);
        String objectPath = buildObjectPath(user, docType, file);
        
        try (InputStream is = file.getInputStream()) {
            ValidationResult validationResult = fileValidationService.scanFile(is, file.getOriginalFilename());
            if (!validationResult.clean()) {
                throw new FileValidationException("File rejected: " + validationResult.reason());
            }
        } catch (FileValidationException e) {
            throw e;
        } catch (IOException e) {
            throw new FileStorageException("Cannot read file for validation: " + file.getOriginalFilename(), e);
        }

        try (InputStream is = file.getInputStream()) {
             fileStorageService.upload(objectPath, is, file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new FileStorageException("Error uploading file: " + file.getOriginalFilename(), e);
        }
        
        try {
            saveDocumentRecord(user, docType, objectPath, file);
        } catch (Exception e) {
             // Compensating transaction: delete file if DB save fails
             try {
                 fileStorageService.delete(objectPath);
                 log.info("Deleted orphaned file {} after DB save failure", objectPath);
             } catch (Exception deleteEx) {
                 log.error("Failed to delete orphaned file {} after DB save failure", objectPath, deleteEx);
             }
             throw e;
        }
    }

    private AppUser getUserByEmail(String email) {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Student not found"));
    }

    private DocumentType getDocumentTypeById(Long docTypeId) {
        return documentTypeRepository.findById(docTypeId)
                .orElseThrow(() -> new DocumentTypeNotFoundException("DocumentType not found"));
    }

    private String buildObjectPath(AppUser user, DocumentType docType, MultipartFile file) {
        String safeFilename = UUID.randomUUID() + getExtension(file.getOriginalFilename());
        return String.format("%s/%s/%s/%s",
                user.getUserRole().getRole(),
                user.getId(),
                docType.getTypeName(),
                safeFilename);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        String ext = filename.substring(filename.lastIndexOf('.'));
        // Allow only known safe extensions
        return ext.matches("\\.[a-zA-Z0-9]{1,10}") ? ext : "";
    }

    private Document saveDocumentRecord(AppUser user, DocumentType docType, String objectPath, MultipartFile file) {
        var student = studentRepository.findStudentByAppUser(user)
                .orElseThrow(() -> new StudentNotFoundException("Student not found for user ID: " + user.getId()));

        Application application = getActiveApplicationForStudent(student);

        var document = Document.builder()
                .documentName(file.getOriginalFilename())
                .documentType(docType)
                .documentPath(objectPath)
                .fileSize(file.getSize())
                .student(student)
                .application(application)
                .documentStatus(PENDING)
                .build();
        return documentRepository.save(document);
    }

    /**
     * Gets the active application for a student.
     * Active application is one with status PENDING, APPROVED, or
     * AWAITING_ALLOCATION.
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
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "No active application found for student. Please create an application first."));
    }

    @Transactional(readOnly = true)
    public DocumentDownloadResponse downloadDocument(Long documentId, String username) {
        var user = getUserByEmail(username);
        var document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        if (!document.getStudent().getAppUser().equals(user) && !user.getUserRole().isAdmin()) {
            throw new UnauthorizedAccessException("Unauthorized access to document");
        }

        String objectPath = document.getDocumentPath();

        InputStream response = fileStorageService.download(objectPath);
        var inputStreamResource = new InputStreamResource(response);
        return DocumentDownloadResponse.builder()
                .filename(document.getDocumentName())
                .resource(inputStreamResource)
                .contentType("application/octet-stream")
                .build();
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getAllDocumentsByUserId(Long userId, String username) {

        var user = getUserByEmail(username);
        if (!user.getId().equals(userId) && !user.getUserRole().isAdmin()) {
            throw new UnauthorizedAccessException("Unauthorized access to documents for user ID: " + userId);
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
        DocumentStatus newStatus = parseDocumentStatus(status);

        Application application = applicationRepository.findByStudent_Id(document.getStudent().getId())
                .stream()
                .filter(app -> app.getStatus() == ApplicationStatus.APPROVED)
                .findFirst()
                .orElseThrow(() -> new ApplicationNotApprovedException(
                        "Documents can only be reviewed for approved applications"));

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
            publishDocumentStatusEvent(updatedDocument, oldStatus, newStatus, comment);
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

    private void publishDocumentStatusEvent(Document document, DocumentStatus oldStatus, DocumentStatus newStatus, String comment) {
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

    @LogActivity(action = "DELETE", entityType = "DOCUMENT", description = "Deleted document {0}", entityIdParamIndex = 0)
    public void deleteDocument(Long documentId, String username) {
        var user = getUserByEmail(username);
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        if (!document.getStudent().getAppUser().equals(user) && !user.getUserRole().isAdmin()) {
            throw new UnauthorizedAccessException("Unauthorized access to document");
        }

        String objectPath = document.getDocumentPath();

        // Step 1: Delete DB record (can be rolled back on failure)
        documentRepository.delete(document);

        // Step 2: Delete file from storage (best-effort; cleanup job handles orphans)
        try {
            fileStorageService.delete(objectPath);
        } catch (FileStorageException e) {
            log.error("DB record deleted but MinIO file deletion failed for key {}: {}", objectPath, e.getMessage());
            // Do not rethrow — the DB record is gone; the cleanup job will remove the orphan
        }
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> searchDocuments(
            String documentName,
            Long studentId,
            String studentName,
            Long documentTypeId,
            String status,
            String applicationStatus,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore,
            Pageable pageable,
            String username) {
        var user = getUserByEmail(username);
        Specification<Document> spec = DocumentSpecification.filterBy(
                documentName, studentId, studentName, documentTypeId, status, applicationStatus, createdAfter,
                createdBefore);

        // If not admin, restrict to user's own documents
        if (!user.getUserRole().isAdmin()) {
            spec = spec.and(DocumentSpecification.belongsToUser(user.getId()));
        }

        Page<Document> documentsPage = documentRepository.findAll(spec, pageable);
        return documentsPage.map(documentMapper::toDocumentResponse);
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> getMyDocuments(
            String documentName,
            Long documentTypeId,
            String status,
            String applicationStatus,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore,
            Pageable pageable) {
        var currentUser = authService.getCurrentUser();
        Specification<Document> spec = DocumentSpecification.belongsToUser(currentUser.getId())
                .and(DocumentSpecification.filterBy(
                        documentName, null, null, documentTypeId, status, applicationStatus, createdAfter,
                        createdBefore));

        Page<Document> documentsPage = documentRepository.findAll(spec, pageable);
        return documentsPage.map(documentMapper::toDocumentResponse);
    }
}
