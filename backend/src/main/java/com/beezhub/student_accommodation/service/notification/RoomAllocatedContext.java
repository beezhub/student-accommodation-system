package com.beezhub.student_accommodation.service.notification;

public record RoomAllocatedContext(
        String studentName,
        String roomNumber,
        String buildingName,
        Integer floorNumber,
        String roomType,
        String bedNumber,
        String allocationId,
        String startDate,
        String endDate,
        String duration,
        String officeLocation,
        String buildingAddress,
        String portalUrl,
        String offerExpiryDate
) implements EmailContext {}
