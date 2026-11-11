package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Building;
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
import com.beezhub.student_accommodation.service.allocation.ReconfirmationService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * "Still coming?" before move-in, end to end on PostgreSQL (GH-496): the student endpoint, checked
 * by {@code AllocationAccessGuard}, and the daily job releasing a place nobody reconfirmed.
 */
@DisplayName("Reconfirmation before move-in (integration)")
class ReconfirmationIntegrationTest extends AbstractIntegrationTest {

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
    @Autowired ReconfirmationService reconfirmationService;

    private final List<AppUser> users = new ArrayList<>();
    private Tenant tenant;
    private Room room;
    private AppUser owner;
    private AppUser otherStudent;
    private RoomAllocation allocation;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        tenant = tenantRepository.save(Tenant.builder().name("Reconfirm Co " + System.nanoTime()).isActive(true).build());
        owner = saveStudentUser();
        otherStudent = saveStudentUser();

        TenantContext.setTenantId(tenant.getId());
        Building building = new Building();
        building.setName("Reconfirm Hall " + System.nanoTime());
        building.setAddress("1 Test Street");
        building.setTenantId(tenant.getId());
        building = buildingRepository.save(building);
        room = new Room();
        room.setBuilding(building);
        room.setRoomNumber("R" + System.nanoTime() % 100000);
        room.setCapacity(1);
        room.setCurrentOccupancy(0);
        room.setStatus("AVAILABLE");
        room.setTenantId(tenant.getId());
        room = roomRepository.save(room);

        Student student = studentRepository.findByAppUser_Id(owner.getId()).orElseThrow();
        Application application = applicationRepository.save(Application.builder()
                .academicYear(testCalendar().year())
                .intake(testCalendar().intake())
                .applicationCode("APP-" + System.nanoTime())
                .applicationDate(LocalDate.now())
                .status(ApplicationStatus.ALLOCATED)
                .student(student)
                .tenantId(tenant.getId())
                .build());

        RoomAllocation accepted = new RoomAllocation();
        accepted.setStudent(student);
        accepted.setRoom(room);
        accepted.setApplication(application);
        accepted.setTenantId(tenant.getId());
        accepted.setStatus(AllocationStatus.ACCEPTED);
        accepted.setAllocationDate(LocalDateTime.now().minusMonths(4));
        accepted.setAcceptedAt(LocalDateTime.now().minusMonths(4));
        accepted.setStayStartDate(LocalDate.now().plusDays(10));
        accepted.setStayEndDate(LocalDate.now().plusMonths(10));
        accepted.setReconfirmRequestedAt(LocalDateTime.now().minusDays(18));
        allocation = roomAllocationRepository.save(accepted);
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        // The release email is written by an async listener after commit; wait for it so its
        // notification row does not outlive the user it points at.
        for (int i = 0; i < 50 && pendingAsyncWork(); i++) {
            Thread.sleep(100);
        }
        TenantContext.clear();
        jdbc.update("DELETE FROM allocation_audit_log WHERE allocation_id = ?", allocation.getId());
        jdbc.update("DELETE FROM room_allocation WHERE room_id = ?", room.getId());
        jdbc.update("DELETE FROM waiting_list_entry WHERE tenant_id = ?", tenant.getId());
        jdbc.update("DELETE FROM application WHERE tenant_id = ?", tenant.getId());
        jdbc.update("DELETE FROM room WHERE id = ?", room.getId());
        jdbc.update("DELETE FROM building WHERE tenant_id = ?", tenant.getId());
        for (AppUser user : users) {
            jdbc.update("DELETE FROM notification WHERE user_id = ?", user.getId());
            jdbc.update("DELETE FROM user_notifications WHERE user_id = ?", user.getId());
            jdbc.update("DELETE FROM student WHERE user_id = ?", user.getId());
            jdbc.update("DELETE FROM app_user WHERE id = ?", user.getId());
        }
        jdbc.update("DELETE FROM activity_logs WHERE tenant_id = ?", tenant.getId());
        jdbc.update("DELETE FROM tenant WHERE id = ?", tenant.getId());
    }

    private boolean pendingAsyncWork() {
        String status = jdbc.queryForObject("SELECT status FROM room_allocation WHERE id = ?", String.class,
                allocation.getId());
        if (!"CANCELLED".equals(status)) {
            return false;
        }
        Integer emails = jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE user_id = ?",
                Integer.class, owner.getId());
        return emails == null || emails == 0;
    }

    @Test
    @DisplayName("A student reconfirms their own place")
    void reconfirm_owner_returnsOkAndKeepsThePlace() {
        ResponseEntity<String> response = post(allocation.getId(), tokenFor(owner));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(roomAllocationRepository.findById(allocation.getId()).orElseThrow().getReconfirmedAt()).isNotNull();

        reconfirmationService.runDaily();

        assertThat(roomAllocationRepository.findById(allocation.getId()).orElseThrow().getStatus())
                .isEqualTo(AllocationStatus.ACCEPTED);
    }

    @Test
    @DisplayName("Another student cannot reconfirm the place")
    void reconfirm_anotherStudent_returnsForbidden() {
        ResponseEntity<String> response = post(allocation.getId(), tokenFor(otherStudent));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(roomAllocationRepository.findById(allocation.getId()).orElseThrow().getReconfirmedAt()).isNull();
    }

    @Test
    @DisplayName("A place nobody reconfirmed is released at the release point")
    void runDaily_notReconfirmed_placeReleasedAndApplicationCancelled() {
        int processed = reconfirmationService.runDaily();

        assertThat(processed).isGreaterThanOrEqualTo(1);
        RoomAllocation released = roomAllocationRepository.findById(allocation.getId()).orElseThrow();
        assertThat(released.getStatus()).isEqualTo(AllocationStatus.CANCELLED);
        String applicationStatus = jdbc.queryForObject(
                "SELECT a.status FROM application a JOIN room_allocation ra ON ra.application_id = a.id WHERE ra.id = ?",
                String.class, allocation.getId());
        assertThat(applicationStatus).isEqualTo("CANCELLED");
        assertThat(roomAllocationRepository.countHoldingBeds(room.getId(), released.getStayStartDate(),
                released.getStayEndDate())).isZero();
    }

    private ResponseEntity<String> post(Long allocationId, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return rest.exchange("/api/room-allocation/" + allocationId + "/reconfirm", HttpMethod.POST,
                new HttpEntity<>(headers), String.class);
    }

    private String tokenFor(AppUser user) {
        return jwtUtil.generateToken(new AppUserDetails(user.getEmail(), "", user.getUserRole().name(), user.getTenantId()));
    }

    private AppUser saveStudentUser() {
        AppUser user = appUserRepository.save(AppUser.builder()
                .email("reconfirm." + System.nanoTime() + "@test.com")
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
