package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.exceptions.EmailNotVerifiedException;
import com.beezhub.student_accommodation.exceptions.BuildingNotFoundException;
import com.beezhub.student_accommodation.exceptions.InvalidStatusException;
import com.beezhub.student_accommodation.aspect.LogActivity;
import com.beezhub.student_accommodation.exceptions.ApplicationNotFoundException;
import com.beezhub.student_accommodation.exceptions.ApplicationWindowClosedException;
import com.beezhub.student_accommodation.exceptions.DuplicateApplicationException;
import com.beezhub.student_accommodation.exceptions.StudentNotFoundException;
import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.mapper.ApplicationMapper;
import com.beezhub.student_accommodation.service.AcademicYearService;
import com.beezhub.student_accommodation.service.AuthService;
import com.beezhub.student_accommodation.service.allocation.ApplicationStatusValidator;

import com.beezhub.student_accommodation.model.dto.ApplicationRequest;
import com.beezhub.student_accommodation.model.dto.ApplicationResponse;

import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.ApplicationBuildingPreference;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Building;

import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.ApplicationBuildingPreferenceRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;
import com.beezhub.student_accommodation.model.entity.DocumentType;

import com.beezhub.student_accommodation.repository.StudentRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import java.util.EnumSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.beezhub.student_accommodation.model.enums.ApplicationStatus.*;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;

@RequiredArgsConstructor
@Slf4j
@Service
public class ApplicationCoreService {

        private final ApplicationRepository applicationRepository;
        private final StudentRepository studentRepository;
        private final BuildingRepository buildingRepository;
        private final ApplicationBuildingPreferenceRepository buildingPreferenceRepository;
        private final DocumentRepository documentRepository;
        private final DocumentTypeRepository documentTypeRepository;
        private final com.beezhub.student_accommodation.service.DocumentTypeService documentTypeService;

        private final ApplicationMapper applicationMapper;
        private final AuthService authService;
        private final AcademicYearService academicYearService;
        private final com.beezhub.student_accommodation.service.allocation.WaitingListService waitingListService;

        /** The student sees the same sentence whether the pre-check or the index refused. */
        private static final String DUPLICATE_APPLICATION_MESSAGE =
                        "You already have an active application with this property company";

        /** Defined in V2026_09_06_1300__unique_active_application_per_student_tenant.sql. */
        private static final String ACTIVE_APPLICATION_INDEX = "ux_application_student_tenant_active";

        @Transactional
        @LogActivity(action = "CREATE", entityType = "APPLICATION", description = "Submitted application for student {0}", entityIdParamIndex = 0)
        public ApplicationResponse submitApplication(@Valid ApplicationRequest request) {
                Long studentId = request.getStudentId();
                log.info("Submitting application for student with ID: {}", studentId);

                // Get student
                Student student = studentRepository.findStudentWithDetailsById(studentId)
                                .orElseThrow(() -> new StudentNotFoundException(
                                                "Student not found with ID: " + studentId));

                // Verify authorization - student can only submit for themselves
                AppUser currentUser = authService.getCurrentUser();

                if (!student.getAppUser().getId().equals(currentUser.getId()) &&
                                !currentUser.getUserRole().isAdmin()) {
                        throw new UnauthorizedAccessException("You can only submit applications for yourself");
                }

                // GH-153: verification is enforced here rather than at login.
                //
                // EmailNotVerifiedException existed, was mapped to 403, and was thrown nowhere:
                // signup issues a token immediately, so verification was cosmetic. Gating login
                // would have locked out every account that had never verified, with no way back
                // in except an email that has to be deliverable in production. Gating submission
                // stops the action that creates a real commitment — a room held, a company's
                // waiting list joined — while leaving everyone able to sign in and fix it.
                //
                // Checked against the applicant's own account, not the caller's: an admin
                // submitting on a student's behalf must not launder an unverified address.
                AppUser applicant = student.getAppUser();
                if (!Boolean.TRUE.equals(applicant.getEmailVerified())) {
                        throw new EmailNotVerifiedException(
                                        "Confirm your email address before submitting an application. "
                                                        + "Check your inbox for the verification link, or request a new one.");
                }

                // Validate that all required student profile fields are complete
                List<String> missingFields = new java.util.ArrayList<>();
                if (student.getStudentNumber() == null || student.getStudentNumber().isBlank()) {
                        missingFields.add("student number");
                }
                if (student.getPhoneNumber() == null || student.getPhoneNumber().isBlank()) {
                        missingFields.add("phone number");
                }
                if (student.getDateOfBirth() == null) {
                        missingFields.add("date of birth");
                }
                if (student.getGender() == null || student.getGender().isBlank()) {
                        missingFields.add("gender");
                }
                // GH-239. Enforced here rather than as a NOT NULL column: existing students have no
                // value and none can be invented for them. This makes it required going forward,
                // for exactly the students it matters for, and the error already reads well.
                // Name and phone only — a relationship or an email with no one to call is not a
                // contact, but a contact without them still is.
                if (student.getEmergencyContactName() == null
                                || student.getEmergencyContactName().isBlank()) {
                        missingFields.add("emergency contact name");
                }
                if (student.getEmergencyContactPhone() == null
                                || student.getEmergencyContactPhone().isBlank()) {
                        missingFields.add("emergency contact phone");
                }
                if (!missingFields.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "Student profile is incomplete. Missing: " + String.join(", ", missingFields)
                                                        + ". Please update your profile before submitting an application.");
                }

