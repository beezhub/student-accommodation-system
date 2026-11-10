package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.aspect.LogActivity;
import com.beezhub.student_accommodation.exceptions.ApplicationNotFoundException;
import com.beezhub.student_accommodation.exceptions.InvalidStatusException;
import com.beezhub.student_accommodation.exceptions.StudentNotFoundException;
import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.mapper.ApplicationMapper;
import com.beezhub.student_accommodation.model.dto.ApplicationResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.service.AuthService;
import com.beezhub.student_accommodation.service.allocation.ApplicationStatusValidator;
import com.beezhub.student_accommodation.service.allocation.WaitingListService;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reading, cancelling and resubmitting a student's applications. Submitting is
 * {@link ApplicationSubmissionService} (GH-495).
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class ApplicationCoreService {

        private final ApplicationRepository applicationRepository;
        private final StudentRepository studentRepository;
        private final ApplicationMapper applicationMapper;
        private final AuthService authService;
        private final WaitingListService waitingListService;

        @Transactional(readOnly = true)
        public ApplicationResponse getApplicationById(Long applicationId) {
                log.info("Retrieving application with ID: {}", applicationId);

                Application application = applicationRepository.findWithDetailsById(applicationId)
                                .orElseThrow(() -> new ApplicationNotFoundException(
                                                "Application not found with ID: " + applicationId));

                // Verify authorization
                AppUser currentUser = authService.getCurrentUser();

                if (currentUser.getUserRole() == UserRole.TENANT_ADMIN) {
                        // TENANT_ADMIN can only see applications belonging to their own tenant.
                        // findById bypasses the Hibernate filter, so we check explicitly.
                        Long callerTenantId = TenantContext.getTenantId();
                        if (callerTenantId == null || !callerTenantId.equals(application.getTenantId())) {
                                throw new ApplicationNotFoundException(
                                                "Application not found with ID: " + applicationId);
                        }
                } else if (!currentUser.getUserRole().isAdmin()
                                && !application.getStudent().getAppUser().getId().equals(currentUser.getId())) {
                        throw new UnauthorizedAccessException("You are not authorized to view this application");
                }

                return applicationMapper.toResponse(application);
        }

        @Transactional(readOnly = true)
        public List<ApplicationResponse> getApplicationsByStudentId(Long studentId) {
                log.info("Retrieving applications for student with ID: {}", studentId);

                // Verify student exists
                Student student = studentRepository.findStudentWithUserById(studentId)
                                .orElseThrow(() -> new StudentNotFoundException(
                                                "Student not found with ID: " + studentId));

                // Verify authorization
                AppUser currentUser = authService.getCurrentUser();

                if (!student.getAppUser().getId().equals(currentUser.getId()) &&
                                !currentUser.getUserRole().isAdmin()) {
                        throw new UnauthorizedAccessException("You are not authorized to view these applications");
                }

                return applicationRepository.findWithDetailsByStudent_Id(studentId)
                                .stream()
                                .map(applicationMapper::toResponse)
                                .collect(Collectors.toList());
        }

        /**
         * Statuses a student may withdraw from themselves.
         *
         * <p>The old rule was PENDING only, which contradicted the state machine: a student asked
         * to amend their application could not withdraw it instead (GH-156).
         *
         * <p>Narrower than the state machine allows, deliberately. That also permits cancelling an
         * APPROVED, AWAITING_ALLOCATION or ALLOCATED application, but by then an offer or a room
         * may exist, and cancelling the application without releasing it would leave the room
         * counted as occupied with nothing pointing at it. Declining the offer is the route for
         * that, and it frees the room and refills the waiting list.
         */
        private static final Set<ApplicationStatus> STUDENT_CANCELLABLE_STATUSES = EnumSet.of(
                        ApplicationStatus.PENDING,
                        ApplicationStatus.UNDER_REVIEW,
                        ApplicationStatus.CHANGES_REQUESTED);

        @Transactional
        // The action stays DELETE for continuity with the log rows already written under it.
        @LogActivity(action = "DELETE", entityType = "APPLICATION", description = "Canceled application {0}", entityIdParamIndex = 0)

        public void cancelApplication(Long applicationId) {
                log.info("Canceling application with ID: {}", applicationId);

                Application application = applicationRepository.findById(applicationId)
                                .orElseThrow(() -> new ApplicationNotFoundException(
                                                "Application not found with ID: " + applicationId));

                // Verify authorization
                AppUser currentUser = authService.getCurrentUser();

                if (!application.getStudent().getAppUser().getId().equals(currentUser.getId())) {
                        throw new UnauthorizedAccessException("You can only cancel your own applications");
                }

                if (!STUDENT_CANCELLABLE_STATUSES.contains(application.getStatus())
                                || !application.getStatus().canTransitionTo(ApplicationStatus.CANCELLED)) {
                        throw new InvalidStatusException(
                                        "An application that is " + application.getStatus().getStatus().toLowerCase()
                                                        + " cannot be cancelled");
                }

                // Cancelled, not deleted. Deleting destroyed the record a student and an
                // administrator may both need to refer to, and left the activity log pointing at an
                // id that no longer resolves. CANCELLED is already a terminal status the rest of
                // the system understands, and it is excluded from the active-application check, so
                // the student is just as free to re-apply.
                application.setStatus(ApplicationStatus.CANCELLED);
                application.setRemarks("Cancelled by the student.");
                applicationRepository.save(application);
                // An admin can queue an application in any status, so even one that is not yet
                // approved may have a row here.
                waitingListService.closeEntriesForApplication(applicationId);
                log.info("action=applicationCancelled applicationId={}", applicationId);
        }

        /**
         * Sends an application back to the review queue after the student has made the changes an
         * admin asked for (GH-454).
         *
         * <p>Students are tenant 0, so the tenant filter does nothing for them, and this ownership
         * check is what stops one student acting on another's application.
         *
         * <p>The explicit status check comes first because the state machine alone is too wide:
         * UNDER_REVIEW -> PENDING and PENDING -> PENDING are both legal, and neither is a student's
         * to make.
         *
         * <p>The remarks are kept so the reviewer can see what was asked for. The next decision on
         * the application overwrites them. No event is published: the listener has nothing to send
         * for PENDING, and it would leave a notification row behind.
         */
        @Transactional
        @LogActivity(action = "UPDATE", entityType = "APPLICATION", description = "Resubmitted application {0}", entityIdParamIndex = 0)
        public ApplicationResponse resubmitApplication(Long applicationId) {
                Application application = applicationRepository.findById(applicationId)
                                .orElseThrow(() -> new ApplicationNotFoundException(
                                                "Application not found with ID: " + applicationId));

                AppUser currentUser = authService.getCurrentUser();
                if (!application.getStudent().getAppUser().getId().equals(currentUser.getId())) {
                        throw new UnauthorizedAccessException("You can only resubmit your own applications");
                }

                if (application.getStatus() != ApplicationStatus.CHANGES_REQUESTED) {
                        throw new InvalidStatusException("Only an application with changes requested can be resubmitted");
                }
                ApplicationStatusValidator.validate(application, ApplicationStatus.PENDING);

                application.setStatus(ApplicationStatus.PENDING);
                Application saved = applicationRepository.save(application);
                log.info("action=applicationResubmitted applicationId={}", applicationId);

                return applicationMapper.toResponse(saved);
        }

        @Transactional(readOnly = true)
        public List<ApplicationResponse> getMyApplications() {
                log.info("Retrieving applications for current user");

                AppUser currentUser = authService.getCurrentUser();

                Student student = studentRepository.findByAppUser_Id(currentUser.getId())
                                .orElseThrow(() -> new StudentNotFoundException("Student profile not found for user"));

                return applicationRepository.findWithDetailsByStudent_Id(student.getId())
                                .stream()
                                .map(applicationMapper::toResponse)
                                .collect(Collectors.toList());
        }

}
