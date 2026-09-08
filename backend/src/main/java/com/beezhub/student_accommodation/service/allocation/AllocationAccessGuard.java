package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Ownership checks for allocation endpoints that students may call.
 *
 * <p>Students are platform-tenant users, so the Hibernate tenant filter does nothing for them
 * and {@code findById} would happily return another student's allocation. Every read or
 * mutation a student can reach by id must therefore confirm the allocation (or the student
 * record) belongs to the caller. Staff roles pass through: their scope is enforced by
 * {@code @PreAuthorize} on the controller and, for tenant admins, by the tenant filter.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationAccessGuard {

    private final AuthService authService;
    private final StudentRepository studentRepository;

    /**
     * Throws unless the current user is staff or is the student the allocation belongs to.
     */
    public void assertCanAccessAllocation(RoomAllocation allocation) {
        AppUser caller = authService.getCurrentUser();
        if (!requiresOwnership(caller)) {
            return;
        }
        Long ownerUserId = allocation.getStudent() == null || allocation.getStudent().getAppUser() == null
                ? null
                : allocation.getStudent().getAppUser().getId();
        if (ownerUserId == null || !ownerUserId.equals(caller.getId())) {
            log.warn("action=allocationAccessDenied allocationId={} callerUserId={}", allocation.getId(), caller.getId());
            throw new UnauthorizedAccessException("You are not authorized to access this room allocation");
        }
    }

    /**
     * Throws unless the current user is staff or is the student with the given id.
     */
    public void assertCanAccessStudent(Long studentId) {
        AppUser caller = authService.getCurrentUser();
        if (!requiresOwnership(caller)) {
            return;
        }
        boolean owns = studentRepository.findByAppUser_Id(caller.getId())
                .map(student -> student.getId() != null && student.getId().equals(studentId))
                .orElse(false);
        if (!owns) {
            log.warn("action=studentAllocationAccessDenied studentId={} callerUserId={}", studentId, caller.getId());
            throw new UnauthorizedAccessException("You are not authorized to access this student's allocation data");
        }
    }

    /** Students, and any user with no role, must prove ownership; staff roles do not. */
    private static boolean requiresOwnership(AppUser caller) {
        return caller.getUserRole() == null || caller.getUserRole() == UserRole.STUDENT;
    }
}
