package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.*;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.enums.StagedAllocationStatus;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.service.RoomAllocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomAllocationControllerTest {

    @Mock
    private RoomAllocationService roomAllocationService;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private RoomAllocationController controller;

    private AppUser adminUser;
    private RoomAllocationResponse allocationResponse;

    @BeforeEach
    void setUp() {
        adminUser = new AppUser();
        adminUser.setId(100L);
        adminUser.setEmail("admin@test.com");

        allocationResponse = new RoomAllocationResponse();
        allocationResponse.setId(1L);
    }

    private void mockAdmin() {
        when(authentication.getName()).thenReturn("admin@test.com");
        when(appUserRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
    }

    @Test
    void getAllRoomAllocations() {
        PageResponse<RoomAllocationResponse> page = new PageResponse<>();
        when(roomAllocationService.getAllRoomAllocations(any(Pageable.class))).thenReturn(page);

        ResponseEntity<PageResponse<RoomAllocationResponse>> response = controller.getAllRoomAllocations(Pageable.unpaged());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(page);
    }

    @Test
    void getAllRoomAllocationsNoPagination() {
        when(roomAllocationService.getAllRoomAllocations()).thenReturn(List.of(allocationResponse));

        ResponseEntity<List<RoomAllocationResponse>> response = controller.getAllRoomAllocationsNoPagination();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void searchRoomAllocations() {
        PageResponse<RoomAllocationResponse> page = new PageResponse<>();
        when(roomAllocationService.searchRoomAllocations(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(page);

        ResponseEntity<PageResponse<RoomAllocationResponse>> response = controller.searchRoomAllocations(
                1L, 2L, 3L, LocalDateTime.now(), LocalDateTime.now(), "John", "101", "ACTIVE", 4L, 5L, Pageable.unpaged());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getRoomAllocationById() {
        when(roomAllocationService.getRoomAllocationById(1L)).thenReturn(allocationResponse);

        ResponseEntity<RoomAllocationResponse> response = controller.getRoomAllocationById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(allocationResponse);
    }

    @Test
    void createRoomAllocation() {
        RoomAllocationRequest request = new RoomAllocationRequest();
        when(roomAllocationService.createRoomAllocation(request)).thenReturn(allocationResponse);

        ResponseEntity<RoomAllocationResponse> response = controller.createRoomAllocation(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void updateRoomAllocation() {
        RoomAllocationRequest request = new RoomAllocationRequest();
        when(roomAllocationService.updateRoomAllocation(1L, request)).thenReturn(allocationResponse);

        ResponseEntity<RoomAllocationResponse> response = controller.updateRoomAllocation(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void acceptOffer() {
        AcceptOfferRequest request = new AcceptOfferRequest();
        when(roomAllocationService.acceptOffer(1L, request)).thenReturn(allocationResponse);

        ResponseEntity<RoomAllocationResponse> response = controller.acceptOffer(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void declineOffer() {
        DeclineOfferRequest request = new DeclineOfferRequest();
        when(roomAllocationService.declineOffer(1L, request)).thenReturn(allocationResponse);

        ResponseEntity<RoomAllocationResponse> response = controller.declineOffer(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void checkIn() {
        when(roomAllocationService.confirmCheckIn(1L, false)).thenReturn(allocationResponse);

        ResponseEntity<RoomAllocationResponse> response = controller.checkIn(1L, false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void checkOut() {
        when(roomAllocationService.confirmCheckOut(1L)).thenReturn(allocationResponse);

        ResponseEntity<RoomAllocationResponse> response = controller.checkOut(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteRoomAllocation() {
        ResponseEntity<Void> response = controller.deleteRoomAllocation(1L);

        verify(roomAllocationService).deleteRoomAllocation(1L);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void endRoomAllocation() {
        when(roomAllocationService.endAllocation(1L)).thenReturn(allocationResponse);

        ResponseEntity<RoomAllocationResponse> response = controller.endRoomAllocation(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getActiveAllocationByStudentId() {
        when(roomAllocationService.getActiveAllocationByStudentId(1L)).thenReturn(allocationResponse);

        ResponseEntity<RoomAllocationResponse> response = controller.getActiveAllocationByStudentId(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void allocateRoomByApplication() {
        AllocateByApplicationRequest request = new AllocateByApplicationRequest();
        request.setApplicationId(1L);
        request.setRoomId(2L);
        when(roomAllocationService.allocateRoomByApplication(1L, 2L)).thenReturn(allocationResponse);

        ResponseEntity<RoomAllocationResponse> response = controller.allocateRoomByApplication(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void getAllocationsByStatus() {
        PageResponse<RoomAllocationResponse> page = new PageResponse<>();
        when(roomAllocationService.getAllocationsByStatus(eq("ACTIVE"), any(Pageable.class))).thenReturn(page);

        ResponseEntity<PageResponse<RoomAllocationResponse>> response = controller.getAllocationsByStatus("ACTIVE", Pageable.unpaged());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getRoomAvailability() {
        RoomAvailabilityResponse availability = new RoomAvailabilityResponse();
        when(roomAllocationService.getRoomAvailability(1L)).thenReturn(availability);

        ResponseEntity<RoomAvailabilityResponse> response = controller.getRoomAvailability(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void isRoomAvailable() {
        when(roomAllocationService.isRoomAvailable(1L)).thenReturn(true);

        ResponseEntity<Boolean> response = controller.isRoomAvailable(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isTrue();
    }

    @Test
    void checkStudentEligibility() {
        AllocationEligibilityResponse eligibility = new AllocationEligibilityResponse();
        when(roomAllocationService.checkStudentEligibility(1L)).thenReturn(eligibility);

        ResponseEntity<AllocationEligibilityResponse> response = controller.checkStudentEligibility(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getEligibleRoomsForStudent() {
        when(roomAllocationService.getEligibleRoomsForStudent(1L, 2L)).thenReturn(List.of(new EligibleRoomResponse()));

        ResponseEntity<List<EligibleRoomResponse>> response = controller.getEligibleRoomsForStudent(1L, 2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getAllocationHistory() {
        when(roomAllocationService.getAllocationHistory(1L)).thenReturn(List.of(new AllocationAuditLogResponse()));

        ResponseEntity<List<AllocationAuditLogResponse>> response = controller.getAllocationHistory(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}