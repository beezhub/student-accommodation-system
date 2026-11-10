package com.beezhub.student_accommodation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntakeWindowPolicyTest {

    private static final Long COMPANY = 42L;
    private static final LocalDate MOVE_IN = LocalDate.of(2027, 7, 15);

    @Mock private SystemSettingService systemSettingService;
    @InjectMocks private IntakeWindowPolicy policy;

    @Test
    void windowFor_companySettings_countBackFromMoveIn() {
        when(systemSettingService.getSettingForTenant(COMPANY, IntakeWindowPolicy.OPEN_DAYS_KEY)).thenReturn("120");
        when(systemSettingService.getSettingForTenant(COMPANY, IntakeWindowPolicy.CLOSE_DAYS_KEY)).thenReturn("14");

        IntakeWindowPolicy.Window window = policy.windowFor(COMPANY, MOVE_IN);

        assertThat(window.open()).isEqualTo(LocalDate.of(2027, 3, 17));
        assertThat(window.close()).isEqualTo(LocalDate.of(2027, 7, 1));
    }

    @Test
    void windowFor_missingOrInvalidSettings_useTheDefaults() {
        when(systemSettingService.getSettingForTenant(COMPANY, IntakeWindowPolicy.OPEN_DAYS_KEY)).thenReturn(null);
        when(systemSettingService.getSettingForTenant(COMPANY, IntakeWindowPolicy.CLOSE_DAYS_KEY)).thenReturn("soon");

        IntakeWindowPolicy.Window window = policy.windowFor(COMPANY, MOVE_IN);

        assertThat(window.open()).isEqualTo(MOVE_IN.minusDays(IntakeWindowPolicy.DEFAULT_OPEN_DAYS));
        assertThat(window.close()).isEqualTo(MOVE_IN.minusDays(IntakeWindowPolicy.DEFAULT_CLOSE_DAYS));
    }

    @Test
    void windowFor_negativeDays_useTheDefault() {
        when(systemSettingService.getSettingForTenant(COMPANY, IntakeWindowPolicy.OPEN_DAYS_KEY)).thenReturn("-5");
        when(systemSettingService.getSettingForTenant(COMPANY, IntakeWindowPolicy.CLOSE_DAYS_KEY)).thenReturn(" ");

        assertThat(policy.windowFor(COMPANY, MOVE_IN).open())
                .isEqualTo(MOVE_IN.minusDays(IntakeWindowPolicy.DEFAULT_OPEN_DAYS));
    }

    /** A company that closes earlier than it opens would get a window that never opens. */
    @Test
    void windowFor_closeLeadLongerThanOpenLead_closesOnTheOpeningDay() {
        when(systemSettingService.getSettingForTenant(COMPANY, IntakeWindowPolicy.OPEN_DAYS_KEY)).thenReturn("10");
        when(systemSettingService.getSettingForTenant(COMPANY, IntakeWindowPolicy.CLOSE_DAYS_KEY)).thenReturn("30");

        IntakeWindowPolicy.Window window = policy.windowFor(COMPANY, MOVE_IN);

        assertThat(window.close()).isEqualTo(window.open());
    }
}
