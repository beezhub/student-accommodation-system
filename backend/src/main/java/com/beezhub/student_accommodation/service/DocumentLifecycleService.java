package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.aspect.LogActivity;
import com.beezhub.student_accommodation.exceptions.DocumentNotFoundException;
import com.beezhub.student_accommodation.exceptions.DocumentTypeNotFoundException;
import com.beezhub.student_accommodation.exceptions.FileStorageException;
import com.beezhub.student_accommodation.exceptions.FileValidationException;
import com.beezhub.student_accommodation.exceptions.StudentNotFoundException;
import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.exceptions.UserNotFoundException;
import com.beezhub.student_accommodation.mapper.DocumentMapper;
import com.beezhub.student_accommodation.model.dto.DocumentResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.ApplicationDocument;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.ApplicationDocumentRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.service.document.FileStorageService;
import com.beezhub.student_accommodation.service.document.FileValidationService;
import com.beezhub.student_accommodation.service.document.ValidationResult;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.beezhub.student_accommodation.model.enums.DocumentStatus.PENDING;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentLifecycleService {

    private final FileStorageService fileStorageService;
    private final FileValidationService fileValidationService;
    private final DocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final StudentRepository studentRepository;
    private final AppUserRepository appUserRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final DocumentMapper documentMapper;

    @Transactional
    @Timed(value = "service.document.upload", percentiles = {0.5, 0.95, 0.99})
    @LogActivity(action = "UPLOAD", entityType = "DOCUMENT", description = "Uploaded {0} documents", entityIdParamIndex = -1)
    public List<DocumentResponse> uploadDocuments(List<MultipartFile> files, String email, List<Long> documentTypeIds) {
        var user = getUserByEmail(email);
        List<DocumentResponse> documentResponses = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            DocumentType docType = getDocumentTypeById(documentTypeIds.get(i));
            Document document = validateAndUploadFile(file, user, docType);
            documentResponses.add(documentMapper.toDocumentResponse(document));
        }
        return documentResponses;
    }

    @Transactional
    public void uploadSingleDocument(MultipartFile file, String username, Long documentTypeId) {
        var user = getUserByEmail(username);
        DocumentType docType = getDocumentTypeById(documentTypeId);
        validateAndUploadFile(file, user, docType);
    }

    @Transactional
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
        } catch (RuntimeException e) {
            log.error("DB record deleted but MinIO file deletion failed for key {}: {}", objectPath, e.getMessage());
            // Do not rethrow — the DB record is gone; the cleanup job will remove the
            // orphan
        }
    }

    private Document validateAndUploadFile(MultipartFile file, AppUser user, DocumentType docType) {
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
            return saveDocumentRecord(user, docType, objectPath, file);
        } catch (RuntimeException e) {
            // Compensating transaction: delete file if DB save fails
            try {
                fileStorageService.delete(objectPath);
                log.info("Deleted orphaned file {} after DB save failure", objectPath);
            } catch (RuntimeException deleteEx) {
                log.error("Failed to delete orphaned file {} after DB save failure", objectPath, deleteEx);
            }
            throw e; // Rethrow original exception
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
        if (filename == null || !filename.contains("."))
            return "";
        String ext = filename.substring(filename.lastIndexOf('.'));
        // Allow only known safe extensions
        return ext.matches("\\.[a-zA-Z0-9]{1,10}") ? ext : "";
    }

    private Document saveDocumentRecord(AppUser user, DocumentType docType, String objectPath, MultipartFile file) {
        var student = studentRepository.findByAppUser(user)
                .orElseThrow(() -> new StudentNotFoundException("Student not found for user ID: " + user.getId()));

        var document = Document.builder()
                .documentName(file.getOriginalFilename())
                .documentType(docType)
                .documentPath(objectPath)
                .fileSize(file.getSize())
                .student(student)
                .documentStatus(PENDING)
                .build();
        Document savedDocument = documentRepository.save(document);

        linkDocumentToActiveApplication(student, savedDocument);

        return savedDocument;
    }

    private static final List<ApplicationStatus> ACTIVE_STATUSES = List.of(
            ApplicationStatus.PENDING,
            ApplicationStatus.UNDER_REVIEW,
            ApplicationStatus.CHANGES_REQUESTED,
            ApplicationStatus.APPROVED,
            ApplicationStatus.AWAITING_ALLOCATION
    );

    private void linkDocumentToActiveApplication(Student student, Document document) {
        applicationRepository.findByStudent_Id(student.getId())
                .stream()
                .filter(app -> ACTIVE_STATUSES.contains(app.getStatus()))
                .findFirst()
                .ifPresent(application -> {
                    if (!applicationDocumentRepository.existsByApplication_IdAndDocument_Id(
                            application.getId(), document.getId())) {
                        ApplicationDocument junction = ApplicationDocument.builder()
                                .application(application)
                                .document(document)
                                .build();
                        applicationDocumentRepository.save(junction);
                        log.info("Linked document {} to application {}", document.getId(), application.getId());
                    }
                });
    }
}
