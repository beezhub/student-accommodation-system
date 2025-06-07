package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.config.MinioConfig;
import com.beezhub.student_accommodation.mapper.DocumentMapper;
import com.beezhub.student_accommodation.model.dto.DocumentDownloadResponse;
import com.beezhub.student_accommodation.model.dto.DocumentResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
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
    private final AppUserRepository appUserRepository;
    private final StudentRepository studentRepository;
    private final DocumentMapper documentMapper;

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
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!document.getStudent().getAppUser().equals(user) && !user.getUserRole().isAdmin()) {
            throw new RuntimeException("Unauthorized access to document");
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

        var document = Document.builder()
                .documentName(file.getOriginalFilename())
                .documentType(docType)
                .documentPath(objectPath)
                .student(student)
                .documentStatus(PENDING)
                .build();
        return documentRepository.save(document);
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
        var user = getUserByEmail(username);
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!document.getStudent().getAppUser().equals(user) && !user.getUserRole().isAdmin()) {
            throw new RuntimeException("Unauthorized access to document");
        }

        try {
            document.setDocumentStatus(DocumentStatus.valueOf(status.toUpperCase()));
            Document updatedDocument = documentRepository.save(document);
            return documentMapper.toDocumentResponse(updatedDocument);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid document status: " + status, e);
        }
    }

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
}