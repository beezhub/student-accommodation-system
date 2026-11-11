package com.beezhub.student_accommodation.model.event;

import java.time.LocalDate;

/**
 * A student extended their stay into the next intake (GH-498).
 *
 * @param allocationId        the allocation whose stay moved
 * @param intakeName          the intake the stay now runs through
 * @param previousStayEndDate the planned check-out before the extension
 * @param stayEndDate         the planned check-out now
 */
public record StayExtendedEvent(Long allocationId, String intakeName, LocalDate previousStayEndDate,
                                LocalDate stayEndDate) {
}
