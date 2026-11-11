package com.beezhub.student_accommodation.service.notification;

public record AllocationProposalContext(
        String studentName,
        String roomNumber,
        String buildingName,
        Integer roomCapacity,
        String portalUrl
) implements EmailContext {}
