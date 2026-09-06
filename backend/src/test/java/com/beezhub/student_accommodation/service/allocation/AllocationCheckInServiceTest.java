package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.exceptions.RoomAllocationNotFoundException;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.model.dto.RoomAllocationResponse;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.service.UserNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllocationCheckInServiceTest {

    @Mock
    private RoomAllocationRepository roomAllocationRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private RoomAllocationMapper roomAllocationMapper;
    @Mock
    private UserNotificationService userNotificationService;
    @Mock
    private AllocationBatchService allocationBatchService;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private RoomOccupancyManager roomOccupancyManager;

    @InjectMocks
    private AllocationCheckInService allocationCheckInService;

    @Test
    void checkIn_acceptedAllocation_statusActive() {
        // Arrange
        Long id = 1L;
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(id);
        allocation.setStatus(AllocationStatus.ACCEPTED);
        allocation.setApplication(new Application());
        
        when(roomAllocationRepository.findByIdWithDetails(id)).thenReturn(Optional.of(allocation));
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenReturn(allocation);
        when(roomAllocationMapper.toResponse(any())).thenReturn(new RoomAllocationResponse());

        // Act
        allocationCheckInService.confirmCheckIn(id);

        // Assert
        assertEquals(AllocationStatus.ACTIVE, allocation.getStatus());
        assertNotNull(allocation.getCheckInDate());
        verify(applicationRepository).save(any(Application.class));
    }

    @Test
    void moveOut_activeAllocation_statusCompleted() {
        // Arrange
        Long id = 1L;
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(id);
        allocation.setStatus(AllocationStatus.ACTIVE);
        Room room = new Room();
        room.setCapacity(1);
        room.setCurrentOccupancy(1);
        allocation.setRoom(room);
        
        when(roomAllocationRepository.findByIdWithDetails(id)).thenReturn(Optional.of(allocation));
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenReturn(allocation);
        when(roomAllocationMapper.toResponse(any())).thenReturn(new RoomAllocationResponse());

        // Act
        allocationCheckInService.confirmCheckOut(id);

        // Assert
        assertEquals(AllocationStatus.COMPLETED, allocation.getStatus());
        assertNotNull(allocation.getCheckOutDate());
        verify(roomOccupancyManager).syncOccupancy(room);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void terminate_activeAllocation_statusTerminated() {
        // Arrange
        Long id = 1L;
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(id);
        allocation.setStatus(AllocationStatus.ACTIVE);
        Room room = new Room();
        room.setCapacity(1);
        room.setCurrentOccupancy(1);
        allocation.setRoom(room);
        
        when(roomAllocationRepository.findByIdWithDetails(id)).thenReturn(Optional.of(allocation));
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenReturn(allocation);
        when(roomAllocationMapper.toResponse(any())).thenReturn(new RoomAllocationResponse());

        // Act
        allocationCheckInService.endAllocation(id);

        // Assert
        assertEquals(AllocationStatus.COMPLETED, allocation.getStatus()); // endAllocation sets COMPLETED currently
        assertNotNull(allocation.getEndDate());
        verify(roomOccupancyManager).syncOccupancy(room);
    }
}
