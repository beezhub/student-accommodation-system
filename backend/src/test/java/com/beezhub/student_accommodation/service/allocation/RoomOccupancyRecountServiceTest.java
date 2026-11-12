package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GH-541: an accepted place starts and stops counting on its stay dates, but nothing wrote to the
 * room on those dates, and check-in never recounted at all. The nightly job recounts every room.
 */
@ExtendWith(MockitoExtension.class)
class RoomOccupancyRecountServiceTest {

    @Mock
    private RoomRepository roomRepository;
    @Mock
    private RoomOccupancyManager roomOccupancyManager;

    private RoomOccupancyRecountService service;

    @BeforeEach
    void setUp() {
        service = new RoomOccupancyRecountService(roomRepository, roomOccupancyManager, directTemplate());
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void recountAll_everyRoom_syncsEachRoomAndCountsCorrections() {
        Room first = room(1L);
        Room second = room(2L);
        Room third = room(3L);
        when(roomRepository.findAllIds()).thenReturn(List.of(1L, 2L, 3L));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(first));
        when(roomRepository.findById(2L)).thenReturn(Optional.of(second));
        when(roomRepository.findById(3L)).thenReturn(Optional.of(third));
        when(roomOccupancyManager.syncOccupancy(first)).thenReturn(true);
        when(roomOccupancyManager.syncOccupancy(second)).thenReturn(false);
        when(roomOccupancyManager.syncOccupancy(third)).thenReturn(true);

        int corrected = service.recountAll();

        assertThat(corrected).isEqualTo(2);
        verify(roomOccupancyManager).syncOccupancy(first);
        verify(roomOccupancyManager).syncOccupancy(second);
        verify(roomOccupancyManager).syncOccupancy(third);
    }

    @Test
    void recountAll_oneRoomFails_otherRoomsStillRecounted() {
        Room first = room(1L);
        Room second = room(2L);
        when(roomRepository.findAllIds()).thenReturn(List.of(1L, 2L));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(first));
        when(roomRepository.findById(2L)).thenReturn(Optional.of(second));
        when(roomOccupancyManager.syncOccupancy(first)).thenThrow(new RuntimeException("lock timeout"));
        when(roomOccupancyManager.syncOccupancy(second)).thenReturn(true);

        int corrected = service.recountAll();

        assertThat(corrected).isEqualTo(1);
        verify(roomOccupancyManager).syncOccupancy(second);
    }

    @Test
    void recountAll_roomDeletedMeanwhile_skipped() {
        when(roomRepository.findAllIds()).thenReturn(List.of(9L));
        when(roomRepository.findById(9L)).thenReturn(Optional.empty());

        assertThat(service.recountAll()).isZero();
        verify(roomOccupancyManager, never()).syncOccupancy(any());
    }

    /** Platform maintenance: every company's rooms, so no tenant filter during the run. */
    @Test
    void recountAll_tenantContextSet_runsWithoutTenantAndRestoresIt() {
        TenantContext.setTenantId(7L);
        Room room = room(1L);
        AtomicReference<Long> tenantDuringSync = new AtomicReference<>(-1L);
        when(roomRepository.findAllIds()).thenReturn(List.of(1L));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomOccupancyManager.syncOccupancy(room)).thenAnswer(invocation -> {
            tenantDuringSync.set(TenantContext.getTenantId());
            return true;
        });

        service.recountAll();

        assertThat(tenantDuringSync.get()).isNull();
        assertThat(TenantContext.getTenantId()).isEqualTo(7L);
    }

    @Test
    void recountAll_noTenantContext_leavesNoneBehind() {
        when(roomRepository.findAllIds()).thenReturn(List.of());

        assertThat(service.recountAll()).isZero();
        assertThat(TenantContext.getTenantId()).isNull();
    }

    private static Room room(Long id) {
        Room room = new Room();
        room.setId(id);
        return room;
    }

    /** Runs the callback inline, so the job's looping is what is under test, not Spring's TX. */
    private static TransactionTemplate directTemplate() {
        return new TransactionTemplate() {
            @Override
            public <T> T execute(TransactionCallback<T> action) {
                return action.doInTransaction(new SimpleTransactionStatus());
            }
        };
    }
}
