package com.beezhub.student_accommodation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import com.beezhub.student_accommodation.exceptions.ResourceInUseException;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Runs on real PostgreSQL because the damage is done by the foreign keys:
 * {@code room_allocation.room_id} is ON DELETE CASCADE and {@code room.building_id} is ON DELETE
 * SET NULL, and a Mockito test sees neither.
 *
 * <p>Runs as a tenant admin so the tenant filter is on, the same as the real request.
 */
class RoomAndBuildingDeleteIntegrationTest extends AbstractIntegrationTest {

    @Autowired private RoomService roomService;
    @Autowired private BuildingService buildingService;
    @Autowired private RoomRepository roomRepository;
    @Autowired private BuildingRepository buildingRepository;
    @Autowired private RoomAllocationRepository roomAllocationRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final List<Student> createdStudents = new ArrayList<>();
    private Long tenantId;

    @BeforeEach
    void seedTenant() {
        tenantId = jdbcTemplate.queryForObject(
                "INSERT INTO tenant (name, config, is_active, created_at, updated_at) "
                        + "VALUES (?, '{}', true, current_timestamp, current_timestamp) RETURNING id",
                Long.class, "Delete Test " + System.nanoTime());
        TenantContext.setTenantId(tenantId);
    }

    /**
     * The containers are shared with other test classes, and some of those delete every building.
     * Rooms left behind here would then lose their building and collide on
     * {@code uq_room_unassigned_room_number}.
     */
    @AfterEach
    void cleanUp() {
        TenantContext.clear();
        jdbcTemplate.update("DELETE FROM room_allocation WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM room WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM building WHERE tenant_id = ?", tenantId);
        createdStudents.forEach(student -> {
            jdbcTemplate.update("DELETE FROM student WHERE id = ?", student.getId());
            jdbcTemplate.update("DELETE FROM app_user WHERE id = ?", student.getAppUser().getId());
        });
        jdbcTemplate.update("DELETE FROM tenant WHERE id = ?", tenantId);
    }

    @ParameterizedTest
    @EnumSource(value = AllocationStatus.class, names = {"OFFERED", "ACCEPTED", "ACTIVE"})
    void deleteRoom_liveAllocation_refusedAndNothingLost(AllocationStatus live) {
        Room room = givenRoom(givenBuilding(), "101");
        RoomAllocation allocation = givenAllocation(room, live);

        assertThatThrownBy(() -> roomService.deleteRoom(room.getId()))
                .isInstanceOf(ResourceInUseException.class);

        assertThat(roomRepository.existsById(room.getId())).isTrue();
        assertThat(roomAllocationRepository.existsById(allocation.getId())).isTrue();
    }

    @Test
    void deleteRoom_onlyEndedAllocations_deleted() {
        Room room = givenRoom(givenBuilding(), "102");
        givenAllocation(room, AllocationStatus.COMPLETED);

        roomService.deleteRoom(room.getId());

        assertThat(roomRepository.existsById(room.getId())).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = AllocationStatus.class, names = {"OFFERED", "ACCEPTED", "ACTIVE"})
    void deleteBuilding_roomWithLiveAllocation_refusedAndNothingLost(AllocationStatus live) {
        Building building = givenBuilding();
        Room occupied = givenRoom(building, "201");
        Room empty = givenRoom(building, "202");
        RoomAllocation allocation = givenAllocation(occupied, live);

        assertThatThrownBy(() -> buildingService.deleteBuilding(building.getId()))
                .isInstanceOf(ResourceInUseException.class);

        assertThat(buildingRepository.existsById(building.getId())).isTrue();
        assertThat(roomBuildingId(occupied)).isEqualTo(building.getId());
        assertThat(roomBuildingId(empty)).isEqualTo(building.getId());
        assertThat(roomAllocationRepository.existsById(allocation.getId())).isTrue();
    }

    /**
     * A room with no building still counts as available, so the allocator would go on offering a
     * bed in a building that no longer exists. That is why these rooms go with the building.
     */
    @Test
    void deleteBuilding_roomsWithNoLiveAllocations_roomsDeletedWithTheBuilding() {
        Building building = givenBuilding();
        Room empty = givenRoom(building, "301");
        Room finished = givenRoom(building, "302");
        givenAllocation(finished, AllocationStatus.COMPLETED);

        buildingService.deleteBuilding(building.getId());

        assertThat(buildingRepository.existsById(building.getId())).isFalse();
        assertThat(roomRepository.findRoomsOpenForOffers())
                .extracting(Room::getId)
                .doesNotContain(empty.getId(), finished.getId());
        assertThat(roomRepository.existsById(empty.getId())).isFalse();
        assertThat(roomRepository.existsById(finished.getId())).isFalse();
    }

    /**
     * {@code uq_room_unassigned_room_number} allows one building-less room per number across the
     * whole platform, so leaving rooms behind also made the next delete fail with a false
     * "unique value" error, even for a different company.
     */
    @Test
    void deleteBuilding_twoBuildingsWithTheSameRoomNumber_bothDeleted() {
        Building first = givenBuilding();
        givenRoom(first, "401");
        Building second = givenBuilding();
        givenRoom(second, "401");

        buildingService.deleteBuilding(first.getId());
        buildingService.deleteBuilding(second.getId());

        assertThat(buildingRepository.existsById(first.getId())).isFalse();
        assertThat(buildingRepository.existsById(second.getId())).isFalse();
    }

    private Long roomBuildingId(Room room) {
        return jdbcTemplate.queryForObject(
                "SELECT building_id FROM room WHERE id = ?", Long.class, room.getId());
    }

    private Building givenBuilding() {
        Building building = new Building();
        building.setName("Delete Test " + System.nanoTime());
        building.setAddress("1 Test Street");
        building.setTenantId(tenantId);
        return buildingRepository.save(building);
    }

    private Room givenRoom(Building building, String roomNumber) {
        Room room = new Room();
        room.setRoomNumber(roomNumber);
        room.setBuilding(building);
        room.setTenantId(tenantId);
        room.setCapacity(2);
        room.setCurrentOccupancy(0);
        room.setStatus("AVAILABLE");
        return roomRepository.save(room);
    }

    private RoomAllocation givenAllocation(Room room, AllocationStatus status) {
        AppUser user = new AppUser();
        user.setEmail("delete-" + System.nanoTime() + "@test.local");
        user.setUserPassword("not-a-real-password");
        user.setFirstName("Delete");
        user.setLastName("Test");
        user.setUserRole(UserRole.STUDENT);
        user.setTenantId(0L);
        user = appUserRepository.save(user);

        Student student = new Student();
        student.setAppUser(user);
        student.setRegistrationDate(new Date());
        student = studentRepository.save(student);
        createdStudents.add(student);

        RoomAllocation allocation = new RoomAllocation();
        allocation.setStudent(student);
        allocation.setRoom(room);
        allocation.setTenantId(tenantId);
        allocation.setStatus(status);
        allocation.setAllocationDate(LocalDateTime.now());
        return roomAllocationRepository.saveAndFlush(allocation);
    }
}
