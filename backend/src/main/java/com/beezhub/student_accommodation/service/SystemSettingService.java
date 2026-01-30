package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.entity.SystemSetting;
import com.beezhub.student_accommodation.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;

    public static final String AUTO_ALLOCATION_ENABLED = "auto_allocation_enabled";

    /**
     * Retrieves all system settings as a key-value map.
     */
    public Map<String, String> getAllSettings() {
        List<SystemSetting> settings = systemSettingRepository.findAll();
        Map<String, String> settingsMap = new HashMap<>();
        for (SystemSetting setting : settings) {
            settingsMap.put(setting.getSettingKey(), setting.getSettingValue());
        }
        return settingsMap;
    }

    /**
     * Retrieves a single setting value by key.
     */
    public String getSetting(String key) {
        return systemSettingRepository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .orElse(null);
    }

    /**
     * Updates a system setting value.
     */
    @Transactional
    public void updateSetting(String key, String value) {
        log.info("Updating system setting: {} = {}", key, value);
        Optional<SystemSetting> settingOpt = systemSettingRepository.findBySettingKey(key);

        if (settingOpt.isPresent()) {
            SystemSetting setting = settingOpt.get();
            setting.setSettingValue(value);
            systemSettingRepository.save(setting);
        } else {
            // Create new setting if it doesn't exist (though usually seeded via migration)
            SystemSetting newSetting = SystemSetting.builder()
                    .settingKey(key)
                    .settingValue(value)
                    .description("Auto-generated setting")
                    .build();
            systemSettingRepository.save(newSetting);
        }
    }

    /**
     * Helper to check if auto-allocation is enabled.
     */
    public boolean isAutoAllocationEnabled() {
        String value = getSetting(AUTO_ALLOCATION_ENABLED);
        return Boolean.parseBoolean(value);
    }
}
