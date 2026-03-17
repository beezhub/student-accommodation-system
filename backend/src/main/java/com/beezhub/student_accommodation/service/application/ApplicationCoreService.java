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

import com.beezhub.student_accommodation.repository.StudentRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.beezhub.student_accommodation.model.enums.ApplicationStatus.PENDING;

@RequiredArgsConstructor
@Slf4j
@Service
public class ApplicationCoreService {

        private final ApplicationRepository applicationRepository;
        private final StudentRepository studentRepository;
        private final BuildingRepository buildingRepository;
        private final ApplicationBuildingPreferenceRepository buildingPreferenceRepository;

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

                // Check for existing pending application
                if (applicationRepository.existsByStudent_IdAndStatus(studentId, PENDING)) {
                        throw new DuplicateApplicationException("Student already has a pending application");
                }

                // Validate application window is open
                com.beezhub.student_accommodation.model.entity.AcademicYear currentYear = academicYearService
                                .getCurrentAcademicYearEntity();
                if (!academicYearService.isApplicationWindowOpen(currentYear)) {
                        throw new ApplicationWindowClosedException(
                                        "Applications are not currently open. Window: " +
                                                        currentYear.getApplicationOpenDate() + " to " +
                                                        currentYear.getApplicationCloseDate());
                }

                // Validate and get preferred building if provided (legacy single field)
                Building preferredBuilding = null;
                if (request.getPreferredBuildingId() != null) {
                        preferredBuilding = buildingRepository.findById(request.getPreferredBuildingId())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Building not found with ID: "
                                                                        + request.getPreferredBuildingId()));
                        log.info("Setting preferred building {} for application", preferredBuilding.getName());
                }

                // Validate multi-preference building IDs
                List<Long> preferredBuildingIds = request.getPreferredBuildingIds();
                if (preferredBuildingIds != null && preferredBuildingIds.size() > 3) {
                        throw new RuntimeException("You can select at most 3 preferred buildings");
                }

                // Create and save application
                Application application = Application.builder()
                                .applicationCode(generateApplicationCode())
                                .applicationDate(LocalDate.now())
                                .status(PENDING)
                                .student(student)
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

                // Persist ordered building preferences
                if (preferredBuildingIds != null && !preferredBuildingIds.isEmpty()) {
                        List<ApplicationBuildingPreference> preferences = new ArrayList<>();
                        for (int i = 0; i < preferredBuildingIds.size(); i++) {
                                Long buildingId = preferredBuildingIds.get(i);
                                Building building = buildingRepository.findById(buildingId)
                                                .orElseThrow(() -> new RuntimeException(
                                                                "Building not found with ID: " + buildingId));
                                preferences.add(ApplicationBuildingPreference.builder()
                                                .application(savedApplication)
                                                .building(building)
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

        public ApplicationResponse getApplicationById(Long applicationId) {
                log.info("Retrieving application with ID: {}", applicationId);

                Application application = applicationRepository.findById(applicationId)
                                .orElseThrow(() -> new ApplicationNotFoundException(
                                                "Application not found with ID: " + applicationId));

                // Verify authorization
                AppUser currentUser = authService.getCurrentUser();

                if (!application.getStudent().getAppUser().getId().equals(currentUser.getId()) &&
                                !currentUser.getUserRole().isAdmin()) {
                        throw new UnauthorizedAccessException("You are not authorized to view this application");
                }

                return applicationMapper.toResponse(application);
        }

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

                return applicationRepository.findByStudent_Id(studentId)
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
