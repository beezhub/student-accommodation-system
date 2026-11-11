package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReturningResidentPolicyTest {

    @Mock ApplicationRepository applicationRepository;
    @Mock RoomAllocationRepository roomAllocationRepository;
    @InjectMocks ReturningResidentPolicy policy;

    @Test
    void isReturning_checkedInApplication_withoutAskingAboutRooms() {
        when(applicationRepository.existsByStudent_IdAndTenantIdAndStatus(1L, 7L, ApplicationStatus.CHECKED_IN))
                .thenReturn(true);

        assertThat(policy.isReturning(1L, 7L)).isTrue();
        verifyNoInteractions(roomAllocationRepository);
    }

    @Test
    void isReturning_readsActiveCompletedAndTerminatedAllocations() {
        when(roomAllocationRepository.existsByStudent_IdAndTenantIdAndStatusIn(any(), any(), any())).thenReturn(true);

        assertThat(policy.isReturning(1L, 7L)).isTrue();
        verify(roomAllocationRepository).existsByStudent_IdAndTenantIdAndStatusIn(1L, 7L,
                EnumSet.of(AllocationStatus.ACTIVE, AllocationStatus.COMPLETED, AllocationStatus.TERMINATED));
    }

    @Test
    void isReturning_neither_isFalse() {
        assertThat(policy.isReturning(1L, 7L)).isFalse();
    }

    @Test
    void isReturning_missingIds_isFalse() {
        assertThat(policy.isReturning(null, 7L)).isFalse();
        assertThat(policy.isReturning(1L, null)).isFalse();
        verifyNoInteractions(applicationRepository, roomAllocationRepository);
    }
}
