package com.beezhub.student_accommodation.service.notification;

public record AllocationMovedContext(
        String studentName,
        String oldRoomNumber,
        String oldBuildingName,
        String newRoomNumber,
        String newBuildingName,
        String newRoomType,
        String effectiveDate,
        String reason,
        String portalUrl
) implements EmailContext {}
