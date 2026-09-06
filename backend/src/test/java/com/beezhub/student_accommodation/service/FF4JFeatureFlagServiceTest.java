package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.config.FF4jConfig;
import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import org.ff4j.FF4j;
import org.ff4j.store.InMemoryFeatureStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.sql.DataSource;

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
        // Matches the configured bean. With autocreate on, every "unknown feature" case below
        // passes for the wrong reason: the feature is created on the spot (GH-203).
        ff4j.autoCreate(false);
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
        assertFalse(ff4j.getFeatureStore().exist("non-existent-feature"),
                "Reading a flag must never bring it into existence");
    }

    // ── GH-203: autocreate let an admin mint feature rows from a URL path segment ────────────

    @Test
    void enable_whenFlagDoesNotExist_isRejectedRatherThanCreatingIt() {
        // POST /api/admin/features/{anything}/enable used to return 200 and insert a row named
        // from the path segment, so a typo produced a lookalike flag that did nothing.
        ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class,
                () -> featureFlagService.enable("auto-allocaton"));

        assertTrue(thrown.getMessage().contains("auto-allocaton"),
                "The message should name the id, so an admin can see the typo");
        assertFalse(ff4j.getFeatureStore().exist("auto-allocaton"),
                "A refused enable must not leave a feature row behind");
    }

    @Test
    void disable_whenFlagDoesNotExist_isRejectedRatherThanCreatingIt() {
        assertThrows(ResourceNotFoundException.class,
                () -> featureFlagService.disable("auto-allocaton"));

        assertFalse(ff4j.getFeatureStore().exist("auto-allocaton"));
    }

    @Test
    void enable_whenFlagIdIsAbsurdlyLong_theMessageIsTruncated() {
        // The id arrives as a URL path segment, so it is unbounded until the service caps it.
        ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class,
                () -> featureFlagService.enable("x".repeat(5000)));

        assertTrue(thrown.getMessage().length() < 200,
                "An unbounded id must not be echoed back in full");
    }

    /**
     * The guard belongs on the bean the application runs with, not only on the FF4j this test
     * builds. {@code getFF4j} touches the datasource zero times, so a mock is enough.
     */
    @Test
    void configuredBean_shipsWithAutocreateOff() {
        FF4j configured = new FF4jConfig().getFF4j(Mockito.mock(DataSource.class));

        assertFalse(configured.isAutocreate(),
                "autocreate lets an admin mint feature rows from a URL path segment (GH-203)");
    }
}
