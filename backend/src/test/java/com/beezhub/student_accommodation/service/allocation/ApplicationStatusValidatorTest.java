package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.exceptions.InvalidStatusException;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * GH-137: six application-status writes live in the allocation package and none of them consulted
 * the transition rules. The only caller was the admin service, so the allocation flow could put an
 * application into any state it liked.
 */
class ApplicationStatusValidatorTest {

    private static Application application(ApplicationStatus status) {
        Application application = new Application();
        application.setId(1L);
        application.setStatus(status);
        return application;
    }

    /**
     * The exploitable case. Rejecting an application does not cancel its outstanding offer, so the
     * student could still accept it: the allocation guard passed, because the allocation really was
     * OFFERED, and REJECTED was silently overwritten with ALLOCATED.
     */
    @Test
    void validate_rejectedApplicationBeingAllocated_isRefused() {
        assertThatThrownBy(() -> ApplicationStatusValidator.validate(
                application(ApplicationStatus.REJECTED), ApplicationStatus.ALLOCATED))
                .isInstanceOf(InvalidStatusException.class);
    }

    /** Check-in resurrected a rejected application the same way. */
    @Test
    void validate_rejectedApplicationBeingCheckedIn_isRefused() {
        assertThatThrownBy(() -> ApplicationStatusValidator.validate(
                application(ApplicationStatus.REJECTED), ApplicationStatus.CHECKED_IN))
                .isInstanceOf(InvalidStatusException.class);
    }

    /**
     * The ordinary accept path. Three of the four offer-creation paths never move the application,
     * so it is still APPROVED when the student accepts — the enum was wrong to forbid this, and
     * guarding without amending it would have broken the happy path instead of the exploit.
     */
    @Test
    void validate_approvedApplicationBeingAllocated_isAllowed() {
        assertThatCode(() -> ApplicationStatusValidator.validate(
                application(ApplicationStatus.APPROVED), ApplicationStatus.ALLOCATED))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_awaitingAllocationBeingAllocated_isAllowed() {
        assertThatCode(() -> ApplicationStatusValidator.validate(
                application(ApplicationStatus.AWAITING_ALLOCATION), ApplicationStatus.ALLOCATED))
                .doesNotThrowAnyException();
    }

    /** Nothing to write is not an illegal write. */
    @Test
    void validate_nullApplication_isAllowed() {
        assertThatCode(() -> ApplicationStatusValidator.validate(null, ApplicationStatus.ALLOCATED))
                .doesNotThrowAnyException();
        assertThat(ApplicationStatusValidator.canApply(null, ApplicationStatus.ALLOCATED)).isTrue();
    }

    /** The two shapes must never disagree about the same input. */
    @ParameterizedTest
    @EnumSource(ApplicationStatus.class)
    void canApply_agreesWithValidate(ApplicationStatus from) {
        Application application = application(from);
        boolean allowed = ApplicationStatusValidator.canApply(application, ApplicationStatus.CANCELLED);

        if (allowed) {
            assertThatCode(() -> ApplicationStatusValidator.validate(
                    application, ApplicationStatus.CANCELLED)).doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> ApplicationStatusValidator.validate(
                    application, ApplicationStatus.CANCELLED)).isInstanceOf(InvalidStatusException.class);
        }
    }

    /**
     * Expiry must not overwrite an administrator decision. CANCELLED is absent deliberately:
     * cancelling an already-cancelled application is a self-transition, which the rules allow and
     * which writes the same value, so there is nothing to protect against there.
     */
    @ParameterizedTest
    @EnumSource(value = ApplicationStatus.class, names = { "REJECTED", "EXPIRED", "CHECKED_IN" })
    void canApply_terminalApplicationBeingCancelled_isRefused(ApplicationStatus terminal) {
        assertThat(ApplicationStatusValidator.canApply(
                application(terminal), ApplicationStatus.CANCELLED)).isFalse();
    }

    @Test
    void canApply_liveApplicationBeingCancelled_isAllowed() {
        assertThat(ApplicationStatusValidator.canApply(
                application(ApplicationStatus.AWAITING_ALLOCATION), ApplicationStatus.CANCELLED)).isTrue();
    }
}
