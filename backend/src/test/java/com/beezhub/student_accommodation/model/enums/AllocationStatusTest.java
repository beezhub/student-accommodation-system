package com.beezhub.student_accommodation.model.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AllocationStatus State Machine Tests")
class AllocationStatusTest {

    /**
     * Test all valid transitions return true
     */
    @ParameterizedTest(name = "{0} -> {1} should be VALID")
    @MethodSource("validTransitions")
    @DisplayName("Valid transitions return true")
    void validTransitions_returnTrue(AllocationStatus from, AllocationStatus to) {
        assertThat(from.canTransitionTo(to))
                .as("Transition from %s to %s should be valid", from, to)
                .isTrue();
    }

    /**
     * Test all invalid transitions return false
     */
    @ParameterizedTest(name = "{0} -> {1} should be INVALID")
    @MethodSource("invalidTransitions")
    @DisplayName("Invalid transitions return false")
    void invalidTransitions_returnFalse(AllocationStatus from, AllocationStatus to) {
        assertThat(from.canTransitionTo(to))
                .as("Transition from %s to %s should be invalid", from, to)
                .isFalse();
    }

    /**
     * Test self-transitions are always allowed
     */
    @Test
    @DisplayName("Self-transitions are allowed for all statuses")
    void selfTransitions_alwaysAllowed() {
        for (AllocationStatus status : AllocationStatus.values()) {
            assertThat(status.canTransitionTo(status))
                    .as("Status %s should allow transition to itself", status)
                    .isTrue();
        }
    }

    /**
     * Test terminal states cannot transition to any other status
     */
    @Test
    @DisplayName("Terminal states cannot transition to any other status")
    void terminalStates_cannotTransition() {
        AllocationStatus[] terminalStates = {
                AllocationStatus.DECLINED,
                AllocationStatus.EXPIRED,
                AllocationStatus.CHECK_IN_EXPIRED,
                AllocationStatus.COMPLETED,
                AllocationStatus.TERMINATED,
                AllocationStatus.CANCELLED
        };

        for (AllocationStatus terminal : terminalStates) {
            for (AllocationStatus target : AllocationStatus.values()) {
                if (terminal != target) { // Self-transitions are allowed
                    assertThat(terminal.canTransitionTo(target))
                            .as("Terminal status %s should not transition to %s", terminal, target)
                            .isFalse();
                }
            }
        }
    }

    /**
     * Test complete 9x9 transition matrix
     */
    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("allTransitions")
    @DisplayName("Complete transition matrix validation")
    void allTransitions_validateCorrectly(AllocationStatus from, AllocationStatus to, boolean expected) {
        assertThat(from.canTransitionTo(to))
                .as("Transition from %s to %s", from, to)
                .isEqualTo(expected);
    }

    // ========================================
    // Test Data Providers
    // ========================================

    static Stream<Arguments> validTransitions() {
        return Stream.of(
                // OFFERED transitions
                Arguments.of(AllocationStatus.OFFERED, AllocationStatus.ACCEPTED),
                Arguments.of(AllocationStatus.OFFERED, AllocationStatus.DECLINED),
                Arguments.of(AllocationStatus.OFFERED, AllocationStatus.EXPIRED),
                Arguments.of(AllocationStatus.OFFERED, AllocationStatus.CANCELLED),

                // ACCEPTED transitions
                Arguments.of(AllocationStatus.ACCEPTED, AllocationStatus.ACTIVE),
                Arguments.of(AllocationStatus.ACCEPTED, AllocationStatus.CHECK_IN_EXPIRED),
                Arguments.of(AllocationStatus.ACCEPTED, AllocationStatus.CANCELLED),

                // ACTIVE transitions
                Arguments.of(AllocationStatus.ACTIVE, AllocationStatus.COMPLETED),
                Arguments.of(AllocationStatus.ACTIVE, AllocationStatus.TERMINATED),
                Arguments.of(AllocationStatus.ACTIVE, AllocationStatus.CANCELLED),

                // Self-transitions (spot check)
                Arguments.of(AllocationStatus.OFFERED, AllocationStatus.OFFERED),
                Arguments.of(AllocationStatus.ACTIVE, AllocationStatus.ACTIVE),
                Arguments.of(AllocationStatus.COMPLETED, AllocationStatus.COMPLETED)
        );
    }

