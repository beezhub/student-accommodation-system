package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.dto.MessageResponse;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.WaitingListEntry;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.repository.WaitingListRepository;
import com.beezhub.student_accommodation.service.NotificationService;
import com.beezhub.student_accommodation.exceptions.ApplicationNotFoundException;
import com.beezhub.student_accommodation.exceptions.StudentNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.beezhub.student_accommodation.model.event.AllocationDeclinedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaitingListService {

    private final WaitingListRepository waitingListRepository;
    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final RoomAllocationRepository roomAllocationRepository;
    private final ApplicationEventPublisher eventPublisher;

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
}