                // Validate at least one building preference is provided (required to identify the tenant)
                List<Long> preferredBuildingIds = request.getPreferredBuildingIds();
                boolean hasMultiPrefs = preferredBuildingIds != null && !preferredBuildingIds.isEmpty();
                boolean hasLegacyPref = request.getPreferredBuildingId() != null;
                if (!hasMultiPrefs && !hasLegacyPref) {
                        throw new IllegalArgumentException(
                                        "At least one building preference is required to identify the property company");
                }

                // Validate max 3 preferences
                if (hasMultiPrefs && preferredBuildingIds.size() > 3) {
                        throw new IllegalArgumentException("You can select at most 3 preferred buildings");
                }

                // Resolve all buildings upfront to derive and validate the tenant
                List<Building> resolvedPreferenceBuildings = new ArrayList<>();
                if (hasMultiPrefs) {
                        for (Long buildingId : preferredBuildingIds) {
                                resolvedPreferenceBuildings.add(
                                        buildingRepository.findById(buildingId)
                                                .orElseThrow(() -> new BuildingNotFoundException(
                                                        "Building not found with ID: " + buildingId)));
                        }
                }

                Building legacyBuilding = null;
                if (hasLegacyPref) {
                        legacyBuilding = buildingRepository.findById(request.getPreferredBuildingId())
                                        .orElseThrow(() -> new BuildingNotFoundException(
                                                        "Building not found with ID: " + request.getPreferredBuildingId()));
                        // Only add to validation list if not already covered by multi-prefs
                        if (!hasMultiPrefs) {
                                resolvedPreferenceBuildings.add(legacyBuilding);
                        }
                }

                // All buildings must belong to the same property company (tenant)
                Set<Long> distinctTenantIds = resolvedPreferenceBuildings.stream()
                                .map(Building::getTenantId)
                                .collect(Collectors.toSet());
                if (distinctTenantIds.size() > 1) {
                        throw new IllegalArgumentException(
                                        "All preferred buildings must belong to the same property company");
                }
                Long applicationTenantId = resolvedPreferenceBuildings.get(0).getTenantId();

                // Validate application window is open
                com.beezhub.student_accommodation.model.entity.AcademicYear currentYear = academicYearService
                                .getCurrentAcademicYearEntity();
                if (!academicYearService.isApplicationWindowOpen(currentYear)) {
                        throw new ApplicationWindowClosedException(
                                        "Applications are not currently open. Window: " +
                                                        currentYear.getApplicationOpenDate() + " to " +
                                                        currentYear.getApplicationCloseDate());
                }

