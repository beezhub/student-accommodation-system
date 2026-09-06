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
import com.beezhub.student_accommodation.specification.BuildingBrowseSpecification;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * GH-221: three read paths decided availability from the denormalised {@code room.status} string.
 *
 * <p>That string is written by {@code RoomOccupancyManager.deriveStatus}, which reports
 * {@code AVAILABLE} only when a room is <em>completely</em> empty — one occupant in a two-bed room
 * makes it {@code OCCUPIED}. So every shared room with a free bed was counted as having none: the
 * per-building count undercounted, the browse filter hid buildings that had space, and the detail
 * view repeated the same test by hand.
 *
 * <p>All three now derive from the live allocation count, the same set
 * {@code countActiveAllocationsByRoomId} counts and that occupancy is written from, so read and
 * write agree.
 */
class RoomAvailabilityQueryTest extends AbstractJpaTest {

    @Autowired private RoomRepository roomRepository;
    @Autowired private BuildingRepository buildingRepository;
    @Autowired private RoomAllocationRepository roomAllocationRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private TenantRepository tenantRepository;

    private Building building;
    private Long tenantId;

    @BeforeEach
    void setUp() {
        // AbstractJpaTest builds the schema from the entity mappings with Flyway off, so the tenant
        // table exists but is empty — the platform tenant (id 0) is a Flyway seed. PostgreSQL
        // enforces the tenant_id foreign key, so a fixture cannot simply assume 0 exists.
        Tenant tenant = new Tenant();
        tenant.setName("Availability Test Company");
        tenantId = tenantRepository.saveAndFlush(tenant).getId();

        building = new Building();
        building.setName("Availability Test");
        building.setAddress("1 Test Street");
        building.setTenantId(tenantId);
        building = buildingRepository.saveAndFlush(building);
    }

    /**
     * The case the old definition got wrong: a shared room with a bed still free. Its status reads
     * OCCUPIED, so `status = 'AVAILABLE'` skipped it.
     */
    @Test
    void aSharedRoomWithAFreeBedCountsAsAvailable() {
        Room shared = room("S1", 2, "OCCUPIED");
        hold(shared, AllocationStatus.ACTIVE);

        assertThat(roomRepository.countAvailableRoomsByBuildingId(building.getId())).isEqualTo(1L);
    }

    @Test
    void aFullRoomDoesNotCount() {
        Room full = room("F1", 1, "FULL");
        hold(full, AllocationStatus.ACTIVE);

        assertThat(roomRepository.countAvailableRoomsByBuildingId(building.getId())).isZero();
    }

    @Test
    void anEmptyRoomCounts() {
        room("E1", 2, "AVAILABLE");

        assertThat(roomRepository.countAvailableRoomsByBuildingId(building.getId())).isEqualTo(1L);
    }

    /** An administrative decision about the room, not a statement about how many students are in it. */
    @Test
    void aMaintenanceRoomNeverCountsHoweverEmpty() {
        room("M1", 5, "MAINTENANCE");

        assertThat(roomRepository.countAvailableRoomsByBuildingId(building.getId())).isZero();
    }

    /**
     * An offer reserves a bed until it lapses, or two students could be offered the last one, so
     * all three holding statuses count against capacity.
     */
    @ParameterizedTest
    @EnumSource(value = AllocationStatus.class, names = {"ACTIVE", "ACCEPTED", "OFFERED"})
    void everyStatusThatHoldsABedCountsAgainstCapacity(AllocationStatus holding) {
        Room single = room("H1", 1, "OCCUPIED");
        hold(single, holding);

        assertThat(roomRepository.countAvailableRoomsByBuildingId(building.getId())).isZero();
    }

