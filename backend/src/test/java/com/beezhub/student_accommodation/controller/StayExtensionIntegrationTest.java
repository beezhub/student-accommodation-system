package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.Intake;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.repository.TenantRepository;
import com.beezhub.student_accommodation.security.jwt.JwtUtil;
import com.beezhub.student_accommodation.security.user.AppUserDetails;
import com.beezhub.student_accommodation.service.DocumentTypeService;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * "Stay for Semester 2" end to end on PostgreSQL (GH-498): the student endpoints, checked by
 * {@code AllocationAccessGuard}, the overlap count through the gap between intakes, and the room
 * lock that gives the last bed to one of two students asking at once.
 */
@DisplayName("Stay extension (integration)")
class StayExtensionIntegrationTest extends AbstractIntegrationTest {

    private static final LocalDate S1_START = LocalDate.of(2091, 2, 1);
    private static final LocalDate S1_END = LocalDate.of(2091, 6, 30);
    private static final LocalDate S2_START = LocalDate.of(2091, 7, 15);
    private static final LocalDate S2_END = LocalDate.of(2091, 11, 30);

    @Autowired TestRestTemplate rest;
    @Autowired JwtUtil jwtUtil;
    @Autowired JdbcTemplate jdbc;
    @Autowired TenantRepository tenantRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired BuildingRepository buildingRepository;
    @Autowired RoomRepository roomRepository;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired RoomAllocationRepository roomAllocationRepository;
    @Autowired DocumentTypeService documentTypeService;