    static Stream<Arguments> invalidTransitions() {
        return Stream.of(
                // Terminal states cannot transition to any non-self status
                Arguments.of(AllocationStatus.DECLINED, AllocationStatus.OFFERED),
                Arguments.of(AllocationStatus.DECLINED, AllocationStatus.ACCEPTED),
                Arguments.of(AllocationStatus.EXPIRED, AllocationStatus.OFFERED),
                Arguments.of(AllocationStatus.EXPIRED, AllocationStatus.ACCEPTED),
                Arguments.of(AllocationStatus.CHECK_IN_EXPIRED, AllocationStatus.ACTIVE),
                Arguments.of(AllocationStatus.COMPLETED, AllocationStatus.OFFERED),
                Arguments.of(AllocationStatus.COMPLETED, AllocationStatus.ACTIVE),
                Arguments.of(AllocationStatus.TERMINATED, AllocationStatus.ACTIVE),
                Arguments.of(AllocationStatus.CANCELLED, AllocationStatus.OFFERED),
                Arguments.of(AllocationStatus.CANCELLED, AllocationStatus.ACCEPTED),

                // Reverse transitions (going backwards in the flow)
                Arguments.of(AllocationStatus.ACCEPTED, AllocationStatus.OFFERED),
                Arguments.of(AllocationStatus.ACTIVE, AllocationStatus.OFFERED),
                Arguments.of(AllocationStatus.ACTIVE, AllocationStatus.ACCEPTED),

                // Invalid forward jumps
                Arguments.of(AllocationStatus.OFFERED, AllocationStatus.ACTIVE),
                Arguments.of(AllocationStatus.OFFERED, AllocationStatus.COMPLETED),
                Arguments.of(AllocationStatus.OFFERED, AllocationStatus.TERMINATED),
                Arguments.of(AllocationStatus.OFFERED, AllocationStatus.CHECK_IN_EXPIRED),
                Arguments.of(AllocationStatus.ACCEPTED, AllocationStatus.COMPLETED),
                Arguments.of(AllocationStatus.ACCEPTED, AllocationStatus.TERMINATED),
                Arguments.of(AllocationStatus.ACCEPTED, AllocationStatus.OFFERED),
                Arguments.of(AllocationStatus.ACCEPTED, AllocationStatus.DECLINED),
                Arguments.of(AllocationStatus.ACCEPTED, AllocationStatus.EXPIRED)
        );
    }

