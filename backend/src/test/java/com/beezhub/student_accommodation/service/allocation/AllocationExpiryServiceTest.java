package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.AllocationMethod;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AllocationExpiryServiceTest {

    @Mock
    private RoomAllocationRepository roomAllocationRepository;

    @Mock
    private Environment environment;

    @Mock
    private OfferExpiryService offerExpiryService;

    @Mock
    private CheckInExpiryService checkInExpiryService;

    @Mock
    private AllocationBatchService allocationBatchService;

    @InjectMocks
    private AllocationExpiryService allocationExpiryService;

    private RoomAllocation testAllocation;
    private RoomAllocation testAllocation2;

    @BeforeEach
    void setUp() {
        // Set up test allocations
        testAllocation = new RoomAllocation();
        testAllocation.setId(1L);
        testAllocation.setStatus(AllocationStatus.OFFERED);
        testAllocation.setOfferExpiryDate(LocalDateTime.now().minusDays(1));

        testAllocation2 = new RoomAllocation();
        testAllocation2.setId(2L);
        testAllocation2.setStatus(AllocationStatus.ACCEPTED);
        testAllocation2.setCheckInExpiryDate(LocalDateTime.now().minusDays(1));

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
        verify(offerExpiryService, never()).expireSingleOffer(any());
        verify(allocationBatchService, never()).batchAllocateFromWaitingList(anyInt());
    }

    @Test
    void expireOffers_singleExpiredOffer_expiresAndCancelsApplication() {
        // Given
        when(roomAllocationRepository.findByStatusAndOfferExpiryDateBefore(any(), any()))
                .thenReturn(List.of(testAllocation));
        when(environment.getProperty(eq("app.allocation.batch-on-expiry"), eq(Boolean.class), anyBoolean()))
                .thenReturn(true);

        // When
        allocationExpiryService.expireOffers();

        // Then
        verify(offerExpiryService).expireSingleOffer(testAllocation);
        verify(allocationBatchService).batchAllocateFromWaitingList(1);
    }

    @Test
    void expireOffers_alreadyAccepted_skips() {
        // Given - This test doesn't apply to the new architecture
        // The query itself filters by status=OFFERED, so accepted offers won't be returned
        // The test is now covered by the delegated service tests
        when(roomAllocationRepository.findByStatusAndOfferExpiryDateBefore(any(), any()))
                .thenReturn(Collections.emptyList());

        // When
        allocationExpiryService.expireOffers();

        // Then
        verify(offerExpiryService, never()).expireSingleOffer(any());
    }

    @Test
    void expireOffers_multipleAllocations_oneFailsOthersContinue() {
        // Given
        RoomAllocation allocation1 = testAllocation;
        RoomAllocation allocation2 = new RoomAllocation();
        allocation2.setId(2L);
        allocation2.setStatus(AllocationStatus.OFFERED);

        when(roomAllocationRepository.findByStatusAndOfferExpiryDateBefore(any(), any()))
                .thenReturn(Arrays.asList(allocation1, allocation2));
        doNothing().when(offerExpiryService).expireSingleOffer(allocation1);
        doThrow(new RuntimeException("Database error")).when(offerExpiryService).expireSingleOffer(allocation2);
        when(environment.getProperty(eq("app.allocation.batch-on-expiry"), eq(Boolean.class), anyBoolean()))
                .thenReturn(true);

        // When
        allocationExpiryService.expireOffers();

        // Then
        verify(offerExpiryService).expireSingleOffer(allocation1);
        verify(offerExpiryService).expireSingleOffer(allocation2);
        verify(allocationBatchService).batchAllocateFromWaitingList(1);
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
        verify(checkInExpiryService, never()).expireSingleCheckIn(any());
        verify(allocationBatchService, never()).batchAllocateFromWaitingList(anyInt());
    }

    @Test
    void expireCheckIns_singleExpiredCheckIn_expiresAndCancelsApplication() {
        // Given
        when(roomAllocationRepository.findByStatusAndCheckInExpiryDateBefore(any(), any()))
                .thenReturn(List.of(testAllocation2));
        when(environment.getProperty(eq("app.allocation.batch-on-expiry"), eq(Boolean.class), anyBoolean()))
                .thenReturn(true);

        // When
        allocationExpiryService.expireCheckIns();

        // Then
        verify(checkInExpiryService).expireSingleCheckIn(testAllocation2);
        verify(allocationBatchService).batchAllocateFromWaitingList(1);
    }

    @Test
    void expireCheckIns_alreadyCheckedIn_skips() {
        // Given - This test doesn't apply to the new architecture
        // The query itself filters by status=ACCEPTED, so checked-in allocations won't be returned
        when(roomAllocationRepository.findByStatusAndCheckInExpiryDateBefore(any(), any()))
                .thenReturn(Collections.emptyList());

        // When
        allocationExpiryService.expireCheckIns();

        // Then
        verify(checkInExpiryService, never()).expireSingleCheckIn(any());
    }

    @Test
    void expireCheckIns_multipleAllocations_oneFailsOthersContinue() {
        // Given
        RoomAllocation allocation1 = testAllocation2;
        RoomAllocation allocation2 = new RoomAllocation();
        allocation2.setId(3L);
        allocation2.setStatus(AllocationStatus.ACCEPTED);

        when(roomAllocationRepository.findByStatusAndCheckInExpiryDateBefore(any(), any()))
                .thenReturn(Arrays.asList(allocation1, allocation2));
        doNothing().when(checkInExpiryService).expireSingleCheckIn(allocation1);
        doThrow(new RuntimeException("Database error")).when(checkInExpiryService).expireSingleCheckIn(allocation2);
        when(environment.getProperty(eq("app.allocation.batch-on-expiry"), eq(Boolean.class), anyBoolean()))
                .thenReturn(true);

        // When
        allocationExpiryService.expireCheckIns();

        // Then
        verify(checkInExpiryService).expireSingleCheckIn(allocation1);
        verify(checkInExpiryService).expireSingleCheckIn(allocation2);
        verify(allocationBatchService).batchAllocateFromWaitingList(1);
    }
}