                // Check that all required document types are present in the student's pack.
                // Uses name-based matching: student docs may reference platform type IDs while
                // the tenant may have shadow types with different IDs but the same name.
                Set<String> uploadedTypeNames = documentRepository.findDocumentTypeNamesByStudentId(studentId)
                                .stream().map(String::toLowerCase).collect(Collectors.toSet());
                Set<String> requiredTypeNames = documentTypeService.getEffectiveRequiredTypeNames(applicationTenantId);
                List<String> missingTypeNames = requiredTypeNames.stream()
                                .filter(name -> !uploadedTypeNames.contains(name))
                                .collect(Collectors.toList());
                if (!missingTypeNames.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "Missing required documents: " + String.join(", ", missingTypeNames));
                }

                // Check for existing active application per student per tenant (one per property
                // company). This is the fast path and the source of the message; it is not what
                // makes the invariant hold. Two concurrent submits both pass this check, which is
                // why ux_application_student_tenant_active exists and why the insert below is
                // flushed inside this method (GH-201).
                //
                // Keep this list and the index predicate in
                // V2026_09_06_1300__unique_active_application_per_student_tenant.sql identical.
                List<ApplicationStatus> activeStatuses = List.of(
                                PENDING, UNDER_REVIEW, CHANGES_REQUESTED, APPROVED, AWAITING_ALLOCATION, ALLOCATED);
                if (applicationRepository.existsByStudent_IdAndTenantIdAndStatusIn(studentId, applicationTenantId, activeStatuses)) {
                        throw new DuplicateApplicationException(DUPLICATE_APPLICATION_MESSAGE);
                }

                // Determine legacy preferred building for the application entity
                Building preferredBuilding = hasLegacyPref ? legacyBuilding
                                : resolvedPreferenceBuildings.get(0);
                log.info("Derived tenant_id={} from building '{}' for application",
                                applicationTenantId, preferredBuilding.getName());

                // Create and save application — tenantId explicitly set, bypassing TenantListener
                Application application = Application.builder()
                                .applicationCode(generateApplicationCode())
                                .applicationDate(LocalDate.now())
                                .status(PENDING)
                                .student(student)
                                .tenantId(applicationTenantId)
                                .requiresAccessibility(
                                                request.getRequiresAccessibility() != null
                                                                ? request.getRequiresAccessibility()
                                                                : false)
                                .specialRequirements(request.getSpecialRequirements())
                                .institutionName(request.getInstitutionName())
                                .courseName(request.getCourseName())
                                .preferredBuilding(preferredBuilding)
                                .academicYear(currentYear)
                                .build();

                // saveAndFlush, not save: the INSERT would otherwise be deferred to commit, which is
                // outside this method, and the constraint violation would surface as an unhandled
                // 500 rather than the duplicate message the student gets on the fast path.
                Application savedApplication;
                try {
                        savedApplication = applicationRepository.saveAndFlush(application);
                } catch (DataIntegrityViolationException e) {
                        if (!isDuplicateActiveApplication(e)) {
                                throw e;
                        }
                        log.info("action=duplicateApplicationRejected reason=uniqueIndex student={} tenant={}",
                                        studentId, applicationTenantId);
                        throw new DuplicateApplicationException(DUPLICATE_APPLICATION_MESSAGE);
                }

                // Persist ordered building preferences (buildings already resolved)
                if (hasMultiPrefs) {
                        List<ApplicationBuildingPreference> preferences = new ArrayList<>();
                        for (int i = 0; i < resolvedPreferenceBuildings.size(); i++) {
                                preferences.add(ApplicationBuildingPreference.builder()
                                                .application(savedApplication)
                                                .building(resolvedPreferenceBuildings.get(i))
                                                .preferenceOrder(i + 1)
                                                .build());
                        }
                        buildingPreferenceRepository.saveAll(preferences);
                        log.info("Saved {} building preference(s) for application {}",
                                        preferences.size(), savedApplication.getApplicationCode());
                }

                log.info("Application submitted successfully with code: {}", savedApplication.getApplicationCode());

                return applicationMapper.toResponse(savedApplication);
        }

        /**
         * Whether this violation is the one-active-application-per-company index rather than any
         * other constraint on the table.
         *
         * <p>Matched on the index name, which appears in the PostgreSQL detail message, so an
         * unrelated violation is not reported to the student as a duplicate application. Renaming
         * the index in the migration without renaming it here turns a 409 back into a 500, which is
         * why both sides say so.
         */
        private static boolean isDuplicateActiveApplication(DataIntegrityViolationException e) {
                for (Throwable cause = e; cause != null; cause = cause.getCause()) {
                        String message = cause.getMessage();
                        if (message != null && message.contains(ACTIVE_APPLICATION_INDEX)) {
                                return true;
                        }
                }
                return false;
        }

        /**
         * Uniqueness comes from the database sequence alone, so nothing here reads, parses or
         * compares an existing code. That is what removes the concurrent-submit race, the
         * lexicographic MAX wraparound past 999 and the tenant-filtered read at once (GH-143).
         *
         * <p>The year is decorative and the sequence does not reset with it, so the ordinal
         * continues across years. Padding stays at three digits to match every code already
         * issued; past 999 it simply renders wider, which is harmless now that no code is parsed.
         * Rolled-back submits leave gaps, because nextval is not transactional.
         */
        private String generateApplicationCode() {
                // A native query makes Hibernate flush the session first. Nothing is dirty at this
                // point, since this runs before save(); do not move it after entity mutations.
                long sequence = applicationRepository.nextApplicationCodeSequence();
                return String.format("APP-%d-%03d", LocalDate.now().getYear(), sequence);
        }

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

                return applicationRepository.findByStudent_Id(student.getId())
                                .stream()
                                .map(applicationMapper::toResponse)
                                .collect(Collectors.toList());
        }

}
