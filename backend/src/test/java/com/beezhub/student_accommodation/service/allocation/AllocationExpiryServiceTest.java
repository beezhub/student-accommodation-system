package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.AllocationMethod;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.service.RoomAllocationService;
import com.beezhub.student_accommodation.service.NotificationService;
import com.beezhub.student_accommodation.service.UserNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.core.env.Environment;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AllocationExpiryServiceTest {

    @Mock
    private RoomAllocationRepository roomAllocationRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private WaitingListRepository waitingListRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserNotificationService userNotificationService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private RoomOccupancyManager roomOccupancyManager;

    @Mock
    private RoomAllocationService roomAllocationService;

    @Mock
    private Environment environment;

    @InjectMocks
    private AllocationExpiryService allocationExpiryService;

    private RoomAllocation testAllocation;
    private Application testApplication;
    private Student testStudent;
    private Room testRoom;
    private Building testBuilding;
    private AppUser testUser;
    private WaitingListEntry testWaitingListEntry;

    @BeforeEach
    void setUp() {
        // Set config values
        ReflectionTestUtils.setField(allocationExpiryService, "offerExpiryDays", 7);
        ReflectionTestUtils.setField(allocationExpiryService, "checkInExpiryDays", 7);

        // Set up test entities
        testUser = new AppUser();
        testUser.setId(1L);
        testUser.setEmail("student@test.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");

        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setAppUser(testUser);

        testBuilding = new Building();
        testBuilding.setId(1L);
        testBuilding.setName("Test Building");

        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setRoomNumber("101");
        testRoom.setCapacity(2);
        testRoom.setCurrentOccupancy(1);
        testRoom.setStatus("OCCUPIED");
        testRoom.setBuilding(testBuilding);

        testApplication = new Application();
        testApplication.setId(1L);
        testApplication.setApplicationCode("APP-001");
        testApplication.setStatus(ApplicationStatus.APPROVED);
        testApplication.setStudent(testStudent);

        testAllocation = new RoomAllocation();
        testAllocation.setId(1L);
        testAllocation.setStudent(testStudent);
        testAllocation.setRoom(testRoom);
        testAllocation.setApplication(testApplication);
        testAllocation.setStatus(AllocationStatus.OFFERED);
        testAllocation.setAllocationDate(LocalDateTime.now().minusDays(8));
        testAllocation.setOfferExpiryDate(LocalDateTime.now().minusDays(1));
        testAllocation.setAllocationMethod(AllocationMethod.MANUAL);

        testWaitingListEntry = new WaitingListEntry();
        testWaitingListEntry.setId(1L);
        testWaitingListEntry.setStudent(testStudent);
        testWaitingListEntry.setStatus("WAITING");

        // Mock transaction template to execute the callback immediately
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(org.springframework.transaction.TransactionStatus.class));
        });

        // Mock environment
        lenient().when(environment.getProperty(eq("app.allocation.batch-on-expiry"), eq(Boolean.class), anyBoolean()))
                .thenReturn(true);
    }

    // ==================== Offer Expiry Tests ====================

    @Test
    void expireOffers_noExpiredOffers_doesNothing() {
        // Given
        when(roomAllocationRepository.findByStatusAndOfferExpiryDateBefore(any(), any()))
                .thenReturn(Collections.emptyList());

        // When
        allocationExpiryService.expireOffers();

        // Then
        verify(roomAllocationRepository).findByStatusAndOfferExpiryDateBefore(
                eq(AllocationStatus.OFFERED), any(LocalDateTime.class));
        verify(roomAllocationRepository, never()).save(any());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void expireOffers_singleExpiredOffer_expiresAndCancelsApplication() {
        // Given
        when(roomAllocationRepository.findByStatusAndOfferExpiryDateBefore(any(), any()))
                .thenReturn(List.of(testAllocation));
        when(roomAllocationRepository.findByIdWithDetails(testAllocation.getId()))
                .thenReturn(Optional.of(testAllocation));
        
        // Mock occupancy update
        doAnswer(invocation -> {
            Room room = invocation.getArgument(0);
            int change = invocation.getArgument(1);
            room.setCurrentOccupancy(room.getCurrentOccupancy() + change);
            if (room.getCurrentOccupancy() == 0) room.setStatus("AVAILABLE");
            return null;
        }).when(roomOccupancyManager).updateOccupancy(any(Room.class), anyInt());

        lenient().when(waitingListRepository.findByStudent_IdAndStatus(testStudent.getId(), "WAITING"))
                .thenReturn(Optional.of(testWaitingListEntry));

        // When
        allocationExpiryService.expireOffers();

        // Then
        // Verify allocation was updated
        ArgumentCaptor<RoomAllocation> allocationCaptor = ArgumentCaptor.forClass(RoomAllocation.class);
        verify(roomAllocationRepository, atLeastOnce()).save(allocationCaptor.capture());
        RoomAllocation savedAllocation = allocationCaptor.getValue();
        assertEquals(AllocationStatus.EXPIRED, savedAllocation.getStatus());
        assertNotNull(savedAllocation.getEndDate());

        // Verify room occupancy was decreased
        assertEquals(0, testRoom.getCurrentOccupancy());
        assertEquals("AVAILABLE", testRoom.getStatus());

        // Verify application was cancelled
        ArgumentCaptor<Application> applicationCaptor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository, atLeastOnce()).save(applicationCaptor.capture());
        Application savedApplication = applicationCaptor.getValue();
        assertEquals(ApplicationStatus.CANCELLED, savedApplication.getStatus());
        assertTrue(savedApplication.getRemarks().contains("expired without response"));

        // Verify waiting list entry was cancelled (if exists)
        verify(waitingListRepository, atLeast(0)).save(any(WaitingListEntry.class));

        // Verify notifications were sent
        verify(notificationService, atLeastOnce()).sendOfferExpiredNotification(any(RoomAllocation.class));
        verify(userNotificationService, atLeastOnce()).createOfferExpiredNotification(
                eq(testUser.getId()), eq("101"), eq("Test Building"));

        // Verify batch allocation triggered
        verify(roomAllocationService).batchAllocateFromWaitingList(1);
    }

    @Test
    void expireOffers_alreadyAccepted_skips() {
        // Given
        testAllocation.setStatus(AllocationStatus.ACCEPTED);
        when(roomAllocationRepository.findByStatusAndOfferExpiryDateBefore(any(), any()))
                .thenReturn(List.of(testAllocation));
        when(roomAllocationRepository.findByIdWithDetails(testAllocation.getId()))
                .thenReturn(Optional.of(testAllocation));

        // When
        allocationExpiryService.expireOffers();

        // Then
        // Should skip processing since status changed
        verify(roomAllocationRepository, never()).save(any());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void expireOffers_multipleAllocations_oneFailsOthersContinue() {
        // Given
        RoomAllocation allocation1 = testAllocation;
        RoomAllocation allocation2 = new RoomAllocation();
        allocation2.setId(2L);
        allocation2.setStudent(testStudent);
        allocation2.setRoom(testRoom);
        allocation2.setApplication(testApplication);
        allocation2.setStatus(AllocationStatus.OFFERED);

        when(roomAllocationRepository.findByStatusAndOfferExpiryDateBefore(any(), any()))
                .thenReturn(Arrays.asList(allocation1, allocation2));
        when(roomAllocationRepository.findByIdWithDetails(allocation1.getId()))
                .thenReturn(Optional.of(allocation1));
        when(roomAllocationRepository.findByIdWithDetails(allocation2.getId()))
                .thenThrow(new RuntimeException("Database error"));
        
        // Mock occupancy update for success case
        doNothing().when(roomOccupancyManager).updateOccupancy(any(Room.class), anyInt());

        // When
        allocationExpiryService.expireOffers();

        // Then
        // First allocation should be saved (succeeded)
        verify(roomAllocationRepository, atLeastOnce()).save(any(RoomAllocation.class));
        // Application should still be saved for successful expiry
        verify(applicationRepository, atLeastOnce()).save(any(Application.class));
    }

    // ==================== Check-In Expiry Tests ====================

    @Test
    void expireCheckIns_noExpiredCheckIns_doesNothing() {
        // Given
        when(roomAllocationRepository.findByStatusAndCheckInExpiryDateBefore(any(), any()))
                .thenReturn(Collections.emptyList());

        // When
        allocationExpiryService.expireCheckIns();

        // Then
        verify(roomAllocationRepository).findByStatusAndCheckInExpiryDateBefore(
                eq(AllocationStatus.ACCEPTED), any(LocalDateTime.class));
        verify(roomAllocationRepository, never()).save(any());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void expireCheckIns_singleExpiredCheckIn_expiresAndCancelsApplication() {
        // Given
        testAllocation.setStatus(AllocationStatus.ACCEPTED);
        testAllocation.setCheckInExpiryDate(LocalDateTime.now().minusDays(1));
        when(roomAllocationRepository.findByStatusAndCheckInExpiryDateBefore(any(), any()))
                .thenReturn(List.of(testAllocation));
        when(roomAllocationRepository.findByIdWithDetails(testAllocation.getId()))
                .thenReturn(Optional.of(testAllocation));
        
        // Mock occupancy update
        doAnswer(invocation -> {
            Room room = invocation.getArgument(0);
            int change = invocation.getArgument(1);
            room.setCurrentOccupancy(room.getCurrentOccupancy() + change);
            if (room.getCurrentOccupancy() == 0) room.setStatus("AVAILABLE");
            return null;
        }).when(roomOccupancyManager).updateOccupancy(any(Room.class), anyInt());

        lenient().when(waitingListRepository.findByStudent_IdAndStatus(testStudent.getId(), "WAITING"))
                .thenReturn(Optional.of(testWaitingListEntry));

        // When
        allocationExpiryService.expireCheckIns();

        // Then
        // Verify allocation was updated
        ArgumentCaptor<RoomAllocation> allocationCaptor = ArgumentCaptor.forClass(RoomAllocation.class);
        verify(roomAllocationRepository, atLeastOnce()).save(allocationCaptor.capture());
        RoomAllocation savedAllocation = allocationCaptor.getValue();
        assertEquals(AllocationStatus.CHECK_IN_EXPIRED, savedAllocation.getStatus());
        assertNotNull(savedAllocation.getEndDate());

        // Verify room occupancy was decreased
        assertEquals(0, testRoom.getCurrentOccupancy());
        assertEquals("AVAILABLE", testRoom.getStatus());

        // Verify application was cancelled
        ArgumentCaptor<Application> applicationCaptor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository, atLeastOnce()).save(applicationCaptor.capture());
        Application savedApplication = applicationCaptor.getValue();
        assertEquals(ApplicationStatus.CANCELLED, savedApplication.getStatus());
        assertTrue(savedApplication.getRemarks().contains("failed to check in"));

        // Verify waiting list entry was cancelled (if exists)
        verify(waitingListRepository, atLeast(0)).save(any(WaitingListEntry.class));

        // Verify notifications were sent
        verify(notificationService, atLeastOnce()).sendCheckInExpiredNotification(any(RoomAllocation.class));
        verify(userNotificationService, atLeastOnce()).createCheckInExpiredNotification(
                eq(testUser.getId()), eq("101"), eq("Test Building"));

        // Verify batch allocation triggered
        verify(roomAllocationService).batchAllocateFromWaitingList(1);
    }

    @Test
    void expireCheckIns_alreadyCheckedIn_skips() {
        // Given
        testAllocation.setStatus(AllocationStatus.ACTIVE);
        when(roomAllocationRepository.findByStatusAndCheckInExpiryDateBefore(any(), any()))
                .thenReturn(List.of(testAllocation));
        when(roomAllocationRepository.findByIdWithDetails(testAllocation.getId()))
                .thenReturn(Optional.of(testAllocation));

        // When
        allocationExpiryService.expireCheckIns();

        // Then
        // Should skip processing since status changed
        verify(roomAllocationRepository, never()).save(any());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void expireCheckIns_multipleAllocations_oneFailsOthersContinue() {
        // Given
        RoomAllocation allocation1 = testAllocation;
        allocation1.setStatus(AllocationStatus.ACCEPTED);
        RoomAllocation allocation2 = new RoomAllocation();
        allocation2.setId(2L);
        allocation2.setStudent(testStudent);
        allocation2.setRoom(testRoom);
        allocation2.setApplication(testApplication);
        allocation2.setStatus(AllocationStatus.ACCEPTED);

        when(roomAllocationRepository.findByStatusAndCheckInExpiryDateBefore(any(), any()))
                .thenReturn(Arrays.asList(allocation1, allocation2));
        when(roomAllocationRepository.findByIdWithDetails(allocation1.getId()))
                .thenReturn(Optional.of(allocation1));
        when(roomAllocationRepository.findByIdWithDetails(allocation2.getId()))
                .thenThrow(new RuntimeException("Database error"));
        
        // Mock occupancy update for success case
        doNothing().when(roomOccupancyManager).updateOccupancy(any(Room.class), anyInt());

        // When
        allocationExpiryService.expireCheckIns();

        // Then
        // First allocation should be saved (succeeded)
        verify(roomAllocationRepository, atLeastOnce()).save(any(RoomAllocation.class));
        // Application should still be saved for successful expiry
        verify(applicationRepository, atLeastOnce()).save(any(Application.class));
    }
}
