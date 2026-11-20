# Release notes: next release

| Field | Value |
|-------|-------|
| **Range** | `fdf90c90..origin/develop` (`92d51649`) |
| **Previous release** | `main` at `12f16061` (#574, 16 November 2026), whose tree equals `develop` at `fdf90c90` (`fix(infra): count ECR builds by tag, not every manifest (#563)`) |
| **PROD today** | `92d27435` (#558), from Deploy PROD run 8 on 15 November. #561 and #574 are on NONPROD but not on PROD. Read [PROD is one release behind](#prod-is-one-release-behind) first. |
| **Pull requests** | 1 (see [Included pull requests](#included-pull-requests)) |
| **Flyway migrations** | None. The latest stays `V2026_11_15_1000`. |
| **UAT plan** | No separate plan. The release checks are in [After deploy](#after-deploy); the academic year cases are in [`test-cases/11-REF.md`](../../test-cases/11-REF.md). |

## Summary

Platform admins and platform staff can assign an academic year the platform created to a company.
The company gets its own copy of the year, with the same name, dates, institution and intakes, and
can change it like any year it created. The platform year stays as it is, so it can go to several
companies.

This closes a gap the 11 November release left. Since #509 a company takes applications only for
its own academic years, and a platform admin could not create a year for a specific company
(#508). A platform admin can now set up a company's calendar by assigning a platform year to it.

The platform's academic year list now shows who owns each year, since it mixes every company's
years and a year and its copy looked the same.

There are no migrations, settings, environment variables, jobs or emails in this release. On its
own, it rolls back with a plain redeploy.

## New for platform admins and platform staff

- **Assign to a company.** On `/admin/academic-years`, each platform year has a building icon,
  "Assign to a company", next to edit and delete. It opens a list of active companies. The company
  gets its own copy of the year and its intakes, with the same dates and application windows. The
  page confirms with "2027 assigned to Acme Residences. They can now change their copy." (#576)
- **The platform year does not change.** It can be assigned to several companies, and
  applications already filed under it keep pointing at it. (#576)
- **The company's rules apply to the copy.** A name the company already uses for that institution,
  or dates that overlap one of its years for that institution, are refused, and the dialog shows
  the reason. Assigning the same year twice to one company is therefore refused the second time.
  (#576)
- **Owner label.** Each year in the list shows "Platform" or the name of the company that owns it.
  (#576)

## New for company admins

- A year assigned to your company appears in your list like a year you created. You can edit it,
  add or change intakes, close applications, copy it and, while it has no applications, delete it.
  Changes you make do not reach the platform year, and later changes to the platform year do not
  reach your copy. (#576)
- You are not told when a year is assigned to your company. Nothing else changes on your screen:
  company admins see no owner labels and cannot assign. (#576)

## New for students

Nothing changes directly. Once a company has a year, its intakes' application windows decide when
students can apply to that company, as for a year the company created.

## Fixes and security hardening

None in this release.

## Operations and deployment

### PROD is one release behind

The last Deploy PROD run (run 8, 15 November) deployed `92d27435`, the #558 release. No Deploy PROD
run has deployed what reached `main` after it, so the PROD images do not have:

| PR | What it does |
|----|--------------|
| #561 (hotfix) | fix(waitlist): stop the dashboard showing a false waiting-list error |
| #564 | fix(allocation): scope the waiting-list rescore to the admin's tenant |
| #565 | fix(documents): let students upload a company's own required type |
| #566 | refactor(email): remove the unused welcome and password-reset templates |
| #567 | fix(documents): send each expiry reminder at most once a day |
| #570 | refactor(frontend): split the allocate dialog into field components |

The rest of #574 does not change the images: #563 is Terraform, applied by hand; #569 changes a
test only; and #572 adds the Restart PROD workflow, which works from `main` already.

#567 adds the migration `V2026_11_15_1000__document_expiry_reminded_at.sql`
(`ALTER TABLE document ADD COLUMN expiry_reminded_at TIMESTAMP`, nullable, empty until the next
reminder).

Deploy PROD always deploys a whole NONPROD run, so the next PROD deploy of `main` carries all of
the above as well as #576. Recommended order:

1. Run **Deploy PROD** with the #574 NONPROD run id `43933310771` (Deploy NONPROD run 79, already
   green), and check PROD.
2. Then merge this release into `main` and promote its NONPROD run as usual.

That keeps the migration in its own deploy, so this release can still be rolled back with a plain
redeploy. If PROD was updated by hand since 15 November, this section does not apply.

### Flyway migrations

None. The schema is the same as after `V2026_11_15_1000`.

### Settings, environment, jobs and emails

- No new or changed settings, environment variables or `application.yml` keys.
- No new scheduled jobs, email templates or notifications.
- No infrastructure, nginx or workflow changes.

### New and changed API endpoints

New:

| Method | Path | Roles | PR |
|--------|------|-------|----|
| POST | `/api/academic-years/{id}/assign` | PLATFORM_ADMIN, PLATFORM_STAFF | #576 |

Request body: `{"tenantId": 42}`. Responses:

| Case | Status | Message |
|------|--------|---------|
| Assigned | 201 | The company's new year, with `companyName` set |
| `tenantId` missing | 400 | `Choose a company` |
| The year already belongs to a company | 400 | `{name} already belongs to a company. Only a platform year can be assigned.` |
| `tenantId` is 0 | 400 | `Choose a company. The platform already has this year.` |
| Company deactivated | 400 | `Property company '{name}' is deactivated and cannot be given new records. Reactivate it first.` |
| Year or company not found | 404 | `Academic year not found with ID: {id}` or `Property company not found with id: {id}` |
| Name taken, or dates overlap a company year for the same institution | 409 | `There is already a year called '{name}' for {institution}` or `These dates overlap ...` |
| Caller is TENANT_ADMIN, BUILDING_STAFF or STUDENT | 403 | |

Changed:

| Endpoint | Change | PR |
|----------|--------|----|
| `GET /api/academic-years` | Each year gains `companyName`: the owning company's name for platform callers, empty for platform years and for company admin callers. | #576 |
| `AcademicYearResponse` | Gains `companyName`. It is set only by the two endpoints above. | #576 |

### Audit trail and logs

- Each assignment writes an `activity_log` row with action `ASSIGN`, entity type `ACADEMIC_YEAR`,
  the platform year's id, and the description `Assigned academic year {id} to a company`.
- The backend logs `action=academicYearAssigned sourceId={platform year} academicYearId={copy}
  tenantId={company}`.

### Before deploy

1. Decide the PROD order in [PROD is one release behind](#prod-is-one-release-behind).
2. Optionally, list the active companies with no current or upcoming year, which a platform admin
   may want to assign one to, and the platform years that can be assigned:

   ```sql
   SELECT t.id, t.name
   FROM tenant t
   WHERE t.id <> 0
     AND t.is_active
     AND NOT EXISTS (
       SELECT 1 FROM academic_year y
       WHERE y.tenant_id = t.id AND y.end_date >= CURRENT_DATE
     )
   ORDER BY t.name;

   SELECT id, name, institution_id, start_date, end_date
   FROM academic_year
   WHERE tenant_id = 0 AND end_date >= CURRENT_DATE
   ORDER BY start_date;
   ```

3. Tell platform admins and platform staff about "Assign to a company", and company admins that a
   year can now appear in their list without them creating it.

### After deploy

On NONPROD:

1. Sign in as a platform admin and open `/admin/academic-years`. Every year shows "Platform" or a
   company name, and only platform years have the building icon.
2. Assign a platform year to a test company. The confirmation names the company, a year with the
   same name, dates and intakes appears under that company, and the platform year is unchanged.
3. Assign the same year to the same company again. The dialog shows "There is already a year
   called ..." and nothing is added.
4. Sign in as that company's admin. The year is in their list, with no owner labels and no assign
   button. Change an intake on the copy and check the platform year's intake did not change.
5. On the activity log screen, an `ASSIGN` entry is listed for the platform year.
6. Delete the test copy if it is not wanted. That is allowed while it has no applications.

On PROD, run the "Checks after any deploy" in `docs/PROD_RUNBOOK.md`. Do not assign a year on
PROD only to test it: the copy is a real year that the company admins see, and students can apply
under it as soon as one of its intake windows is open.

### Rollback

- **This release on its own** (with #574 already on PROD): it applies no migration, so run
  **Deploy PROD** again with the previous release's NONPROD run id, `43933310771` (#574), as
  `docs/PROD_RUNBOOK.md` ("Rolling back") allows. Years created by assigning stay. The previous
  release lists and edits them as ordinary company years; it only lacks the owner label and the
  assign button.
- **Together with #574 in one deploy**: the deploy also applies `V2026_11_15_1000`, and by the
  runbook's rule a rollback past a migration needs a database restore with the previous images
  (`docs/BACKUP_AND_RESTORE.md`). The NONPROD run for the #558 commit PROD runs now is
  `43318874586`. Otherwise fix forward.

## Breaking changes and behaviour changes

- No breaking API change. `companyName` is a new field and existing clients can ignore it.
- Platform roles can now put a year into a specific company, through assign. Before, a year they
  created always belonged to the platform (#508).
- For a company that had no year of its own and fell back to platform years,
  `GET /api/academic-years/current` returns the company's copy after an assignment, for the
  institutions the copy covers.

## Known limitations and open decisions

Decisions recorded in #576 that a person should confirm:

1. **PLATFORM_STAFF may assign.** Assigning changes company data, not the platform defaults that
   GH-238 keeps from staff. Staff also reach the other academic year endpoints through the role
   hierarchy (`PLATFORM_STAFF > TENANT_ADMIN` in `SecurityConfig`), so they can create, edit and
   delete years too. The 11 November notes say staff can no longer do that; the hierarchy says
   otherwise, and no test covers it either way.
2. **Copy, not move.** The copy is a snapshot. Nothing links it to the platform year, so later
   changes to either side stay on that side.
3. **Windows copied as they are.** Intake application windows are copied from the platform year,
   not worked out again from the company's own `intake_window_open_days_before` and
   `intake_window_close_days_before` settings. A window that has closed on the platform year is
   closed on the copy.
4. **No institution check.** Assigning does not check that the company has buildings for the
   copy's institution.
5. **No notice to the company.** Company admins get no email or in-app notification.
6. **Audit detail.** The activity log row names the platform year, not the company; the company is
   only in the backend log line.
7. **Activity log filters.** The activity log screen has no `ASSIGN` action or `ACADEMIC_YEAR`
   entity type in its filters, the same as the existing `COPY` action. The rows are listed but
   cannot be filtered to.
8. **"Copy last year" on the platform list** (from #508, not changed here). It copies the newest
   year in the institution group, which can be another company's year, because the platform list
   mixes every company.

How it was checked:

- CI on #576 and on the push to `develop` (CI run 614): backend build and tests, including three
  integration tests for assign over HTTP (platform admin assigns and the company admin then sees
  the year, a second assign gives 409, a company admin gets 403 and nothing is copied), frontend
  lint and tests, and both images.
- The page and dialog were checked in a browser against a mocked API only. NONPROD is the first
  run against a real backend.

## Included pull requests

In merge order on `develop`.

| PR | Title | Linked issues |
|----|-------|---------------|
| [#576](https://github.com/beezhub/student-accommodation-system/pull/576) | feat(academic-years): let the platform assign its years to a company | Closes #575 |
