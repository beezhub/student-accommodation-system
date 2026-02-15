package com.beezhub.student_accommodation.model.event;

import com.beezhub.student_accommodation.model.entity.StagedAllocation;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;

public record StagedAllocationConfirmedEvent(StagedAllocation stagedAllocation, RoomAllocation roomAllocation) {
}
