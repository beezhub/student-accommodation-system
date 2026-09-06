package com.beezhub.student_accommodation.service.allocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
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
import java.time.LocalDateTime;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * GH-196: accept and the hourly offer-expiry sweep could interleave.
 *
 * <p>GH-139 enforced the offer deadline on the accept path, shrinking the window from up to an hour
 * to milliseconds, but did not close it. {@code RoomAllocation} declared no {@code @Version} and the
 * accept path reads through {@code findByIdWithDetails}, a plain JOIN FETCH with no lock mode, so
 * this ordering was reachable:
 *
 * <ol>
 *   <li>accept reads the row as OFFERED and passes the deadline guard
 *   <li>the sweep commits EXPIRED and releases the room's occupancy
 *   <li>accept commits ACCEPTED over it
 * </ol>
 *
 * <p>leaving an ACCEPTED allocation on a room whose occupancy had already been given back — an
 * undercount that lets the room be over-allocated.
 *
 * <p>Runs against real PostgreSQL rather than the H2 the unit tests use, because what is under test
 * is the database round trip: the version predicate in the UPDATE and the row count it returns.
 * Interleaving is staged deterministically with separate committed transactions rather than raced
 * with threads, so the test cannot pass or fail by timing.
 */
class AllocationOptimisticLockIntegrationTest extends AbstractIntegrationTest {

    @Autowired private RoomAllocationRepository roomAllocationRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private BuildingRepository buildingRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    @Test
    void acceptCommittingAfterTheExpirySweepLosesInsteadOfOverwritingIt() {
        Long allocationId = givenAnOfferedAllocation();

        // 1. The accept path reads the row while it is still OFFERED.
        RoomAllocation readByAccept = inTransaction(
                () -> roomAllocationRepository.findById(allocationId).orElseThrow());
        assertThat(readByAccept.getStatus()).isEqualTo(AllocationStatus.OFFERED);

        // 2. The sweep commits EXPIRED first, in its own transaction.
        inTransaction(() -> {
            RoomAllocation bySweep = roomAllocationRepository.findById(allocationId).orElseThrow();
            bySweep.setStatus(AllocationStatus.EXPIRED);
            return roomAllocationRepository.saveAndFlush(bySweep);
        });

        // 3. Accept tries to commit ACCEPTED over it, from the instance it read in step 1. Without
        //    a version column this succeeded silently, which is the whole defect.
        readByAccept.setStatus(AllocationStatus.ACCEPTED);
        assertThatThrownBy(() -> inTransaction(() -> roomAllocationRepository.saveAndFlush(readByAccept)))
                .isInstanceOf(OptimisticLockingFailureException.class);

        RoomAllocation committed = inTransaction(
                () -> roomAllocationRepository.findById(allocationId).orElseThrow());
        assertThat(committed.getStatus())
                .as("the sweep's decision must stand, not be overwritten by the stale accept")
                .isEqualTo(AllocationStatus.EXPIRED);
    }

    @Test
    void anUncontendedSaveStillWorksAndAdvancesTheVersion() {
        Long allocationId = givenAnOfferedAllocation();

        Long versionBefore = inTransaction(
                () -> roomAllocationRepository.findById(allocationId).orElseThrow().getVersion());

        inTransaction(() -> {
            RoomAllocation allocation = roomAllocationRepository.findById(allocationId).orElseThrow();
            allocation.setStatus(AllocationStatus.ACCEPTED);
            return roomAllocationRepository.saveAndFlush(allocation);
        });

        RoomAllocation after = inTransaction(
                () -> roomAllocationRepository.findById(allocationId).orElseThrow());
        assertThat(after.getStatus()).isEqualTo(AllocationStatus.ACCEPTED);
        assertThat(after.getVersion()).isGreaterThan(versionBefore);
    }

    private <T> T inTransaction(java.util.function.Supplier<T> work) {
        return transactionTemplate.execute(status -> work.get());
    }

    private Long givenAnOfferedAllocation() {
        return inTransaction(() -> {
            String unique = String.valueOf(System.nanoTime());

            AppUser user = new AppUser();
            user.setEmail("lock-" + unique + "@test.local");
            user.setUserPassword("not-a-real-password");
            user.setFirstName("Lock");
            user.setLastName("Test");
            user.setUserRole(UserRole.STUDENT);
            user.setTenantId(0L);
            user = appUserRepository.save(user);

            Student student = new Student();
            student.setAppUser(user);
            student.setRegistrationDate(new Date());
            student = studentRepository.save(student);

            Building building = new Building();
            building.setName("Lock Test " + unique);
            building.setAddress("1 Test Street");
            building.setTenantId(0L);
            building = buildingRepository.save(building);

            Room room = new Room();
            room.setRoomNumber("L" + unique);
            room.setBuilding(building);
            room.setTenantId(0L);
            room.setCapacity(2);
            room.setCurrentOccupancy(1);
            room.setStatus("OCCUPIED");
            room = roomRepository.save(room);

            RoomAllocation allocation = new RoomAllocation();
            allocation.setStudent(student);
            allocation.setRoom(room);
            allocation.setTenantId(0L);
            allocation.setStatus(AllocationStatus.OFFERED);
            allocation.setAllocationDate(LocalDateTime.now());
            allocation.setOfferExpiryDate(LocalDateTime.now().plusDays(1));
            return roomAllocationRepository.saveAndFlush(allocation).getId();
        });
    }
}
