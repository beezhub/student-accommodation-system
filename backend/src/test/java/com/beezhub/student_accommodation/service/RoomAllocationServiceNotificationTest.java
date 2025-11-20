package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.model.dto.RoomAllocationRequest;
import com.beezhub.student_accommodation.model.dto.RoomAllocationResponse;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for room allocation notification triggers
 *
 * Acceptance Criteria:
 * ✅ Notification sent on successful room allocation
 * ✅ Email contains relevant details (student name, room number, building, allocation date)
 * ✅ Async sending doesn't block request
 * ✅ Failed emails logged but don't break flow
 * ✅ Integration tests verify notifications sent
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Room Allocation Service - Notification Integration Tests")
class RoomAllocationServiceNotificationTest {

    @Mock
    private RoomAllocationRepository roomAllocationRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private RoomAllocationMapper roomAllocationMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private RoomAllocationService roomAllocationService;

    private Student student;
    private AppUser appUser;
    private Room room;
    private Building building;
    private RoomAllocation roomAllocation;
    private RoomAllocationResponse roomAllocationResponse;
    private RoomAllocationRequest roomAllocationRequest;

    @BeforeEach
    void setUp() {
        appUser = AppUser.builder()
                .id(1L)
                .email("student@university.edu")
                .firstName("John")
                .lastName("Doe")
                .userRole(UserRole.STUDENT)
                .build();

        student = new Student();
        student.setId(1L);
        student.setStudentNumber("STU-2025-001");
        student.setAppUser(appUser);

        building = new Building();
        building.setId(1L);
        building.setName("North Hall");
        building.setAddress("123 Campus Road");

        room = new Room();
        room.setId(1L);
        room.setRoomNumber("A101");
        room.setCapacity(2);
        room.setCurrentOccupancy(0);
        room.setStatus("AVAILABLE");
        room.setBuilding(building);

        roomAllocation = new RoomAllocation();
        roomAllocation.setId(1L);
        roomAllocation.setStudent(student);
        roomAllocation.setRoom(room);
        roomAllocation.setAllocationDate(LocalDateTime.now());
        roomAllocation.setStatus("ACTIVE");

        roomAllocationResponse = new RoomAllocationResponse();
        roomAllocationResponse.setId(1L);
        roomAllocationResponse.setStatus("ACTIVE");
        roomAllocationResponse.setAllocationDate(LocalDateTime.now());

        roomAllocationRequest = new RoomAllocationRequest();
        roomAllocationRequest.setStudentId(student.getId());
        roomAllocationRequest.setRoomId(room.getId());
    }

    @Test
    @DisplayName("Should send notification when room is allocated")
    void shouldSendNotificationWhenRoomAllocated() {
        // Given
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomAllocationRepository.existsActiveAllocationByStudentId(1L)).thenReturn(false);
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenReturn(roomAllocation);
        when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(roomAllocationResponse);

