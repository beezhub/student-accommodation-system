package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.service.SystemSettingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** GH-496: reconfirmation lead times come through the settings cascade. */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ReconfirmationPolicyTest {

    @Mock
    private SystemSettingService systemSettingService;

    @InjectMocks
    private ReconfirmationPolicy policy;

    @Test
    void leadTimesFor_noSettings_usesDefaults() {
        assertThat(policy.leadTimesFor(7L)).isEqualTo(new ReconfirmationPolicy.LeadTimes(42, 28, 14));
    }

    @Test
    void leadTimesFor_companyOverride_used() {
        when(systemSettingService.getSettingForTenant(7L, ReconfirmationPolicy.ASK_DAYS_KEY)).thenReturn("21");
        when(systemSettingService.getSettingForTenant(7L, ReconfirmationPolicy.RELEASE_DAYS_KEY)).thenReturn(" 7 ");

        assertThat(policy.leadTimesFor(7L)).isEqualTo(new ReconfirmationPolicy.LeadTimes(42, 21, 7));
    }

    @Test
    void leadTimesFor_invalidOrNegative_fallsBack() {
        when(systemSettingService.getSettingForTenant(7L, ReconfirmationPolicy.MIN_LEAD_DAYS_KEY)).thenReturn("six");
        when(systemSettingService.getSettingForTenant(7L, ReconfirmationPolicy.ASK_DAYS_KEY)).thenReturn("-3");

        assertThat(policy.leadTimesFor(7L)).isEqualTo(new ReconfirmationPolicy.LeadTimes(42, 28, 14));
    }

    @Test
    void leadTimesFor_releaseAfterAsk_clampedToTheAskPoint() {
        when(systemSettingService.getSettingForTenant(7L, ReconfirmationPolicy.RELEASE_DAYS_KEY)).thenReturn("40");

        assertThat(policy.leadTimesFor(7L).releaseDaysBefore()).isEqualTo(28);
    }

    @Test
    void mustReconfirm_onlyAboveTheMinimumLeadTime() {
        ReconfirmationPolicy.LeadTimes leadTimes = new ReconfirmationPolicy.LeadTimes(42, 28, 14);
        LocalDate moveIn = LocalDate.of(2027, 2, 1);

        assertThat(leadTimes.mustReconfirm(LocalDateTime.of(2026, 12, 21, 9, 0), moveIn)).isTrue();
        assertThat(leadTimes.mustReconfirm(LocalDateTime.of(2026, 12, 22, 9, 0), moveIn)).isFalse();
        assertThat(leadTimes.mustReconfirm(null, moveIn)).isFalse();
        assertThat(leadTimes.askOn(moveIn)).isEqualTo(LocalDate.of(2027, 1, 4));
        assertThat(leadTimes.releaseOn(moveIn)).isEqualTo(LocalDate.of(2027, 1, 18));
    }
}
