package com.beezhub.student_accommodation.exceptions;

import com.beezhub.student_accommodation.model.enums.AllocationStatus;

public class InvalidAllocationStatusTransitionException extends RuntimeException {
    public InvalidAllocationStatusTransitionException(AllocationStatus from, AllocationStatus to) {
        super(String.format("Invalid allocation status transition from %s to %s", from.getStatus(), to.getStatus()));
    }

    public InvalidAllocationStatusTransitionException(String message) {
        super(message);
    }
}
