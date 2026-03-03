package com.beezhub.student_accommodation.service.notification;

public record AllocationRejectedContext(
        String studentName,
        String roomNumber,
        String buildingName,
        String proposalId,
        String reason,
        Integer waitingListPosition,
        String portalUrl
) implements EmailContext {}
