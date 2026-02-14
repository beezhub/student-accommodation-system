package com.beezhub.student_accommodation.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AllocationFreedEvent extends ApplicationEvent {

    private final Long roomId;

    public AllocationFreedEvent(Object source, Long roomId) {
        super(source);
        this.roomId = roomId;
    }
}