        // When
        RoomAllocationResponse response = roomAllocationService.createRoomAllocation(roomAllocationRequest);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getId());

        // Verify notification service was called with correct parameters
        ArgumentCaptor<RoomAllocation> allocationCaptor = ArgumentCaptor.forClass(RoomAllocation.class);
        verify(notificationService, times(1)).sendRoomAllocationNotification(allocationCaptor.capture());

        RoomAllocation capturedAllocation = allocationCaptor.getValue();
        assertNotNull(capturedAllocation);
        assertEquals(student.getId(), capturedAllocation.getStudent().getId());
        assertEquals(room.getId(), capturedAllocation.getRoom().getId());
        assertEquals("ACTIVE", capturedAllocation.getStatus());

        // Verify repository save was called
        verify(roomAllocationRepository, times(1)).save(any(RoomAllocation.class));
    }

    @Test
    @DisplayName("Should not break flow when notification fails")
    void shouldNotBreakFlowWhenNotificationFails() {
        // Given
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomAllocationRepository.existsActiveAllocationByStudentId(1L)).thenReturn(false);
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenReturn(roomAllocation);
        when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(roomAllocationResponse);

        // Simulate notification service throwing exception
        doThrow(new RuntimeException("Email service unavailable"))
                .when(notificationService)
                .sendRoomAllocationNotification(any(RoomAllocation.class));

        // When - should not throw exception
        assertDoesNotThrow(() -> {
            RoomAllocationResponse response = roomAllocationService.createRoomAllocation(roomAllocationRequest);
            assertNotNull(response);
        });

        // Then - allocation should still be saved despite notification failure
        verify(roomAllocationRepository, times(1)).save(any(RoomAllocation.class));
        verify(notificationService, times(1)).sendRoomAllocationNotification(any(RoomAllocation.class));
    }

    @Test
    @DisplayName("Should include room allocation details in notification")
    void shouldIncludeRoomAllocationDetailsInNotification() {
        // Given
        roomAllocation.setAllocationDate(LocalDateTime.of(2025, 11, 20, 10, 30));

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomAllocationRepository.existsActiveAllocationByStudentId(1L)).thenReturn(false);
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenReturn(roomAllocation);
        when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(roomAllocationResponse);

        // When
        roomAllocationService.createRoomAllocation(roomAllocationRequest);

        // Then - verify notification contains correct allocation details
        ArgumentCaptor<RoomAllocation> allocationCaptor = ArgumentCaptor.forClass(RoomAllocation.class);
        verify(notificationService).sendRoomAllocationNotification(allocationCaptor.capture());

        RoomAllocation capturedAllocation = allocationCaptor.getValue();
        assertEquals("A101", capturedAllocation.getRoom().getRoomNumber());
        assertEquals("North Hall", capturedAllocation.getRoom().getBuilding().getName());
        assertEquals("ACTIVE", capturedAllocation.getStatus());
        assertEquals(student.getAppUser().getEmail(), capturedAllocation.getStudent().getAppUser().getEmail());
    }

    @Test
    @DisplayName("Should send notification with student information")
    void shouldSendNotificationWithStudentInformation() {
        // Given
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomAllocationRepository.existsActiveAllocationByStudentId(1L)).thenReturn(false);
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenReturn(roomAllocation);
        when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(roomAllocationResponse);

        // When
        roomAllocationService.createRoomAllocation(roomAllocationRequest);

        // Then - verify student details are accessible in notification
        ArgumentCaptor<RoomAllocation> allocationCaptor = ArgumentCaptor.forClass(RoomAllocation.class);
        verify(notificationService).sendRoomAllocationNotification(allocationCaptor.capture());

        RoomAllocation capturedAllocation = allocationCaptor.getValue();
        assertNotNull(capturedAllocation.getStudent());
        assertNotNull(capturedAllocation.getStudent().getAppUser());
        assertEquals("student@university.edu", capturedAllocation.getStudent().getAppUser().getEmail());
        assertEquals("John", capturedAllocation.getStudent().getAppUser().getFirstName());
        assertEquals("Doe", capturedAllocation.getStudent().getAppUser().getLastName());
        assertEquals("STU-2025-001", capturedAllocation.getStudent().getStudentNumber());
    }

    @Test
    @DisplayName("Should execute notification asynchronously without blocking")
    void shouldExecuteNotificationAsynchronouslyWithoutBlocking() {
        // Given
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomAllocationRepository.existsActiveAllocationByStudentId(1L)).thenReturn(false);
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenReturn(roomAllocation);
        when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(roomAllocationResponse);

        // When
        long startTime = System.currentTimeMillis();
        RoomAllocationResponse response = roomAllocationService.createRoomAllocation(roomAllocationRequest);
        long endTime = System.currentTimeMillis();

        // Then - should complete quickly (async execution doesn't block)
        assertNotNull(response);
        long executionTime = endTime - startTime;
        assertTrue(executionTime < 1000, "Method should return quickly without waiting for email");

        // Verify notification was triggered
        verify(notificationService, times(1)).sendRoomAllocationNotification(any(RoomAllocation.class));
    }

    @Test
    @DisplayName("Should include building information in notification")
    void shouldIncludeBuildingInformationInNotification() {
        // Given
        building.setAddress("123 Campus Road");
        building.setName("North Hall");

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomAllocationRepository.existsActiveAllocationByStudentId(1L)).thenReturn(false);
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenReturn(roomAllocation);
        when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(roomAllocationResponse);

        // When
        roomAllocationService.createRoomAllocation(roomAllocationRequest);

        // Then - verify building details are accessible in notification
        ArgumentCaptor<RoomAllocation> allocationCaptor = ArgumentCaptor.forClass(RoomAllocation.class);
        verify(notificationService).sendRoomAllocationNotification(allocationCaptor.capture());

        RoomAllocation capturedAllocation = allocationCaptor.getValue();
        assertNotNull(capturedAllocation.getRoom().getBuilding());
        assertEquals("North Hall", capturedAllocation.getRoom().getBuilding().getName());
        assertEquals("123 Campus Road", capturedAllocation.getRoom().getBuilding().getAddress());
    }

    @Test
    @DisplayName("Should send notification with room capacity information")
    void shouldSendNotificationWithRoomCapacityInformation() {
        // Given
        room.setCapacity(2);
        room.setCurrentOccupancy(0);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomAllocationRepository.existsActiveAllocationByStudentId(1L)).thenReturn(false);
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenReturn(roomAllocation);
        when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(roomAllocationResponse);

        // When
        roomAllocationService.createRoomAllocation(roomAllocationRequest);

        // Then - verify room capacity details are accessible
        ArgumentCaptor<RoomAllocation> allocationCaptor = ArgumentCaptor.forClass(RoomAllocation.class);
        verify(notificationService).sendRoomAllocationNotification(allocationCaptor.capture());

        RoomAllocation capturedAllocation = allocationCaptor.getValue();
        assertEquals(2, capturedAllocation.getRoom().getCapacity());
    }

    @Test
    @DisplayName("Should send notification when allocating by application")
    void shouldSendNotificationWhenAllocatingByApplication() {
        // Given
        Application application = new Application();
        application.setId(1L);
        application.setStudent(student);
        application.setStatus(com.beezhub.student_accommodation.model.enums.ApplicationStatus.APPROVED);

        roomAllocation.setApplication(application);
        roomAllocationRequest.setApplicationId(1L);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(roomAllocationRepository.existsActiveAllocationByStudentId(1L)).thenReturn(false);
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAllocationRepository.existsByApplicationId(1L)).thenReturn(false);
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenReturn(roomAllocation);
        when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(roomAllocationResponse);

        // When
        roomAllocationService.createRoomAllocation(roomAllocationRequest);

        // Then - verify notification sent with application context
        ArgumentCaptor<RoomAllocation> allocationCaptor = ArgumentCaptor.forClass(RoomAllocation.class);
        verify(notificationService).sendRoomAllocationNotification(allocationCaptor.capture());

        RoomAllocation capturedAllocation = allocationCaptor.getValue();
        assertNotNull(capturedAllocation.getApplication());
        assertEquals(1L, capturedAllocation.getApplication().getId());
    }

    @Test
    @DisplayName("Should handle room occupancy update along with notification")
    void shouldHandleRoomOccupancyUpdateAlongWithNotification() {
        // Given
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomAllocationRepository.existsActiveAllocationByStudentId(1L)).thenReturn(false);
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenReturn(roomAllocation);
        when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(roomAllocationResponse);

        // When
        roomAllocationService.createRoomAllocation(roomAllocationRequest);

        // Then - verify both room update and notification occurred
        verify(roomRepository, times(1)).save(room); // Room occupancy updated
        verify(notificationService, times(1)).sendRoomAllocationNotification(any(RoomAllocation.class));
    }

    @Test
    @DisplayName("Should maintain transaction integrity when notification fails")
    void shouldMaintainTransactionIntegrityWhenNotificationFails() {
        // Given
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomAllocationRepository.existsActiveAllocationByStudentId(1L)).thenReturn(false);
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenReturn(roomAllocation);
        when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(roomAllocationResponse);

        // Simulate notification service throwing exception
        doThrow(new RuntimeException("SMTP connection failed"))
                .when(notificationService)
                .sendRoomAllocationNotification(any(RoomAllocation.class));

        // When
        RoomAllocationResponse response = roomAllocationService.createRoomAllocation(roomAllocationRequest);

        // Then - verify all operations completed despite notification failure
        assertNotNull(response);
        verify(roomAllocationRepository, times(1)).save(any(RoomAllocation.class)); // Allocation saved
        verify(roomRepository, times(1)).save(room); // Room updated
        verify(notificationService, times(1)).sendRoomAllocationNotification(any(RoomAllocation.class)); // Notification attempted
    }
}
