package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * Who is a returning resident (GH-499): a student who was checked in at the same company in an
 * earlier year or is living there now. That is a CHECKED_IN application, or an ACTIVE, COMPLETED or
 * TERMINATED allocation, with that company. A lease terminated early still counts; the flag records
 * history. It has no effect on scoring, windows or rooms.
 *
 * <p>The company is passed explicitly: the student submitting is a platform-tenant user, so the
 * tenant filter does nothing here.
 */
@Component
@RequiredArgsConstructor
public class ReturningResidentPolicy {

    static final Set<AllocationStatus> LIVED_THERE =
            EnumSet.of(AllocationStatus.ACTIVE, AllocationStatus.COMPLETED, AllocationStatus.TERMINATED);

    private final ApplicationRepository applicationRepository;
    private final RoomAllocationRepository roomAllocationRepository;

    public boolean isReturning(Long studentId, Long companyId) {
        if (studentId == null || companyId == null) {
            return false;
        }
        return applicationRepository.existsByStudent_IdAndTenantIdAndStatus(studentId, companyId,
                        ApplicationStatus.CHECKED_IN)
                || roomAllocationRepository.existsByStudent_IdAndTenantIdAndStatusIn(studentId, companyId, LIVED_THERE);
    }
}
