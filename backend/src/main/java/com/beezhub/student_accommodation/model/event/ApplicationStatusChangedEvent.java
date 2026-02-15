package com.beezhub.student_accommodation.model.event;

import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;

public record ApplicationStatusChangedEvent(Application application, ApplicationStatus oldStatus,
        ApplicationStatus newStatus) {
}
