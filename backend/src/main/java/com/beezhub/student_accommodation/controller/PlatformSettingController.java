package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/platform/system-settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlatformSettingController {

    private final SystemSettingService systemSettingService;

    @GetMapping
    public ResponseEntity<Map<String, String>> getAllPlatformSettings() {
        return ResponseEntity.ok(systemSettingService.getAllPlatformSettings());
    }

    @PutMapping("/{key}")
    public ResponseEntity<Void> updatePlatformSetting(
            @PathVariable String key,
            @RequestBody Map<String, String> payload) {
        systemSettingService.updatePlatformSetting(key, payload.get("value"));
        return ResponseEntity.ok().build();
    }
}
