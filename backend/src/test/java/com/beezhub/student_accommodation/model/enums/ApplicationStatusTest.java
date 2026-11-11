package com.beezhub.student_accommodation.model.enums;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/** The application state machine. */
class ApplicationStatusTest {

    /** GH-497: an unfinished application expires when its intake's stay ends. */
    @ParameterizedTest(name = "{0} -> EXPIRED")
    @CsvSource({"PENDING", "UNDER_REVIEW", "CHANGES_REQUESTED", "APPROVED", "AWAITING_ALLOCATION", "ALLOCATED"})
    void canTransitionTo_expiredFromAnUnfinishedStatus_isAllowed(ApplicationStatus from) {
        assertThat(from.canTransitionTo(ApplicationStatus.EXPIRED)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> EXPIRED")
    @CsvSource({"CHECKED_IN", "REJECTED", "CANCELLED"})
    void canTransitionTo_expiredFromAFinishedStatus_isRefused(ApplicationStatus from) {
        assertThat(from.canTransitionTo(ApplicationStatus.EXPIRED)).isFalse();
    }

    @ParameterizedTest(name = "EXPIRED -> {0}")
    @CsvSource({"PENDING", "UNDER_REVIEW", "CHANGES_REQUESTED", "APPROVED", "AWAITING_ALLOCATION", "ALLOCATED",
            "CHECKED_IN", "REJECTED", "CANCELLED"})
    void canTransitionTo_fromExpired_isRefused(ApplicationStatus to) {
        assertThat(ApplicationStatus.EXPIRED.canTransitionTo(to)).isFalse();
    }
}
