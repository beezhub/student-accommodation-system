package com.beezhub.student_accommodation.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.beezhub.student_accommodation.AbstractJpaTest;
import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Institution;
import com.beezhub.student_accommodation.model.entity.Intake;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.IntakeType;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.specification.ApplicationSpecification;
import com.beezhub.student_accommodation.specification.RoomAllocationSpecification;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** GH-496: the admin lists filter by institution and intake, and the dashboard lists overdue check-outs. */
class AdminFilterQueryTest extends AbstractJpaTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

    @Autowired private EntityManager em;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private RoomAllocationRepository roomAllocationRepository;

    private Long tenantId;
    private Institution university;
    private Institution college;
    private Intake semester1;
    private Intake semester2;
    private Room room;
    private int counter;

    @BeforeEach
    void setUp() {
        Tenant tenant = new Tenant();
        tenant.setName("Filter Co");
        em.persist(tenant);
        tenantId = tenant.getId();

        university = institution("University X");
        college = institution("College Y");

        AcademicYear year = AcademicYear.builder().tenantId(tenantId).name("2027")
                .startDate(LocalDate.of(2027, 1, 1)).endDate(LocalDate.of(2027, 12, 31)).build();
        em.persist(year);
        semester1 = intake(year, "Semester 1", LocalDate.of(2027, 2, 1), LocalDate.of(2027, 6, 30));
        semester2 = intake(year, "Semester 2", LocalDate.of(2027, 7, 15), LocalDate.of(2027, 11, 30));

        Building building = new Building();
        building.setName("Filter Hall");
        building.setAddress("1 Test Street");
        building.setTenantId(tenantId);
        em.persist(building);
        room = new Room();
        room.setBuilding(building);
        room.setRoomNumber("1");
        room.setCapacity(4);
        room.setCurrentOccupancy(0);
        room.setStatus("AVAILABLE");
        room.setTenantId(tenantId);
        em.persist(room);
        em.flush();
    }

    @Test
    void applications_filteredByInstitutionAndIntake() {
        Application uniS1 = application(university, semester1);
        Application uniS2 = application(university, semester2);
        Application collegeS1 = application(college, semester1);

        assertThat(applicationRepository.findAll(ApplicationSpecification.forInstitutionAndIntake(university.getId(), null)))
                .containsExactlyInAnyOrder(uniS1, uniS2);
        assertThat(applicationRepository.findAll(ApplicationSpecification.forInstitutionAndIntake(null, semester1.getId())))
                .containsExactlyInAnyOrder(uniS1, collegeS1);
        assertThat(applicationRepository.findAll(
                ApplicationSpecification.forInstitutionAndIntake(university.getId(), semester1.getId())))
                .containsExactly(uniS1);
        assertThat(applicationRepository.findAll(ApplicationSpecification.forInstitutionAndIntake(null, null)))
                .contains(uniS1, uniS2, collegeS1);
    }

    @Test
    void allocations_filteredByTheirApplicationsInstitutionAndIntake() {
        RoomAllocation uniS1 = allocation(application(university, semester1), AllocationStatus.ACCEPTED, null);
        allocation(application(college, semester2), AllocationStatus.ACCEPTED, null);

        assertThat(roomAllocationRepository.findAll(
                RoomAllocationSpecification.forInstitutionAndIntake(university.getId(), semester1.getId())))
                .extracting(RoomAllocation::getId).containsExactly(uniS1.getId());
        assertThat(roomAllocationRepository.findAll(
                RoomAllocationSpecification.forInstitutionAndIntake(college.getId(), semester1.getId()))).isEmpty();
    }

    @Test
    void overdueCheckOuts_onlyCheckedInResidentsPastTheirStayEnd() {
        RoomAllocation overdue = allocation(application(university, semester1), AllocationStatus.ACTIVE,
                LocalDate.of(2026, 11, 1));
        RoomAllocation endsToday = allocation(application(university, semester1), AllocationStatus.ACTIVE, TODAY);
        allocation(application(university, semester1), AllocationStatus.ACTIVE, LocalDate.of(2026, 11, 30));
        allocation(application(university, semester1), AllocationStatus.COMPLETED, LocalDate.of(2026, 11, 1));

        assertThat(roomAllocationRepository.findAll(RoomAllocationSpecification.overdueCheckOuts(TODAY)))
                .extracting(RoomAllocation::getId)
                .containsExactlyInAnyOrder(overdue.getId(), endsToday.getId());
    }

    private Institution institution(String name) {
        Institution institution = new Institution();
        institution.setName(name + " " + System.nanoTime());
        em.persist(institution);
        return institution;
    }

    private Intake intake(AcademicYear year, String name, LocalDate start, LocalDate end) {
        Intake intake = Intake.builder().tenantId(tenantId).academicYear(year).intakeType(IntakeType.SEMESTER)
                .name(name).stayStartDate(start).stayEndDate(end)
                .applicationOpenDate(start.minusDays(120)).applicationCloseDate(start.minusDays(14)).build();
        em.persist(intake);
        return intake;
    }

    private Application application(Institution institution, Intake intake) {
        counter++;
        AppUser user = new AppUser();
        user.setEmail("filter-" + counter + "@test.local");
        user.setUserPassword("not-a-real-password");
        user.setFirstName("Filter");
        user.setLastName("Test");
        user.setUserRole(UserRole.STUDENT);
        user.setTenantId(tenantId);
        em.persist(user);
        Student student = new Student();
        student.setAppUser(user);
        student.setRegistrationDate(new Date());
        em.persist(student);
        Application application = Application.builder()
                .applicationCode("APP-F-" + counter)
                .applicationDate(LocalDate.of(2026, 7, 1))
                .status(ApplicationStatus.ALLOCATED)
                .student(student)
                .tenantId(tenantId)
                .institution(institution)
                .intake(intake)
                .academicYear(intake.getAcademicYear())
                .build();
        em.persist(application);
        em.flush();
        return application;
    }

    private RoomAllocation allocation(Application application, AllocationStatus status, LocalDate stayEnd) {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setRoom(room);
        allocation.setStudent(application.getStudent());
        allocation.setApplication(application);
        allocation.setTenantId(tenantId);
        allocation.setStatus(status);
        allocation.setAllocationDate(LocalDateTime.now());
        if (stayEnd != null) {
            allocation.setStayStartDate(stayEnd.minusMonths(5));
            allocation.setStayEndDate(stayEnd);
        }
        em.persist(allocation);
        em.flush();
        return allocation;
    }
}