    @ParameterizedTest
    @EnumSource(value = AllocationStatus.class, names = {"DECLINED", "EXPIRED", "CANCELLED", "COMPLETED"})
    void aFinishedAllocationReleasesItsBed(AllocationStatus finished) {
        Room single = room("R1", 1, "OCCUPIED");
        hold(single, finished);

        assertThat(roomRepository.countAvailableRoomsByBuildingId(building.getId())).isEqualTo(1L);
    }

    /** The list view and the detail view must not disagree about the same building. */
    @Test
    void thePerBuildingCountAgreesWithTheSingleBuildingCount() {
        hold(room("S1", 2, "OCCUPIED"), AllocationStatus.ACTIVE);
        hold(room("F1", 1, "FULL"), AllocationStatus.ACTIVE);
        room("E1", 2, "AVAILABLE");
        room("M1", 5, "MAINTENANCE");

        long single = roomRepository.countAvailableRoomsByBuildingId(building.getId());
        long grouped = roomRepository.findAvailableRoomCountByBuilding().stream()
                .filter(row -> building.getId().equals(row[0]))
                .map(row -> (Long) row[1])
                .findFirst()
                .orElse(0L);

        assertThat(single).isEqualTo(2L);
        assertThat(grouped).isEqualTo(single);
    }

    @Test
    void theBrowseFilterFindsABuildingWhoseOnlySpaceIsInASharedRoom() {
        // Before the fix this building vanished from the "available" filter, because its only room
        // with space read as OCCUPIED.
        hold(room("S1", 2, "OCCUPIED"), AllocationStatus.ACTIVE);

        List<Building> matched = buildingRepository.findAll(
                BuildingBrowseSpecification.filterBy(null, null, true, null, null));

        assertThat(matched).extracting(Building::getId).contains(building.getId());
    }

    @Test
    void theBrowseFilterExcludesABuildingWithNoSpaceLeft() {
        hold(room("F1", 1, "FULL"), AllocationStatus.ACTIVE);
        room("M1", 5, "MAINTENANCE");

        List<Building> matched = buildingRepository.findAll(
                BuildingBrowseSpecification.filterBy(null, null, true, null, null));

        assertThat(matched).extracting(Building::getId).doesNotContain(building.getId());
    }

    /**
     * The allocation candidate query did not exclude MAINTENANCE, and nothing downstream did
     * either, so auto-allocation could place a student in a room an admin had parked.
     */
    @Test
    void allocationCandidatesExcludeMaintenanceRooms() {
        room("M1", 5, "MAINTENANCE");
        room("E1", 2, "AVAILABLE");

        List<Room> candidates = roomRepository.findRoomsWithAvailableCapacity();

        assertThat(candidates).extracting(Room::getStatus).doesNotContain("MAINTENANCE");
        assertThat(candidates).extracting(Room::getRoomNumber).contains("E1");
    }

    private Room room(String number, int capacity, String status) {
        Room room = new Room();
        room.setBuilding(building);
        room.setRoomNumber(number);
        room.setCapacity(capacity);
        room.setCurrentOccupancy(0);
        room.setStatus(status);
        room.setTenantId(tenantId);
        return roomRepository.saveAndFlush(room);
    }

    private void hold(Room room, AllocationStatus status) {
        AppUser user = new AppUser();
        user.setEmail("avail-" + room.getRoomNumber() + status + "@test.local");
        user.setUserPassword("not-a-real-password");
        user.setFirstName("Avail");
        user.setLastName("Test");
        user.setUserRole(UserRole.STUDENT);
        user.setTenantId(tenantId);
        user = appUserRepository.saveAndFlush(user);

        Student student = new Student();
        student.setAppUser(user);
        student.setRegistrationDate(new Date());
        student = studentRepository.saveAndFlush(student);

        RoomAllocation allocation = new RoomAllocation();
        allocation.setRoom(room);
        allocation.setStudent(student);
        allocation.setTenantId(tenantId);
        allocation.setStatus(status);
        allocation.setAllocationDate(LocalDateTime.now());
        roomAllocationRepository.saveAndFlush(allocation);
    }
}
