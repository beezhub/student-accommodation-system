-- Remove the example rows the baseline seed puts on every database, where they are not wanted (#365).
--
-- V2026_02_19_2101__seed_reference_data.sql inserts two example institutions, a "2026" academic year
-- and the demo administrator next to the real reference data. Flyway migrations are immutable, so
-- they reach PROD too, where a student could pick "University of Example" on an application.
--
-- Controlled by the Flyway placeholder exampleData (EXAMPLE_DATA). The default, true, keeps the rows
-- for local development and NONPROD, which rely on them. PROD must set false, and cannot forget:
-- docker-compose.aws.yml requires EXAMPLE_DATA, so a deploy without it stops before Flyway runs. That
-- matters because this migration runs once; applied with true, it would never remove them.
--
-- Not removed, by decision on #365:
-- - Tenant 1 ("Default Tenant"). PROD keeps it and renames it to its real first tenant in the
--   platform admin UI, so nothing set up inside it is lost.
-- - The reference data: document types, accommodation types, years of study, the platform tenant.
--
-- Each row goes only if nothing points at it. The foreign keys would otherwise do real damage: SET
-- NULL empties a real student's institution or a real application's academic year, and CASCADE
-- deletes building links and the demo administrator's audit history. A row still in use stays, and
-- the WARNING below names it in the backend's startup log.
--
-- Rows are matched on every seeded value, not just the name, so a real institution or year that
-- happens to share a name is never touched.

DO $$
BEGIN
    IF lower('${exampleData}') = 'true' THEN
        RETURN;
    END IF;

    DELETE FROM institution i
    WHERE ((i.name = 'University of Example' AND i.email = 'UniversityOfExample@example.com')
        OR (i.name = 'Example College' AND i.email = 'ExampleCollege@example.com'))
      AND NOT EXISTS (SELECT 1 FROM student s WHERE s.institution_id = i.id)
      AND NOT EXISTS (SELECT 1 FROM building_institution b WHERE b.institution_id = i.id);

    IF EXISTS (SELECT 1 FROM institution
               WHERE email IN ('UniversityOfExample@example.com', 'ExampleCollege@example.com')) THEN
        RAISE WARNING 'action=exampleDataKept table=institution reason=referencedByStudentOrBuilding';
    END IF;

    DELETE FROM academic_year y
    WHERE y.name = '2026'
      AND y.tenant_id = 1
      AND y.start_date = DATE '2026-02-01'
      AND y.end_date = DATE '2026-11-30'
      AND NOT EXISTS (SELECT 1 FROM application a WHERE a.academic_year_id = y.id)
      AND NOT EXISTS (SELECT 1 FROM room_allocation r WHERE r.academic_year_id = y.id);

    IF EXISTS (SELECT 1 FROM academic_year
               WHERE name = '2026' AND tenant_id = 1
                 AND start_date = DATE '2026-02-01' AND end_date = DATE '2026-11-30') THEN
        RAISE WARNING 'action=exampleDataKept table=academic_year reason=referencedByApplicationOrAllocation';
    END IF;

    -- Locked since V2026_09_05_1000__disable_seeded_admin.sql, so it should have no history, but
    -- every table that points at app_user is checked rather than assumed.
    DELETE FROM app_user u
    WHERE u.email = 'admin@studentaccommodation.com'
      AND u.user_password = 'DISABLED-NO-LOGIN'
      AND NOT EXISTS (SELECT 1 FROM student t WHERE t.user_id = u.id)
      AND NOT EXISTS (SELECT 1 FROM activity_logs l WHERE l.user_id = u.id)
      AND NOT EXISTS (SELECT 1 FROM allocation_audit_log l WHERE l.admin_user_id = u.id)
      AND NOT EXISTS (SELECT 1 FROM allocation_rules r WHERE r.created_by = u.id)
      AND NOT EXISTS (SELECT 1 FROM document_comments c WHERE c.admin_id = u.id)
      AND NOT EXISTS (SELECT 1 FROM staged_allocations s WHERE s.proposed_by = u.id)
      AND NOT EXISTS (SELECT 1 FROM notification n WHERE n.user_id = u.id)
      AND NOT EXISTS (SELECT 1 FROM user_notifications n WHERE n.user_id = u.id)
      AND NOT EXISTS (SELECT 1 FROM password_reset_token t WHERE t.user_id = u.id)
      AND NOT EXISTS (SELECT 1 FROM email_verification_token t WHERE t.user_id = u.id);

    IF EXISTS (SELECT 1 FROM app_user WHERE email = 'admin@studentaccommodation.com') THEN
        RAISE WARNING 'action=exampleDataKept table=app_user reason=demoAdminReferencedOrNotLocked';
    END IF;
END $$;
