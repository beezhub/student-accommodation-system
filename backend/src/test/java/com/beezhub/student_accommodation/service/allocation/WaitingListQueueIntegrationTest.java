package com.beezhub.student_accommodation.service.allocation;

import static org.assertj.core.api.Assertions.assertThat;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import com.beezhub.student_accommodation.config.FF4jConfig;
import com.beezhub.student_accommodation.model.dto.BatchAllocationResult;
import com.beezhub.student_accommodation.model.entity.AllocationRule;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.ApplicationBuildingPreference;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAttributes;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.model.entity.WaitingListEntry;
import com.beezhub.student_accommodation.model.entity.YearOfStudy;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.GenderRestriction;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.model.event.RoomVacatedEvent;
import com.beezhub.student_accommodation.repository.AllocationRuleRepository;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.RoomAttributesRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.repository.TenantRepository;
import com.beezhub.student_accommodation.repository.WaitingListRepository;
import com.beezhub.student_accommodation.repository.YearOfStudyRepository;
import com.beezhub.student_accommodation.security.jwt.JwtUtil;
import com.beezhub.student_accommodation.security.user.AppUserDetails;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.ff4j.FF4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The waiting-list screen and the batch job against a real database.
 *
 * <p>Both read the queue in one session and score it in another, so a lazy association the query did
 * not fetch only fails here. The Mockito tests hand the strategy plain objects and cannot see it.
 */
class WaitingListQueueIntegrationTest extends AbstractIntegrationTest {

    private static final String WAITING_LIST_URL = "/api/admin/allocation/waiting-list";

    @Autowired private TestRestTemplate rest;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private FF4j ff4j;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private AllocationBatchService allocationBatchService;
    @Autowired private AllocationScoringOrchestrator allocationScoringOrchestrator;

    @Autowired private TenantRepository tenantRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private BuildingRepository buildingRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomAttributesRepository roomAttributesRepository;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private WaitingListRepository waitingListRepository;
    @Autowired private YearOfStudyRepository yearOfStudyRepository;
    @Autowired private AllocationRuleRepository allocationRuleRepository;

    private final List<Long> userIds = new ArrayList<>();
    private final List<Long> studentIds = new ArrayList<>();
    private final List<Long> buildingIds = new ArrayList<>();
    private final List<Long> ruleIds = new ArrayList<>();

