package com.beezhub.student_accommodation.service.notification;

import java.math.BigDecimal;
import java.util.List;

public record AllocationProposalContext(
        String studentName,
        String roomNumber,
        String buildingName,
        Integer floorNumber,
        Integer roomCapacity,
        String proposalId,
        BigDecimal score,
        List<String> warnings,
        String moveInDate,
        String portalUrl
) implements EmailContext {}
