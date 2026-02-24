package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.entity.SystemSetting;
import com.beezhub.student_accommodation.repository.SystemSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.dao.IncorrectResultSizeDataAccessException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SystemSettingService}.
 *
 * <p>Tenant isolation is enforced at the persistence layer by Hibernate's {@code tenantFilter}.
 * These tests verify that the service layer faithfully delegates to the repository and does not
 * inadvertently return values belonging to a different tenant.
 *
 * <p>Two scenarios are covered:
 * <ol>
 *   <li><strong>Filter active</strong> – {@link #getSettingByKey_returnsOnlyCurrentTenantValue()}
 *       simulates the normal request path: the repository mock returns only the current-tenant
 *       setting (as the real Hibernate filter would), and the service returns that value — not a
 *       value from another tenant's row.</li>
 *   <li><strong>Filter disabled</strong> –
 *       {@link #getSetting_whenTenantFilterDisabledAndMultipleRowsExist_throwsIncorrectResultSize()}
 *       and
 *       {@link #updateSetting_whenTenantFilterDisabledAndMultipleRowsExist_throwsIncorrectResultSize()}
 *       cover the failure mode that occurs in contexts without the filter (e.g. scheduled jobs or
 *       {@code @Async} tasks): when multiple tenants share the same {@code setting_key}, the
 *       repository throws {@link org.springframework.dao.IncorrectResultSizeDataAccessException}
 *       rather than silently returning an arbitrary tenant's row. Callers in such contexts must
 *       explicitly enable the Hibernate filter on the current session before querying.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class SystemSettingServiceTest {

    @Mock
    private SystemSettingRepository systemSettingRepository;

    @InjectMocks
    private SystemSettingService systemSettingService;

    private static final Long TENANT_1_ID = 1L;
    private static final Long TENANT_2_ID = 2L;
    private static final String KEY = "auto_allocation_enabled";

    private SystemSetting tenant1Setting;
    private SystemSetting tenant2Setting;

    @BeforeEach
    void setUp() {
        tenant1Setting = SystemSetting.builder()
                .id(1L)
                .tenantId(TENANT_1_ID)
                .settingKey(KEY)
                .settingValue("true")
                .build();

        tenant2Setting = SystemSetting.builder()
                .id(2L)
                .tenantId(TENANT_2_ID)
                .settingKey(KEY)
                .settingValue("false")
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // TASK-1.03: Tenant-scoped setting lookup
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getSettingByKey_returnsOnlyCurrentTenantValue")
    void getSettingByKey_returnsOnlyCurrentTenantValue() {
        // Arrange: repository (with tenant filter active for tenant 1) returns only tenant 1's row
        when(systemSettingRepository.findBySettingKey(KEY))
                .thenReturn(Optional.of(tenant1Setting));

        // Act
        String result = systemSettingService.getSetting(KEY);

        // Assert: service returns tenant 1's value, not tenant 2's "false"
        assertThat(result).isEqualTo("true");
        verify(systemSettingRepository).findBySettingKey(KEY);
        verifyNoMoreInteractions(systemSettingRepository);
    }

    @Test
    @DisplayName("getSetting_whenKeyAbsentForCurrentTenant_returnsNull")
    void getSetting_whenKeyAbsentForCurrentTenant_returnsNull() {
        // Arrange: repository returns empty (key exists for another tenant, filter excludes it)
        when(systemSettingRepository.findBySettingKey(KEY))
                .thenReturn(Optional.empty());

        // Act
        String result = systemSettingService.getSetting(KEY);

        // Assert: service returns null — it does not fall back to another tenant's value
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getAllSettings_returnsOnlyCurrentTenantSettings")
    void getAllSettings_returnsOnlyCurrentTenantSettings() {
        // Arrange: repository (with tenant filter active for tenant 1) returns only tenant 1's rows
        SystemSetting otherSetting = SystemSetting.builder()
                .id(3L)
                .tenantId(TENANT_1_ID)
                .settingKey("offer_expiry_days")
                .settingValue("7")
                .build();
        when(systemSettingRepository.findAll())
                .thenReturn(List.of(tenant1Setting, otherSetting));

        // Act
        Map<String, String> result = systemSettingService.getAllSettings();

        // Assert: only tenant 1's settings are present
        assertThat(result).hasSize(2)
                .containsEntry(KEY, "true")
                .containsEntry("offer_expiry_days", "7");
    }

    @Test
    @DisplayName("isAutoAllocationEnabled_returnsTrueWhenSettingIsTrue")
    void isAutoAllocationEnabled_returnsTrueWhenSettingIsTrue() {
        when(systemSettingRepository.findBySettingKey(SystemSettingService.AUTO_ALLOCATION_ENABLED))
                .thenReturn(Optional.of(tenant1Setting)); // tenant1Setting has value "true"

        assertThat(systemSettingService.isAutoAllocationEnabled()).isTrue();
    }

    @Test
    @DisplayName("isAutoAllocationEnabled_returnsFalseWhenSettingIsFalse")
    void isAutoAllocationEnabled_returnsFalseWhenSettingIsFalse() {
        // Use a current-tenant setting whose value is "false" to test boolean parsing
        SystemSetting tenant1FalseSetting = SystemSetting.builder()
                .id(4L)
                .tenantId(TENANT_1_ID)
                .settingKey(SystemSettingService.AUTO_ALLOCATION_ENABLED)
                .settingValue("false")
                .build();

        when(systemSettingRepository.findBySettingKey(SystemSettingService.AUTO_ALLOCATION_ENABLED))
                .thenReturn(Optional.of(tenant1FalseSetting));

        assertThat(systemSettingService.isAutoAllocationEnabled()).isFalse();
    }

    @Test
    @DisplayName("isAutoAllocationEnabled_returnsFalseWhenSettingAbsent")
    void isAutoAllocationEnabled_returnsFalseWhenSettingAbsent() {
        when(systemSettingRepository.findBySettingKey(SystemSettingService.AUTO_ALLOCATION_ENABLED))
                .thenReturn(Optional.empty());

        assertThat(systemSettingService.isAutoAllocationEnabled()).isFalse();
    }

    // ─────────────────────────────────────────────────────────────
    // TASK-1.03: Filter-disabled failure mode (cross-tenant clash)
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getSetting_whenTenantFilterDisabledAndMultipleRowsExist_throwsIncorrectResultSize")
    void getSetting_whenTenantFilterDisabledAndMultipleRowsExist_throwsIncorrectResultSize() {
        // Simulates the failure mode where tenantFilter is NOT active (e.g. a scheduled-job or
        // @Async context) and two tenant rows share the same setting_key.  The repository throws
        // IncorrectResultSizeDataAccessException rather than silently returning an arbitrary row.
        when(systemSettingRepository.findBySettingKey(KEY))
                .thenThrow(new IncorrectResultSizeDataAccessException(1, 2));

        // Assert: exception propagates — callers in system-level contexts must ensure the filter
        // is enabled before invoking any key-based lookup.
        assertThatThrownBy(() -> systemSettingService.getSetting(KEY))
                .isInstanceOf(IncorrectResultSizeDataAccessException.class);
    }

    @Test
    @DisplayName("updateSetting_whenTenantFilterDisabledAndMultipleRowsExist_throwsIncorrectResultSize")
    void updateSetting_whenTenantFilterDisabledAndMultipleRowsExist_throwsIncorrectResultSize() {
        // Same failure mode for the write path: updateSetting also calls findBySettingKey and
        // must not silently overwrite an arbitrary tenant's row when the filter is absent.
        when(systemSettingRepository.findBySettingKey(KEY))
                .thenThrow(new IncorrectResultSizeDataAccessException(1, 2));

        assertThatThrownBy(() -> systemSettingService.updateSetting(KEY, "new-value"))
                .isInstanceOf(IncorrectResultSizeDataAccessException.class);
    }
}
