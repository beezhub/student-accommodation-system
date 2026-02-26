package com.beezhub.student_accommodation.service;

import org.ff4j.FF4j;
import org.ff4j.store.InMemoryFeatureStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FF4JFeatureFlagService.
 */
class FF4JFeatureFlagServiceTest {

    private static final String TEST_FEATURE = "test-feature";

    private FF4j ff4j;
    private FF4JFeatureFlagService featureFlagService;

    @BeforeEach
    void setUp() {
        ff4j = new FF4j();
        ff4j.setFeatureStore(new InMemoryFeatureStore());
        ff4j.createFeature(TEST_FEATURE, false, "Test feature flag");
        featureFlagService = new FF4JFeatureFlagService(ff4j);
    }

    @Test
    void isEnabled_whenFlagActive_returnsTrue() {
        ff4j.enable(TEST_FEATURE);

        assertTrue(featureFlagService.isEnabled(TEST_FEATURE),
                "isEnabled should return true when the feature flag is enabled");
    }

    @Test
    void isEnabled_whenFlagInactive_returnsFalse() {
        ff4j.disable(TEST_FEATURE);

        assertFalse(featureFlagService.isEnabled(TEST_FEATURE),
                "isEnabled should return false when the feature flag is disabled");
    }

    @Test
    void enable_activatesFlag() {
        ff4j.disable(TEST_FEATURE);

        featureFlagService.enable(TEST_FEATURE);

        assertTrue(ff4j.check(TEST_FEATURE), "Flag should be enabled after calling enable()");
    }

    @Test
    void disable_deactivatesFlag() {
        ff4j.enable(TEST_FEATURE);

        featureFlagService.disable(TEST_FEATURE);

        assertFalse(ff4j.check(TEST_FEATURE), "Flag should be disabled after calling disable()");
    }

    @Test
    void isEnabled_whenFlagDoesNotExist_returnsFalse() {
        // A non-existent flag should return false (fail-safe) without throwing
        assertFalse(featureFlagService.isEnabled("non-existent-feature"),
                "isEnabled should return false for a non-existent feature (fail-safe)");
    }
}
