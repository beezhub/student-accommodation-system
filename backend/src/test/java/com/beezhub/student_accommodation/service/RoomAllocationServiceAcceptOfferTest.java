package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.AcceptOfferRequest;
import com.beezhub.student_accommodation.model.dto.RoomAllocationResponse;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.service.allocation.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomAllocationServiceAcceptOfferTest {

    @Mock
    private RoomAllocationQueryService roomAllocationQueryService;
    @Mock
    private AllocationEligibilityService allocationEligibilityService;
    @Mock
    private AllocationLifecycleService allocationLifecycleService;
    @Mock
    private AllocationBatchService allocationBatchService;
    @Mock
    private AllocationCheckInService allocationCheckInService;
    @Mock
    private AllocationExpiryService allocationExpiryService;

    @InjectMocks
    private RoomAllocationService roomAllocationService;

    private AcceptOfferRequest request;

    @BeforeEach
    void setUp() {
        request = new AcceptOfferRequest();
        request.setExpectedCheckInDate(LocalDateTime.now().plusDays(2));
    }

    @Test
    @DisplayName("Should delegate acceptOffer to AllocationLifecycleService")
    void shouldDelegateAcceptOffer() {
        // Arrange
        Long allocationId = 1L;
        RoomAllocationResponse expectedResponse = new RoomAllocationResponse();
        expectedResponse.setId(allocationId);
        expectedResponse.setStatus(AllocationStatus.ACCEPTED.name());

        when(allocationLifecycleService.acceptOffer(allocationId, request)).thenReturn(expectedResponse);

        // Act
        RoomAllocationResponse response = roomAllocationService.acceptOffer(allocationId, request);

        // Assert
        assertThat(response).isEqualTo(expectedResponse);
        verify(allocationLifecycleService).acceptOffer(allocationId, request);
    }
}
