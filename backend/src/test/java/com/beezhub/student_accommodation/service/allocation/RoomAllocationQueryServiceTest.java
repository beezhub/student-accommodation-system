package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.exceptions.RoomAllocationNotFoundException;
import com.beezhub.student_accommodation.exceptions.RoomNotFoundException;
import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.mapper.StagedAllocationMapper;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.dto.RoomAllocationResponse;
import com.beezhub.student_accommodation.model.dto.RoomAvailabilityResponse;
import com.beezhub.student_accommodation.model.dto.StagedAllocationResponse;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.StagedAllocation;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.StagedAllocationStatus;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.repository.StagedAllocationRepository;
import com.beezhub.student_accommodation.service.tenant.TenantOwnershipGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomAllocationQueryServiceTest {

    @Mock
    private RoomAllocationRepository roomAllocationRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private StagedAllocationRepository stagedAllocationRepository;
    @Mock
    private RoomAllocationMapper roomAllocationMapper;
    @Mock
    private StagedAllocationMapper stagedAllocationMapper;
    @Mock
    private AllocationAccessGuard allocationAccessGuard;

    @Mock
    private TenantOwnershipGuard tenantOwnershipGuard;

    @InjectMocks
    private RoomAllocationQueryService queryService;

    private RoomAllocation allocation;
    private RoomAllocationResponse allocationResponse;
    private Room room;
    private StagedAllocation stagedAllocation;
    private StagedAllocationResponse stagedAllocationResponse;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(queryService, "stayCapacityGuard",
                new com.beezhub.student_accommodation.service.allocation.StayCapacityGuard(roomAllocationRepository,
                        java.time.Clock.fixed(java.time.Instant.parse("2026-11-10T08:00:00Z"),
                                java.time.ZoneId.of("Africa/Johannesburg"))));
        Building building = new Building();
        building.setId(1L);
        building.setName("Building A");

        room = new Room();
        room.setId(1L);
        room.setRoomNumber("101");
        room.setCapacity(2);
        room.setBuilding(building);
        room.setStatus("AVAILABLE");

        allocation = new RoomAllocation();
        allocation.setId(1L);
        allocation.setRoom(room);

        allocationResponse = new RoomAllocationResponse();
        allocationResponse.setId(1L);

        stagedAllocation = new StagedAllocation();
        stagedAllocation.setId(1L);
        stagedAllocation.setBatchId("BATCH1");
        stagedAllocation.setStatus(StagedAllocationStatus.PROPOSED);

        stagedAllocationResponse = new StagedAllocationResponse();
        stagedAllocationResponse.setId(1L);
    }

    @Test
    void getAllRoomAllocations_ReturnsList() {
        when(roomAllocationRepository.findAllWithDetails()).thenReturn(List.of(allocation));
        when(roomAllocationMapper.toResponseList(any())).thenReturn(List.of(allocationResponse));

        List<RoomAllocationResponse> result = queryService.getAllRoomAllocations();

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllRoomAllocations_Paginated_ReturnsPage() {
        Page<RoomAllocation> page = new PageImpl<>(List.of(allocation));
        when(roomAllocationRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(roomAllocationMapper.toResponse(any())).thenReturn(allocationResponse);

        PageResponse<RoomAllocationResponse> result = queryService.getAllRoomAllocations(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void searchRoomAllocations_ReturnsPage() {
        Page<RoomAllocation> page = new PageImpl<>(List.of(allocation));
        when(roomAllocationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(roomAllocationMapper.toResponse(any())).thenReturn(allocationResponse);

        PageResponse<RoomAllocationResponse> result = queryService.searchRoomAllocations(1L, 1L, 1L, LocalDateTime.now(), LocalDateTime.now(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getRoomAllocationById_callerNotOwner_throwsAndNothingReturned() {
        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));
        doThrow(new UnauthorizedAccessException("not yours"))
                .when(allocationAccessGuard).assertCanAccessAllocation(allocation);

        assertThrows(UnauthorizedAccessException.class, () -> queryService.getRoomAllocationById(1L));

        verify(roomAllocationMapper, never()).toResponse(any(RoomAllocation.class));
    }

    @Test
    void getActiveAllocationByStudentId_callerNotThatStudent_throwsBeforeQuery() {
        doThrow(new UnauthorizedAccessException("not yours"))
                .when(allocationAccessGuard).assertCanAccessStudent(42L);

        assertThrows(UnauthorizedAccessException.class, () -> queryService.getActiveAllocationByStudentId(42L));

        verify(roomAllocationRepository, never()).findLiveAllocationsByStudentId(any());
    }

    @Test
    void getActiveAllocationByStudentId_withApplication_populatesPreviousDeclineCount() {
        // GH-109: students cannot call the admin allocation search, so the count travels on the
        // allocation itself and the decline dialog can warn before a second or third decline.
        Application application = new Application();
        application.setId(7L);
        allocation.setApplication(application);
        when(roomAllocationRepository.findLiveAllocationsByStudentId(1L)).thenReturn(List.of(allocation));
        when(roomAllocationMapper.toResponse(allocation)).thenReturn(allocationResponse);
        when(roomAllocationRepository.countDeclinedAllocationsByApplicationId(7L)).thenReturn(2L);

        RoomAllocationResponse result = queryService.getActiveAllocationByStudentId(1L);

        assertThat(result.getPreviousDeclineCount()).isEqualTo(2L);
    }

    @Test
    void getRoomAllocationById_withApplication_populatesPreviousDeclineCount() {
        Application application = new Application();
        application.setId(7L);
        allocation.setApplication(application);
        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));
        when(roomAllocationMapper.toResponse(allocation)).thenReturn(allocationResponse);
        when(roomAllocationRepository.countDeclinedAllocationsByApplicationId(7L)).thenReturn(1L);

        assertThat(queryService.getRoomAllocationById(1L).getPreviousDeclineCount()).isEqualTo(1L);
    }

    @Test
    void getActiveAllocationByStudentId_withoutApplication_declineCountIsZeroAndNotQueried() {
        allocation.setApplication(null);
        when(roomAllocationRepository.findLiveAllocationsByStudentId(1L)).thenReturn(List.of(allocation));
        when(roomAllocationMapper.toResponse(allocation)).thenReturn(allocationResponse);

        RoomAllocationResponse result = queryService.getActiveAllocationByStudentId(1L);

        assertThat(result.getPreviousDeclineCount()).isZero();
        verify(roomAllocationRepository, never()).countDeclinedAllocationsByApplicationId(any());
    }

    @Test
    void getRoomAllocationById_Existing_ReturnsResponse() {
        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));
        when(roomAllocationMapper.toResponse(any())).thenReturn(allocationResponse);

        RoomAllocationResponse result = queryService.getRoomAllocationById(1L);

        assertThat(result).isNotNull();
    }

    @Test
    void getRoomAllocationById_NotFound_ThrowsException() {
        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.empty());

        assertThrows(RoomAllocationNotFoundException.class, () -> queryService.getRoomAllocationById(1L));
    }

    @Test
    void getActiveAllocationByStudentId_Existing_ReturnsResponse() {
        when(roomAllocationRepository.findLiveAllocationsByStudentId(1L)).thenReturn(List.of(allocation));
        when(roomAllocationMapper.toResponse(any())).thenReturn(allocationResponse);

        RoomAllocationResponse result = queryService.getActiveAllocationByStudentId(1L);

        assertThat(result).isNotNull();
    }

    @Test
    void getActiveAllocationByStudentId_NotFound_ThrowsException() {
        when(roomAllocationRepository.findLiveAllocationsByStudentId(1L)).thenReturn(List.of());

        assertThrows(RoomAllocationNotFoundException.class, () -> queryService.getActiveAllocationByStudentId(1L));
    }

    @Test
    void isRoomAvailable_RoomExistsAndAvailable_ReturnsTrue() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomAllocationRepository.countHoldingBeds(eq(1L), any(), any())).thenReturn(1L); // capacity is 2

        boolean result = queryService.isRoomAvailable(1L);

        assertThat(result).isTrue();
    }
    
    @Test
    void isRoomAvailable_RoomExistsAndFull_ReturnsFalse() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomAllocationRepository.countHoldingBeds(eq(1L), any(), any())).thenReturn(2L); // capacity is 2

        boolean result = queryService.isRoomAvailable(1L);

        assertThat(result).isFalse();
    }

    @Test
    void isRoomAvailable_NotFound_ThrowsException() {
        when(roomRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RoomNotFoundException.class, () -> queryService.isRoomAvailable(1L));
    }

    @Test
    void getRoomAvailableCapacity_ReturnsCorrectValue() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomAllocationRepository.countHoldingBeds(eq(1L), any(), any())).thenReturn(1L);

        int result = queryService.getRoomAvailableCapacity(1L);

        assertThat(result).isEqualTo(1);
    }

    @Test
    void findAvailableRoom_ReturnsRoom() {
        when(roomRepository.findFirstByStatusOrderByBuilding_IdAscRoomNumberAsc("AVAILABLE")).thenReturn(Optional.of(room));

        Room result = queryService.findAvailableRoom();

        assertThat(result).isEqualTo(room);
    }
    
    @Test
    void findAvailableRoom_ReturnsNullIfNotFound() {
        when(roomRepository.findFirstByStatusOrderByBuilding_IdAscRoomNumberAsc("AVAILABLE")).thenReturn(Optional.empty());

        Room result = queryService.findAvailableRoom();

        assertThat(result).isNull();
    }

    @Test
    void getAllocationsByStatus_ReturnsPage() {
        Page<RoomAllocation> page = new PageImpl<>(List.of(allocation));
        when(roomAllocationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(roomAllocationMapper.toResponse(any())).thenReturn(allocationResponse);

        PageResponse<RoomAllocationResponse> result = queryService.getAllocationsByStatus("ACTIVE", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getRoomAvailability_ReturnsAvailabilityResponse() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomAllocationRepository.countHoldingBeds(eq(1L), any(), any())).thenReturn(1L);

        RoomAvailabilityResponse result = queryService.getRoomAvailability(1L);

        assertThat(result.getRoomId()).isEqualTo(1L);
        assertThat(result.getAvailableSpots()).isEqualTo(1);
        assertThat(result.isAvailable()).isTrue();
    }

    @Test
    void getStagedAllocations_WithBatchIdAndStatus() {
        Page<StagedAllocation> page = new PageImpl<>(List.of(stagedAllocation));
        when(stagedAllocationRepository.findByBatchIdAndStatus(eq("BATCH1"), eq(StagedAllocationStatus.PROPOSED), any(Pageable.class))).thenReturn(page);
        when(stagedAllocationMapper.toResponse(any())).thenReturn(stagedAllocationResponse);

        PageResponse<StagedAllocationResponse> result = queryService.getStagedAllocations("BATCH1", StagedAllocationStatus.PROPOSED, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getStagedAllocations_WithBatchIdOnly() {
        Page<StagedAllocation> page = new PageImpl<>(List.of(stagedAllocation));
        when(stagedAllocationRepository.findByBatchId(eq("BATCH1"), any(Pageable.class))).thenReturn(page);
        when(stagedAllocationMapper.toResponse(any())).thenReturn(stagedAllocationResponse);

        PageResponse<StagedAllocationResponse> result = queryService.getStagedAllocations("BATCH1", null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getStagedAllocations_WithStatusOnly() {
        Page<StagedAllocation> page = new PageImpl<>(List.of(stagedAllocation));
        when(stagedAllocationRepository.findByStatus(eq(StagedAllocationStatus.PROPOSED), any(Pageable.class))).thenReturn(page);
        when(stagedAllocationMapper.toResponse(any())).thenReturn(stagedAllocationResponse);

        PageResponse<StagedAllocationResponse> result = queryService.getStagedAllocations(null, StagedAllocationStatus.PROPOSED, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getStagedAllocations_NoFilters() {
        Page<StagedAllocation> page = new PageImpl<>(List.of(stagedAllocation));
        when(stagedAllocationRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(stagedAllocationMapper.toResponse(any())).thenReturn(stagedAllocationResponse);

        PageResponse<StagedAllocationResponse> result = queryService.getStagedAllocations(null, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    /** GH-496: a resident with an offer for next year sees the offer first, since it lapses within days. */
    @Test
    void getActiveAllocationByStudentId_residentWithOfferForNextYear_returnsTheOffer() {
        RoomAllocation living = new RoomAllocation();
        living.setId(10L);
        living.setStatus(AllocationStatus.ACTIVE);
        living.setStayStartDate(java.time.LocalDate.of(2026, 2, 1));
        living.setStayEndDate(java.time.LocalDate.of(2026, 11, 30));
        RoomAllocation offer = new RoomAllocation();
        offer.setId(11L);
        offer.setStatus(AllocationStatus.OFFERED);
        offer.setStayStartDate(java.time.LocalDate.of(2027, 2, 1));
        offer.setStayEndDate(java.time.LocalDate.of(2027, 11, 30));
        when(roomAllocationRepository.findLiveAllocationsByStudentId(1L)).thenReturn(List.of(living, offer));
        when(roomAllocationMapper.toResponse(offer)).thenReturn(new RoomAllocationResponse());

        queryService.getActiveAllocationByStudentId(1L);

        verify(roomAllocationMapper).toResponse(offer);
    }

    /** GH-496: overdue check-outs are asked for with today's date, longest overdue first. */
    @Test
    void getOverdueCheckOuts_ordersByStayEnd() {
        RoomAllocation overdue = new RoomAllocation();
        overdue.setId(20L);
        when(roomAllocationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(overdue)));
        when(roomAllocationMapper.toResponse(overdue)).thenReturn(new RoomAllocationResponse());

        var page = queryService.getOverdueCheckOuts(org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        org.mockito.ArgumentCaptor<org.springframework.data.domain.Pageable> pageable =
                org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(roomAllocationRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class),
                pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("stayEndDate")).isNotNull();
    }
}