    private Long tenantA;
    private Long tenantB;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        // Other classes share this database, and the refill serves one student only.
        jdbc.update("UPDATE waiting_list_entry SET status = 'CANCELLED' WHERE status = 'WAITING'");
        tenantA = newTenant("A");
        tenantB = newTenant("B");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        ff4j.disable(FF4jConfig.FEATURE_AUTO_ALLOCATION);
        ruleIds.forEach(id -> jdbc.update("DELETE FROM allocation_rules WHERE id = ?", id));
        for (Long id : studentIds) {
            jdbc.update("DELETE FROM waiting_list_entry WHERE student_id = ?", id);
            jdbc.update("DELETE FROM application WHERE student_id = ?", id);
            jdbc.update("DELETE FROM student WHERE id = ?", id);
        }
        userIds.forEach(id -> jdbc.update("DELETE FROM app_user WHERE id = ?", id));
        for (Long id : buildingIds) {
            jdbc.update("DELETE FROM room WHERE building_id = ?", id);
            jdbc.update("DELETE FROM building WHERE id = ?", id);
        }
        // Tenant rows stay: an async notification may still be stamping one, and nothing reads them.
    }

    // ==================== GET /api/admin/allocation/waiting-list ====================

    @Test
    void getWaitingList_applicantWithBuildingPreferenceAndFreeRoom_returnsBestRoom() {
        Building building = newBuilding(tenantA);
        Room room = newRoom(building, tenantA);
        Student student = newStudent("Ada", null, null);
        enqueue(student, newApplication(student, tenantA, building, null), tenantA);

        ResponseEntity<JsonNode> response = getWaitingListAs(UserRole.PLATFORM_ADMIN, 0L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode row = rowFor(response.getBody(), student);
        assertThat(row.get("bestMatchRoomId").asLong()).isEqualTo(room.getId());
    }

    @Test
    void getWaitingList_tenantAdmin_seesOnlyOwnTenantEntries() {
        Building buildingA = newBuilding(tenantA);
        newRoom(buildingA, tenantA);
        Student ownStudent = newStudent("Ada", null, null);
        enqueue(ownStudent, newApplication(ownStudent, tenantA, buildingA, null), tenantA);

        Building buildingB = newBuilding(tenantB);
        newRoom(buildingB, tenantB);
        Student otherStudent = newStudent("Bea", null, null);
        enqueue(otherStudent, newApplication(otherStudent, tenantB, buildingB, null), tenantB);

        ResponseEntity<JsonNode> response = getWaitingListAs(UserRole.TENANT_ADMIN, tenantA);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(studentIdsIn(response.getBody())).containsExactly(ownStudent.getId());
        JsonNode row = rowFor(response.getBody(), ownStudent);
        // Students are platform-tenant users, so a filtered join must not lose their name.
        assertThat(row.get("studentName").asText()).startsWith("Ada");
        assertThat(row.get("studentEmail").asText()).isEqualTo(ownStudent.getAppUser().getEmail());
    }

    @Test
    void getWaitingList_tenantAdminAndApplicationStampedPlatform_keepsApplicationOnRow() {
        // A platform admin's application carries tenant 0 while the entry carries the owning tenant.
        Building building = newBuilding(tenantA);
        newRoom(building, tenantA);
        Student student = newStudent("Ada", null, null);
        Application application = newApplication(student, 0L, building, null);
        enqueue(student, application, tenantA);

        ResponseEntity<JsonNode> response = getWaitingListAs(UserRole.TENANT_ADMIN, tenantA);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rowFor(response.getBody(), student).get("applicationId").asLong())
                .isEqualTo(application.getId());
    }

    @Test
    void getWaitingList_platformAdmin_seesEveryTenant() {
        Building buildingA = newBuilding(tenantA);
        newRoom(buildingA, tenantA);
        Student studentA = newStudent("Ada", null, null);
        enqueue(studentA, newApplication(studentA, tenantA, buildingA, null), tenantA);

        Building buildingB = newBuilding(tenantB);
        newRoom(buildingB, tenantB);
        Student studentB = newStudent("Bea", null, null);
        enqueue(studentB, newApplication(studentB, tenantB, buildingB, null), tenantB);

        ResponseEntity<JsonNode> response = getWaitingListAs(UserRole.PLATFORM_ADMIN, 0L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(studentIdsIn(response.getBody()))
                .containsExactlyInAnyOrder(studentA.getId(), studentB.getId());
    }

    // ==================== Batch job ====================

    @Test
    void batchAllocate_applicantWithBuildingPreference_allocatesStudent() {
        ff4j.enable(FF4jConfig.FEATURE_AUTO_ALLOCATION);
        Building building = newBuilding(tenantA);
        Room room = newRoom(building, tenantA);
        Student student = newStudent("Ada", null, null);
        Long entryId = enqueue(student, newApplication(student, tenantA, building, null), tenantA);

        BatchAllocationResult result = allocationBatchService.batchAllocateFromWaitingList(10);

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(allocationOf(student)).containsEntry("room_id", room.getId())
                .containsEntry("tenant_id", tenantA);
        assertThat(waitingListRepository.findById(entryId).orElseThrow().getStatus()).isEqualTo("ALLOCATED");
    }

    @Test
    void batchAllocate_studentWithYearOfStudyAndRestrictedRoom_allocatesStudent() {
        ff4j.enable(FF4jConfig.FEATURE_AUTO_ALLOCATION);
        Building building = newBuilding(tenantA);
        Room room = newRoom(building, tenantA);
        restrictRoom(room, 2, 3, null);
        Student student = newStudent("Ada", newYearOfStudy("2"), null);
        enqueue(student, newApplication(student, tenantA, building, null), tenantA);

        BatchAllocationResult result = allocationBatchService.batchAllocateFromWaitingList(10);

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(allocationOf(student)).containsEntry("room_id", room.getId());
    }

    @Test
    void batchAllocate_twoTenants_eachStudentGetsARoomOfOwnTenant() {
        ff4j.enable(FF4jConfig.FEATURE_AUTO_ALLOCATION);
        Building buildingA = newBuilding(tenantA);
        Room roomA = newRoom(buildingA, tenantA);
        Student studentA = newStudent("Ada", null, null);
        enqueue(studentA, newApplication(studentA, tenantA, buildingA, null), tenantA);

        Building buildingB = newBuilding(tenantB);
        Room roomB = newRoom(buildingB, tenantB);
        Student studentB = newStudent("Bea", null, null);
        enqueue(studentB, newApplication(studentB, tenantB, buildingB, null), tenantB);

        BatchAllocationResult result = allocationBatchService.batchAllocateFromWaitingList(10);

        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(allocationOf(studentA)).containsEntry("room_id", roomA.getId())
                .containsEntry("tenant_id", tenantA);
        assertThat(allocationOf(studentB)).containsEntry("room_id", roomB.getId())
                .containsEntry("tenant_id", tenantB);
    }

    // ==================== Fixtures ====================

    private ResponseEntity<JsonNode> getWaitingListAs(UserRole role, Long tenantId) {
        AppUser admin = newUser(role, tenantId, "Admin");
        String token = jwtUtil.generateToken(
                new AppUserDetails(admin.getEmail(), "", role.name(), tenantId));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return rest.exchange(WAITING_LIST_URL, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
    }

    private static List<Long> studentIdsIn(JsonNode rows) {
        return StreamSupport.stream(rows.spliterator(), false)
                .map(row -> row.get("studentId").asLong())
                .toList();
    }

    private static JsonNode rowFor(JsonNode rows, Student student) {
        return StreamSupport.stream(rows.spliterator(), false)
                .filter(row -> row.get("studentId").asLong() == student.getId())
                .findFirst()
                .orElseThrow();
    }

    /** The student's allocation as room_id and tenant_id, or an empty map if there is none. */
    private Map<String, Object> allocationOf(Student student) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT room_id, tenant_id FROM room_allocation WHERE student_id = ?", student.getId());
        assertThat(rows).as("allocations for student %d", student.getId()).hasSizeLessThan(2);
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private Long newTenant(String label) {
        return tenantRepository.save(Tenant.builder()
                .name("Queue " + label + " " + System.nanoTime())
                .isActive(true)
                .build()).getId();
    }

    private AppUser newUser(UserRole role, Long tenantId, String firstName) {
        AppUser user = appUserRepository.save(AppUser.builder()
                .email("queue-" + System.nanoTime() + "@test.local")
                .firstName(firstName)
                .lastName("Test")
                .userPassword("not-a-real-password")
                .userRole(role)
                .tenantId(tenantId)
                .emailVerified(true)
                .mustChangePassword(false)
                .build());
        userIds.add(user.getId());
        return user;
    }

    private Student newStudent(String firstName, YearOfStudy yearOfStudy, String specialRequirements) {
        Student student = new Student();
        student.setAppUser(newUser(UserRole.STUDENT, 0L, firstName));
        student.setRegistrationDate(new Date());
        student.setYearOfStudy(yearOfStudy);
        student.setSpecialRequirements(specialRequirements);
        student = studentRepository.save(student);
        studentIds.add(student.getId());
        return student;
    }

    private void setGender(Student student, String gender) {
        student.setGender(gender);
        studentRepository.save(student);
    }

    private YearOfStudy newYearOfStudy(String year) {
        YearOfStudy yearOfStudy = new YearOfStudy();
        yearOfStudy.setYearOfStudy(year);
        return yearOfStudyRepository.save(yearOfStudy);
    }

    private Building newBuilding(Long tenantId) {
        Building building = new Building();
        building.setName("Queue Hall " + System.nanoTime());
        building.setAddress("1 Test Street");
        building.setTenantId(tenantId);
        building = buildingRepository.save(building);
        buildingIds.add(building.getId());
        return building;
    }

    private Room newRoom(Building building, Long tenantId) {
        return newRoom(building, tenantId, 2);
    }

    private Room newRoom(Building building, Long tenantId, int capacity) {
        Room room = new Room();
        room.setRoomNumber("Q" + System.nanoTime());
        room.setBuilding(building);
        room.setTenantId(tenantId);
        room.setCapacity(capacity);
        room.setCurrentOccupancy(0);
        room.setStatus("AVAILABLE");
        return roomRepository.save(room);
    }

    private void restrictRoom(Room room, Integer minYear, Integer maxYear, GenderRestriction gender) {
        RoomAttributes attributes = new RoomAttributes();
        attributes.setRoom(room);
        attributes.setMinYearOfStudy(minYear);
        attributes.setMaxYearOfStudy(maxYear);
        attributes.setGenderRestriction(gender);
        roomAttributesRepository.save(attributes);
    }

    private Application newApplication(Student student, Long tenantId, Building preferred, String requirements) {
        Application application = new Application();
        application.setApplicationCode("Q-" + System.nanoTime());
        application.setApplicationDate(LocalDate.now());
        application.setStatus(ApplicationStatus.APPROVED);
        application.setStudent(student);
        application.setTenantId(tenantId);
        application.setPreferredBuilding(preferred);
        application.setSpecialRequirements(requirements);
        application.setBuildingPreferences(new ArrayList<>(List.of(ApplicationBuildingPreference.builder()
                .application(application)
                .building(preferred)
                .preferenceOrder(1)
                .build())));
        return applicationRepository.save(application);
    }

    private Long enqueue(Student student, Application application, Long tenantId) {
        WaitingListEntry entry = new WaitingListEntry();
        entry.setStudent(student);
        entry.setApplication(application);
        entry.setStatus("WAITING");
        entry.setTenantId(tenantId);
        return waitingListRepository.save(entry).getId();
    }

    private void newRule(Long tenantId, int priority, BigDecimal nsfasWeight) {
        AllocationRule rule = AllocationRule.builder()
                .ruleName("Queue rule " + System.nanoTime())
                .tenantId(tenantId)
                .active(true)
                .priority(priority)
                .nsfasPriorityWeight(nsfasWeight)
                .build();
        ruleIds.add(allocationRuleRepository.save(rule).getId());
    }
}
