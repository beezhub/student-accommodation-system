package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Exposes individual system settings to any authenticated user (including students).
 * Uses the cascade model: tenant override -> platform default.
 */
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class PublicSettingController {

    private final SystemSettingService systemSettingService;

    @GetMapping("/{key}")
    public ResponseEntity<Map<String, String>> getSetting(@PathVariable String key) {
        String value = systemSettingService.getSetting(key);
        if (value != null) {
            return ResponseEntity.ok(Map.of("key", key, "value", value));
        }
        return ResponseEntity.ok(Map.of("key", key));
    }
}
