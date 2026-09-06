package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentLifecycleServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired private DocumentLifecycleService documentLifecycleService;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private DocumentTypeRepository documentTypeRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private AppUserRepository appUserRepository;

    private AppUser user;
    private Student student;
    private DocumentType docType;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        documentTypeRepository.deleteAll();
        studentRepository.deleteAll();
        appUserRepository.deleteAll();

        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);

        user = new AppUser();
        user.setEmail("doc-test-" + System.currentTimeMillis() + "@example.com");
        user.setUserPassword("secret");
        user.setFirstName("Doc");
        user.setLastName("Tester");
        user.setUserRole(UserRole.STUDENT);
        user.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        user.setEmailVerified(true);
        appUserRepository.save(user);

        student = new Student();
        student.setAppUser(user);
        student.setStudentNumber("S-" + System.currentTimeMillis());
        student = studentRepository.save(student);

        docType = new DocumentType();
        docType.setTypeName("Passport_" + System.currentTimeMillis());
        docType.setIsRequired(true);
        docType.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        docType = documentTypeRepository.save(docType);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of()));
    }

    @AfterEach
    void tearDown() {
        documentRepository.deleteAll();
        documentTypeRepository.deleteAll();
        studentRepository.deleteAll();
        appUserRepository.deleteAll();
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    @DisplayName("uploadSingleDocument replaces existing document of same type (upsert)")
    void uploadSingleDocument_upsert_replacesExistingDocument() {
        MockMultipartFile firstFile = new MockMultipartFile(
                "file", "id-v1.pdf", "application/pdf", pdfBytes("first file content"));
        MockMultipartFile secondFile = new MockMultipartFile(
                "file", "id-v2.pdf", "application/pdf", pdfBytes("second file content"));

        // First upload
        documentLifecycleService.uploadSingleDocument(firstFile, user.getEmail(), docType.getId());

        List<Document> afterFirst = documentRepository.findAllByStudent_Id(student.getId());
        assertThat(afterFirst).hasSize(1);
        String firstPath = afterFirst.get(0).getDocumentPath();
        assertThat(afterFirst.get(0).getDocumentName()).isEqualTo("id-v1.pdf");

        // Second upload of the same type — upsert
        documentLifecycleService.uploadSingleDocument(secondFile, user.getEmail(), docType.getId());

        List<Document> afterSecond = documentRepository.findAllByStudent_Id(student.getId());
        assertThat(afterSecond).hasSize(1);
        String secondPath = afterSecond.get(0).getDocumentPath();

        // New file should have a different path (UUID-based)
        assertThat(secondPath).isNotEqualTo(firstPath);
        assertThat(afterSecond.get(0).getDocumentName()).isEqualTo("id-v2.pdf");

        // Unique constraint query should return the new document only
        Optional<Document> slot = documentRepository.findByStudentIdAndDocumentTypeId(
                student.getId(), docType.getId());
        assertThat(slot).isPresent();
        assertThat(slot.get().getDocumentName()).isEqualTo("id-v2.pdf");
    }

    @Test
    @DisplayName("student upload stays visible while a tenant-scoped view is active (GH-112)")
    void uploadSingleDocument_isVisibleInsideTenantScopedView() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "proof.pdf", "application/pdf", pdfBytes("proof content"));
        documentLifecycleService.uploadSingleDocument(file, user.getEmail(), docType.getId());

        Long uploadedId = documentRepository.findAllByStudent_Id(student.getId()).get(0).getId();

        // A tenant admin's request runs with a non-platform tenant in context. Documents used to
        // carry the tenant filter and were stamped with the student's platform tenant (0), so every
        // tenant-scoped read returned nothing and review checklists showed NOT_UPLOADED.
        TenantContext.setTenantId(99L);

        assertThat(documentRepository.findById(uploadedId)).isPresent();
        assertThat(documentRepository.findAllByStudent_Id(student.getId()))
                .extracting(Document::getId)
                .containsExactly(uploadedId);
        assertThat(documentRepository.findByStudentIdAndDocumentTypeId(student.getId(), docType.getId()))
                .isPresent();
    }

    @Test
    @DisplayName("uploadSingleDocument creates document when none exists for that type")
    void uploadSingleDocument_createsDocument_whenNoExistingSlot() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "transcript.pdf", "application/pdf", pdfBytes("transcript content"));

        documentLifecycleService.uploadSingleDocument(file, user.getEmail(), docType.getId());

        List<Document> docs = documentRepository.findAllByStudent_Id(student.getId());
        assertThat(docs).hasSize(1);
        assertThat(docs.get(0).getDocumentName()).isEqualTo("transcript.pdf");
        assertThat(docs.get(0).getDocumentType().getId()).isEqualTo(docType.getId());
        assertThat(docs.get(0).getStudent().getId()).isEqualTo(student.getId());
    }

    /**
     * A minimal but genuine PDF. Uploads are validated against the file bytes now, so a fixture
     * whose content is prose is correctly rejected (GH-144).
     */
    private static byte[] pdfBytes(String marker) {
        return ("%PDF-1.7\n% " + marker + "\n%%EOF\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }
}