    /**
     * Complete 9x9 transition matrix for comprehensive testing
     * This ensures every single possible transition is explicitly tested
     */
    static Stream<Arguments> allTransitions() {
        return Stream.of(
                // FROM: OFFERED
                Arguments.of(AllocationStatus.OFFERED, AllocationStatus.OFFERED, true),  // self
                Arguments.of(AllocationStatus.OFFERED, AllocationStatus.ACCEPTED, true),
                Arguments.of(AllocationStatus.OFFERED, AllocationStatus.DECLINED, true),
                Arguments.of(AllocationStatus.OFFERED, AllocationStatus.EXPIRED, true),
                Arguments.of(AllocationStatus.OFFERED, AllocationStatus.CHECK_IN_EXPIRED, false),
                Arguments.of(AllocationStatus.OFFERED, AllocationStatus.ACTIVE, false),
                Arguments.of(AllocationStatus.OFFERED, AllocationStatus.COMPLETED, false),
                Arguments.of(AllocationStatus.OFFERED, AllocationStatus.TERMINATED, false),
                Arguments.of(AllocationStatus.OFFERED, AllocationStatus.CANCELLED, true),

                // FROM: ACCEPTED
                Arguments.of(AllocationStatus.ACCEPTED, AllocationStatus.OFFERED, false),
                Arguments.of(AllocationStatus.ACCEPTED, AllocationStatus.ACCEPTED, true),  // self
                Arguments.of(AllocationStatus.ACCEPTED, AllocationStatus.DECLINED, false),
                Arguments.of(AllocationStatus.ACCEPTED, AllocationStatus.EXPIRED, false),
                Arguments.of(AllocationStatus.ACCEPTED, AllocationStatus.CHECK_IN_EXPIRED, true),
                Arguments.of(AllocationStatus.ACCEPTED, AllocationStatus.ACTIVE, true),
                Arguments.of(AllocationStatus.ACCEPTED, AllocationStatus.COMPLETED, false),
                Arguments.of(AllocationStatus.ACCEPTED, AllocationStatus.TERMINATED, false),
                Arguments.of(AllocationStatus.ACCEPTED, AllocationStatus.CANCELLED, true),

                // FROM: DECLINED
                Arguments.of(AllocationStatus.DECLINED, AllocationStatus.OFFERED, false),
                Arguments.of(AllocationStatus.DECLINED, AllocationStatus.ACCEPTED, false),
                Arguments.of(AllocationStatus.DECLINED, AllocationStatus.DECLINED, true),  // self
                Arguments.of(AllocationStatus.DECLINED, AllocationStatus.EXPIRED, false),
                Arguments.of(AllocationStatus.DECLINED, AllocationStatus.CHECK_IN_EXPIRED, false),
                Arguments.of(AllocationStatus.DECLINED, AllocationStatus.ACTIVE, false),
                Arguments.of(AllocationStatus.DECLINED, AllocationStatus.COMPLETED, false),
                Arguments.of(AllocationStatus.DECLINED, AllocationStatus.TERMINATED, false),
                Arguments.of(AllocationStatus.DECLINED, AllocationStatus.CANCELLED, false),

                // FROM: EXPIRED
                Arguments.of(AllocationStatus.EXPIRED, AllocationStatus.OFFERED, false),
                Arguments.of(AllocationStatus.EXPIRED, AllocationStatus.ACCEPTED, false),
                Arguments.of(AllocationStatus.EXPIRED, AllocationStatus.DECLINED, false),
                Arguments.of(AllocationStatus.EXPIRED, AllocationStatus.EXPIRED, true),  // self
                Arguments.of(AllocationStatus.EXPIRED, AllocationStatus.CHECK_IN_EXPIRED, false),
                Arguments.of(AllocationStatus.EXPIRED, AllocationStatus.ACTIVE, false),
                Arguments.of(AllocationStatus.EXPIRED, AllocationStatus.COMPLETED, false),
                Arguments.of(AllocationStatus.EXPIRED, AllocationStatus.TERMINATED, false),
                Arguments.of(AllocationStatus.EXPIRED, AllocationStatus.CANCELLED, false),

                // FROM: CHECK_IN_EXPIRED
                Arguments.of(AllocationStatus.CHECK_IN_EXPIRED, AllocationStatus.OFFERED, false),
                Arguments.of(AllocationStatus.CHECK_IN_EXPIRED, AllocationStatus.ACCEPTED, false),
                Arguments.of(AllocationStatus.CHECK_IN_EXPIRED, AllocationStatus.DECLINED, false),
                Arguments.of(AllocationStatus.CHECK_IN_EXPIRED, AllocationStatus.EXPIRED, false),
                Arguments.of(AllocationStatus.CHECK_IN_EXPIRED, AllocationStatus.CHECK_IN_EXPIRED, true),  // self
                Arguments.of(AllocationStatus.CHECK_IN_EXPIRED, AllocationStatus.ACTIVE, false),
                Arguments.of(AllocationStatus.CHECK_IN_EXPIRED, AllocationStatus.COMPLETED, false),
                Arguments.of(AllocationStatus.CHECK_IN_EXPIRED, AllocationStatus.TERMINATED, false),
                Arguments.of(AllocationStatus.CHECK_IN_EXPIRED, AllocationStatus.CANCELLED, false),

                // FROM: ACTIVE
                Arguments.of(AllocationStatus.ACTIVE, AllocationStatus.OFFERED, false),
                Arguments.of(AllocationStatus.ACTIVE, AllocationStatus.ACCEPTED, false),
                Arguments.of(AllocationStatus.ACTIVE, AllocationStatus.DECLINED, false),
                Arguments.of(AllocationStatus.ACTIVE, AllocationStatus.EXPIRED, false),
                Arguments.of(AllocationStatus.ACTIVE, AllocationStatus.CHECK_IN_EXPIRED, false),
                Arguments.of(AllocationStatus.ACTIVE, AllocationStatus.ACTIVE, true),  // self
                Arguments.of(AllocationStatus.ACTIVE, AllocationStatus.COMPLETED, true),
                Arguments.of(AllocationStatus.ACTIVE, AllocationStatus.TERMINATED, true),
                Arguments.of(AllocationStatus.ACTIVE, AllocationStatus.CANCELLED, true),

                // FROM: COMPLETED
                Arguments.of(AllocationStatus.COMPLETED, AllocationStatus.OFFERED, false),
                Arguments.of(AllocationStatus.COMPLETED, AllocationStatus.ACCEPTED, false),
                Arguments.of(AllocationStatus.COMPLETED, AllocationStatus.DECLINED, false),
                Arguments.of(AllocationStatus.COMPLETED, AllocationStatus.EXPIRED, false),
                Arguments.of(AllocationStatus.COMPLETED, AllocationStatus.CHECK_IN_EXPIRED, false),
                Arguments.of(AllocationStatus.COMPLETED, AllocationStatus.ACTIVE, false),
                Arguments.of(AllocationStatus.COMPLETED, AllocationStatus.COMPLETED, true),  // self
                Arguments.of(AllocationStatus.COMPLETED, AllocationStatus.TERMINATED, false),
                Arguments.of(AllocationStatus.COMPLETED, AllocationStatus.CANCELLED, false),

                // FROM: TERMINATED
                Arguments.of(AllocationStatus.TERMINATED, AllocationStatus.OFFERED, false),
                Arguments.of(AllocationStatus.TERMINATED, AllocationStatus.ACCEPTED, false),
                Arguments.of(AllocationStatus.TERMINATED, AllocationStatus.DECLINED, false),
                Arguments.of(AllocationStatus.TERMINATED, AllocationStatus.EXPIRED, false),
                Arguments.of(AllocationStatus.TERMINATED, AllocationStatus.CHECK_IN_EXPIRED, false),
                Arguments.of(AllocationStatus.TERMINATED, AllocationStatus.ACTIVE, false),
                Arguments.of(AllocationStatus.TERMINATED, AllocationStatus.COMPLETED, false),
                Arguments.of(AllocationStatus.TERMINATED, AllocationStatus.TERMINATED, true),  // self
                Arguments.of(AllocationStatus.TERMINATED, AllocationStatus.CANCELLED, false),

                // FROM: CANCELLED
                Arguments.of(AllocationStatus.CANCELLED, AllocationStatus.OFFERED, false),
                Arguments.of(AllocationStatus.CANCELLED, AllocationStatus.ACCEPTED, false),
                Arguments.of(AllocationStatus.CANCELLED, AllocationStatus.DECLINED, false),
                Arguments.of(AllocationStatus.CANCELLED, AllocationStatus.EXPIRED, false),
                Arguments.of(AllocationStatus.CANCELLED, AllocationStatus.CHECK_IN_EXPIRED, false),
                Arguments.of(AllocationStatus.CANCELLED, AllocationStatus.ACTIVE, false),
                Arguments.of(AllocationStatus.CANCELLED, AllocationStatus.COMPLETED, false),
                Arguments.of(AllocationStatus.CANCELLED, AllocationStatus.TERMINATED, false),
                Arguments.of(AllocationStatus.CANCELLED, AllocationStatus.CANCELLED, true)  // self
        );
    }
}
