package com.beezhub.student_accommodation.service.document;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import com.beezhub.student_accommodation.exceptions.DocumentsNotReadyException;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.repository.TenantRepository;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Expiring documents on PostgreSQL (GH-498): the expiry is the end of the company's academic year
 * the document was uploaded in, worked out per company, and an expired one holds offers back.
 */
@DisplayName("Expiring documents (integration)")
class DocumentExpiryIntegrationTest extends AbstractIntegrationTest {

    @Autowired DocumentExpiryPolicy documentExpiryPolicy;
    @Autowired DocumentReadinessGuard documentReadinessGuard;
    @Autowired JdbcTemplate jdbc;
    @Autowired TenantRepository tenantRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired StudentRepository studentRepository;

    private final List<AppUser> users = new ArrayList<>();
    private final String typeName = "Proof " + (System.nanoTime() % 1_000_000_000L);
    private Long companyA;
    private Long companyB;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        companyA = tenantRepository.save(Tenant.builder().name("Expiry A " + System.nanoTime()).isActive(true).build()).getId();
        companyB = tenantRepository.save(Tenant.builder().name("Expiry B " + System.nanoTime()).isActive(true).build()).getId();
        TenantContext.clear();
        for (Long company : List.of(companyA, companyB)) {
            jdbc.update("INSERT INTO document_type (type_name, is_required, is_expiring, tenant_id) VALUES (?, true, true, ?)",
                    typeName, company);
        }
        year(companyA, "A 2026", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 11, 30));
        year(companyA, "A 2027", LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30));
        year(companyB, "B 2026", LocalDate.of(2026, 1, 15), LocalDate.of(2026, 12, 10));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        for (AppUser user : users) {
            jdbc.update("DELETE FROM document WHERE student_id IN (SELECT id FROM student WHERE user_id = ?)", user.getId());
            jdbc.update("DELETE FROM student WHERE user_id = ?", user.getId());
            jdbc.update("DELETE FROM app_user WHERE id = ?", user.getId());
        }
        for (Long company : List.of(companyA, companyB)) {
            jdbc.update("DELETE FROM document_type WHERE tenant_id = ?", company);
            jdbc.update("DELETE FROM academic_year WHERE tenant_id = ?", company);
            jdbc.update("DELETE FROM tenant WHERE id = ?", company);
        }
    }

    @Test
    @DisplayName("A June upload expires at the end of that year")
    void expiryOf_juneUpload_expiresAtThatYearsEnd() {
        assertThat(documentExpiryPolicy.forCompany(companyA)
                .expiryOf(typeName, LocalDateTime.of(2026, 6, 10, 12, 0), null))
                .contains(LocalDate.of(2026, 11, 30));
    }

    @Test
    @DisplayName("An upload between two years belongs to the next one")
    void expiryOf_uploadBetweenTwoYears_belongsToTheNextOne() {
        assertThat(documentExpiryPolicy.forCompany(companyA)
                .expiryOf(typeName, LocalDateTime.of(2026, 12, 15, 12, 0), null))
                .contains(LocalDate.of(2027, 11, 30));
    }

    @Test
    @DisplayName("No later year means no expiry until one is created")
    void expiryOf_noLaterYear_hasNoExpiry() {
        assertThat(documentExpiryPolicy.forCompany(companyA)
                .expiryOf(typeName, LocalDateTime.of(2027, 12, 15, 12, 0), null)).isEmpty();
    }

    @Test
    @DisplayName("Two companies can give the same document different expiry dates")
    void expiryOf_twoCompanies_differentDates() {
        LocalDateTime june = LocalDateTime.of(2026, 6, 10, 12, 0);

        assertThat(documentExpiryPolicy.forCompany(companyA).expiryOf(typeName, june, null))
                .contains(LocalDate.of(2026, 11, 30));
        assertThat(documentExpiryPolicy.forCompany(companyB).expiryOf(typeName, june, null))
                .contains(LocalDate.of(2026, 12, 10));
    }

    @Test
    @DisplayName("An expired approved document holds the offer back; a current one does not")
    void assertReadyForRoom_expiredDocumentRefused_currentDocumentPasses() {
        LocalDate today = documentExpiryPolicy.today();
        LocalDate lastYear = today.minusYears(1);
        year(companyB, "B last", lastYear.minusMonths(2), lastYear.plusMonths(2));
        year(companyB, "B now", today.minusMonths(1), today.plusMonths(6));
        Student expired = student();
        Student current = student();
        document(expired, companyB, lastYear.atTime(10, 0));
        document(current, companyB, today.atStartOfDay());

        assertThatThrownBy(() -> documentReadinessGuard.assertReadyForRoom(expired, companyB, null))
                .isInstanceOf(DocumentsNotReadyException.class)
                .hasMessageContaining(typeName + " (expired on");
        assertThatCode(() -> documentReadinessGuard.assertReadyForRoom(current, companyB, null))
                .doesNotThrowAnyException();
    }

    private void year(Long tenantId, String name, LocalDate start, LocalDate end) {
        jdbc.update("INSERT INTO academic_year (name, start_date, end_date, tenant_id) VALUES (?, ?, ?, ?)",
                name, start, end, tenantId);
    }

    private void document(Student student, Long tenantId, LocalDateTime uploadedAt) {
        Long typeId = jdbc.queryForObject("SELECT id FROM document_type WHERE tenant_id = ? AND type_name = ?",
                Long.class, tenantId, typeName);
        jdbc.update("""
                INSERT INTO document (document_name, document_type_id, document_path, document_status, file_size,
                                      student_id, tenant_id, created_at, updated_at, version)
                VALUES ('proof.pdf', ?, ?, 'APPROVED', 10, ?, 0, ?, ?, 0)
                """, typeId, "student/" + student.getId() + "/proof.pdf", student.getId(),
                Timestamp.valueOf(uploadedAt), Timestamp.valueOf(uploadedAt));
    }

    private Student student() {
        AppUser user = appUserRepository.save(AppUser.builder()
                .email("expiry." + System.nanoTime() + "@test.com")
                .firstName("Test")
                .lastName("Student")
                .userPassword("$2a$10$hashed")
                .userRole(UserRole.STUDENT)
                .tenantId(TenantContext.PLATFORM_TENANT_ID)
                .emailVerified(true)
                .mustChangePassword(false)
                .build());
        users.add(user);
        Student student = new Student();
        student.setAppUser(user);
        student.setStudentNumber("S-" + System.nanoTime());
        student.setPhoneNumber("0123456789");
        student.setDateOfBirth(LocalDate.of(2000, 5, 14));
        student.setGender("Female");
        return studentRepository.save(student);
    }
}
