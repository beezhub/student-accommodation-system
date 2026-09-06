package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.exceptions.InvalidStatusException;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * Guards writes to an application status from the allocation package.
 *
 * <p>GH-137: six such writes live here and none of them consulted
 * {@link ApplicationStatus#canTransitionTo}. The only caller in the codebase was the admin service,
 * so the allocation flow could put an application into any state it liked.
 *
 * <p>The exploitable case: rejecting an application does not cancel its outstanding offer, so the
 * student could still accept it. The allocation-side guard passed, because the allocation was
 * genuinely OFFERED, and the application status was silently overwritten from REJECTED to
 * ALLOCATED — un-rejecting the application and keeping the room. Check-in and the expiry jobs
 * clobbered the same decision in their own ways.
 *
 * <p>Two shapes deliberately: {@link #validate} throws, for the user-facing paths where the caller
 * asked for something illegal; {@link #canApply} reports, for background jobs where the right
 * response is to skip that row and keep going.
 */
@Slf4j
final class ApplicationStatusValidator {

    private ApplicationStatusValidator() {
    }

    /**
     * Whether the write is legal. A null application has nothing to write and nothing illegal about
     * it, so it reports true, matching what {@link #validate} does with the same input.
     */
    static boolean canApply(Application application, ApplicationStatus target) {
        if (application == null || application.getStatus() == null) {
            return true;
        }
        return application.getStatus().canTransitionTo(target);
    }

    /** Throws when the write would be illegal. */
    static void validate(Application application, ApplicationStatus target) {
        if (canApply(application, target)) {
            return;
        }
        log.warn("action=applicationStatusTransitionRejected applicationId={} from={} to={}",
                application.getId(), application.getStatus(), target);
        throw new InvalidStatusException(
                "This application is " + application.getStatus().getStatus().toLowerCase()
                        + " and cannot be moved to " + target.getStatus().toLowerCase());
    }
}
