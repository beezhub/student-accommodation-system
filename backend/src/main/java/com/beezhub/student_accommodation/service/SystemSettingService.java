package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.model.entity.SystemSetting;
import com.beezhub.student_accommodation.repository.SystemSettingRepository;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@org.springframework.transaction.annotation.Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;

    /**
     * Retired. Auto-allocation is gated solely by the FF4j {@code auto-allocation} flag now; this
     * row is deleted by {@code V2026_09_06_1600__retire_auto_allocation_setting.sql} (GH-207).
     *
     * <p>The key is kept in {@link #PLATFORM_ONLY_KEYS} rather than dropped, so a tenant admin
     * cannot create an override row for a key that no longer controls anything and be misled by it.
     */
    public static final String AUTO_ALLOCATION_ENABLED = "auto_allocation_enabled";

    /** Settings only the platform tenant may create, change or remove. */
    static final java.util.Set<String> PLATFORM_ONLY_KEYS = java.util.Set.of(AUTO_ALLOCATION_ENABLED);

    // ──────────────────────────────────────────────────────────────────────────
    // Platform-level (tenant_id = 0) — filter-bypassing native queries
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns the platform-level value for {@code key}, or {@code null} if absent.
     * Uses a native query so the Hibernate tenantFilter is not applied.
     */
    public String getPlatformSetting(String key) {
        return systemSettingRepository.findPlatformSettingByKey(key)
                .map(SystemSetting::getSettingValue)
                .orElse(null);
    }

    /**
     * Returns all platform-level (tenant_id = 0) settings as a key-value map.
     */
    public Map<String, String> getAllPlatformSettings() {
        return systemSettingRepository.findAllPlatformSettings().stream()
                .collect(Collectors.toMap(SystemSetting::getSettingKey, SystemSetting::getSettingValue));
    }

    /**
     * Upserts a platform-level setting (tenant_id = 0).
     * TenantListener will not override an explicitly-set tenantId.
     */
    @Transactional
    public void updatePlatformSetting(String key, String value) {
        log.info("Updating platform setting: {} = {}", key, value);
        Optional<SystemSetting> existing = systemSettingRepository.findPlatformSettingByKey(key);
        if (existing.isPresent()) {
            existing.get().setSettingValue(value);
            systemSettingRepository.save(existing.get());
        } else {
            SystemSetting newSetting = SystemSetting.builder()
                    .tenantId(TenantContext.PLATFORM_TENANT_ID)
                    .settingKey(key)
                    .settingValue(value)
                    .description("Platform setting")
                    .build();
            systemSettingRepository.save(newSetting);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tenant-level with cascade fallback
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Cascade lookup: returns the current tenant's value for {@code key} if present,
     * otherwise falls back to the platform-level value. Returns {@code null} if
     * neither level has a row for the key.
     */
    public String getSetting(String key) {
        String tenantValue = systemSettingRepository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .orElse(null);
        return tenantValue != null ? tenantValue : getPlatformSetting(key);
    }

    /**
     * Cascade lookup for a named company rather than the caller: that company's value, else the
     * platform default. Used where a platform admin acts on a company's data, such as filling in
     * an intake's window for that company's year (GH-494).
     */
    public String getSettingForTenant(Long tenantId, String key) {
        if (tenantId != null && !TenantContext.PLATFORM_TENANT_ID.equals(tenantId)) {
            Optional<SystemSetting> own = systemSettingRepository.findByTenantIdAndKey(tenantId, key);
            if (own.isPresent() && own.get().getSettingValue() != null) {
                return own.get().getSettingValue();
            }
        }
        return getPlatformSetting(key);
    }

    /**
     * Returns a merged map of effective settings for the current tenant.
     * Platform defaults are the base; tenant-specific rows override matching keys.
     */
    public Map<String, String> getAllSettings() {
        Map<String, String> merged = new HashMap<>(getAllPlatformSettings());
        systemSettingRepository.findAll()
                .forEach(s -> merged.put(s.getSettingKey(), s.getSettingValue()));
        return merged;
    }

    /**
     * Upserts a setting scoped to the current tenant (tenant override).
     */
    @Transactional
    public void updateSetting(String key, String value) {
        assertNotPlatformOnly(key);
        log.info("Updating tenant setting: {} = {}", key, value);
        Optional<SystemSetting> settingOpt = systemSettingRepository.findBySettingKey(key);
        if (settingOpt.isPresent()) {
            settingOpt.get().setSettingValue(value);
            systemSettingRepository.save(settingOpt.get());
        } else {
            SystemSetting newSetting = SystemSetting.builder()
                    .settingKey(key)
                    .settingValue(value)
                    .description("Tenant override")
                    .build();
            systemSettingRepository.save(newSetting);
        }
    }

    /**
     * Removes the current tenant's override row for {@code key}, reverting to the
     * platform default. Throws {@link ResourceNotFoundException} if no override exists.
     * Guards against accidentally deleting platform rows when called in a
     * PLATFORM_ADMIN context (where the filter is inactive).
     */
    @Transactional
    public void deleteSetting(String key) {
        assertNotPlatformOnly(key);
        SystemSetting setting = systemSettingRepository.findBySettingKey(key)
                .filter(s -> !TenantContext.PLATFORM_TENANT_ID.equals(s.getTenantId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No tenant override found for key: " + key));
        systemSettingRepository.delete(setting);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Rejects a tenant-scoped write to a setting that only the platform may own.
     *
     * <p>The tenant-scoped endpoint passed every key straight through, so a tenant admin could
     * create an override row for {@code auto_allocation_enabled} and turn batch allocation on or
     * off for themselves, contradicting the documented platform-only rule (GH-127).
     * The check lives here rather than in the controller so any future caller inherits it.
     */
    private void assertNotPlatformOnly(String key) {
        if (!PLATFORM_ONLY_KEYS.contains(key)) {
            return;
        }
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !TenantContext.PLATFORM_TENANT_ID.equals(tenantId)) {
            log.warn("action=platformOnlySettingRejected key={} tenantId={}", key, tenantId);
            throw new UnauthorizedAccessException(
                    "Setting '" + key + "' can only be changed by a platform administrator");
        }
    }

}
