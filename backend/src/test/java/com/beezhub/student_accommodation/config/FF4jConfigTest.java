package com.beezhub.student_accommodation.config;

import org.ff4j.FF4j;
import org.ff4j.store.InMemoryFeatureStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FF4jConfig.
 * Note: Integration tests with full Spring context are in RoomAllocationServiceTest
 * and other service-level tests.
 */
@ExtendWith(MockitoExtension.class)
class FF4jConfigTest {

    private FF4j ff4j;

    @BeforeEach
    void setUp() {
        // Create an in-memory FF4j instance for testing
        ff4j = new FF4j();
        InMemoryFeatureStore featureStore = new InMemoryFeatureStore();
        ff4j.setFeatureStore(featureStore);

        // Create the auto-allocation feature
        ff4j.createFeature(FF4jConfig.FEATURE_AUTO_ALLOCATION, false, "Enable automatic room allocation logic");
    }

    @Test
    void shouldHaveAutoAllocationFeature() {
        // Verify the feature exists in the feature store
        assertTrue(ff4j.exist(FF4jConfig.FEATURE_AUTO_ALLOCATION),
                "Auto-allocation feature should exist");
    }

    @Test
    void shouldBeAbleToEnableAutoAllocation() {
        // Test that we can enable the feature
        ff4j.enable(FF4jConfig.FEATURE_AUTO_ALLOCATION);
        assertTrue(ff4j.check(FF4jConfig.FEATURE_AUTO_ALLOCATION),
                "Auto-allocation should be enabled after calling enable()");
    }

    @Test
    void shouldBeAbleToDisableAutoAllocation() {
        // First enable the feature
        ff4j.enable(FF4jConfig.FEATURE_AUTO_ALLOCATION);
        assertTrue(ff4j.check(FF4jConfig.FEATURE_AUTO_ALLOCATION));

        // Test that we can disable the feature
        ff4j.disable(FF4jConfig.FEATURE_AUTO_ALLOCATION);
        assertFalse(ff4j.check(FF4jConfig.FEATURE_AUTO_ALLOCATION),
                "Auto-allocation should be disabled after calling disable()");
    }

    @Test
    void shouldToggleFeatureCorrectly() {
        // Initial state is disabled
        assertFalse(ff4j.check(FF4jConfig.FEATURE_AUTO_ALLOCATION));

        // Enable
        ff4j.enable(FF4jConfig.FEATURE_AUTO_ALLOCATION);
        assertTrue(ff4j.check(FF4jConfig.FEATURE_AUTO_ALLOCATION));

        // Disable
        ff4j.disable(FF4jConfig.FEATURE_AUTO_ALLOCATION);
        assertFalse(ff4j.check(FF4jConfig.FEATURE_AUTO_ALLOCATION));

        // Enable again
        ff4j.enable(FF4jConfig.FEATURE_AUTO_ALLOCATION);
        assertTrue(ff4j.check(FF4jConfig.FEATURE_AUTO_ALLOCATION));
    }

    @Test
    void shouldVerifyFeatureConstantValue() {
        // Verify the constant matches expected value
        assertEquals("auto-allocation", FF4jConfig.FEATURE_AUTO_ALLOCATION,
                "Feature constant should be 'auto-allocation'");
    }
}
