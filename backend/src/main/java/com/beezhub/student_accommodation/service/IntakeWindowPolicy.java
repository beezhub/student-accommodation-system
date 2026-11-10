package com.beezhub.student_accommodation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Fills in an intake's application window from its move-in date (GH-494): applications open
 * {@value #OPEN_DAYS_KEY} days and close {@value #CLOSE_DAYS_KEY} days before move-in. Both are
 * system settings with a platform default that a company can override; admins edit only the
 * windows that differ.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IntakeWindowPolicy {

    public static final String OPEN_DAYS_KEY = "intake_window_open_days_before";
    public static final String CLOSE_DAYS_KEY = "intake_window_close_days_before";
    static final int DEFAULT_OPEN_DAYS = 120;
    static final int DEFAULT_CLOSE_DAYS = 14;

    private final SystemSettingService systemSettingService;

    /** The window for a stay starting on {@code stayStart}, with {@code tenantId}'s lead times. */
    public Window windowFor(Long tenantId, LocalDate stayStart) {
        int openDays = days(tenantId, OPEN_DAYS_KEY, DEFAULT_OPEN_DAYS);
        int closeDays = days(tenantId, CLOSE_DAYS_KEY, DEFAULT_CLOSE_DAYS);
        if (closeDays > openDays) {
            log.warn("action=intakeWindowSettingsReversed tenantId={} openDays={} closeDays={}",
                    tenantId, openDays, closeDays);
            closeDays = openDays;
        }
        return new Window(stayStart.minusDays(openDays), stayStart.minusDays(closeDays));
    }

    private int days(Long tenantId, String key, int fallback) {
        String value = systemSettingService.getSettingForTenant(tenantId, key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            int days = Integer.parseInt(value.trim());
            return days < 0 ? fallback : days;
        } catch (NumberFormatException e) {
            log.warn("action=intakeWindowSettingInvalid tenantId={} key={}", tenantId, key);
            return fallback;
        }
    }

    /** An application window, both dates inclusive. */
    public record Window(LocalDate open, LocalDate close) {
    }
}
