package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    /** Tenant-scoped lookup — Hibernate tenantFilter applies automatically. */
    Optional<SystemSetting> findBySettingKey(String settingKey);

    /**
     * Platform-level lookup (tenant_id = 0).
     * Uses a native query to bypass the Hibernate tenantFilter,
     * which would otherwise exclude rows with tenant_id = 0 for non-platform sessions.
     */
    @Query(value = "SELECT * FROM system_settings WHERE tenant_id = 0 AND setting_key = :key LIMIT 1",
           nativeQuery = true)
    Optional<SystemSetting> findPlatformSettingByKey(@Param("key") String key);

    /**
     * All platform-level settings (tenant_id = 0).
     * Uses a native query to bypass the Hibernate tenantFilter.
     */
    @Query(value = "SELECT * FROM system_settings WHERE tenant_id = 0 ORDER BY setting_key",
           nativeQuery = true)
    List<SystemSetting> findAllPlatformSettings();
}
