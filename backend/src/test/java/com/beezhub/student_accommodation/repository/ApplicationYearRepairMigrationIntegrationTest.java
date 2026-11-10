package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GH-495 step 3: rows pointing at another company's year end up on a copy owned by their own
 * company, with its Annual intake, and nothing is deleted. Flyway has already run when the context
 * starts, so the script is run again here, in a transaction as Flyway runs it, against rows inserted
 * afterwards; it is written to be idempotent, which this also checks.
 */
class ApplicationYearRepairMigrationIntegrationTest extends AbstractIntegrationTest {

    private static final String REPAIR = "db/migration/V2026_11_10_1300__repair_application_years_and_intakes.sql";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private TransactionTemplate transactionTemplate;

    private final List<Long> tenants = new ArrayList<>();
    private Long userId;
    private Long studentId;
    private Long institutionId;
    private Long companyA;
    private Long companyB;
    private Long yearOfB;

    @BeforeEach
    void setUp() {
        companyA = tenant("Repair A");
        companyB = tenant("Repair B");
        institutionId = jdbc.queryForObject("INSERT INTO institution (name) VALUES (?) RETURNING id", Long.class,
                "Repair U " + suffix());
        userId = jdbc.queryForObject("""
                INSERT INTO app_user (email, user_password, user_role, first_name, last_name, email_verified, tenant_id)
                VALUES (?, 'x', 'STUDENT', 'Re', 'Pair', true, 0) RETURNING id
                """, Long.class, "repair-" + suffix() + "@example.com");
        studentId = jdbc.queryForObject("""
                INSERT INTO student (student_number, registration_date, user_id, institution_id)
                VALUES (?, CURRENT_DATE, ?, ?) RETURNING id
                """, Long.class, "R-" + suffix(), userId, institutionId);
        yearOfB = jdbc.queryForObject("""
                INSERT INTO academic_year (name, start_date, end_date, application_open_date, application_close_date, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, "R" + suffix(), Date.valueOf(LocalDate.of(2033, 2, 1)),
                Date.valueOf(LocalDate.of(2033, 11, 30)), Date.valueOf(LocalDate.of(2032, 9, 1)),
                Date.valueOf(LocalDate.of(2033, 1, 31)), companyB);
        jdbc.update("""
                INSERT INTO intake (tenant_id, academic_year_id, intake_type, name, stay_start_date, stay_end_date,
                                    application_open_date, application_close_date)
                VALUES (?, ?, 'ANNUAL', 'Annual', '2033-02-01', '2033-11-30', '2032-09-01', '2033-01-31')
                """, companyB, yearOfB);
    }

    @AfterEach
    void tearDown() {
        for (Long tenant : tenants) {
            jdbc.update("DELETE FROM application WHERE tenant_id = ?", tenant);
            jdbc.update("DELETE FROM academic_year WHERE tenant_id = ?", tenant);
        }
        jdbc.update("DELETE FROM student WHERE id = ?", studentId);
        jdbc.update("DELETE FROM app_user WHERE id = ?", userId);
        tenants.forEach(id -> jdbc.update("DELETE FROM tenant WHERE id = ?", id));
        jdbc.update("DELETE FROM institution WHERE id = ?", institutionId);
    }

    @Test
    void repair_applicationOnAnotherCompanysYear_movesToACopyOwnedByItsOwnCompany() throws IOException {
        Long applicationId = application(companyA, yearOfB, LocalDate.of(2032, 10, 1));

        runRepairTwice();

        Map<String, Object> row = jdbc.queryForMap("""
                SELECT a.academic_year_id, a.intake_id, a.institution_id, y.tenant_id AS year_tenant, y.name,
                       y.start_date, y.end_date, i.tenant_id AS intake_tenant, i.intake_type, i.academic_year_id AS intake_year
                FROM application a
                JOIN academic_year y ON y.id = a.academic_year_id
                JOIN intake i ON i.id = a.intake_id
                WHERE a.id = ?
                """, applicationId);
        String sourceName = jdbc.queryForObject("SELECT name FROM academic_year WHERE id = ?", String.class, yearOfB);
        assertThat(((Number) row.get("year_tenant")).longValue()).isEqualTo(companyA);
        assertThat(row.get("name")).isEqualTo(sourceName);
        assertThat(((Date) row.get("start_date")).toLocalDate()).isEqualTo(LocalDate.of(2033, 2, 1));
        assertThat(((Date) row.get("end_date")).toLocalDate()).isEqualTo(LocalDate.of(2033, 11, 30));
        assertThat(((Number) row.get("intake_tenant")).longValue()).isEqualTo(companyA);
        assertThat(row.get("intake_type")).isEqualTo("ANNUAL");
        assertThat(row.get("intake_year")).isEqualTo(row.get("academic_year_id"));
        assertThat(((Number) row.get("institution_id")).longValue()).isEqualTo(institutionId);

        // Nothing deleted, and run twice still gives company A exactly one copy.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM academic_year WHERE id = ?", Integer.class, yearOfB))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM academic_year WHERE tenant_id = ? AND name = ?",
                Integer.class, companyA, sourceName)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM intake i JOIN academic_year y ON y.id = i.academic_year_id "
                + "WHERE y.tenant_id = ? AND y.name = ?", Integer.class, companyA, sourceName)).isEqualTo(1);
    }

    @Test
    void repair_applicationWithNoYear_takesItsCompanysYearContainingTheDate() throws IOException {
        Long yearOfA = jdbc.queryForObject("""
                INSERT INTO academic_year (name, start_date, end_date, application_open_date, application_close_date, tenant_id)
                VALUES (?, '2034-02-01', '2034-11-30', '2033-09-01', '2034-01-31', ?) RETURNING id
                """, Long.class, "A" + suffix(), companyA);
        Long annualOfA = jdbc.queryForObject("""
                INSERT INTO intake (tenant_id, academic_year_id, intake_type, name, stay_start_date, stay_end_date,
                                    application_open_date, application_close_date)
                VALUES (?, ?, 'ANNUAL', 'Annual', '2034-02-01', '2034-11-30', '2033-09-01', '2034-01-31') RETURNING id
                """, Long.class, companyA, yearOfA);
        Long applicationId = application(companyA, null, LocalDate.of(2034, 3, 1));

        runRepairTwice();

        Map<String, Object> row = jdbc.queryForMap("SELECT academic_year_id, intake_id FROM application WHERE id = ?",
                applicationId);
        assertThat(((Number) row.get("academic_year_id")).longValue()).isEqualTo(yearOfA);
        assertThat(((Number) row.get("intake_id")).longValue()).isEqualTo(annualOfA);
    }

    private void runRepairTwice() throws IOException {
        String script = new ClassPathResource(REPAIR).getContentAsString(StandardCharsets.UTF_8);
        transactionTemplate.executeWithoutResult(status -> jdbc.execute(script));
        transactionTemplate.executeWithoutResult(status -> jdbc.execute(script));
    }

    private Long application(Long tenant, Long year, LocalDate applied) {
        return jdbc.queryForObject("""
                INSERT INTO application (application_code, application_date, status, student_id, academic_year_id, tenant_id)
                VALUES (?, ?, 'CANCELLED', ?, ?, ?) RETURNING id
                """, Long.class, "REP-" + suffix(), Date.valueOf(applied), studentId, year, tenant);
    }

    private Long tenant(String name) {
        Long id = jdbc.queryForObject("INSERT INTO tenant (name, is_active) VALUES (?, true) RETURNING id", Long.class,
                name + " " + suffix());
        tenants.add(id);
        return id;
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
