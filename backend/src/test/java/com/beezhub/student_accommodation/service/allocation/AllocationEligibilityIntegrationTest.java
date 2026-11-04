package com.beezhub.student_accommodation.service.allocation;

import static org.assertj.core.api.Assertions.assertThat;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import com.beezhub.student_accommodation.model.dto.EligibleRoomResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAttributes;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.YearOfStudy;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.RoomAttributesRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.repository.YearOfStudyRepository;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The room picker behind manual allocation and Move Student, against a real database.
 *
 * <p>The student is loaded in one session and the room checks run in another, so reading a lazy
 * association of the student only fails here. The Mockito tests hand it plain objects.
 */
class AllocationEligibilityIntegrationTest extends AbstractIntegrationTest {

    @Autowired private AllocationEligibilityService allocationEligibilityService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private BuildingRepository buildingRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomAttributesRepository roomAttributesRepository;
    @Autowired private YearOfStudyRepository yearOfStudyRepository;

    private AppUser user;
    private Student student;
    private Building building;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);

        YearOfStudy yearOfStudy = new YearOfStudy();
        yearOfStudy.setYearOfStudy("2");
        yearOfStudy = yearOfStudyRepository.save(yearOfStudy);

        AppUser newUser = new AppUser();
        newUser.setEmail("eligibility-" + System.nanoTime() + "@test.local");
        newUser.setUserPassword("not-a-real-password");
        newUser.setFirstName("Ada");
        newUser.setLastName("Test");
        newUser.setUserRole(UserRole.STUDENT);
        newUser.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        user = appUserRepository.save(newUser);

        Student newStudent = new Student();
        newStudent.setAppUser(user);
        newStudent.setRegistrationDate(new Date());
        newStudent.setYearOfStudy(yearOfStudy);
        student = studentRepository.save(newStudent);

        Building newBuilding = new Building();
        newBuilding.setName("Eligibility Hall " + System.nanoTime());
        newBuilding.setAddress("1 Test Street");
        newBuilding.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        building = buildingRepository.save(newBuilding);
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM student WHERE id = ?", student.getId());
        jdbc.update("DELETE FROM app_user WHERE id = ?", user.getId());
        jdbc.update("DELETE FROM room WHERE building_id = ?", building.getId());
        jdbc.update("DELETE FROM building WHERE id = ?", building.getId());
        TenantContext.clear();
    }

    @Test
    void getEligibleRoomsForStudent_studentWithYearAndRoomWithYearLimits_returnsRoomInRange() {
        Room inRange = newRoomForYears(2, 3);
        Room tooSenior = newRoomForYears(3, 4);

        List<EligibleRoomResponse> rooms =
                allocationEligibilityService.getEligibleRoomsForStudent(student.getId(), building.getId());

        assertThat(rooms).extracting(EligibleRoomResponse::getRoomId)
                .contains(inRange.getId())
                .doesNotContain(tooSenior.getId());
        EligibleRoomResponse match = rooms.stream()
                .filter(room -> room.getRoomId().equals(inRange.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(match.getBuildingName()).isEqualTo(building.getName());
        assertThat(match.getMinYearOfStudy()).isEqualTo(2);
    }

    @Test
    void getEligibleRoomsForStudent_noBuildingFilterAndRoomWithYearLimits_returnsRoomInRange() {
        Room inRange = newRoomForYears(1, 2);

        List<EligibleRoomResponse> rooms =
                allocationEligibilityService.getEligibleRoomsForStudent(student.getId(), null);

        assertThat(rooms).extracting(EligibleRoomResponse::getRoomId).contains(inRange.getId());
    }

    private Room newRoomForYears(int minYear, int maxYear) {
        Room room = new Room();
        room.setRoomNumber("E" + System.nanoTime());
        room.setBuilding(building);
        room.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        room.setCapacity(2);
        room.setCurrentOccupancy(0);
        room.setStatus("AVAILABLE");
        room = roomRepository.save(room);

        RoomAttributes attributes = new RoomAttributes();
        attributes.setRoom(room);
        attributes.setMinYearOfStudy(minYear);
        attributes.setMaxYearOfStudy(maxYear);
        roomAttributesRepository.save(attributes);
        return room;
    }
}
