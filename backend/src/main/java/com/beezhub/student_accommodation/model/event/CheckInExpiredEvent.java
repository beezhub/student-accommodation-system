package com.beezhub.student_accommodation.model.event;

import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CheckInExpiredEvent {
    private final RoomAllocation allocation;
}
