package com.beezhub.student_accommodation.service;

/**
 * Abstraction layer over the underlying feature flag implementation (FF4J).
 * All service code must use this interface — not the FF4j bean directly.
 * This decouples business logic from the flag library, enabling a future swap
 * without touching any business code.
 *
 * <p>Per ARCH 3.5 and TASK-3.02.</p>
 */
public interface FeatureFlagService {

    /**
     * Returns {@code true} if the named feature flag is currently enabled.
     *
     * @param featureName the feature flag identifier (e.g. "auto-allocation")
     * @return {@code true} if enabled
     */
    boolean isEnabled(String featureName);

    /**
     * Enables the named feature flag.
     *
     * @param featureName the feature flag identifier
     */
    void enable(String featureName);

    /**
     * Disables the named feature flag.
     *
     * @param featureName the feature flag identifier
     */
    void disable(String featureName);
}
