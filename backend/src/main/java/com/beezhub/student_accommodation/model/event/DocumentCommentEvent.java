package com.beezhub.student_accommodation.model.event;

import com.beezhub.student_accommodation.model.entity.Document;

public record DocumentCommentEvent(Document document, String comment, String authorName) {
}
