package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.exceptions.InvalidAllocationStatusTransitionException;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import lombok.experimental.UtilityClass;

@UtilityClass
class AllocationStatusValidator {

    /**
     * Validates that a status transition is allowed according to the
     * AllocationStatus state machine.
     * Throws InvalidAllocationStatusTransitionException if the transition is not
     * valid.
     */
    public static void validate(RoomAllocation allocation, AllocationStatus newStatus) {
        AllocationStatus currentStatus = allocation.getStatus();
        if (currentStatus != null && !currentStatus.canTransitionTo(newStatus)) {
            throw new InvalidAllocationStatusTransitionException(currentStatus, newStatus);
        }
    }
}
