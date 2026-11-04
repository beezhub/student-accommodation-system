package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.aspect.LogActivity;
import com.beezhub.student_accommodation.exceptions.DocumentLockedException;
import com.beezhub.student_accommodation.exceptions.DocumentNotFoundException;
import com.beezhub.student_accommodation.exceptions.DocumentTypeNotFoundException;
import com.beezhub.student_accommodation.exceptions.FileStorageException;
import com.beezhub.student_accommodation.exceptions.FileValidationException;
import com.beezhub.student_accommodation.exceptions.StudentNotFoundException;
import com.beezhub.student_accommodation.exceptions.UserNotFoundException;
import com.beezhub.student_accommodation.mapper.DocumentMapper;
import com.beezhub.student_accommodation.model.dto.DocumentResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.model.event.StoredFileDeletionEvent;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.service.document.DocumentAccessGuard;
import com.beezhub.student_accommodation.service.document.FileStorageService;
import com.beezhub.student_accommodation.service.document.FileValidationService;
import com.beezhub.student_accommodation.service.document.ValidationResult;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.beezhub.student_accommodation.model.enums.DocumentStatus.PENDING;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentLifecycleService {

    private final FileStorageService fileStorageService;
    private final FileValidationService fileValidationService;
    private final DocumentRepository documentRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final DocumentTypeRepository documentTypeRepository;
    private final StudentRepository studentRepository;
    private final AppUserRepository appUserRepository;
    private final DocumentMapper documentMapper;
    private final DocumentAccessGuard documentAccessGuard;

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

    /**
     * Uploads a single document for the authenticated student, replacing any existing
     * document of the same type (upsert semantics — one slot per document type per student).
     */
    @Transactional
    public void uploadSingleDocument(MultipartFile file, String username, Long documentTypeId) {
        var user = getUserByEmail(username);
        DocumentType docType = getDocumentTypeById(documentTypeId);

        // Upsert: if the student already has a document of this type, delete it first
        Student student = studentRepository.findByAppUser(user)
                .orElseThrow(() -> new StudentNotFoundException("Student not found for user ID: " + user.getId()));

        Optional<Document> existing = documentRepository.findByStudentIdAndDocumentTypeId(
                student.getId(), documentTypeId);

        if (existing.isPresent()) {
            Document old = existing.get();
            assertNotLockedByReview(old, student.getId());
            String oldPath = old.getDocumentPath();
            documentRepository.delete(old);
            documentRepository.flush();
            // After commit, not here. Deleting inline destroyed the old object before the
            // replacement was even validated, so a rejected upload rolled back to a row
            // pointing at nothing and the download failed forever (GH-145).
            eventPublisher.publishEvent(new StoredFileDeletionEvent(oldPath));
        }

        validateAndUploadFile(file, user, docType);
    }

    @Transactional
    @LogActivity(action = "DELETE", entityType = "DOCUMENT", description = "Deleted document {0}", entityIdParamIndex = 0)
    public void deleteDocument(Long documentId, String username) {
        var user = getUserByEmail(username);
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        documentAccessGuard.assertCanAccessDocument(document, user);

        boolean isAdmin = user.getUserRole().isAdmin();
        // Admins are not locked out: this is the only way to unlock an approved document while
        // the application is live, since a document cannot be rejected once the application has
        // moved past APPROVED.
        if (!isAdmin) {
            assertNotLockedByReview(document, document.getStudent().getId());
        }

        String objectPath = document.getDocumentPath();
        documentRepository.delete(document);

        // After commit. Inline, the object was destroyed while the transaction could still roll
        // back — including from the activity-log write this method is annotated with, which
        // joins the same transaction and runs after this line (GH-145).
        eventPublisher.publishEvent(new StoredFileDeletionEvent(objectPath));
    }

    /**
     * Application statuses under which an approved document is still doing work.
     *
     * <p>Was a private copy of this list. It now shares one definition with the document-review
     * guard, which had a different and much narrower idea of when an application counts, so a
     * document could be locked in a state where no admin could unlock it (GH-217).
     */
    private static final Set<ApplicationStatus> LIVE_APPLICATION_STATUSES = ApplicationStatus.LIVE_STATUSES;

    /**
     * Refuses to let a student change a document an administrator has already approved while an
     * application that approval supports is still live.
     *
     * <p>Neither the replace nor the delete path looked at the document status, so the evidence
     * behind an approved or allocated application could be swapped or wiped after review. The
     * required-documents checklist and the allocation gate both read the live pack by student id,
     * so it changed underneath them silently (GH-145).
     *
     * <p>Keyed on the document type NAME rather than its id. Tenants get shadow document types
     * that share a name under a different id by design, so an id-keyed check would let the same
     * logical document be re-uploaded against a sibling type. Name is also how the checklist and
     * the allocation gate read the pack, so this matches what actually consumes it.
     */
    private void assertNotLockedByReview(Document document, Long studentId) {
        if (document.getDocumentStatus() != DocumentStatus.APPROVED) {
            return;
        }
        if (!applicationRepository.existsByStudent_IdAndStatusIn(studentId, LIVE_APPLICATION_STATUSES)) {
            return;
        }
        String typeName = document.getDocumentType() != null
                ? document.getDocumentType().getTypeName()
                : "document";
        log.warn("action=documentChangeBlocked reason=approvedWithLiveApplication documentId={} studentId={}",
                document.getId(), studentId);
        throw new DocumentLockedException(
                "Your " + typeName + " has already been approved and cannot be changed while your "
                        + "application is active. Ask an administrator to remove it before uploading a new one.");
    }

    private Document validateAndUploadFile(MultipartFile file, AppUser user, DocumentType docType) {
        String objectPath = buildObjectPath(user, docType, file);

        String resolvedContentType;
        try (InputStream is = file.getInputStream()) {
            ValidationResult validationResult =
                    fileValidationService.scanFile(is, file.getOriginalFilename(), file.getSize());
            if (!validationResult.clean()) {
                throw new FileValidationException("File rejected: " + validationResult.reason());
            }
            resolvedContentType = validationResult.resolvedContentType();
        } catch (FileValidationException e) {
            throw e;
        } catch (IOException e) {
            throw new FileStorageException("Cannot read file for validation: " + file.getOriginalFilename(), e);
        }

        try (InputStream is = file.getInputStream()) {
            // The type decided from the bytes, never file.getContentType(). That header is
            // attacker-chosen, was stored verbatim and echoed back on download, so the client
            // controlled the response Content-Type (GH-144).
            fileStorageService.upload(objectPath, is, file.getSize(), resolvedContentType);
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
        if (filename == null || !filename.contains("."))
            return "";
        String ext = filename.substring(filename.lastIndexOf('.'));
        return ext.matches("\\.[a-zA-Z0-9]{1,10}") ? ext : "";
    }

    private Document saveDocumentRecord(AppUser user, DocumentType docType, String objectPath, MultipartFile file) {
        var student = studentRepository.findByAppUser(user)
                .orElseThrow(() -> new StudentNotFoundException("Student not found for user ID: " + user.getId()));

        var document = Document.builder()
                // Documents are platform-level and student-owned; tenant_id is NOT NULL in the
                // schema but carries no scoping meaning (see the Document javadoc).
                .tenantId(com.beezhub.student_accommodation.service.tenant.TenantContext.PLATFORM_TENANT_ID)
                .documentName(file.getOriginalFilename())
                .documentType(docType)
                .documentPath(objectPath)
                .fileSize(file.getSize())
                .student(student)
                .documentStatus(PENDING)
                .build();
        return documentRepository.save(document);
    }
}
