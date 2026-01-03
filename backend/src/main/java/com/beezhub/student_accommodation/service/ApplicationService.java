package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.aspect.LogActivity;
import com.beezhub.student_accommodation.exceptions.ApplicationNotFoundException;
import com.beezhub.student_accommodation.exceptions.DuplicateApplicationException;
import com.beezhub.student_accommodation.exceptions.StudentNotFoundException;
import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.mapper.ApplicationMapper;
import com.beezhub.student_accommodation.model.dto.ApplicationResponse;
import com.beezhub.student_accommodation.model.dto.ApplicationStatusUpdateRequest;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.specification.ApplicationSpecification;
import com.beezhub.student_accommodation.util.PageMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static com.beezhub.student_accommodation.model.enums.ApplicationStatus.*;

@RequiredArgsConstructor
@Slf4j
@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final ApplicationMapper applicationMapper;
    private final AuthService authService;
    private final NotificationService notificationService;
    private final UserNotificationService userNotificationService;

    @Transactional
    @LogActivity(action = "CREATE", entityType = "APPLICATION", description = "Submitted application for student {0}", entityIdParamIndex = 0)
    public ApplicationResponse submitApplication(Long studentId) {
        log.info("Submitting application for student with ID: {}", studentId);

        // Get student
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with ID: " + studentId));

        // Verify authorization - student can only submit for themselves
        AppUser currentUser = authService.getCurrentUser();

        if (!student.getAppUser().getId().equals(currentUser.getId()) &&
            !currentUser.getUserRole().equals(UserRole.ADMIN)) {
            throw new UnauthorizedAccessException("You can only submit applications for yourself");
        }

        // Check for existing pending application
        if (applicationRepository.existsByStudent_IdAndStatus(studentId, PENDING)) {
            throw new DuplicateApplicationException("Student already has a pending application");
        }

        // Create and save application
        Application application = Application.builder()
                .applicationCode(generateApplicationCode())
                .applicationDate(LocalDate.now())
                .status(PENDING)
                .student(student)
                .build();

        Application savedApplication = applicationRepository.save(application);
        log.info("Application submitted successfully with code: {}", savedApplication.getApplicationCode());

        return applicationMapper.toResponse(savedApplication);
    }

    private String generateApplicationCode() {
        int year = LocalDate.now().getYear();
        long count = applicationRepository.countByApplicationCodeStartingWith("APP-" + year + "-");
        String sequence = String.format("%03d", count + 1);
        return "APP-" + year + "-" + sequence;
    }

    public ApplicationResponse getApplicationById(Long applicationId) {
        log.info("Retrieving application with ID: {}", applicationId);
        
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found with ID: " + applicationId));
        
        // Verify authorization
        AppUser currentUser = authService.getCurrentUser();
        
        if (!application.getStudent().getAppUser().getId().equals(currentUser.getId()) && 
            !currentUser.getUserRole().equals(UserRole.ADMIN)) {
            throw new UnauthorizedAccessException("You are not authorized to view this application");
        }
        
        return applicationMapper.toResponse(application);
    }

    public List<ApplicationResponse> getApplicationsByStudentId(Long studentId) {
        log.info("Retrieving applications for student with ID: {}", studentId);
        
        // Verify student exists
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with ID: " + studentId));
        
        // Verify authorization
        AppUser currentUser = authService.getCurrentUser();
        
        if (!student.getAppUser().getId().equals(currentUser.getId()) && 
            !currentUser.getUserRole().equals(UserRole.ADMIN)) {
            throw new UnauthorizedAccessException("You are not authorized to view these applications");
        }
        
        return applicationRepository.findByStudent_Id(studentId)
                .stream()
                .map(applicationMapper::toResponse)
                .toList();
    }

    public List<ApplicationResponse> getApplicationsByStatus(String status) {
       log.info("Retrieving applications for status {}", status);
        ApplicationStatus applicationStatus = ApplicationStatus.valueOf(status.toUpperCase());
        return applicationRepository.findByStatus(applicationStatus)
                .stream()
                .map(applicationMapper::toResponse)
                .toList();
    }
    
    @Transactional
    @LogActivity(action = "UPDATE", entityType = "APPLICATION", description = "Updated application {0} status to {1}", entityIdParamIndex = 0)
    public ApplicationResponse updateApplicationStatus(Long applicationId, @Valid ApplicationStatusUpdateRequest request) {
        log.info("Updating application {} to status: {}", applicationId, request.getStatus());

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found with ID: " + applicationId));

        ApplicationStatus oldStatus = application.getStatus();
        ApplicationStatus newStatus = ApplicationStatus.valueOf(request.getStatus().toUpperCase());
        application.setStatus(newStatus);
        application.setRemarks(request.getRemarks());

        Application updatedApplication = applicationRepository.save(application);
        log.info("Application {} status updated from {} to: {}", applicationId, oldStatus, newStatus);

        // Send notification if status changed to APPROVED or REJECTED
        if (newStatus != oldStatus && (newStatus == APPROVED || newStatus == REJECTED)) {
            try {
                log.info("Triggering notification for application {} with status {}", applicationId, newStatus);
                // Send email notification
                notificationService.sendApplicationStatusNotification(updatedApplication, newStatus);

                // Create in-app notification
                Long studentUserId = application.getStudent().getAppUser().getId();
                userNotificationService.createApplicationStatusNotification(
                        studentUserId,
                        applicationId,
                        newStatus == APPROVED,
                        request.getRemarks()
                );
            } catch (Exception e) {
                // Log error but don't fail the request - notification will be retried
                log.error("Failed to trigger notification for application {} - notification will be retried by scheduler",
                        applicationId, e);
            }
        }

        return applicationMapper.toResponse(updatedApplication);
    }
    
    @Transactional
    @LogActivity(action = "DELETE", entityType = "APPLICATION", description = "Canceled application {0}", entityIdParamIndex = 0)
    public void cancelApplication(Long applicationId) {
        log.info("Canceling application with ID: {}", applicationId);

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found with ID: " + applicationId));

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
                .toList();
    }
    
    public List<ApplicationResponse> getAllApplications() {
        log.info("Retrieving all applications");
        return applicationRepository.findAll()
                .stream()
                .map(applicationMapper::toResponse)
                .toList();
    }

    public PageResponse<ApplicationResponse> getAllApplications(Pageable pageable) {
        log.info("Retrieving paginated applications");
        Page<Application> page = applicationRepository.findAll(pageable);
        Page<ApplicationResponse> responsePage = page.map(applicationMapper::toResponse);
        return PageMapper.toPageResponse(responsePage);
    }

    public PageResponse<ApplicationResponse> searchApplications(String applicationCode, String studentName, ApplicationStatus status, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        log.info("Searching applications with filters - code: {}, student: {}, status: {}, startDate: {}, endDate: {}", 
                applicationCode, studentName, status, startDate, endDate);
        Specification<Application> spec = ApplicationSpecification.filterBy(applicationCode, studentName, status, startDate, endDate);
        Page<Application> page = applicationRepository.findAll(spec, pageable);
        Page<ApplicationResponse> responsePage = page.map(applicationMapper::toResponse);
        log.info("Found {} applications matching criteria", page.getNumberOfElements());
        return PageMapper.toPageResponse(responsePage);
    }
}
