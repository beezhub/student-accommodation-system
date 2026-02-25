package com.beezhub.student_accommodation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ff4j.FF4j;
import org.springframework.stereotype.Service;

/**
 * FF4J-backed implementation of {@link FeatureFlagService}.
 * This is the only class in the codebase that is permitted to reference the
 * {@link FF4j} bean directly — all other service and controller code must
 * inject {@link FeatureFlagService}.
 *
 * <p>Per ARCH 3.5 and TASK-3.02.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FF4JFeatureFlagService implements FeatureFlagService {

    private final FF4j ff4j;

    @Override
    public boolean isEnabled(String featureName) {
        try {
            return ff4j.check(featureName);
        } catch (Exception e) {
            log.warn("FeatureFlag check failed for '{}', defaulting to false: {}", featureName, e.getMessage());
            return false;
        }
    }

    @Override
    public void enable(String featureName) {
        ff4j.enable(featureName);
    }

    @Override
    public void disable(String featureName) {
        ff4j.disable(featureName);
    }
}
