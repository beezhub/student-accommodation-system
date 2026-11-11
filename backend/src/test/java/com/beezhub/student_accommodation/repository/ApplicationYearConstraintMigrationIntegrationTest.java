package com.beezhub.student_accommodation.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * GH-496: the last backfill before application.academic_year_id and intake_id become NOT NULL, and
 * the constraint itself, which is left out rather than inventing a year for rows nothing can repair.
 *
 * <p>Flyway has already run when the context starts, so the columns are NOT NULL and the year's
 * window columns are gone. Each case puts the schema back as it was before those migrations, runs
 * the scripts, and rolls everything back; PostgreSQL DDL is transactional.
 */
class ApplicationYearConstraintMigrationIntegrationTest extends AbstractIntegrationTest {

    private static final String REPAIR = "db/migration/V2026_11_10_1700__repair_remaining_application_years_and_intakes.sql";
    private static final String CONSTRAINT = "db/migration/V2026_11_10_1710__application_year_intake_not_null.sql";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private TransactionTemplate transactionTemplate;

    @Test
    void repair_yearWithoutIntake_getsAnAnnualIntake_andTheConstraintIsAdded() {
        transactionTemplate.executeWithoutResult(status -> {
            beforeTheConstraint();
            Long company = tenant();
            Long student = student();
            Long year = jdbc.queryForObject("""
                    INSERT INTO academic_year (name, start_date, end_date, application_open_date, application_close_date, tenant_id)
                    VALUES (?, '2035-02-01', '2035-11-30', '2034-09-01', '2035-01-31', ?) RETURNING id
                    """, Long.class, "C" + suffix(), company);
            Long noIntake = application(company, year, student);
            Long noYear = application(company, null, student);

            run(REPAIR);
            run(REPAIR);

            Map<String, Object> intake = jdbc.queryForMap(
                    "SELECT intake_type, stay_start_date, application_open_date FROM intake WHERE academic_year_id = ?", year);
            assertThat(intake.get("intake_type")).isEqualTo("ANNUAL");
            assertThat(intake.get("stay_start_date").toString()).isEqualTo("2035-02-01");
            assertThat(intake.get("application_open_date").toString()).isEqualTo("2034-09-01");
            Long annual = jdbc.queryForObject("SELECT id FROM intake WHERE academic_year_id = ?", Long.class, year);
            assertThat(intakeOf(noIntake)).isEqualTo(annual);
            assertThat(jdbc.queryForObject("SELECT academic_year_id FROM application WHERE id = ?", Long.class, noYear))
                    .isEqualTo(year);

            // Only this test's rows were empty, so both constraints go on.
            jdbc.update("UPDATE application SET academic_year_id = ? WHERE academic_year_id IS NULL", year);
            jdbc.update("UPDATE application SET intake_id = ? WHERE intake_id IS NULL", annual);
            run(CONSTRAINT);
            assertThat(nullable("academic_year_id")).isEqualTo("NO");
            assertThat(nullable("intake_id")).isEqualTo("NO");
            status.setRollbackOnly();
        });
    }

    @Test
    void constraint_companyWithNoYear_leftNullableAndNothingDeleted() {
        transactionTemplate.executeWithoutResult(status -> {
            beforeTheConstraint();
            Long company = tenant();
            Long orphan = application(company, null, student());

            run(REPAIR);
            run(CONSTRAINT);

            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM application WHERE id = ?", Integer.class, orphan))
                    .isEqualTo(1);
            assertThat(nullable("academic_year_id")).isEqualTo("YES");
            assertThat(nullable("intake_id")).isEqualTo("YES");
            status.setRollbackOnly();
        });
    }

    /** The schema as it was before V2026_11_10_1700 to 1720, inside the test's transaction. */
    private void beforeTheConstraint() {
        jdbc.execute("ALTER TABLE application ALTER COLUMN academic_year_id DROP NOT NULL");
        jdbc.execute("ALTER TABLE application ALTER COLUMN intake_id DROP NOT NULL");
        jdbc.execute("ALTER TABLE academic_year ADD COLUMN application_open_date DATE");
        jdbc.execute("ALTER TABLE academic_year ADD COLUMN application_close_date DATE");
    }

    private void run(String script) {
        try {
            jdbc.execute(new ClassPathResource(script).getContentAsString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String nullable(String column) {
        return jdbc.queryForObject("""
                SELECT is_nullable FROM information_schema.columns
                WHERE table_name = 'application' AND column_name = ?
                """, String.class, column);
    }

    private Long intakeOf(Long applicationId) {
        return jdbc.queryForObject("SELECT intake_id FROM application WHERE id = ?", Long.class, applicationId);
    }

    private Long tenant() {
        return jdbc.queryForObject("INSERT INTO tenant (name, is_active) VALUES (?, true) RETURNING id", Long.class,
                "Constraint " + suffix());
    }

    private Long student() {
        Long user = jdbc.queryForObject("""
                INSERT INTO app_user (email, user_password, user_role, first_name, last_name, email_verified, tenant_id)
                VALUES (?, 'x', 'STUDENT', 'Con', 'Straint', true, 0) RETURNING id
                """, Long.class, "constraint-" + suffix() + "@example.com");
        return jdbc.queryForObject("""
                INSERT INTO student (student_number, registration_date, user_id)
                VALUES (?, CURRENT_DATE, ?) RETURNING id
                """, Long.class, "C-" + suffix(), user);
    }

    private Long application(Long tenant, Long year, Long student) {
        return jdbc.queryForObject("""
                INSERT INTO application (application_code, application_date, status, student_id, academic_year_id, tenant_id)
                VALUES (?, '2035-03-01', 'CANCELLED', ?, ?, ?) RETURNING id
                """, Long.class, "CON-" + suffix(), student, year, tenant);
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
