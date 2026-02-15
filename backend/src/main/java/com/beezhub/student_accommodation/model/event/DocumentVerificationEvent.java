package com.beezhub.student_accommodation.model.event;

import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;

public record DocumentVerificationEvent(Document document, DocumentStatus oldStatus, DocumentStatus newStatus,
        String comment) {
}
