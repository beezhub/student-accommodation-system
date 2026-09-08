package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ff4j.FF4j;
import org.ff4j.exception.FeatureNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Map;

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

    /** The id comes from a URL path segment, so it is unbounded until this caps it. */
    private static final int MAX_ECHOED_FEATURE_NAME = 64;

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
        try {
            ff4j.enable(featureName);
        } catch (FeatureNotFoundException e) {
            throw unknownFeature(featureName, e);
        }
    }

    @Override
    public void disable(String featureName) {
        try {
            ff4j.disable(featureName);
        } catch (FeatureNotFoundException e) {
            throw unknownFeature(featureName, e);
        }
    }

    /**
     * With FF4j autocreate off, enabling or disabling an id that has no row raises
     * {@link FeatureNotFoundException} instead of silently creating the feature (GH-203). Translated
     * here because this is the one class permitted to know about FF4j types, and mapped to a 404:
     * the caller named something that does not exist.
     *
     * <p>The id is echoed back so a platform admin can see their typo, truncated because it comes
     * from a URL path segment and is otherwise unbounded.
     */
    private static ResourceNotFoundException unknownFeature(String featureName, RuntimeException cause) {
        log.warn("action=featureFlagNotFound feature={}", featureName);
        String shown = featureName == null ? "" : featureName;
        if (shown.length() > MAX_ECHOED_FEATURE_NAME) {
            shown = shown.substring(0, MAX_ECHOED_FEATURE_NAME) + "...";
        }
        ResourceNotFoundException translated =
                new ResourceNotFoundException("No feature flag named '" + shown + "'");
        translated.initCause(cause);
        return translated;
    }

    @Override
    public Map<String, Object> getFeatures() {
        return Map.copyOf(ff4j.getFeatures());
    }
}
