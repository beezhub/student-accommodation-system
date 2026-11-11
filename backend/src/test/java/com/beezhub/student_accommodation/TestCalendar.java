package com.beezhub.student_accommodation;

import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.model.entity.Intake;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.UUID;

/**
 * An academic year with one Annual intake for a company, for integration tests that save an
 * application. Since GH-496 {@code application.academic_year_id} and {@code intake_id} are NOT NULL.
 *
 * <p>Written with JDBC so it works whatever the tenant context; {@link #year()} and
 * {@link #intake()} are references by id, enough for an application's foreign keys. Call
 * {@link #delete()} after the applications that point at it are gone.
 */
public record TestCalendar(JdbcTemplate jdbc, Long yearId, Long intakeId) {

    /** A year far from today, so it never becomes any company's current year in another test. */
    public static TestCalendar create(JdbcTemplate jdbc, Long tenantId) {
        LocalDate start = LocalDate.of(2090, 2, 1);
        LocalDate end = LocalDate.of(2090, 11, 30);
        Long yearId = jdbc.queryForObject("""
                INSERT INTO academic_year (name, start_date, end_date, tenant_id)
                VALUES (?, ?, ?, ?) RETURNING id
                """, Long.class, "T" + UUID.randomUUID().toString().substring(0, 8), start, end, tenantId);
        Long intakeId = jdbc.queryForObject("""
                INSERT INTO intake (tenant_id, academic_year_id, intake_type, name, stay_start_date, stay_end_date,
                                    application_open_date, application_close_date)
                VALUES (?, ?, 'ANNUAL', 'Annual', ?, ?, ?, ?) RETURNING id
                """, Long.class, tenantId, yearId, start, end, start.minusDays(120), start.minusDays(14));
        return new TestCalendar(jdbc, yearId, intakeId);
    }

    public AcademicYear year() {
        return AcademicYear.builder().id(yearId).build();
    }

    public Intake intake() {
        return Intake.builder().id(intakeId).build();
    }

    /** Removes the year and its intake, once no application points at them. */
    public void delete() {
        jdbc.update("DELETE FROM intake WHERE id = ?", intakeId);
        jdbc.update("DELETE FROM academic_year WHERE id = ?", yearId);
    }
}
