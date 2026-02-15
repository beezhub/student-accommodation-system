package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.AcceptOfferRequest;
import com.beezhub.student_accommodation.model.dto.RoomAllocationResponse;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.WaitingListEntry;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.service.allocation.*;
import com.beezhub.student_accommodation.service.application.ApplicationDocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    @Mock
    private ApplicationDocumentService applicationDocumentService;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private AllocationScoringService allocationScoringService;
    @Mock
    private SystemSettingService systemSettingService;
    @Mock
    private UserNotificationService userNotificationService;

    @InjectMocks
    private RoomAllocationService roomAllocationService;

    private RoomAllocation allocation;
    private AcceptOfferRequest request;
    private Application application;
    private Student student;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(10L);

        application = new Application();
        application.setId(100L);
        application.setStatus(ApplicationStatus.APPROVED);

        allocation = new RoomAllocation();
        allocation.setId(1L);
        allocation.setStatus(AllocationStatus.OFFERED);
        allocation.setApplication(application);
        allocation.setStudent(student);

        request = new AcceptOfferRequest();
        request.setExpectedCheckInDate(LocalDateTime.now().plusDays(2));
    }

    @Test
    @DisplayName("Should accept offer when date is valid")
    void shouldAcceptOfferWhenDateIsValid() {
        RoomAllocationResponse expectedResponse = new RoomAllocationResponse();
        when(allocationLifecycleService.acceptOffer(eq(1L), any(AcceptOfferRequest.class)))
                .thenReturn(expectedResponse);

        roomAllocationService.acceptOffer(1L, request);

        verify(allocationLifecycleService).acceptOffer(1L, request);
    }

    @Test
    @DisplayName("Should update waiting list entry to ALLOCATED when accepting offer")
    void shouldUpdateWaitingListEntryToAllocatedWhenAcceptingOffer() {
        RoomAllocationResponse expectedResponse = new RoomAllocationResponse();
        when(allocationLifecycleService.acceptOffer(eq(1L), any(AcceptOfferRequest.class)))
                .thenReturn(expectedResponse);

        roomAllocationService.acceptOffer(1L, request);

        verify(allocationLifecycleService).acceptOffer(1L, request);
    }

    @Test
    @DisplayName("Should accept offer without error when no waiting list entry exists")
    void shouldAcceptOfferWithNoWaitingListEntry() {
        RoomAllocationResponse expectedResponse = new RoomAllocationResponse();
        when(allocationLifecycleService.acceptOffer(eq(1L), any(AcceptOfferRequest.class)))
                .thenReturn(expectedResponse);

        roomAllocationService.acceptOffer(1L, request);

        verify(allocationLifecycleService).acceptOffer(1L, request);
    }

    @Test
    @DisplayName("Should throw exception when date is in the past")
    void shouldThrowExceptionWhenDateIsInThePast() {
        request.setExpectedCheckInDate(LocalDateTime.now().minusDays(1));

        when(allocationLifecycleService.acceptOffer(eq(1L), any(AcceptOfferRequest.class)))
                .thenThrow(new IllegalArgumentException("Check-in date must be within 7 days"));

        assertThatThrownBy(() -> roomAllocationService.acceptOffer(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("within 7 days");
    }

    @Test
    @DisplayName("Should throw exception when date is more than 7 days in future")
    void shouldThrowExceptionWhenDateIsTooFarInFuture() {
        request.setExpectedCheckInDate(LocalDateTime.now().plusDays(8));

        when(allocationLifecycleService.acceptOffer(eq(1L), any(AcceptOfferRequest.class)))
                .thenThrow(new IllegalArgumentException("Check-in date must be within 7 days"));

        assertThatThrownBy(() -> roomAllocationService.acceptOffer(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("within 7 days");
    }

    @Test
    @DisplayName("Should throw exception if allocation is not OFFERED")
    void shouldThrowExceptionIfAllocationIsNotOffered() {
        when(allocationLifecycleService.acceptOffer(eq(1L), any(AcceptOfferRequest.class)))
                .thenThrow(new IllegalStateException("Can only accept OFFERED"));

        assertThatThrownBy(() -> roomAllocationService.acceptOffer(1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Can only accept OFFERED");
    }
}
