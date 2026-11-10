package com.beezhub.student_accommodation.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.beezhub.student_accommodation.AbstractJpaTest;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.service.allocation.StayPeriod;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * GH-496: room capacity is counted by overlapping planned stays, and occupancy by who is in the
 * room today. Run against PostgreSQL, since the date comparisons live in the queries.
 */
class StayOverlapQueryTest extends AbstractJpaTest {

    private static final StayPeriod SEMESTER_1 = new StayPeriod(LocalDate.of(2027, 2, 1), LocalDate.of(2027, 6, 30));
    private static final StayPeriod SEMESTER_2 = new StayPeriod(LocalDate.of(2027, 7, 15), LocalDate.of(2027, 11, 30));
    private static final StayPeriod ANNUAL_2027 = new StayPeriod(LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30));
    private static final StayPeriod ANNUAL_2026 = new StayPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 11, 30));
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

    @Autowired private RoomRepository roomRepository;
    @Autowired private BuildingRepository buildingRepository;
    @Autowired private RoomAllocationRepository roomAllocationRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private TenantRepository tenantRepository;

    private Long tenantId;
    private Room room;
    private int students;

    @BeforeEach
    void setUp() {
        Tenant tenant = new Tenant();
        tenant.setName("Stay Overlap Company");
        tenantId = tenantRepository.saveAndFlush(tenant).getId();

        Building building = new Building();
        building.setName("Stay Overlap");
        building.setAddress("1 Test Street");
        building.setTenantId(tenantId);
        building = buildingRepository.saveAndFlush(building);

        room = new Room();
        room.setBuilding(building);
        room.setRoomNumber("12");
        room.setCapacity(1);
        room.setCurrentOccupancy(0);
        room.setStatus("AVAILABLE");
        room.setTenantId(tenantId);
        room = roomRepository.saveAndFlush(room);
    }

    @Test
    void semester1AndSemester2_shareABed() {
        hold(newStudent(), AllocationStatus.ACCEPTED, SEMESTER_1);

        assertThat(beds(SEMESTER_2)).isZero();
        assertThat(beds(SEMESTER_1)).isEqualTo(1L);
    }

    @Test
    void anAnnualStay_blocksBothSemesters() {
        hold(newStudent(), AllocationStatus.OFFERED, ANNUAL_2027);

        assertThat(beds(SEMESTER_1)).isEqualTo(1L);
        assertThat(beds(SEMESTER_2)).isEqualTo(1L);
    }

    @Test
    void a2026StayAndA2027Stay_neverClash() {
        hold(newStudent(), AllocationStatus.ACTIVE, ANNUAL_2026);

        assertThat(beds(ANNUAL_2027)).isZero();
    }

    @Test
    void anAllocationWithNoPlannedDates_blocksEveryStay() {
        hold(newStudent(), AllocationStatus.ACCEPTED, StayPeriod.UNBOUNDED);

        assertThat(beds(ANNUAL_2027)).isEqualTo(1L);
        assertThat(beds(ANNUAL_2026)).isEqualTo(1L);
    }

    @Test
    void aFinishedAllocation_releasesTheBed() {
        hold(newStudent(), AllocationStatus.CANCELLED, ANNUAL_2027);

        assertThat(beds(ANNUAL_2027)).isZero();
    }

    @Test
    void occupancyToday_countsOnlyStaysThatIncludeToday() {
        hold(newStudent(), AllocationStatus.ACCEPTED, ANNUAL_2026);
        hold(newStudent(), AllocationStatus.ACCEPTED, ANNUAL_2027);

        assertThat(roomAllocationRepository.countInRoomOn(room.getId(), TODAY)).isEqualTo(1L);
    }

    /** A resident who has not checked out is still in the room, whatever the planned dates say. */
    @Test
    void occupancyToday_countsAnOverdueResident() {
        hold(newStudent(), AllocationStatus.ACTIVE, new StayPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 6, 30)));

        assertThat(roomAllocationRepository.countInRoomOn(room.getId(), TODAY)).isEqualTo(1L);
        assertThat(roomAllocationRepository.countByRoom_IdAndStatus(room.getId(), AllocationStatus.ACTIVE)).isEqualTo(1L);
    }

    @Test
    void a2026Resident_isFreeFor2027_butNotForAnOverlappingStay() {
        Student resident = newStudent();
        hold(resident, AllocationStatus.ACTIVE, ANNUAL_2026);

        assertThat(roomAllocationRepository.existsLiveAllocationForStudentDuring(
                resident.getId(), ANNUAL_2027.from(), ANNUAL_2027.until())).isFalse();
        StayPeriod semester2026 = new StayPeriod(LocalDate.of(2026, 7, 15), LocalDate.of(2026, 11, 30));
        assertThat(roomAllocationRepository.existsLiveAllocationForStudentDuring(
                resident.getId(), semester2026.from(), semester2026.until())).isTrue();
    }

    @Test
    void liveAllocations_aStudentCanHoldTwo() {
        Student resident = newStudent();
        hold(resident, AllocationStatus.ACTIVE, ANNUAL_2026);
        hold(resident, AllocationStatus.ACCEPTED, ANNUAL_2027);

        assertThat(roomAllocationRepository.findLiveAllocationsByStudentId(resident.getId())).hasSize(2);
    }

    private long beds(StayPeriod stay) {
        return roomAllocationRepository.countHoldingBeds(room.getId(), stay.from(), stay.until());
    }

    private Student newStudent() {
        students++;
        AppUser user = new AppUser();
        user.setEmail("stay-" + students + "@test.local");
        user.setUserPassword("not-a-real-password");
        user.setFirstName("Stay");
        user.setLastName("Test");
        user.setUserRole(UserRole.STUDENT);
        user.setTenantId(tenantId);
        user = appUserRepository.saveAndFlush(user);

        Student student = new Student();
        student.setAppUser(user);
        student.setRegistrationDate(new Date());
        return studentRepository.saveAndFlush(student);
    }

    private void hold(Student student, AllocationStatus status, StayPeriod stay) {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setRoom(room);
        allocation.setStudent(student);
        allocation.setTenantId(tenantId);
        allocation.setStatus(status);
        allocation.setAllocationDate(LocalDateTime.now());
        stay.applyTo(allocation);
        roomAllocationRepository.saveAndFlush(allocation);
    }
}
