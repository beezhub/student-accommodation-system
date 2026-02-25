package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.config.FF4jConfig;
import com.beezhub.student_accommodation.service.FeatureFlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ff4j.FF4j;
import org.ff4j.core.Feature;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/features")
@RequiredArgsConstructor
@Tag(name = "Feature Flags", description = "Manage system feature toggles via FF4J")
public class FeatureController {

    private final FF4j ff4j;
    private final FeatureFlagService featureFlagService;

    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Get all feature flags")
    public ResponseEntity<Map<String, Feature>> getAllFeatures() {
        return ResponseEntity.ok(ff4j.getFeatures());
    }

    @GetMapping("/{featureId}/check")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Check if a feature is enabled")
    public ResponseEntity<Boolean> checkFeature(@PathVariable String featureId) {
        return ResponseEntity.ok(featureFlagService.isEnabled(featureId));
    }

    @PostMapping("/{featureId}/enable")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Enable a feature")
    public ResponseEntity<Void> enableFeature(@PathVariable String featureId) {
        featureFlagService.enable(featureId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{featureId}/disable")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Disable a feature")
    public ResponseEntity<Void> disableFeature(@PathVariable String featureId) {
        featureFlagService.disable(featureId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/auto-allocation")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Check auto-allocation status specifically")
    public ResponseEntity<Boolean> isAutoAllocationEnabled() {
        return ResponseEntity.ok(featureFlagService.isEnabled(FF4jConfig.FEATURE_AUTO_ALLOCATION));
    }
}