    private final List<AppUser> users = new ArrayList<>();
    private Tenant tenant;
    private Long yearId;
    private Long semester1;
    private Long semester2;
    private Long annual;
    private Room room;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        tenant = tenantRepository.save(Tenant.builder().name("Stay Co " + System.nanoTime()).isActive(true).build());
        TenantContext.clear();
        yearId = jdbc.queryForObject("INSERT INTO academic_year (name, start_date, end_date, tenant_id) "
                + "VALUES ('2091', ?, ?, ?) RETURNING id", Long.class,
                LocalDate.of(2091, 1, 15), LocalDate.of(2091, 12, 10), tenant.getId());
        semester1 = intake("SEMESTER", "Semester 1", S1_START, S1_END);
        semester2 = intake("SEMESTER", "Semester 2", S2_START, S2_END);
        annual = intake("ANNUAL", "Annual", S1_START, S2_END);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        // The confirmation is written by an async listener after commit; give it time to land so
        // its notification row does not outlive the user it points at.
        Thread.sleep(500);
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        if (room != null) {
            jdbc.update("DELETE FROM allocation_audit_log WHERE allocation_id IN "
                    + "(SELECT id FROM room_allocation WHERE room_id = ?)", room.getId());
            jdbc.update("DELETE FROM room_allocation WHERE room_id = ?", room.getId());
        }
        jdbc.update("DELETE FROM application WHERE tenant_id = ?", tenant.getId());
        if (room != null) {
            jdbc.update("DELETE FROM room WHERE id = ?", room.getId());
        }
        jdbc.update("DELETE FROM building WHERE tenant_id = ?", tenant.getId());
        for (AppUser user : users) {
            jdbc.update("DELETE FROM notification WHERE user_id = ?", user.getId());
            jdbc.update("DELETE FROM user_notifications WHERE user_id = ?", user.getId());
            jdbc.update("DELETE FROM document WHERE student_id IN (SELECT id FROM student WHERE user_id = ?)", user.getId());
            jdbc.update("DELETE FROM student WHERE user_id = ?", user.getId());
            jdbc.update("DELETE FROM app_user WHERE id = ?", user.getId());
        }
        jdbc.update("DELETE FROM document_type WHERE tenant_id = ?", tenant.getId());
        jdbc.update("DELETE FROM intake WHERE academic_year_id = ?", yearId);
        jdbc.update("DELETE FROM academic_year WHERE tenant_id = ?", tenant.getId());
        jdbc.update("DELETE FROM activity_logs WHERE tenant_id = ?", tenant.getId());
        jdbc.update("DELETE FROM tenant WHERE id = ?", tenant.getId());
    }

    @Test
    @DisplayName("A Semester 1 resident stays for Semester 2 straight through the gap")
    void extend_bedFree_movesTheStayEndThroughTheGap() {
        givenRoom(1);
        AppUser resident = studentUser();
        RoomAllocation allocation = livingIn(resident, semester1, S1_START, S1_END);

        ResponseEntity<String> option = call(HttpMethod.GET, allocation.getId(), resident);
        ResponseEntity<String> extended = call(HttpMethod.POST, allocation.getId(), resident);

        assertThat(option.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(option.getBody()).contains("\"allowed\":true").contains("Semester 2");
        assertThat(extended.getStatusCode()).isEqualTo(HttpStatus.OK);
        RoomAllocation after = roomAllocationRepository.findById(allocation.getId()).orElseThrow();
        assertThat(after.getStayEndDate()).isEqualTo(S2_END);
        assertThat(after.getStayStartDate()).isEqualTo(S1_START);
        assertThat(after.getStatus()).isEqualTo(AllocationStatus.ACTIVE);
        assertThat(jdbc.queryForObject("SELECT room_id FROM room_allocation WHERE id = ?", Long.class,
                allocation.getId())).isEqualTo(room.getId());
        // The bed counts as taken in the gap between the semesters.
        assertThat(roomAllocationRepository.countHoldingBeds(room.getId(), S1_END.plusDays(1), S2_START)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM activity_logs WHERE action = 'EXTEND_STAY' "
                + "AND entity_id = ?", Integer.class, allocation.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("Refused when the bed has been taken for Semester 2")
    void extend_bedTaken_refused() {
        givenRoom(1);
        AppUser resident = studentUser();
        RoomAllocation allocation = livingIn(resident, semester1, S1_START, S1_END);
        offeredForSemester2(studentUser());

        ResponseEntity<String> option = call(HttpMethod.GET, allocation.getId(), resident);
        ResponseEntity<String> extended = call(HttpMethod.POST, allocation.getId(), resident);

        assertThat(option.getBody()).contains("\"allowed\":false").contains("\"nextIntakeName\":\"Semester 2\"");
        assertThat(extended.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(roomAllocationRepository.findById(allocation.getId()).orElseThrow().getStayEndDate()).isEqualTo(S1_END);
    }

    @Test
    @DisplayName("Refused while a required expiring document has expired")
    void extend_expiredDocument_refused() {
        givenRoom(1);
        AppUser resident = studentUser();
        RoomAllocation allocation = livingIn(resident, semester1, S1_START, S1_END);
        jdbc.update("INSERT INTO academic_year (name, start_date, end_date, tenant_id) VALUES ('2020', ?, ?, ?)",
                LocalDate.of(2020, 2, 1), LocalDate.of(2020, 11, 30), tenant.getId());
        Long proofType = jdbc.queryForObject("INSERT INTO document_type (type_name, is_required, is_expiring, tenant_id) "
                + "VALUES ('Proof of Registration', true, true, ?) RETURNING id", Long.class, tenant.getId());
        document(studentOf(resident), proofType, LocalDateTime.of(2020, 6, 1, 10, 0));

        ResponseEntity<String> option = call(HttpMethod.GET, allocation.getId(), resident);
        ResponseEntity<String> extended = call(HttpMethod.POST, allocation.getId(), resident);

        assertThat(option.getBody()).contains("\"allowed\":false").contains("expired on 30 November 2020");
        assertThat(extended.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("An Annual stay cannot be extended")
    void extend_annualStay_refused() {
        givenRoom(1);
        AppUser resident = studentUser();
        RoomAllocation allocation = livingIn(resident, annual, S1_START, S2_END);

        ResponseEntity<String> extended = call(HttpMethod.POST, allocation.getId(), resident);

        assertThat(extended.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(extended.getBody()).contains("Annual stay cannot be extended");
    }

    @Test
    @DisplayName("Another student gets 403")
    void extend_anotherStudent_forbidden() {
        givenRoom(1);
        AppUser resident = studentUser();
        AppUser other = studentUser();
        RoomAllocation allocation = livingIn(resident, semester1, S1_START, S1_END);

        assertThat(call(HttpMethod.GET, allocation.getId(), other).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.POST, allocation.getId(), other).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(roomAllocationRepository.findById(allocation.getId()).orElseThrow().getStayEndDate()).isEqualTo(S1_END);
    }

    @Test
    @DisplayName("Two concurrent requests for the last bed give one success")
    void extend_twoStudentsForTheLastBed_oneSucceeds() throws Exception {
        givenRoom(2);
        AppUser first = studentUser();
        AppUser second = studentUser();
        RoomAllocation firstPlace = livingIn(first, semester1, S1_START, S1_END);
        RoomAllocation secondPlace = livingIn(second, semester1, S1_START, S1_END);
        offeredForSemester2(studentUser());

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<HttpStatusCode> a = pool.submit(() -> {
                start.await();
                return call(HttpMethod.POST, firstPlace.getId(), first).getStatusCode();
            });
            Future<HttpStatusCode> b = pool.submit(() -> {
                start.await();
                return call(HttpMethod.POST, secondPlace.getId(), second).getStatusCode();
            });
            start.countDown();
            List<HttpStatusCode> outcomes = List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));

            assertThat(outcomes).containsExactlyInAnyOrder(HttpStatus.OK, HttpStatus.CONFLICT);
        } finally {
            pool.shutdownNow();
        }
        assertThat(roomAllocationRepository.countHoldingBeds(room.getId(), S2_START, S2_END)).isEqualTo(2);
    }

    // ---------------------------------------------------------------------------------- helpers

    private Long intake(String type, String name, LocalDate start, LocalDate end) {
        return jdbc.queryForObject("""
                INSERT INTO intake (tenant_id, academic_year_id, intake_type, name, stay_start_date, stay_end_date,
                                    application_open_date, application_close_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, tenant.getId(), yearId, type, name, start, end, start.minusDays(120), start.minusDays(14));
    }

    private void givenRoom(int capacity) {
        TenantContext.setTenantId(tenant.getId());
        Building building = new Building();
        building.setName("Stay Hall " + System.nanoTime());
        building.setAddress("1 Test Street");
        building.setTenantId(tenant.getId());
        building = buildingRepository.save(building);
        Room saved = new Room();
        saved.setBuilding(building);
        saved.setRoomNumber("S" + System.nanoTime() % 100000);
        saved.setCapacity(capacity);
        saved.setCurrentOccupancy(0);
        saved.setStatus("AVAILABLE");
        saved.setTenantId(tenant.getId());
        room = roomRepository.save(saved);
        TenantContext.clear();
    }

    /** A checked-in resident, with every required document approved. */
    private RoomAllocation livingIn(AppUser user, Long intakeId, LocalDate stayStart, LocalDate stayEnd) {
        Student student = studentOf(user);
        for (DocumentType type : documentTypeService.getEffectiveDocumentTypes(tenant.getId())) {
            if (Boolean.TRUE.equals(type.getIsRequired())) {
                document(student, type.getId(), LocalDateTime.now());
            }
        }
        TenantContext.setTenantId(tenant.getId());
        try {
            Application application = applicationRepository.save(Application.builder()
                    .academicYear(AcademicYear.builder().id(yearId).build())
                    .intake(Intake.builder().id(intakeId).build())
                    .applicationCode("APP-" + System.nanoTime())
                    .applicationDate(LocalDate.now())
                    .status(ApplicationStatus.CHECKED_IN)
                    .student(student)
                    .tenantId(tenant.getId())
                    .build());
            return allocation(student, application, AllocationStatus.ACTIVE, stayStart, stayEnd);
        } finally {
            TenantContext.clear();
        }
    }

    /** Another student's offer for Semester 2 in the same room. */
    private void offeredForSemester2(AppUser user) {
        TenantContext.setTenantId(tenant.getId());
        try {
            allocation(studentOf(user), null, AllocationStatus.OFFERED, S2_START, S2_END);
        } finally {
            TenantContext.clear();
        }
    }

    private RoomAllocation allocation(Student student, Application application, AllocationStatus status,
            LocalDate stayStart, LocalDate stayEnd) {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setStudent(student);
        allocation.setRoom(room);
        allocation.setApplication(application);
        allocation.setTenantId(tenant.getId());
        allocation.setStatus(status);
        allocation.setAllocationDate(LocalDateTime.now().minusMonths(4));
        allocation.setStayStartDate(stayStart);
        allocation.setStayEndDate(stayEnd);
        if (status == AllocationStatus.OFFERED) {
            allocation.setOfferExpiryDate(LocalDateTime.now().plusDays(5));
        }
        return roomAllocationRepository.save(allocation);
    }

    private void document(Student student, Long typeId, LocalDateTime uploadedAt) {
        jdbc.update("""
                INSERT INTO document (document_name, document_type_id, document_path, document_status, file_size,
                                      student_id, tenant_id, created_at, updated_at, version)
                VALUES ('doc.pdf', ?, ?, 'APPROVED', 10, ?, 0, ?, ?, 0)
                """, typeId, "student/" + student.getId() + "/" + typeId + ".pdf", student.getId(),
                Timestamp.valueOf(uploadedAt), Timestamp.valueOf(uploadedAt));
    }

    private ResponseEntity<String> call(HttpMethod method, Long allocationId, AppUser user) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtUtil.generateToken(new AppUserDetails(user.getEmail(), "",
                user.getUserRole().name(), user.getTenantId())));
        return rest.exchange("/api/room-allocation/" + allocationId + "/stay-extension", method,
                new HttpEntity<>(headers), String.class);
    }

    private Student studentOf(AppUser user) {
        return studentRepository.findByAppUser_Id(user.getId()).orElseThrow();
    }

    private AppUser studentUser() {
        AppUser user = appUserRepository.save(AppUser.builder()
                .email("stay." + System.nanoTime() + "@test.com")
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
        studentRepository.save(student);
        return user;
    }
}
