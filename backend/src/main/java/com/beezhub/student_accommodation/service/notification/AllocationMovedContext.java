package com.beezhub.student_accommodation.service.notification;

public record AllocationMovedContext(
        String studentName,
        String oldRoomNumber,
        String oldBuildingName,
        Integer oldFloorNumber,
        String newRoomNumber,
        String newBuildingName,
        Integer newFloorNumber,
        String newRoomType,
        String effectiveDate,
        String reason,
        String portalUrl
) implements EmailContext {}
