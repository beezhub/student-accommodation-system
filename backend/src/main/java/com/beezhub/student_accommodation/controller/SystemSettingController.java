package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/system-settings")
@RequiredArgsConstructor
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> getAllSettings() {
        return ResponseEntity.ok(systemSettingService.getAllSettings());
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateSetting(@PathVariable String key, @RequestBody Map<String, String> payload) {
        String value = payload.get("value");
        systemSettingService.updateSetting(key, value);
        return ResponseEntity.ok().build();
    }
}
