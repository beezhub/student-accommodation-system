package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.dto.MessageResponse;
import com.beezhub.student_accommodation.model.dto.WaitlistPositionDto;
import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.WaitingListEntry;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.repository.WaitingListRepository;
import com.beezhub.student_accommodation.service.AcademicYearService;
import com.beezhub.student_accommodation.service.AuthService;
import com.beezhub.student_accommodation.service.NotificationService;
import com.beezhub.student_accommodation.exceptions.ApplicationNotFoundException;
import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.exceptions.StudentNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.beezhub.student_accommodation.model.event.AllocationDeclinedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaitingListService {

    /** Minimum recent allocations required before an estimated wait time is shown. */
    private static final int MIN_ALLOCATIONS_FOR_ESTIMATE = 5;
    /** Rolling window (days) used to measure recent allocation velocity. */
    private static final int ESTIMATE_WINDOW_DAYS = 30;

    private final WaitingListRepository waitingListRepository;
    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final RoomAllocationRepository roomAllocationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthService authService;
    private final AcademicYearService academicYearService;

    @Transactional
    public MessageResponse enqueueWaitingList(Long studentId, Long applicationId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with id: " + studentId));

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Application not found with id: " + applicationId));

        if (waitingListRepository.existsByStudent_IdAndStatus(studentId, "WAITING")) {
            log.info("Student {} is already on the waiting list", studentId);
            return new MessageResponse("Student is already on the waiting list");
        }

        if (roomAllocationRepository.existsActiveAllocationByStudentId(studentId)) {
            log.info("Student {} has an active allocation; cannot add to waiting list", studentId);
            return new MessageResponse("Student already has an active allocation offer");
        }

        enqueueWaitingList(student, application);
        return new MessageResponse("Student added to waiting list");
    }

    public void enqueueWaitingList(Student student, Application application) {
        // Check if student is already on the waiting list to prevent duplicates
        if (waitingListRepository.existsByStudent_IdAndStatus(student.getId(), "WAITING")) {
            log.info("Student {} is already on the waiting list, skipping", student.getId());
            return;
        }

        // Do not enqueue if student already has an active offer (OFFERED/ACCEPTED/ACTIVE).
        // They must accept or decline their current offer before re-entering the queue.
        if (roomAllocationRepository.existsActiveAllocationByStudentId(student.getId())) {
            log.info("Student {} has an active allocation; skipping waiting-list enqueue", student.getId());
            return;
        }

        WaitingListEntry entry = new WaitingListEntry();
        entry.setStudent(student);
        entry.setApplication(application);
        entry.setStatus("WAITING");
        waitingListRepository.save(entry);
        log.warn("No rooms available; student {} placed on waiting list", student.getId());
    }

    @Transactional
    public void updateStatusToAllocated(Long studentId) {
        waitingListRepository.findByStudent_IdAndStatus(studentId, "WAITING")
                .ifPresent(entry -> {
                    entry.setStatus("ALLOCATED");
                    waitingListRepository.save(entry);
                    log.info("Updated waiting list entry for student {} to ALLOCATED", studentId);
                });
    }

    @Transactional
    public void reQueueStudent(RoomAllocation allocation, int totalDeclineCount) {
        Application application = allocation.getApplication();
        Student student = allocation.getStudent();

        if (application == null) {
            log.warn("No application linked to allocation {}; cannot re-queue", allocation.getId());
            return;
        }

        log.info("Re-queueing student {} after decline {}", student.getId(), totalDeclineCount);

        application.setStatus(ApplicationStatus.AWAITING_ALLOCATION);
        String remarks = String.format("Declined offer %d. Added to waiting list.", totalDeclineCount);
        application.setRemarks(remarks);
        applicationRepository.save(application);

        if (!waitingListRepository.existsByStudent_IdAndStatus(student.getId(), "WAITING")) {
            enqueueWaitingList(student, application);
        }

        // Publish re-queue event
        eventPublisher.publishEvent(new AllocationDeclinedEvent(allocation, totalDeclineCount));
    }

    /**
     * Returns the authenticated student's current waiting-list position, the
     * total queue depth, and an estimated wait time — all scoped to the
     * current academic year (tenant scoping is automatic via the Hibernate
     * {@code tenantFilter} on {@link WaitingListEntry} and {@link RoomAllocation}).
     *
     * @throws ResourceNotFoundException if the student has no active waiting-list entry
     */
    @Transactional(readOnly = true)
    public WaitlistPositionDto getMyWaitlistPosition() {
        Student student = resolveCurrentStudent();

        WaitingListEntry entry = waitingListRepository.findByStudent_IdAndStatus(student.getId(), "WAITING")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student is not currently on the waiting list"));

        AcademicYear currentYear = academicYearService.getCurrentAcademicYearEntity();

        long position = waitingListRepository.countByStatusAndApplication_AcademicYear_IdAndCreatedAtLessThan(
                "WAITING", currentYear.getId(), entry.getCreatedAt()) + 1;
        long totalInQueue = waitingListRepository.countByStatusAndApplication_AcademicYear_Id(
                "WAITING", currentYear.getId());

        Integer estimatedWaitDays = estimateWaitDays(position, currentYear.getId());

        return WaitlistPositionDto.builder()
                .position((int) position)
                .totalInQueue((int) totalInQueue)
                .estimatedWaitDays(estimatedWaitDays)
                .build();
    }

    private Student resolveCurrentStudent() {
        AppUser currentUser = authService.getCurrentUser();
        return studentRepository.findByAppUser_Id(currentUser.getId())
                .orElseThrow(() -> new StudentNotFoundException(
                        "Student profile not found for the authenticated user"));
    }

    /**
     * Estimates wait time from recent allocation velocity: allocations created
     * in the last {@value #ESTIMATE_WINDOW_DAYS} days for the current academic
     * year approximate how quickly students are being pulled off the queue.
     * Returns {@code null} when there isn't enough recent history to estimate
     * from ({@code < MIN_ALLOCATIONS_FOR_ESTIMATE}).
     */
    private Integer estimateWaitDays(long position, Long academicYearId) {
        LocalDateTime windowStart = LocalDateTime.now().minusDays(ESTIMATE_WINDOW_DAYS);
        long recentAllocations = roomAllocationRepository
                .countByAcademicYear_IdAndCreatedAtAfter(academicYearId, windowStart);

        if (recentAllocations < MIN_ALLOCATIONS_FOR_ESTIMATE) {
            return null;
        }

        double averageIntervalDays = (double) ESTIMATE_WINDOW_DAYS / recentAllocations;
        return (int) Math.round(position * averageIntervalDays);
    }
}
