-- GH-495 step 4: one active application per student per company per academic year.
--
-- ux_application_student_tenant_active allowed one per company in any year, so a student could not
-- hold a 2026 and a 2027 application with the same company. The new key adds the year and keeps the
-- same status predicate, which must match ApplicationSubmissionService.ACTIVE_STATUSES exactly.
--
-- No repair is needed first: the new key is looser than the old one, so every row the old index
-- allowed is allowed here. NULLS NOT DISTINCT keeps rows without a year (only those step 3 could
-- not place) at one active per company, as before.
--
-- Named, because ApplicationSubmissionService reads the name off the violation to report it as a
-- duplicate. Renaming it here without renaming ACTIVE_APPLICATION_INDEX turns a 409 into a 500.
CREATE UNIQUE INDEX ux_application_student_tenant_year_active
    ON application (student_id, tenant_id, academic_year_id) NULLS NOT DISTINCT
 WHERE status IN ('PENDING', 'UNDER_REVIEW', 'CHANGES_REQUESTED',
                  'APPROVED', 'AWAITING_ALLOCATION', 'ALLOCATED');

DROP INDEX IF EXISTS ux_application_student_tenant_active;
