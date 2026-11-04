package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.exceptions.RoomAllocationNotFoundException;
import com.beezhub.student_accommodation.exceptions.RoomNotFoundException;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.mapper.StagedAllocationMapper;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.dto.RoomAllocationResponse;
import com.beezhub.student_accommodation.model.dto.RoomAvailabilityResponse;
import com.beezhub.student_accommodation.model.dto.StagedAllocationResponse;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.StagedAllocation;
import com.beezhub.student_accommodation.model.enums.StagedAllocationStatus;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.repository.StagedAllocationRepository;
import com.beezhub.student_accommodation.service.tenant.TenantOwnershipGuard;
import com.beezhub.student_accommodation.specification.RoomAllocationSpecification;
import com.beezhub.student_accommodation.util.PageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RoomAllocationQueryService {

    private final RoomAllocationRepository roomAllocationRepository;
    private final RoomRepository roomRepository;
    private final StagedAllocationRepository stagedAllocationRepository;
    private final RoomAllocationMapper roomAllocationMapper;
    private final StagedAllocationMapper stagedAllocationMapper;
    private final AllocationAccessGuard allocationAccessGuard;
    private final TenantOwnershipGuard tenantOwnershipGuard;

    public List<RoomAllocationResponse> getAllRoomAllocations() {
        log.info("Fetching all room allocations");
        List<RoomAllocation> allocations = roomAllocationRepository.findAllWithDetails();
        List<RoomAllocationResponse> allocationResponses = roomAllocationMapper.toResponseList(allocations);
        log.info("Found {} room allocations", allocationResponses.size());
        return allocationResponses;
    }

    public PageResponse<RoomAllocationResponse> getAllRoomAllocations(Pageable pageable) {
        log.info("Fetching paginated room allocations");
        Page<RoomAllocation> allocations = roomAllocationRepository.findAll(pageable);
        Page<RoomAllocationResponse> responsePage = allocations.map(roomAllocationMapper::toResponse);
        log.info("Found {} room allocations in page", allocations.getNumberOfElements());
        return PageMapper.toPageResponse(responsePage);
    }

    public PageResponse<RoomAllocationResponse> searchRoomAllocations(Long studentId, Long roomId, Long buildingId,
            LocalDateTime fromDate, LocalDateTime toDate,
            Pageable pageable) {
        return searchRoomAllocations(studentId, roomId, buildingId, fromDate, toDate, null, null, null, pageable);
    }

    public PageResponse<RoomAllocationResponse> searchRoomAllocations(Long studentId, Long roomId, Long buildingId,
            LocalDateTime fromDate, LocalDateTime toDate,
            String studentName, String roomNumber, String status,
            Pageable pageable) {
        log.info(
                "Searching room allocations with filters - studentId: {}, roomId: {}, buildingId: {}, fromDate: {}, toDate: {}, studentName: {}, roomNumber: {}, status: {}",
                studentId, roomId, buildingId, fromDate, toDate, studentName, roomNumber, status);
        Specification<RoomAllocation> spec = RoomAllocationSpecification.filterBy(studentId, roomId, buildingId,
                fromDate, toDate, studentName, roomNumber, status);
        Page<RoomAllocation> allocations = roomAllocationRepository.findAll(spec, pageable);
        Page<RoomAllocationResponse> responsePage = allocations.map(roomAllocationMapper::toResponse);
        log.info("Found {} room allocations matching criteria", allocations.getNumberOfElements());
        return PageMapper.toPageResponse(responsePage);
    }

    public RoomAllocationResponse getRoomAllocationById(Long id) {
        log.info("Fetching room allocation with id: {}", id);
        RoomAllocation allocation = roomAllocationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RoomAllocationNotFoundException("Room allocation not found with id: " + id));
        allocationAccessGuard.assertCanAccessAllocation(allocation);
        return withPreviousDeclineCount(allocation);
    }

    public RoomAllocationResponse getActiveAllocationByStudentId(Long studentId) {
        log.info("Fetching active allocation for student: {}", studentId);
        allocationAccessGuard.assertCanAccessStudent(studentId);
        RoomAllocation allocation = roomAllocationRepository.findActiveAllocationByStudentId(studentId)
                .orElseThrow(() -> new RoomAllocationNotFoundException(
                        "No active allocation found for student with id: " + studentId));
        return withPreviousDeclineCount(allocation);
    }

    /**
     * Maps the allocation and adds how many times its application has already been declined.
     * Students need that number to be warned before declining again, and they cannot call the
     * admin-only allocation search to derive it themselves (GH-109).
     */
    private RoomAllocationResponse withPreviousDeclineCount(RoomAllocation allocation) {
        RoomAllocationResponse response = roomAllocationMapper.toResponse(allocation);
        if (allocation.getApplication() != null) {
            response.setPreviousDeclineCount(
                    roomAllocationRepository.countDeclinedAllocationsByApplicationId(allocation.getApplication().getId()));
        }
        return response;
    }

    public boolean isRoomAvailable(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found with id: " + roomId));
        tenantOwnershipGuard.assertReadable(room, "Room", roomId);

        long activeAllocations = roomAllocationRepository.countActiveAllocationsByRoomId(roomId);
        boolean available = activeAllocations < room.getCapacity();

        log.info("Room {} availability check: {} (Current: {}, Capacity: {})",
                room.getRoomNumber(), available, activeAllocations, room.getCapacity());

        return available;
    }

    public int getRoomAvailableCapacity(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found with id: " + roomId));

        long activeAllocations = roomAllocationRepository.countActiveAllocationsByRoomId(roomId);
        int available = room.getCapacity() - (int) activeAllocations;

        log.info("Room {} has {} available spots (Capacity: {}, Occupied: {})",
                room.getRoomNumber(), available, room.getCapacity(), activeAllocations);

        return Math.max(0, available);
    }

    public Room findAvailableRoom() {
        Optional<Room> room = roomRepository.findFirstByStatusOrderByBuilding_IdAscRoomNumberAsc("AVAILABLE");
        room.ifPresent(r -> log.info("Found available room {}", r.getRoomNumber()));
        return room.orElse(null);
    }

    public PageResponse<RoomAllocationResponse> getAllocationsByStatus(String status, Pageable pageable) {
        log.info("Fetching allocations with status: {}", status);

        Specification<RoomAllocation> spec = (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"),
                status);

        Page<RoomAllocation> allocations = roomAllocationRepository.findAll(spec, pageable);
        Page<RoomAllocationResponse> responsePage = allocations.map(roomAllocationMapper::toResponse);

        log.info("Found {} allocations with status: {}", allocations.getNumberOfElements(), status);
        return PageMapper.toPageResponse(responsePage);
    }

    public RoomAvailabilityResponse getRoomAvailability(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found with id: " + roomId));
        tenantOwnershipGuard.assertReadable(room, "Room", roomId);

        long activeAllocations = roomAllocationRepository.countActiveAllocationsByRoomId(roomId);
        int availableSpots = room.getCapacity() - (int) activeAllocations;

        RoomAvailabilityResponse response = new RoomAvailabilityResponse();
        response.setRoomId(room.getId());
        response.setRoomNumber(room.getRoomNumber());
        response.setBuildingName(room.getBuilding() != null ? room.getBuilding().getName() : "");
        response.setCapacity(room.getCapacity());
        response.setCurrentOccupancy((int) activeAllocations);
        response.setAvailableSpots(Math.max(0, availableSpots));
        response.setAvailable(availableSpots > 0);
        response.setStatus(room.getStatus());

        log.info("Room {} availability: {} spots available (Capacity: {}, Occupied: {})",
                room.getRoomNumber(), response.getAvailableSpots(), room.getCapacity(), activeAllocations);

        return response;
    }

    public PageResponse<StagedAllocationResponse> getStagedAllocations(String batchId, StagedAllocationStatus status,
            Pageable pageable) {
        Page<StagedAllocation> page = findStagedAllocations(batchId, status, pageable);
        return PageMapper.toPageResponse(page.map(stagedAllocationMapper::toResponse));
    }

    private Page<StagedAllocation> findStagedAllocations(String batchId, StagedAllocationStatus status,
            Pageable pageable) {
        if (batchId != null && status != null) {
            return stagedAllocationRepository.findByBatchIdAndStatus(batchId, status, pageable);
        } else if (batchId != null) {
            return stagedAllocationRepository.findByBatchId(batchId, pageable);
        } else if (status != null) {
            return stagedAllocationRepository.findByStatus(status, pageable);
        } else {
            return stagedAllocationRepository.findAll(pageable);
        }
    }
}
