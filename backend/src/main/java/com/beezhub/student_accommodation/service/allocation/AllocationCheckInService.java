package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.event.AllocationFreedEvent;
import com.beezhub.student_accommodation.exceptions.RoomAllocationNotFoundException;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.model.dto.RoomAllocationResponse;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationCheckInService {

    private final RoomAllocationRepository roomAllocationRepository;
    private final ApplicationRepository applicationRepository;
    private final RoomAllocationMapper roomAllocationMapper;
    private final UserNotificationService userNotificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final RoomOccupancyManager roomOccupancyManager;

    @Transactional
    public RoomAllocationResponse confirmCheckIn(Long id) {
        log.info("Confirming check-in for allocation with id: {}", id);
        RoomAllocation allocation = getAllocation(id);

        AllocationStatusValidator.validate(allocation, AllocationStatus.ACTIVE);
        allocation.setStatus(AllocationStatus.ACTIVE);
        allocation.setCheckInDate(LocalDateTime.now());

        if (allocation.getApplication() != null) {
            Application application = allocation.getApplication();
            application.setStatus(com.beezhub.student_accommodation.model.enums.ApplicationStatus.CHECKED_IN);
            applicationRepository.save(application);
            log.info("Updated application {} status to CHECKED_IN", application.getId());
        }

        RoomAllocation saved = roomAllocationRepository.save(allocation);
        log.info("Successfully checked in allocation with id: {}", id);
        
        try {
            if (allocation.getStudent() != null && allocation.getStudent().getAppUser() != null) {
                userNotificationService.createCheckInConfirmedNotification(
                    allocation.getStudent().getAppUser().getId(),
                    allocation.getRoom().getRoomNumber(),
                    allocation.getRoom().getBuilding() != null ? 
                        allocation.getRoom().getBuilding().getName() : "Unknown Building"
                );
            }
        } catch (Exception e) {
            log.error("Failed to send check-in confirmation notification for allocation {}", id, e);
        }
        
        return roomAllocationMapper.toResponse(saved);
    }

    @Transactional
    public RoomAllocationResponse confirmCheckOut(Long id) {
        log.info("Confirming check-out for allocation with id: {}", id);
        RoomAllocation allocation = getAllocation(id);

        AllocationStatusValidator.validate(allocation, AllocationStatus.COMPLETED);
        allocation.setStatus(AllocationStatus.COMPLETED);
        allocation.setCheckOutDate(LocalDateTime.now());
        allocation.setEndDate(LocalDateTime.now());

        roomOccupancyManager.syncOccupancy(allocation.getRoom());

        RoomAllocation saved = roomAllocationRepository.save(allocation);
        log.info("Successfully checked out allocation with id: {}", id);

        eventPublisher.publishEvent(new AllocationFreedEvent(this, allocation.getRoom().getId()));

        return roomAllocationMapper.toResponse(saved);
    }

    @Transactional
    public RoomAllocationResponse endAllocation(Long id) {
        log.info("Ending room allocation with id: {}", id);
        RoomAllocation allocation = getAllocation(id);

        AllocationStatusValidator.validate(allocation, AllocationStatus.COMPLETED);
        allocation.setStatus(AllocationStatus.COMPLETED);
        allocation.setEndDate(LocalDateTime.now());

        roomOccupancyManager.syncOccupancy(allocation.getRoom());

        RoomAllocation updated = roomAllocationRepository.save(allocation);
        log.info("Successfully ended room allocation with id: {}", id);

        eventPublisher.publishEvent(new AllocationFreedEvent(this, allocation.getRoom().getId()));

        return roomAllocationMapper.toResponse(updated);
    }

    private RoomAllocation getAllocation(Long id) {
        return roomAllocationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RoomAllocationNotFoundException("Room allocation not found with id: " + id));
    }
}
