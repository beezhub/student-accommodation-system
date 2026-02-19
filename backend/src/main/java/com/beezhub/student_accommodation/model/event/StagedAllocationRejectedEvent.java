package com.beezhub.student_accommodation.model.event;

import com.beezhub.student_accommodation.model.entity.StagedAllocation;

public record StagedAllocationRejectedEvent(StagedAllocation stagedAllocation, String reason) {
}
