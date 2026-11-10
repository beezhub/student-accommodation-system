package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GH-136: occupancy was a blind delta applied to the in-memory value, while every capacity decision
 * read the live allocation count. The two had no relationship and drifted three ways — the delta
 * went negative, it overwrote MAINTENANCE, and nothing ever recomputed so no path self-healed.
 *
 * <p>Deriving from the count makes this convergent: any call fixes whatever the previous state was,
 * which is the property each of these tests exercises.
 */
@ExtendWith(MockitoExtension.class)
class RoomOccupancyManagerTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomAllocationRepository roomAllocationRepository;

    /** 10 November 2026, 10:00 in South Africa. */
    @org.mockito.Spy
    private java.time.Clock clock = java.time.Clock.fixed(
            java.time.Instant.parse("2026-11-10T08:00:00Z"), java.time.ZoneId.of("Africa/Johannesburg"));

    @InjectMocks
    private RoomOccupancyManager roomOccupancyManager;

    private Room room(int capacity, Integer currentOccupancy, String status) {
        Room room = new Room();
        room.setId(1L);
        room.setRoomNumber("101");
        room.setCapacity(capacity);
        room.setCurrentOccupancy(currentOccupancy);
        room.setStatus(status);
        return room;
    }

    private void activeAllocations(long count) {
        when(roomAllocationRepository.countInRoomOn(1L, java.time.LocalDate.of(2026, 11, 10))).thenReturn(count);
    }

    @Test
    void syncOccupancy_noAllocations_isAvailable() {
        Room room = room(2, 1, "OCCUPIED");
        activeAllocations(0);

        roomOccupancyManager.syncOccupancy(room);

        assertThat(room.getCurrentOccupancy()).isZero();
        assertThat(room.getStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    void syncOccupancy_belowCapacity_isOccupied() {
        Room room = room(3, 0, "AVAILABLE");
        activeAllocations(1);

        roomOccupancyManager.syncOccupancy(room);

        assertThat(room.getCurrentOccupancy()).isEqualTo(1);
        assertThat(room.getStatus()).isEqualTo("OCCUPIED");
    }

    @Test
    void syncOccupancy_atCapacity_isFull() {
        Room room = room(2, 1, "OCCUPIED");
        activeAllocations(2);

        roomOccupancyManager.syncOccupancy(room);

        assertThat(room.getStatus()).isEqualTo("FULL");
    }

    /**
     * The negative-occupancy defect. A second check-out on an already-completed allocation
     * decremented again, because the status machine permits a self-transition, leaving an empty
     * room at -1 that read as OCCUPIED because -1 is neither 0 nor at capacity.
     */
    @Test
    void syncOccupancy_previouslyNegative_healsToZeroAndAvailable() {
        Room room = room(1, -1, "OCCUPIED");
        activeAllocations(0);

        roomOccupancyManager.syncOccupancy(room);

        assertThat(room.getCurrentOccupancy()).isZero();
        assertThat(room.getStatus()).isEqualTo("AVAILABLE");
    }

    /**
     * The delta cleared MAINTENANCE, so a decline handed a room an admin had parked back to
     * AVAILABLE and into the scoring pool.
     */
    @Test
    void syncOccupancy_maintenanceRoom_keepsMaintenance() {
        Room room = room(2, 1, "MAINTENANCE");
        activeAllocations(0);

        roomOccupancyManager.syncOccupancy(room);

        assertThat(room.getStatus()).isEqualTo("MAINTENANCE");
        assertThat(room.getCurrentOccupancy()).isZero();
    }

    /** Over capacity is a real state to record, not one to throw on mid-transaction. */
    @Test
    void syncOccupancy_overCapacity_recordsTheCountAndStaysFull() {
        Room room = room(2, 2, "FULL");
        activeAllocations(3);

        roomOccupancyManager.syncOccupancy(room);

        assertThat(room.getCurrentOccupancy()).isEqualTo(3);
        assertThat(room.getStatus()).isEqualTo("FULL");
    }

    /**
     * Room carries a version column, so writing an unchanged row would bump it and create
     * avoidable optimistic-lock contention. This is also what makes a repeated call free.
     */
    @Test
    void syncOccupancy_nothingChanged_doesNotWrite() {
        Room room = room(2, 1, "OCCUPIED");
        activeAllocations(1);

        roomOccupancyManager.syncOccupancy(room);

        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void syncOccupancy_nullRoom_doesNothing() {
        roomOccupancyManager.syncOccupancy(null);

        verify(roomRepository, never()).save(any(Room.class));
    }

    /** An unsaved room has no id to count against. */
    @Test
    void syncOccupancy_roomWithoutAnId_doesNothing() {
        Room room = new Room();

        roomOccupancyManager.syncOccupancy(room);

        verify(roomRepository, never()).save(any(Room.class));
    }

    /**
     * GH-496: occupancy is who is in the room today. The query is asked for today's date in South
     * African time, so a 2027 offer (counted only from its move-in day) does not fill the room now.
     */
    @Test
    void syncOccupancy_countsOnlyStaysThatIncludeToday() {
        Room room = room(1, 0, "AVAILABLE");
        when(roomAllocationRepository.countInRoomOn(eq(1L), any())).thenReturn(0L);

        roomOccupancyManager.syncOccupancy(room);

        verify(roomAllocationRepository).countInRoomOn(1L, java.time.LocalDate.of(2026, 11, 10));
        assertThat(room.getCurrentOccupancy()).isZero();
        assertThat(room.getStatus()).isEqualTo("AVAILABLE");
    }
}
