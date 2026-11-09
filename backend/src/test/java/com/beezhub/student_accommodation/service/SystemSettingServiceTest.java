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
import static org.mockito.ArgumentMatchers.argThat;
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

    // The isAutoAllocationEnabled_* cases that stood here are gone with the accessor. Auto-
    // allocation is gated by the FF4j `auto-allocation` flag alone now; the per-application path
    // read this setting while the batch path read the flag, so enabling one left the other
    // silently off (GH-207). The gate is covered in AutoAllocationServiceTest.
    //
    // KEY is still a platform-only key below: the row is deleted, and a tenant admin should not be
    // able to create an override for a key that no longer controls anything.

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

    @Test
    @DisplayName("updateSetting_whenKeyExists_updatesExistingSettingValue")
    void updateSetting_whenKeyExists_updatesExistingSettingValue() {
        when(systemSettingRepository.findBySettingKey(KEY))
                .thenReturn(Optional.of(tenant1Setting));

        systemSettingService.updateSetting(KEY, "updated-value");

        assertThat(tenant1Setting.getSettingValue()).isEqualTo("updated-value");
        verify(systemSettingRepository).save(tenant1Setting);
    }

    @Test
    @DisplayName("updateSetting_whenKeyNotExists_createsNewSetting")
    void updateSetting_whenKeyNotExists_createsNewSetting() {
        when(systemSettingRepository.findBySettingKey("new_key"))
                .thenReturn(Optional.empty());

        systemSettingService.updateSetting("new_key", "new_value");

        verify(systemSettingRepository).save(argThat(s ->
                "new_key".equals(s.getSettingKey()) && "new_value".equals(s.getSettingValue())));
    }

    // ── GH-127: auto_allocation_enabled is platform-only ─────────────────────

    @org.junit.jupiter.api.AfterEach
    void clearTenantContext() {
        com.beezhub.student_accommodation.service.tenant.TenantContext.clear();
    }

    @Test
    @DisplayName("updateSetting_platformOnlyKeyAsTenantAdmin_rejectedWithoutWriting")
    void updateSetting_platformOnlyKeyAsTenantAdmin_rejectedWithoutWriting() {
        com.beezhub.student_accommodation.service.tenant.TenantContext.setTenantId(TENANT_1_ID);

        assertThatThrownBy(() -> systemSettingService.updateSetting(KEY, "true"))
                .isInstanceOf(com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException.class)
                .hasMessageContaining("platform administrator");

        verify(systemSettingRepository, never()).save(any(SystemSetting.class));
    }

    @Test
    @DisplayName("deleteSetting_platformOnlyKeyAsTenantAdmin_rejectedWithoutDeleting")
    void deleteSetting_platformOnlyKeyAsTenantAdmin_rejectedWithoutDeleting() {
        com.beezhub.student_accommodation.service.tenant.TenantContext.setTenantId(TENANT_1_ID);

        assertThatThrownBy(() -> systemSettingService.deleteSetting(KEY))
                .isInstanceOf(com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException.class);

        verify(systemSettingRepository, never()).delete(any(SystemSetting.class));
    }

    @Test
    @DisplayName("updateSetting_platformOnlyKeyInPlatformContext_allowed")
    void updateSetting_platformOnlyKeyInPlatformContext_allowed() {
        com.beezhub.student_accommodation.service.tenant.TenantContext.setTenantId(
                com.beezhub.student_accommodation.service.tenant.TenantContext.PLATFORM_TENANT_ID);
        when(systemSettingRepository.findBySettingKey(KEY)).thenReturn(Optional.empty());

        systemSettingService.updateSetting(KEY, "true");

        verify(systemSettingRepository).save(any(SystemSetting.class));
    }

    @Test
    @DisplayName("updateSetting_ordinaryKeyAsTenantAdmin_stillAllowed")
    void updateSetting_ordinaryKeyAsTenantAdmin_stillAllowed() {
        com.beezhub.student_accommodation.service.tenant.TenantContext.setTenantId(TENANT_1_ID);
        when(systemSettingRepository.findBySettingKey("application_review_days")).thenReturn(Optional.empty());

        systemSettingService.updateSetting("application_review_days", "5");

        verify(systemSettingRepository).save(any(SystemSetting.class));
    }

    // ── Platform-level and cascade paths ─────────────────────────────────────

    @Test
    @DisplayName("getPlatformSetting_whenRowExists_returnsValue")
    void getPlatformSetting_whenRowExists_returnsValue() {
        when(systemSettingRepository.findPlatformSettingByKey("k")).thenReturn(Optional.of(tenant1Setting));

        assertThat(systemSettingService.getPlatformSetting("k")).isEqualTo("true");
    }

    @Test
    @DisplayName("getPlatformSetting_whenRowAbsent_returnsNull")
    void getPlatformSetting_whenRowAbsent_returnsNull() {
        when(systemSettingRepository.findPlatformSettingByKey("k")).thenReturn(Optional.empty());

        assertThat(systemSettingService.getPlatformSetting("k")).isNull();
    }

    @Test
    @DisplayName("getAllPlatformSettings_returnsKeyValueMap")
    void getAllPlatformSettings_returnsKeyValueMap() {
        when(systemSettingRepository.findAllPlatformSettings()).thenReturn(List.of(tenant1Setting));

        assertThat(systemSettingService.getAllPlatformSettings()).containsExactly(Map.entry(KEY, "true"));
    }

    @Test
    @DisplayName("getSetting_whenNoTenantRow_fallsBackToPlatformValue")
    void getSetting_whenNoTenantRow_fallsBackToPlatformValue() {
        when(systemSettingRepository.findBySettingKey("k")).thenReturn(Optional.empty());
        when(systemSettingRepository.findPlatformSettingByKey("k")).thenReturn(Optional.of(tenant2Setting));

        assertThat(systemSettingService.getSetting("k")).isEqualTo("false");
    }

    @Test
    @DisplayName("getAllSettings_tenantRowOverridesPlatformValue")
    void getAllSettings_tenantRowOverridesPlatformValue() {
        SystemSetting platformOnly = SystemSetting.builder()
                .tenantId(0L).settingKey("platform_only").settingValue("p").build();
        SystemSetting platformDefault = SystemSetting.builder()
                .tenantId(0L).settingKey(KEY).settingValue("default").build();
        when(systemSettingRepository.findAllPlatformSettings()).thenReturn(List.of(platformOnly, platformDefault));
        when(systemSettingRepository.findAll()).thenReturn(List.of(tenant1Setting));

        assertThat(systemSettingService.getAllSettings())
                .containsEntry("platform_only", "p")
                .containsEntry(KEY, "true");
    }

    @Test
    @DisplayName("updatePlatformSetting_whenRowExists_updatesValue")
    void updatePlatformSetting_whenRowExists_updatesValue() {
        when(systemSettingRepository.findPlatformSettingByKey(KEY)).thenReturn(Optional.of(tenant1Setting));

        systemSettingService.updatePlatformSetting(KEY, "changed");

        assertThat(tenant1Setting.getSettingValue()).isEqualTo("changed");
        verify(systemSettingRepository).save(tenant1Setting);
    }

    @Test
    @DisplayName("updatePlatformSetting_whenRowAbsent_createsRowOnPlatformTenant")
    void updatePlatformSetting_whenRowAbsent_createsRowOnPlatformTenant() {
        when(systemSettingRepository.findPlatformSettingByKey("new_key")).thenReturn(Optional.empty());

        systemSettingService.updatePlatformSetting("new_key", "v");

        verify(systemSettingRepository).save(argThat(s ->
                Long.valueOf(0L).equals(s.getTenantId())
                        && "new_key".equals(s.getSettingKey())
                        && "v".equals(s.getSettingValue())));
    }

    @Test
    @DisplayName("deleteSetting_whenTenantOverrideExists_deletesIt")
    void deleteSetting_whenTenantOverrideExists_deletesIt() {
        when(systemSettingRepository.findBySettingKey("application_review_days"))
                .thenReturn(Optional.of(tenant1Setting));

        systemSettingService.deleteSetting("application_review_days");

        verify(systemSettingRepository).delete(tenant1Setting);
    }

    @Test
    @DisplayName("deleteSetting_whenOnlyPlatformRowFound_throwsNotFoundAndKeepsRow")
    void deleteSetting_whenOnlyPlatformRowFound_throwsNotFoundAndKeepsRow() {
        SystemSetting platformRow = SystemSetting.builder()
                .tenantId(0L).settingKey("application_review_days").settingValue("5").build();
        when(systemSettingRepository.findBySettingKey("application_review_days"))
                .thenReturn(Optional.of(platformRow));

        assertThatThrownBy(() -> systemSettingService.deleteSetting("application_review_days"))
                .isInstanceOf(com.beezhub.student_accommodation.exceptions.ResourceNotFoundException.class);

        verify(systemSettingRepository, never()).delete(any(SystemSetting.class));
    }

    @Test
    @DisplayName("deleteSetting_whenNoRowFound_throwsNotFound")
    void deleteSetting_whenNoRowFound_throwsNotFound() {
        when(systemSettingRepository.findBySettingKey("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> systemSettingService.deleteSetting("missing"))
                .isInstanceOf(com.beezhub.student_accommodation.exceptions.ResourceNotFoundException.class);
    }
}
