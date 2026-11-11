package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.exceptions.EmailNotVerifiedException;
import com.beezhub.student_accommodation.exceptions.InvalidStatusException;
import com.beezhub.student_accommodation.exceptions.*;
import com.beezhub.student_accommodation.mapper.ApplicationMapper;
import com.beezhub.student_accommodation.model.dto.*;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.service.AcademicYearService;
import com.beezhub.student_accommodation.service.AuthService;
import com.beezhub.student_accommodation.service.DocumentTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationCoreServiceTest {

        @Mock
        private ApplicationRepository applicationRepository;
        @Mock
        private StudentRepository studentRepository;
        @Mock
        private ApplicationMapper applicationMapper;
        @Mock
        private AuthService authService;
        @Mock
        private com.beezhub.student_accommodation.service.allocation.WaitingListService waitingListService;

        @InjectMocks
        private ApplicationCoreService applicationCoreService;

        private Student student;
        private AppUser studentUser;
        private Application application;
        private ApplicationRequest applicationRequest;
        private Building defaultBuilding;

        @BeforeEach
        void setUp() {
                studentUser = new AppUser();
                studentUser.setId(1L);
                studentUser.setEmail("student@test.com");
                studentUser.setUserRole(UserRole.STUDENT);
                // GH-153: submitApplication now refuses an unverified applicant. The gate's own
                // cases below clear this deliberately.
                studentUser.setEmailVerified(true);

                student = new Student();
                student.setId(1L);
                student.setStudentNumber("STU001");
                student.setPhoneNumber("0123456789");
                student.setDateOfBirth(LocalDate.of(2000, 5, 14));
                student.setGender("Female");
                // GH-239: a complete profile now includes an emergency contact, so the happy-path
                // fixture carries one. The gate's own cases below clear it deliberately.
                student.setEmergencyContactName("Thandi Mokoena");
                student.setEmergencyContactPhone("+27 82 123 4567");
                student.setAppUser(studentUser);

                application = Application.builder()
                                .id(1L)
                                .applicationCode("APP-2024-001")
                                .status(ApplicationStatus.PENDING)
                                .student(student)
                                .build();

                defaultBuilding = new Building();
                defaultBuilding.setId(10L);
                defaultBuilding.setName("Block A");
                defaultBuilding.setTenantId(42L);

                applicationRequest = new ApplicationRequest();
                applicationRequest.setStudentId(1L);
                applicationRequest.setRequiresAccessibility(false);
                applicationRequest.setCourseName("Computer Science");
                applicationRequest.setPreferredBuildingIds(List.of(10L));
        }

        @Test
        void getApplicationById_shouldReturnApplication() {
                when(applicationRepository.findWithDetailsById(1L)).thenReturn(Optional.of(application));
                when(authService.getCurrentUser()).thenReturn(studentUser);
                when(applicationMapper.toResponse(application)).thenReturn(new ApplicationResponse());

                ApplicationResponse response = applicationCoreService.getApplicationById(1L);

                assertThat(response).isNotNull();
        }

        @Test
        /**
         * GH-156: this deleted the row. That destroyed a record a student and an administrator may
         * both need to refer to, and left the activity log pointing at an id that no longer
         * resolves.
         */
        void cancelApplication_marksItCancelledRatherThanDeletingIt() {
                when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
                when(authService.getCurrentUser()).thenReturn(studentUser);

                applicationCoreService.cancelApplication(1L);

                assertThat(application.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
                verify(applicationRepository).save(application);
                verify(applicationRepository, never()).delete(any(Application.class));
        }

        @Test
        void cancelApplication_cancelled_closesTheWaitingListEntry() {
                when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
                when(authService.getCurrentUser()).thenReturn(studentUser);

                applicationCoreService.cancelApplication(1L);

                verify(waitingListService).closeEntriesForApplication(1L);
        }

        /**
         * The old rule was PENDING only, which contradicted the state machine: a student asked to
         * amend their application could not withdraw it instead.
         */
        @Test
        void cancelApplication_changesRequested_isAllowed() {
                application.setStatus(ApplicationStatus.CHANGES_REQUESTED);
                when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
                when(authService.getCurrentUser()).thenReturn(studentUser);

                applicationCoreService.cancelApplication(1L);

                assertThat(application.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
        }

        /**
         * Narrower than the state machine allows, deliberately: by APPROVED an offer may exist, and
         * cancelling the application without releasing it would leave the room counted as occupied
         * with nothing pointing at it. Declining the offer is the route for that.
         */
        @Test
        void cancelApplication_afterApproval_isRefused() {
                application.setStatus(ApplicationStatus.APPROVED);
                when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
                when(authService.getCurrentUser()).thenReturn(studentUser);

                assertThatThrownBy(() -> applicationCoreService.cancelApplication(1L))
                                .isInstanceOf(InvalidStatusException.class);
                verify(applicationRepository, never()).save(any(Application.class));
        }

        @Test
        void cancelApplication_alreadyCancelled_isRefused() {
                application.setStatus(ApplicationStatus.CANCELLED);
                when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
                when(authService.getCurrentUser()).thenReturn(studentUser);

                assertThatThrownBy(() -> applicationCoreService.cancelApplication(1L))
                                .isInstanceOf(InvalidStatusException.class);
        }

        // ── Resubmit after changes requested ──────────────────────────────────────

        @Test
        void resubmitApplication_owner_movesToPendingAndKeepsTheReason() {
                application.setStatus(ApplicationStatus.CHANGES_REQUESTED);
                application.setRemarks("Proof of registration is for the wrong year");
                when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
                when(authService.getCurrentUser()).thenReturn(studentUser);
                when(applicationRepository.save(application)).thenReturn(application);

                applicationCoreService.resubmitApplication(1L);

                assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING);
                assertThat(application.getRemarks()).isEqualTo("Proof of registration is for the wrong year");
                verify(applicationRepository).save(application);
                verify(applicationMapper).toResponse(application);
        }

        @Test
        void resubmitApplication_anotherStudent_isRefused() {
                application.setStatus(ApplicationStatus.CHANGES_REQUESTED);
                AppUser otherUser = new AppUser();
                otherUser.setId(2L);
                otherUser.setUserRole(UserRole.STUDENT);
                when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
                when(authService.getCurrentUser()).thenReturn(otherUser);

                assertThatThrownBy(() -> applicationCoreService.resubmitApplication(1L))
                                .isInstanceOf(UnauthorizedAccessException.class);

                assertThat(application.getStatus()).isEqualTo(ApplicationStatus.CHANGES_REQUESTED);
                verify(applicationRepository, never()).save(any(Application.class));
        }

        /**
         * UNDER_REVIEW -> PENDING and PENDING -> PENDING are legal in the state machine, so the
         * transition check alone would let a student pull an application out of an admin's hands.
         */
        @ParameterizedTest
        @EnumSource(value = ApplicationStatus.class, names = "CHANGES_REQUESTED", mode = EnumSource.Mode.EXCLUDE)
        void resubmitApplication_notChangesRequested_isRefused(ApplicationStatus status) {
                application.setStatus(status);
                when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
                when(authService.getCurrentUser()).thenReturn(studentUser);

                assertThatThrownBy(() -> applicationCoreService.resubmitApplication(1L))
                                .isInstanceOf(InvalidStatusException.class);

                assertThat(application.getStatus()).isEqualTo(status);
                verify(applicationRepository, never()).save(any(Application.class));
        }

        /** GH-495: the cards show the intake and year, read with the application in one query. */
        @Test
        void getMyApplications_readsWithDetails() {
                when(authService.getCurrentUser()).thenReturn(studentUser);
                when(studentRepository.findByAppUser_Id(1L)).thenReturn(Optional.of(student));
                when(applicationRepository.findWithDetailsByStudent_Id(1L)).thenReturn(List.of(application));
                when(applicationMapper.toResponse(application)).thenReturn(new ApplicationResponse());

                assertThat(applicationCoreService.getMyApplications()).hasSize(1);
        }

        @Test
        void resubmitApplication_unknownId_isNotFound() {
                when(applicationRepository.findById(1L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> applicationCoreService.resubmitApplication(1L))
                                .isInstanceOf(ApplicationNotFoundException.class);
        }

}
