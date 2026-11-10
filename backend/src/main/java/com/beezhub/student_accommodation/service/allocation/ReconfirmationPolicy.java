package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * When a place accepted long before move-in is asked to reconfirm, and when it is released
 * (GH-496). Three lead times in days before move-in, each a system setting with a platform default
 * that a company can override.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReconfirmationPolicy {

    public static final String MIN_LEAD_DAYS_KEY = "reconfirm_min_lead_days";
    public static final String ASK_DAYS_KEY = "reconfirm_ask_days_before";
    public static final String RELEASE_DAYS_KEY = "reconfirm_release_days_before";
    static final int DEFAULT_MIN_LEAD_DAYS = 42;
    static final int DEFAULT_ASK_DAYS = 28;
    static final int DEFAULT_RELEASE_DAYS = 14;

    private final SystemSettingService systemSettingService;

    /** The lead times for one company. */
    public LeadTimes leadTimesFor(Long tenantId) {
        int minLead = days(tenantId, MIN_LEAD_DAYS_KEY, DEFAULT_MIN_LEAD_DAYS);
        int ask = days(tenantId, ASK_DAYS_KEY, DEFAULT_ASK_DAYS);
        int release = days(tenantId, RELEASE_DAYS_KEY, DEFAULT_RELEASE_DAYS);
        if (release > ask) {
            log.warn("action=reconfirmSettingsReversed tenantId={} askDays={} releaseDays={}", tenantId, ask, release);
            release = ask;
        }
        return new LeadTimes(minLead, ask, release);
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
            log.warn("action=reconfirmSettingInvalid tenantId={} key={}", tenantId, key);
            return fallback;
        }
    }

    /** Lead times in days before move-in. */
    public record LeadTimes(int minLeadDays, int askDaysBefore, int releaseDaysBefore) {

        /** Whether a place accepted at {@code acceptedAt} for {@code moveIn} has to reconfirm. */
        public boolean mustReconfirm(LocalDateTime acceptedAt, LocalDate moveIn) {
            return acceptedAt != null && moveIn != null
                    && !acceptedAt.toLocalDate().isAfter(moveIn.minusDays(minLeadDays));
        }

        public LocalDate askOn(LocalDate moveIn) {
            return moveIn.minusDays(askDaysBefore);
        }

        public LocalDate releaseOn(LocalDate moveIn) {
            return moveIn.minusDays(releaseDaysBefore);
        }
    }
}
