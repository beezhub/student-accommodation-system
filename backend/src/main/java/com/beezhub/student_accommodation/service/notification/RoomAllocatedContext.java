package com.beezhub.student_accommodation.service.notification;

public record RoomAllocatedContext(
        String studentName,
        String roomNumber,
        String buildingName,
        String roomType,
        String startDate,
        String endDate,
        String duration,
        String buildingAddress,
        String portalUrl,
        String offerExpiryDate,
        String mapsUrl
) implements EmailContext {}
