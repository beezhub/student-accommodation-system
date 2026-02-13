package com.beezhub.student_accommodation.model.dto;

import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.WaitingListEntry;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AllocationPair {
    private WaitingListEntry waitingListEntry;
    private ScoredRoom scoredRoom;

    public Student getStudent() {
        return waitingListEntry.getStudent();
    }

    public Application getApplication() {
        return waitingListEntry.getApplication();
    }

    public Long getRoomId() {
        return scoredRoom.getRoomId();
    }
}
