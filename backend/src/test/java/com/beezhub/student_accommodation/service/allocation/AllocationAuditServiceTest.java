package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.dto.AllocationAuditLogResponse;
import com.beezhub.student_accommodation.model.entity.AllocationAuditLog;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.enums.AllocationAuditActionType;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.repository.AllocationAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllocationAuditServiceTest {

    @Mock
    private AllocationAuditLogRepository allocationAuditLogRepository;

    private AllocationAuditService allocationAuditService;

    @BeforeEach
    void setUp() {
        allocationAuditService = new AllocationAuditService(allocationAuditLogRepository);
    }

    @Test
    void recordTransition_statusChange_historyCreated() {
        // Arrange
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(10L);

        when(allocationAuditLogRepository.save(any(AllocationAuditLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        allocationAuditService.logStatusChange(allocation, AllocationStatus.OFFERED, AllocationStatus.ACCEPTED);

        // Assert — an audit log entry was saved with the correct status values
        ArgumentCaptor<AllocationAuditLog> captor = ArgumentCaptor.forClass(AllocationAuditLog.class);
        verify(allocationAuditLogRepository).save(captor.capture());

        AllocationAuditLog saved = captor.getValue();
        assertThat(saved.getActionType()).isEqualTo(AllocationAuditActionType.STATUS_CHANGE);
        assertThat(saved.getPreviousStatus()).isEqualTo(AllocationStatus.OFFERED);
        assertThat(saved.getNewStatus()).isEqualTo(AllocationStatus.ACCEPTED);
        assertThat(saved.getAllocation()).isEqualTo(allocation);
    }

    @Test
    void getHistory_multipleTransitions_orderedByTimestamp() {
        // Arrange
        Long allocationId = 5L;

        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(allocationId);

        AllocationAuditLog entry1 = AllocationAuditLog.builder()
                .id(1L)
                .allocation(allocation)
                .actionType(AllocationAuditActionType.STATUS_CHANGE)
                .previousStatus(AllocationStatus.OFFERED)
                .newStatus(AllocationStatus.ACCEPTED)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .build();

        AllocationAuditLog entry2 = AllocationAuditLog.builder()
                .id(2L)
                .allocation(allocation)
                .actionType(AllocationAuditActionType.STATUS_CHANGE)
                .previousStatus(AllocationStatus.ACCEPTED)
                .newStatus(AllocationStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();

        // Repository returns newest-first (DESC order), matching findByAllocation_IdOrderByCreatedAtDesc
        when(allocationAuditLogRepository.findByAllocation_IdOrderByCreatedAtDesc(
                eq(allocationId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entry2, entry1)));

        // Act
        List<AllocationAuditLogResponse> history = allocationAuditService.getHistory(allocationId);

        // Assert — two entries returned, newest first
        assertThat(history).hasSize(2);

        AllocationAuditLogResponse newest = history.get(0);
        assertThat(newest.getPreviousStatus()).isEqualTo("ACCEPTED");
        assertThat(newest.getNewStatus()).isEqualTo("ACTIVE");

        AllocationAuditLogResponse oldest = history.get(1);
        assertThat(oldest.getPreviousStatus()).isEqualTo("OFFERED");
        assertThat(oldest.getNewStatus()).isEqualTo("ACCEPTED");
    }
}
