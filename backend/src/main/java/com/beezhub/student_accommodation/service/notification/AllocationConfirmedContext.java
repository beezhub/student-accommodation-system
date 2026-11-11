package com.beezhub.student_accommodation.service.notification;

public record AllocationConfirmedContext(
        String studentName,
        String roomNumber,
        String buildingName,
        String roomType,
        String allocationId,
        String moveInDate,
        String startDate,
        String endDate,
        String portalUrl
) implements EmailContext {}
