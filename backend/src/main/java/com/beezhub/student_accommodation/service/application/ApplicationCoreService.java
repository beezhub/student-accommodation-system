package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.aspect.LogActivity;
import com.beezhub.student_accommodation.exceptions.ApplicationNotFoundException;
import com.beezhub.student_accommodation.exceptions.ApplicationWindowClosedException;
import com.beezhub.student_accommodation.exceptions.DuplicateApplicationException;
import com.beezhub.student_accommodation.exceptions.StudentNotFoundException;
import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.mapper.ApplicationMapper;
import com.beezhub.student_accommodation.service.AcademicYearService;
import com.beezhub.student_accommodation.service.AuthService;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
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
                                                .orElseThrow(() -> new RuntimeException(
                                                        "Building not found with ID: " + buildingId)));
                        }
                }

                Building legacyBuilding = null;
                if (hasLegacyPref) {
                        legacyBuilding = buildingRepository.findById(request.getPreferredBuildingId())
                                        .orElseThrow(() -> new RuntimeException(
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

                // Check for existing active application per student per tenant (one per property company)
                List<ApplicationStatus> activeStatuses = List.of(
                                PENDING, UNDER_REVIEW, CHANGES_REQUESTED, APPROVED, AWAITING_ALLOCATION, ALLOCATED);
                if (applicationRepository.existsByStudent_IdAndTenantIdAndStatusIn(studentId, applicationTenantId, activeStatuses)) {
                        throw new DuplicateApplicationException(
                                        "You already have an active application with this property company");
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

                Application savedApplication = applicationRepository.save(application);

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

        private String generateApplicationCode() {
                int year = LocalDate.now().getYear();
                String prefix = "APP-" + year + "-";
                long nextSequence = applicationRepository.findMaxApplicationCodeByPrefix(prefix)
                                .map(maxCode -> {
                                        String sequencePart = maxCode.substring(prefix.length());
                                        return Long.parseLong(sequencePart) + 1;
                                })
                                .orElse(1L);
                return prefix + String.format("%03d", nextSequence);
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

        @Transactional
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

                // Only pending applications can be canceled
                if (!application.getStatus().equals(PENDING)) {
                        throw new IllegalStateException("Only pending applications can be canceled");
                }

                applicationRepository.delete(application);
                log.info("Application {} canceled successfully", applicationId);
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
