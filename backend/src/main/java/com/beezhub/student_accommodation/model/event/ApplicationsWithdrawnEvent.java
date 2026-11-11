package com.beezhub.student_accommodation.model.event;

import java.util.List;

/**
 * A student accepted a place, and their other applications for overlapping dates were withdrawn
 * (GH-497). Carries ids and display values only, since the applications belong to several
 * companies and the listener reads them outside any one company's tenant filter.
 *
 * @param studentId             the student whose applications were withdrawn
 * @param acceptedAllocationId  the place they accepted
 * @param withdrawn             what was withdrawn, one entry per application
 */
public record ApplicationsWithdrawnEvent(Long studentId, Long acceptedAllocationId, List<Withdrawn> withdrawn) {

    /** One withdrawn application and the company it was with. */
    public record Withdrawn(Long applicationId, String applicationCode, Long tenantId, String companyName,
            String intakeName, int offersCancelled) {
    }
}
