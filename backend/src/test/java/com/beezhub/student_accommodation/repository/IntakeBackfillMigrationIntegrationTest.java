package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GH-494: every existing academic year gets exactly one ANNUAL intake with its own dates, so
 * students applying today notice no change. Flyway has already run when the context starts, so the
 * backfill script is run again here against a year inserted without an intake; it is written to be
 * idempotent, which this also checks. Starting the context at all proves {@code validate} accepts
 * the new schema.
 */
class IntakeBackfillMigrationIntegrationTest extends AbstractIntegrationTest {

    private static final String BACKFILL = "db/migration/V2026_11_10_1101__backfill_annual_intakes.sql";

    @Autowired JdbcTemplate jdbc;

    private Long yearId;

    @AfterEach
    void tearDown() {
        if (yearId != null) {
            jdbc.update("DELETE FROM academic_year WHERE id = ?", yearId);
        }
    }

    @Test
    void backfill_existingYear_getsExactlyOneAnnualIntakeWithItsDates() throws IOException {
        yearId = jdbc.queryForObject("""
                INSERT INTO academic_year (name, start_date, end_date, application_open_date, application_close_date, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, "BF-" + UUID.randomUUID().toString().substring(0, 8),
                Date.valueOf(LocalDate.of(2032, 2, 1)), Date.valueOf(LocalDate.of(2032, 11, 30)),
                Date.valueOf(LocalDate.of(2031, 9, 1)), Date.valueOf(LocalDate.of(2032, 1, 31)),
                TenantContext.PLATFORM_TENANT_ID);
        String script = new ClassPathResource(BACKFILL).getContentAsString(StandardCharsets.UTF_8);

        jdbc.execute(script);
        jdbc.execute(script);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM intake WHERE academic_year_id = ?", Integer.class, yearId))
                .isEqualTo(1);
        Map<String, Object> intake = jdbc.queryForMap("SELECT * FROM intake WHERE academic_year_id = ?", yearId);
        assertThat(intake.get("intake_type")).isEqualTo("ANNUAL");
        assertThat(intake.get("name")).isEqualTo("Annual");
        assertThat(((Date) intake.get("stay_start_date")).toLocalDate()).isEqualTo(LocalDate.of(2032, 2, 1));
        assertThat(((Date) intake.get("stay_end_date")).toLocalDate()).isEqualTo(LocalDate.of(2032, 11, 30));
        assertThat(((Date) intake.get("application_open_date")).toLocalDate()).isEqualTo(LocalDate.of(2031, 9, 1));
        assertThat(((Date) intake.get("application_close_date")).toLocalDate()).isEqualTo(LocalDate.of(2032, 1, 31));
        assertThat(((Number) intake.get("tenant_id")).longValue()).isEqualTo(TenantContext.PLATFORM_TENANT_ID);
    }

    @Test
    void migration_everyYearAlreadyInTheDatabase_hasAnIntake() {
        Integer yearsWithout = jdbc.queryForObject("""
                SELECT COUNT(*) FROM academic_year y
                WHERE y.start_date < y.end_date AND y.created_at < (SELECT MIN(installed_on) FROM flyway_schema_history
                      WHERE script = 'V2026_11_10_1101__backfill_annual_intakes.sql')
                  AND NOT EXISTS (SELECT 1 FROM intake i WHERE i.academic_year_id = y.id)
                """, Integer.class);

        assertThat(yearsWithout).isZero();
    }

    @Test
    void platformDefaults_forIntakeWindows_areSeeded() {
        assertThat(jdbc.queryForList("""
                SELECT setting_value FROM system_settings
                WHERE tenant_id = 0 AND setting_key IN ('intake_window_open_days_before', 'intake_window_close_days_before')
                ORDER BY setting_key DESC
                """, String.class)).containsExactly("120", "14");
    }
}
