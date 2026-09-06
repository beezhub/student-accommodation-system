package com.beezhub.student_accommodation.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.beezhub.student_accommodation.AbstractJpaTest;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.service.document.DocumentRetentionJob;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

/**
 * GH-235: nothing expired uploaded documents, so they accumulated for ever.
 *
 * <p>These assert the retention query itself, against a real PostgreSQL, because the rule it
 * encodes is not obvious from the method name: a pack survives on any live application or held bed
 * at <em>any</em> company, and the clock that decides the rest runs from the student's history
 * rather than from the upload.
 */
class DocumentRetentionQueryTest extends AbstractJpaTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 6, 12, 0);
    private static final LocalDateTime CUTOFF = NOW.minusMonths(12);

    private static final LocalDateTime LONG_AGO = CUTOFF.minusMonths(6);
    private static final LocalDateTime RECENTLY = CUTOFF.plusMonths(1);

    @Autowired private DocumentRepository documentRepository;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private RoomAllocationRepository roomAllocationRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private DocumentTypeRepository documentTypeRepository;
    @Autowired private BuildingRepository buildingRepository;
    @Autowired private RoomRepository roomRepository;

    @PersistenceContext private EntityManager entityManager;

    private Long tenantA;
    private Long tenantB;
    private DocumentType idDocument;
    private int uniqueSuffix;

    @BeforeEach
    void setUp() {
        // AbstractJpaTest builds the schema from the entity mappings with Flyway off, so the
        // tenant table exists but is empty — the platform tenant is a Flyway seed and PostgreSQL
        // enforces the foreign key.
        tenantA = newTenant("Company A");
        tenantB = newTenant("Company B");
        idDocument = documentType("ID_DOCUMENT");
    }

    // ── What keeps a pack alive ─────────────────────────────────────────────────────────────

    @ParameterizedTest
    @EnumSource(value = ApplicationStatus.class,
            names = {"PENDING", "UNDER_REVIEW", "CHANGES_REQUESTED", "APPROVED",
                    "AWAITING_ALLOCATION", "ALLOCATED"})
    void aLiveApplicationKeepsThePackHoweverOldItIs(ApplicationStatus live) {
        Student student = newStudent();
        documentFor(student, LONG_AGO);
        applicationFor(student, tenantA, live, LONG_AGO);

        assertThat(expired()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = AllocationStatus.class, names = {"OFFERED", "ACCEPTED", "ACTIVE"})
    void anAllocationHoldingABedKeepsThePack(AllocationStatus held) {
        Student student = newStudent();
        documentFor(student, LONG_AGO);
        applicationFor(student, tenantA, ApplicationStatus.CHECKED_IN, LONG_AGO);
        allocationFor(student, held, LONG_AGO);

        assertThat(expired()).isEmpty();
    }

    /**
     * The case a naive rule gets wrong. CHECKED_IN is terminal in the application state machine —
     * nothing moves an application out of it, not even the student moving out — so a rule keyed on
     * the application alone would keep every former resident's pack for ever, exempting most of
     * the students this rule is about. Residency is the allocation's to report.
     */
    @Test
    void aFormerResidentExpiresEvenThoughTheirApplicationStaysCheckedIn() {
        Student student = newStudent();
        documentFor(student, LONG_AGO);
        applicationFor(student, tenantA, ApplicationStatus.CHECKED_IN, LONG_AGO);
        allocationFor(student, AllocationStatus.COMPLETED, LONG_AGO);

        assertThat(expired()).extracting(d -> d.getStudent().getId())
                .containsExactly(student.getId());
    }

    /** A pack is one pack across every company, so any company's live application saves it. */
    @Test
    void aLiveApplicationAtAnotherCompanyKeepsThePack() {
        Student student = newStudent();
        documentFor(student, LONG_AGO);
        applicationFor(student, tenantA, ApplicationStatus.REJECTED, LONG_AGO);
        applicationFor(student, tenantB, ApplicationStatus.UNDER_REVIEW, LONG_AGO);

        assertThat(expired()).isEmpty();
    }

    // ── The clock ───────────────────────────────────────────────────────────────────────────

    @ParameterizedTest
    @EnumSource(value = ApplicationStatus.class, names = {"REJECTED", "EXPIRED", "CANCELLED"})
    void aClosedApplicationExpiresOnceThePeriodHasPassed(ApplicationStatus closed) {
        Student student = newStudent();
        documentFor(student, LONG_AGO);
        applicationFor(student, tenantA, closed, LONG_AGO);

        assertThat(expired()).hasSize(1);
    }

    @Test
    void aRecentlyClosedApplicationIsStillWithinThePeriod() {
        Student student = newStudent();
        documentFor(student, LONG_AGO);
        applicationFor(student, tenantA, ApplicationStatus.REJECTED, RECENTLY);

        assertThat(expired()).isEmpty();
    }

    /** The clock is the latest thing that happened, not the earliest. */
    @Test
    void theMostRecentClosedApplicationSetsTheClock() {
        Student student = newStudent();
        documentFor(student, LONG_AGO);
        applicationFor(student, tenantA, ApplicationStatus.REJECTED, LONG_AGO);
        applicationFor(student, tenantB, ApplicationStatus.CANCELLED, RECENTLY);

        assertThat(expired()).isEmpty();
    }

    /** A tenancy that ended recently holds the pack even though the application closed long ago. */
    @Test
    void aRecentlyEndedTenancyHoldsThePack() {
        Student student = newStudent();
        documentFor(student, LONG_AGO);
        applicationFor(student, tenantA, ApplicationStatus.CHECKED_IN, LONG_AGO);
        allocationFor(student, AllocationStatus.COMPLETED, RECENTLY);

        assertThat(expired()).isEmpty();
    }

    /**
     * The one case with no application clock to read at all, and the only one where the upload
     * date decides anything.
     */
    @Test
    void aStudentWhoNeverAppliedIsMeasuredFromTheUploadDate() {
        Student neverApplied = newStudent();
        documentFor(neverApplied, LONG_AGO);
        Student uploadedRecently = newStudent();
        documentFor(uploadedRecently, RECENTLY);

        assertThat(expired()).extracting(d -> d.getStudent().getId())
                .containsExactly(neverApplied.getId());
    }

    // ── Shape of the result ─────────────────────────────────────────────────────────────────

    @Test
    void everyDocumentInAnExpiredPackIsReturned() {
        Student student = newStudent();
        documentFor(student, LONG_AGO);
        documentFor(student, LONG_AGO, documentType("PROOF_OF_REGISTRATION"));
        applicationFor(student, tenantA, ApplicationStatus.REJECTED, LONG_AGO);

        assertThat(expired()).hasSize(2);
    }

    @Test
    void thePageSizeIsRespectedSoALargeBacklogIsProcessedInBatches() {
        for (int i = 0; i < 5; i++) {
            Student student = newStudent();
            documentFor(student, LONG_AGO);
            applicationFor(student, tenantA, ApplicationStatus.REJECTED, LONG_AGO);
        }

        assertThat(findExpired(2)).hasSize(2);
        assertThat(findExpired(50)).hasSize(5);
    }

    // ── Fixtures ────────────────────────────────────────────────────────────────────────────

    private List<Document> expired() {
        return findExpired(50);
    }

    private List<Document> findExpired(int pageSize) {
        // The fixtures backdated timestamps in SQL, so the cached entities are stale.
        entityManager.clear();
        return documentRepository.findExpiredByRetention(
                DocumentRetentionJob.IN_USE_APPLICATION_STATUSES,
                DocumentRetentionJob.HELD_ALLOCATION_STATUSES,
                CUTOFF,
                PageRequest.of(0, pageSize));
    }

    private DocumentType documentType(String name) {
        DocumentType type = new DocumentType();
        type.setTypeName(name);
        type.setIsRequired(true);
        type.setTenantId(tenantA);
        return documentTypeRepository.saveAndFlush(type);
    }

    private Long newTenant(String name) {
        Tenant tenant = new Tenant();
        tenant.setName(name);
        return tenantRepository.saveAndFlush(tenant).getId();
    }

    private Student newStudent() {
        int suffix = ++uniqueSuffix;
        AppUser user = new AppUser();
        user.setEmail("retention-" + suffix + "@test.local");
        user.setUserPassword("not-a-real-password");
        user.setFirstName("Retention");
        user.setLastName("Test");
        user.setUserRole(UserRole.STUDENT);
        user = appUserRepository.saveAndFlush(user);

        Student student = new Student();
        student.setAppUser(user);
        student.setRegistrationDate(new Date());
        return studentRepository.saveAndFlush(student);
    }

    private void documentFor(Student student, LocalDateTime uploadedAt) {
        documentFor(student, uploadedAt, idDocument);
    }

    private void documentFor(Student student, LocalDateTime uploadedAt, DocumentType type) {
        Document document = new Document();
        document.setStudent(student);
        document.setDocumentType(type);
        document.setDocumentName("passport.pdf");
        document.setDocumentPath("STUDENT/" + student.getId() + "/" + type.getTypeName() + "/f.pdf");
        document.setFileSize(1024L);
        document.setDocumentStatus(DocumentStatus.APPROVED);
        Document saved = documentRepository.saveAndFlush(document);
        // @PrePersist stamps createdAt with the wall clock, and @PreUpdate would re-stamp
        // updatedAt on any entity write, so the age under test is set by SQL and the persistence
        // context is cleared so the query re-reads it.
        backdate("document", "created_at", saved.getId(), uploadedAt);
    }

    private void applicationFor(Student student, Long tenantId, ApplicationStatus status,
                                LocalDateTime closedAt) {
        Application application = Application.builder()
                .applicationCode("APP-" + (++uniqueSuffix))
                .applicationDate(LocalDate.from(closedAt))
                .status(status)
                .student(student)
                .tenantId(tenantId)
                .build();
        Application saved = applicationRepository.saveAndFlush(application);
        // @Version defaults to 0, so Spring Data treats the entity as existing and merges;
        // the generated id lands on the returned copy, not on the builder's instance.
        backdate("application", "updated_at", saved.getId(), closedAt);
    }

    private void allocationFor(Student student, AllocationStatus status, LocalDateTime endedAt) {
        Building building = new Building();
        building.setName("Retention Test " + (++uniqueSuffix));
        building.setAddress("1 Test Street");
        building.setTenantId(tenantA);
        building = buildingRepository.saveAndFlush(building);

        Room room = new Room();
        room.setBuilding(building);
        room.setRoomNumber("R" + uniqueSuffix);
        room.setCapacity(1);
        room.setCurrentOccupancy(0);
        room.setStatus("AVAILABLE");
        room.setTenantId(tenantA);
        room = roomRepository.saveAndFlush(room);

        RoomAllocation allocation = new RoomAllocation();
        allocation.setRoom(room);
        allocation.setStudent(student);
        allocation.setTenantId(tenantA);
        allocation.setStatus(status);
        allocation.setAllocationDate(endedAt);
        RoomAllocation saved = roomAllocationRepository.saveAndFlush(allocation);
        backdate("room_allocation", "updated_at", saved.getId(), endedAt);
    }

    /**
     * Moves a timestamp into the past in SQL.
     *
     * <p>Every entity here stamps its own timestamps in a lifecycle callback, so writing the field
     * through the repository would be overwritten by the flush that saves it. {@link #findExpired}
     * clears the context before querying, so the rows are re-read rather than served from the
     * cache.
     */
    private void backdate(String table, String column, Long id, LocalDateTime value) {
        entityManager.createNativeQuery(
                        "UPDATE " + table + " SET " + column + " = :value WHERE id = :id")
                .setParameter("value", value)
                .setParameter("id", id)
                .executeUpdate();
    }
}
