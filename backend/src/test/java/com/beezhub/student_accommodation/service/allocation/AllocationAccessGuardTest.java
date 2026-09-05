package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllocationAccessGuardTest {

    @Mock
    private AuthService authService;
    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private AllocationAccessGuard guard;

    private static AppUser user(long id, UserRole role) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUserRole(role);
        return user;
    }

    private static RoomAllocation allocationOwnedBy(AppUser owner, long studentId) {
        Student student = new Student();
        student.setId(studentId);
        student.setAppUser(owner);
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(99L);
        allocation.setStudent(student);
        return allocation;
    }

    // ---- assertCanAccessAllocation ----

    @Test
    void assertCanAccessAllocation_studentOwnsAllocation_passes() {
        AppUser owner = user(10L, UserRole.STUDENT);
        when(authService.getCurrentUser()).thenReturn(owner);

        assertThatCode(() -> guard.assertCanAccessAllocation(allocationOwnedBy(owner, 1L)))
                .doesNotThrowAnyException();
    }

    @Test
    void assertCanAccessAllocation_studentDoesNotOwnAllocation_throwsForbidden() {
        when(authService.getCurrentUser()).thenReturn(user(10L, UserRole.STUDENT));
        RoomAllocation someoneElses = allocationOwnedBy(user(11L, UserRole.STUDENT), 2L);

        assertThatThrownBy(() -> guard.assertCanAccessAllocation(someoneElses))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void assertCanAccessAllocation_allocationWithoutStudentUser_studentDenied() {
        when(authService.getCurrentUser()).thenReturn(user(10L, UserRole.STUDENT));
        RoomAllocation detached = new RoomAllocation();
        detached.setId(5L);

        assertThatThrownBy(() -> guard.assertCanAccessAllocation(detached))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void assertCanAccessAllocation_userWithoutRole_treatedAsStudent() {
        when(authService.getCurrentUser()).thenReturn(user(10L, null));

        assertThatThrownBy(() -> guard.assertCanAccessAllocation(allocationOwnedBy(user(11L, UserRole.STUDENT), 2L)))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"PLATFORM_ADMIN", "TENANT_ADMIN", "BUILDING_STAFF", "ADMIN"})
    void assertCanAccessAllocation_staffRole_passesForAnyAllocation(UserRole role) {
        when(authService.getCurrentUser()).thenReturn(user(1L, role));

        assertThatCode(() -> guard.assertCanAccessAllocation(allocationOwnedBy(user(11L, UserRole.STUDENT), 2L)))
                .doesNotThrowAnyException();
    }

    // ---- assertCanAccessStudent ----

    @Test
    void assertCanAccessStudent_studentIsThatStudent_passes() {
        AppUser caller = user(10L, UserRole.STUDENT);
        when(authService.getCurrentUser()).thenReturn(caller);
        Student own = new Student();
        own.setId(7L);
        when(studentRepository.findByAppUser_Id(10L)).thenReturn(Optional.of(own));

        assertThatCode(() -> guard.assertCanAccessStudent(7L)).doesNotThrowAnyException();
    }

    @Test
    void assertCanAccessStudent_studentAsksForAnotherStudent_throwsForbidden() {
        when(authService.getCurrentUser()).thenReturn(user(10L, UserRole.STUDENT));
        Student own = new Student();
        own.setId(7L);
        when(studentRepository.findByAppUser_Id(10L)).thenReturn(Optional.of(own));

        assertThatThrownBy(() -> guard.assertCanAccessStudent(8L))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void assertCanAccessStudent_studentWithoutStudentRecord_throwsForbidden() {
        when(authService.getCurrentUser()).thenReturn(user(10L, UserRole.STUDENT));
        when(studentRepository.findByAppUser_Id(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.assertCanAccessStudent(7L))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"PLATFORM_ADMIN", "TENANT_ADMIN", "BUILDING_STAFF"})
    void assertCanAccessStudent_staffRole_passesWithoutStudentLookup(UserRole role) {
        when(authService.getCurrentUser()).thenReturn(user(1L, role));

        assertThatCode(() -> guard.assertCanAccessStudent(7L)).doesNotThrowAnyException();
        verify(studentRepository, never()).findByAppUser_Id(any());
    }
}
