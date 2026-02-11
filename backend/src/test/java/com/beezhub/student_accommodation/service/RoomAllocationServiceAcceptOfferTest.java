package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.RoomAllocationNotFoundException;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.model.dto.AcceptOfferRequest;
import com.beezhub.student_accommodation.model.dto.RoomAllocationResponse;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.WaitingListEntry;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.WaitingListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomAllocationServiceAcceptOfferTest {

    @Mock
    private RoomAllocationRepository roomAllocationRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private RoomAllocationMapper roomAllocationMapper;
    @Mock
    private WaitingListRepository waitingListRepository;
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
        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(i -> i.getArguments()[0]);
        when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(new RoomAllocationResponse());
        when(waitingListRepository.findByStudent_IdAndStatus(10L, "WAITING")).thenReturn(Optional.empty());

        roomAllocationService.acceptOffer(1L, request);

        assertThat(allocation.getStatus()).isEqualTo(AllocationStatus.ACCEPTED);
        assertThat(allocation.getExpectedCheckInDate()).isEqualTo(request.getExpectedCheckInDate());
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.ALLOCATED);

        verify(roomAllocationRepository).save(allocation);
        verify(applicationRepository).save(application);
    }

    @Test
    @DisplayName("Should update waiting list entry to ALLOCATED when accepting offer")
    void shouldUpdateWaitingListEntryToAllocatedWhenAcceptingOffer() {
        WaitingListEntry waitingEntry = new WaitingListEntry();
        waitingEntry.setId(5L);
        waitingEntry.setStudent(student);
        waitingEntry.setStatus("WAITING");

        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(i -> i.getArguments()[0]);
        when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(new RoomAllocationResponse());
        when(waitingListRepository.findByStudent_IdAndStatus(10L, "WAITING")).thenReturn(Optional.of(waitingEntry));

        roomAllocationService.acceptOffer(1L, request);

        assertThat(waitingEntry.getStatus()).isEqualTo("ALLOCATED");
        verify(waitingListRepository).save(waitingEntry);
    }

    @Test
    @DisplayName("Should accept offer without error when no waiting list entry exists")
    void shouldAcceptOfferWithNoWaitingListEntry() {
        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(i -> i.getArguments()[0]);
        when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(new RoomAllocationResponse());
        when(waitingListRepository.findByStudent_IdAndStatus(10L, "WAITING")).thenReturn(Optional.empty());

        roomAllocationService.acceptOffer(1L, request);

        assertThat(allocation.getStatus()).isEqualTo(AllocationStatus.ACCEPTED);
        verify(waitingListRepository, never()).save(any(WaitingListEntry.class));
    }

    @Test
    @DisplayName("Should throw exception when date is in the past")
    void shouldThrowExceptionWhenDateIsInThePast() {
        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));

        request.setExpectedCheckInDate(LocalDateTime.now().minusDays(1));

        assertThatThrownBy(() -> roomAllocationService.acceptOffer(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("within 7 days");
    }

    @Test
    @DisplayName("Should throw exception when date is more than 7 days in future")
    void shouldThrowExceptionWhenDateIsTooFarInFuture() {
        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));

        request.setExpectedCheckInDate(LocalDateTime.now().plusDays(8));

        assertThatThrownBy(() -> roomAllocationService.acceptOffer(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("within 7 days");
    }

    @Test
    @DisplayName("Should throw exception if allocation is not OFFERED")
    void shouldThrowExceptionIfAllocationIsNotOffered() {
        allocation.setStatus(AllocationStatus.ACCEPTED);
        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));

        assertThatThrownBy(() -> roomAllocationService.acceptOffer(1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Can only accept OFFERED");
    }
}
