package com.beezhub.student_accommodation.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.beezhub.student_accommodation.AbstractJpaTest;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * GH-514: the expiry reminder job records each reminder with a conditional update, so a document is
 * reminded at most once a day however often the job runs. These assert that update against a real
 * PostgreSQL.
 */
class DocumentExpiryReminderQueryTest extends AbstractJpaTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);
    private static final LocalDateTime MORNING = TODAY.atTime(8, 30);
    private static final LocalDateTime AFTERNOON = TODAY.atTime(15, 0);

    @Autowired private DocumentRepository documentRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private DocumentTypeRepository documentTypeRepository;

    @PersistenceContext private EntityManager entityManager;

    private Document document;

    @BeforeEach
    void setUp() {
        Tenant tenant = new Tenant();
        tenant.setName("Company A");
        Long tenantId = tenantRepository.saveAndFlush(tenant).getId();

        DocumentType type = new DocumentType();
        type.setTypeName("PROOF_OF_REGISTRATION");
        type.setIsRequired(true);
        type.setTenantId(tenantId);
        type = documentTypeRepository.saveAndFlush(type);

        AppUser user = new AppUser();
        user.setEmail("expiry-reminder@test.local");
        user.setUserPassword("not-a-real-password");
        user.setFirstName("Expiry");
        user.setLastName("Test");
        user.setUserRole(UserRole.STUDENT);
        user = appUserRepository.saveAndFlush(user);

        Student student = new Student();
        student.setAppUser(user);
        student.setRegistrationDate(new Date());
        student = studentRepository.saveAndFlush(student);

        Document toSave = new Document();
        toSave.setStudent(student);
        toSave.setDocumentType(type);
        toSave.setDocumentName("registration.pdf");
        toSave.setDocumentPath("STUDENT/" + student.getId() + "/PROOF_OF_REGISTRATION/f.pdf");
        toSave.setFileSize(1024L);
        toSave.setDocumentStatus(DocumentStatus.APPROVED);
        document = documentRepository.saveAndFlush(toSave);
    }

    @Test
    void markExpiryReminded_firstReminderOfTheDay_isRecorded() {
        assertThat(documentRepository.markExpiryReminded(document.getId(), MORNING, TODAY.atStartOfDay()))
                .isEqualTo(1);

        assertThat(reload().getExpiryRemindedAt()).isEqualTo(MORNING);
    }

    @Test
    void markExpiryReminded_secondRunTheSameDay_recordsNothing() {
        documentRepository.markExpiryReminded(document.getId(), MORNING, TODAY.atStartOfDay());

        assertThat(documentRepository.markExpiryReminded(document.getId(), AFTERNOON, TODAY.atStartOfDay()))
                .isZero();
        assertThat(reload().getExpiryRemindedAt()).isEqualTo(MORNING);
    }

    @Test
    void markExpiryReminded_remindedOnAnEarlierDay_isRecordedAgain() {
        documentRepository.markExpiryReminded(document.getId(), MORNING.minusDays(7),
                TODAY.minusDays(7).atStartOfDay());

        assertThat(documentRepository.markExpiryReminded(document.getId(), MORNING, TODAY.atStartOfDay()))
                .isEqualTo(1);
        assertThat(reload().getExpiryRemindedAt()).isEqualTo(MORNING);
    }

    /** The document emails show updated_at as the review date, so a reminder must not move it. */
    @Test
    void markExpiryReminded_leavesUpdatedAtAndTheVersionAlone() {
        Document before = reload();

        documentRepository.markExpiryReminded(document.getId(), MORNING, TODAY.atStartOfDay());

        Document after = reload();
        assertThat(after.getUpdatedAt()).isEqualTo(before.getUpdatedAt());
        assertThat(after.getVersion()).isEqualTo(before.getVersion());
    }

    /** The update bypasses the persistence context, so the row is read again from the database. */
    private Document reload() {
        entityManager.clear();
        return documentRepository.findById(document.getId()).orElseThrow();
    }
}
