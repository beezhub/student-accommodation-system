# Release notes: 11 November 2026

| Field | Value |
|-------|-------|
| **Released** | To `main` in #516 on 11 November 2026 |
| **Range** | `77f80ecf..origin/develop` (`7c5d0c3e`) |
| **Previous release** | `main`, whose tree equals `develop` at `77f80ecf` (`fix(admin): read the fields the API actually sends (#474)`) |
| **Pull requests** | 14 (see [Included pull requests](#included-pull-requests)) |
| **Flyway migrations** | 13, from `V2026_11_10_1000` to `V2026_11_11_1100` |
| **UAT plan** | [`test-cases/21-REL-NEXT.md`](../../test-cases/21-REL-NEXT.md) |

## Summary

This release delivers the academic years and intakes plan from #493. Each company now keeps its
own academic years, per institution it serves, with Annual, Semester or Trimester intakes inside
them. Students apply for an intake, at up to three companies on one form. Rooms are counted by
planned stay dates, so a 2026 resident can be offered a 2027 room and two semester students can
share one bed across the year. Places accepted long before move-in are reconfirmed, unfinished
applications expire with their intake, accepting one offer withdraws overlapping applications
elsewhere, expiring documents are tracked, and residents can extend their stay into the next
intake. Admins see a "Returning" badge on applications from past or current residents.

It also closes two cross-company and student access gaps, shows form errors again, keeps the live
notification stream open through nginx, enforces the backend coverage minimum in CI, and adds
operator documentation for PROD alerts and NONPROD database access.

The release has 13 database migrations, three of which repair existing data and one of which drops
columns. Read [Operations and deployment](#operations-and-deployment) before deploying. A rollback
after deploy needs a database restore.

## New for students

- **Apply for an intake.** The application form lists only buildings that can take the student's
  institution, from any company, up to three buildings in total. Submitting creates one
  application per company, each with that company's buildings in the student's order. If any
  company refuses (window closed, documents missing, already applied), nothing is saved and the
  form keeps what was typed and names the company and the reason. (#509)
- **Plain-language intakes.** Intakes read like "Semester 2, 2027. Move in 15 July." An intake is
  asked for only when more than one is open at a company. If the profile has no institution, the
  form asks for it once and saves it to the profile. (#509)
- **One application per company per year.** A student can hold a 2026 and a 2027 application with
  the same company. A second application to the same company for the same year is refused with a
  link to the existing one ("You already applied to Company B for 2027."). (#509)
- **Open windows banner.** The dashboard shows which companies have application windows open, and
  application cards show the company, year and intake. (#509)
- **Check-in from move-in day.** The expected check-in date now defaults to move-in day, and the
  check-in deadline counts from move-in instead of from the day the offer was accepted. An offer
  accepted months ahead no longer lapses before the student arrives. (#510)
- **"Still coming?"** A place accepted more than 42 days before move-in is confirmed again about
  four weeks before move-in, from a dashboard card. Reminders follow every 7 days. A place still
  not confirmed 14 days before move-in is released and offered to the next student on the waiting
  list. (#510)
- **Applications expire with their intake.** The day after an intake's stay ends, unfinished
  applications for it expire. The student gets an email that names the next open intake at that
  company, or says none is open. (#511)
- **Accepting one offer withdraws the others.** When a student accepts a place, their other
  unfinished applications at other companies whose stay dates overlap are withdrawn, including any
  open offer on them. The student gets one email listing what was withdrawn. (#511)
- **Expiring documents.** When a company marks a document type as expiring (for example proof of
  registration), the documents page shows "Your proof of registration expires on 30 November.
  Upload the new one." Reminder emails start 30 days before expiry. An expired or soon-to-expire
  approved document can be replaced even while an application is live; the replacement waits for
  approval. (#512)
- **Extend a stay.** A resident on a Semester or Trimester stay sees "Stay for Semester 2" on the
  room card when the same bed is free for the next intake and their documents are in order. It
  goes through at once, keeps the same room, and emails a confirmation. Otherwise the card offers
  "Apply for Semester 2". An Annual stay cannot be extended. (#512)

## New for company admins

- **Your own academic years.** Company admins create, edit, copy and delete their own company's
  years at `/admin/academic-years`. A year can be for one institution or "All institutions". Years
  for the same institution cannot overlap; the message names the overlapping year. The current
  year follows the dates; there is no "set current" button any more. (#508)
- **Intakes inside a year.** "Create year" from a pattern (Annual, Semesters or Trimesters) fills
  in the intakes and their application windows; "Copy last year" moves every date on by a year.
  Intakes can be edited inline. Editing dates shows how many applications and allocations are
  affected. A year or intake with applications cannot be deleted; "Close applications now" closes
  its window at the end of today instead. (#508)
- **Window lead times as settings.** New intake windows open 120 days and close 14 days before
  move-in by default. A company can override both through system settings. (#508)
- **Filters by institution and intake** on the applications and allocations lists. (#510)
- **Overdue check-outs** on the admin dashboard: residents whose planned stay has ended and who
  have not checked out, longest overdue first. (#510)
- **Reconfirmation lead times** (42, 28 and 14 days) can be overridden per company. (#510)
- **Withdrawal notices.** When one of your applicants accepts an overlapping place at another
  company, your company admins get an in-app notification about your application. The other
  company is not named. (#511)
- **Expiring document types.** Tick "Expires with the academic year" on a document type. Expiry is
  the end date of your company's academic year, for the student's institution, that the document
  was uploaded in. A required expiring document that is expired, missing or not yet approved holds
  back manual offers, staged proposals and check-in; batch allocation skips the student and keeps
  their waiting-list place. The application list and detail show an expired badge, and the detail
  shows each expiring document's date. The reminder period (30 days) can be overridden. (#512)
- **Stay extension notices.** When a resident extends their stay, company admins get an in-app
  notification. Ending an extended stay uses the existing check-out and end actions. (#512)
- **"Returning" badge** on the application list and detail, for students who were checked in at
  your company before or live there now, including a lease terminated early. It has no effect on
  scoring, windows or rooms. (#512)
- **Check-in refused while the room is occupied.** If every bed in a room is still held by a
  checked-in student, the next student's check-in (or a move into that room) is refused until
  someone checks out. Their offer stands. (#510)

## New for platform admins

- Platform admins see every company's academic years. A year they create belongs to the platform
  (tenant 0). They can edit any company's years but cannot create one for a specific company.
  (#508)
- Platform defaults for the new settings live at tenant 0 and are managed in platform system
  settings: `intake_window_open_days_before`, `intake_window_close_days_before`,
  `reconfirm_min_lead_days`, `reconfirm_ask_days_before`, `reconfirm_release_days_before`,
  `document_expiry_reminder_days`. (#508, #510, #512)
- Platform admins may call the student-facing reconfirm and stay extension endpoints, which follow
  the existing `hasAnyRole('STUDENT', 'PLATFORM_ADMIN')` pattern. (#510, #512)

## Fixes and security hardening

- **Student profile ownership (#491).** A student could create a profile on another student's
  account by naming its user id. Now a student's profile always belongs to the caller (another
  `userId` gives 403), a second profile gives 409, and a platform admin must name an existing
  `STUDENT` user (404 otherwise).
- **Company checks on loads by id (#492).** `POST /api/admin/allocation/waiting-list/enqueue`
  answers 404 for another company's application and refuses an application that belongs to a
  different student. `GET /api/room-allocation/room/{roomId}/availability` and
  `/is-available` answer 404 for another company's room.
- **Academic years per company (#508, closes #439).** Year names are unique per company and
  institution instead of across the platform, so the Academic Years screen works for every company.
  Company admins can only read and change their own years; another company's year answers 404.
- **Allocation access.** The new reconfirm and stay extension endpoints check that the allocation
  belongs to the calling student (403 otherwise). (#510, #512)
- **Form errors shown again (#503).** A global style hid every form field's error and hint text, so
  a form could refuse to submit without saying why. Forms now reserve space under their fields,
  which changes spacing on login, signup, change password, the application form, the create-user
  dialog and the building and room forms. The PR was not checked in a browser.
- **Live notifications through nginx (#504).** nginx cut the quiet notification stream and the
  browser reconnected in a loop. `/api/notifications/stream` now has its own location with
  buffering off and a 35 minute read timeout, above the backend's 30 minute emitter timeout.
- **Scoring no longer skips FULL rooms (#510).** FULL describes today. Scoring now skips only
  MAINTENANCE rooms and checks capacity for the stay being offered.
- **Dates in South African time (#508).** One clock in `Africa/Johannesburg` decides every window,
  stay and expiry check, so "closes 30 June" means the end of 30 June in South Africa.

## Operations and deployment

### Flyway migrations, in order

All are in `backend/src/main/resources/db/migration/` and run at backend startup.

| # | Migration | What it does |
|---|-----------|--------------|
| 1 | `V2026_11_10_1000__academic_year_per_company_institution.sql` | Adds nullable `academic_year.institution_id` (FK, RESTRICT) and its index; replaces the global unique name with `uq_academic_year_tenant_institution_name` (`NULLS NOT DISTINCT`); drops `idx_academic_year_current`. |
| 2 | `V2026_11_10_1100__add_intake.sql` | Creates the `intake` table with type, stay and window checks, two indexes and two deferred constraint triggers that keep stay dates inside the year; seeds `intake_window_open_days_before` (120) and `intake_window_close_days_before` (14) at tenant 0. |
| 3 | `V2026_11_10_1101__backfill_annual_intakes.sql` | Data: one `ANNUAL` intake per existing year from its dates and window. A back-to-front window is swapped; a year that ends before it starts gets no intake. |
| 4 | `V2026_11_10_1200__application_intake_institution.sql` | Adds nullable `application.intake_id` and `application.institution_id` (FKs, RESTRICT) and their indexes. |
| 5 | `V2026_11_10_1300__repair_application_years_and_intakes.sql` | Data: copies each year that another company's rows point at into that company and repoints applications and allocations; copies platform years that have not ended into every company with no year; fills missing years, intakes and institutions. Nothing is deleted. |
| 6 | `V2026_11_10_1400__one_active_application_per_company_year.sql` | Creates `ux_application_student_tenant_year_active` on `(student_id, tenant_id, academic_year_id)` and drops `ux_application_student_tenant_active`. |
| 7 | `V2026_11_10_1500__room_allocation_stay_dates.sql` | Adds `room_allocation.stay_start_date` and `stay_end_date`, backfilled from the application's intake, else the allocation's year; adds `ck_room_allocation_stay_dates` and `idx_room_allocation_room_id_stay_dates`. |
| 8 | `V2026_11_10_1600__room_allocation_reconfirm.sql` | Adds `accepted_at`, `reconfirm_requested_at`, `reconfirm_reminded_at`, `reconfirmed_at` and `idx_room_allocation_status_stay_start_date`; seeds the three reconfirmation settings at tenant 0. |
| 9 | `V2026_11_10_1700__repair_remaining_application_years_and_intakes.sql` | Data: repeats the step 3 rules for applications still without a year or intake, gives a year with no intake one Annual intake, and copies stay dates to allocations whose application now has an intake. |
| 10 | `V2026_11_10_1710__application_year_intake_not_null.sql` | Sets `application.academic_year_id` and `intake_id` NOT NULL, each only if no row is empty; otherwise logs a warning and leaves that column nullable. |
| 11 | `V2026_11_10_1720__drop_retired_academic_year_columns.sql` | Drops `academic_year.is_current`, `application_open_date` and `application_close_date`. |
| 12 | `V2026_11_11_1000__document_type_is_expiring.sql` | Adds `document_type.is_expiring BOOLEAN NOT NULL DEFAULT FALSE`; seeds `document_expiry_reminder_days` (30) at tenant 0. |
| 13 | `V2026_11_11_1100__application_is_returning.sql` | Adds `application.is_returning BOOLEAN NOT NULL DEFAULT FALSE`. Existing applications are not backfilled. |

### Data repairs and warnings to watch for

- `V2026_11_10_1710` can log `RAISE WARNING` lines: `application.academic_year_id left nullable: N
  application(s) have no academic year ...` and `application.intake_id left nullable: N
  application(s) have no intake ...`. The migration still succeeds. It means some applications
  belong to a company with no academic year at all. A person decides what to do with those rows,
  and a later migration adds the constraint. The entity mappings stay optional, so such rows still
  load.
- After deploy, check the result:

  ```sql
  SELECT column_name, is_nullable
  FROM information_schema.columns
  WHERE table_name = 'application' AND column_name IN ('academic_year_id', 'intake_id');

  SELECT COUNT(*) FILTER (WHERE academic_year_id IS NULL) AS no_year,
         COUNT(*) FILTER (WHERE intake_id IS NULL)        AS no_intake
  FROM application;
  ```

- `V2026_11_10_1300` creates copies of academic years (and their intakes) in companies whose rows
  pointed at another company's year, and copies platform years that have not ended into every
  company with no year of its own. Company admins will see these years in their list after the
  deploy. A copied year is not checked against the overlap rule.
- `V2026_11_10_1101` skips a year that ends before it starts, and `V2026_11_10_1500` gives no
  stay dates to an allocation whose year ends before it starts. An allocation with no stay dates
  blocks its bed for every date, as all allocations did before.
- The NOT NULL step and the column drops were planned to land in a later deploy than the code that
  stopped writing and reading them. In this release they all run in one Flyway run at startup.
  That is fine while only one backend version runs at a time; do not run the previous release's
  backend against the migrated database (see rollback below).

### Dropped columns

- `academic_year.is_current`
- `academic_year.application_open_date`
- `academic_year.application_close_date`

Each intake has its own window. The API still returns a year window, worked out from its intakes.

### New settings

All are platform defaults at `tenant_id = 0` in `system_settings`, overridable per company
through the settings cascade. Values verified in the migrations.

| Key | Default | Meaning | Migration |
|-----|---------|---------|-----------|
| `intake_window_open_days_before` | 120 | Days before move-in that a new intake's window opens | `V2026_11_10_1100` |
| `intake_window_close_days_before` | 14 | Days before move-in that a new intake's window closes | `V2026_11_10_1100` |
| `reconfirm_min_lead_days` | 42 | Places accepted more than this many days before move-in must reconfirm | `V2026_11_10_1600` |
| `reconfirm_ask_days_before` | 28 | Days before move-in that "Still coming?" is sent | `V2026_11_10_1600` |
| `reconfirm_release_days_before` | 14 | Days before move-in that an unconfirmed place is released | `V2026_11_10_1600` |
| `document_expiry_reminder_days` | 30 | Days before expiry that reminders start, and from which an approved document can be replaced | `V2026_11_11_1000` |

### New scheduled jobs

All three are in `ScheduledJobRunner`, run daily, are pinned to UTC, and read dates in South
African time. Each sets the tenant context per company and handles each row in its own
transaction. The cron can be overridden with the property shown; none is set in `application.yml`.

| Job | Time (UTC) | SAST | Property | What it does |
|-----|-----------|------|----------|--------------|
| `intakeExpiry` | 05:00 | 07:00 | `app.application.intake-expiry-cron` | Expires unfinished applications the day after their intake's stay ends; emails the student. (#511) |
| `reconfirmation` | 06:00 | 08:00 | `app.allocation.reconfirm-cron` | Asks, reminds and releases accepted places before move-in. (#510) |
| `documentExpiryReminder` | 06:30 | 08:30 | `app.document.expiry-reminder-cron` | Sends document expiry reminders by email and in-app. (#512) |

The document reminder cadence is worked out from the dates and nothing is stored, so if the job
runs twice on the same day, a student due a reminder that day gets it twice.

### New and changed API endpoints

New:

| Method | Path | Roles | PR |
|--------|------|-------|----|
| POST | `/api/academic-years/from-pattern` | PLATFORM_ADMIN, TENANT_ADMIN | #508 |
| POST | `/api/academic-years/{id}/copy` | PLATFORM_ADMIN, TENANT_ADMIN | #508 |
| POST | `/api/academic-years/{id}/intakes` | PLATFORM_ADMIN, TENANT_ADMIN | #508 |
| PUT | `/api/intakes/{id}` | PLATFORM_ADMIN, TENANT_ADMIN | #508 |
| DELETE | `/api/intakes/{id}` | PLATFORM_ADMIN, TENANT_ADMIN | #508 |
| POST | `/api/intakes/{id}/close-applications` | PLATFORM_ADMIN, TENANT_ADMIN | #508 |
| GET | `/api/apply/buildings` | STUDENT | #509 |
| GET | `/api/apply/buildings/{buildingId}/intakes` | STUDENT | #509 |
| GET | `/api/apply/open-windows` | STUDENT | #509 |
| POST | `/api/room-allocation/{id}/reconfirm` | STUDENT, PLATFORM_ADMIN | #510 |
| GET | `/api/admin/stats/overdue-check-outs` | PLATFORM_ADMIN, TENANT_ADMIN | #510 |
| PATCH | `/api/document-type/{id}/expiring?expiring=` | PLATFORM_ADMIN, TENANT_ADMIN | #512 |
| GET | `/api/documents/my/expiry` | STUDENT | #512 |
| GET, POST | `/api/room-allocation/{id}/stay-extension` | STUDENT, PLATFORM_ADMIN | #512 |

Changed:

| Endpoint | Change | PR |
|----------|--------|----|
| `POST /api/academic-years/{id}/set-current` | Removed. | #508 |
| `GET /api/academic-years` | Returns the caller's company's years, paginated (`PageResponse`, default size 50, newest first) instead of a plain list. | #508 |
| `GET /api/academic-years/current` | Resolved by date; takes optional `institutionId`. | #508 |
| `POST`, `PUT`, `DELETE /api/academic-years...` | Roles change from PLATFORM_ADMIN and PLATFORM_STAFF to PLATFORM_ADMIN and TENANT_ADMIN. | #508 |
| `POST /api/applications/submit` | Returns a list of applications (one per company). Request takes `intakeIds` keyed by company id. | #509 |
| `GET /api/applications/search`, `/api/admin/applications/search`, `/api/room-allocation/search` | New optional `institutionId` and `intakeId` filters. | #510 |
| `POST /api/room-allocation/{id}/accept` | `expectedCheckInDate` is optional and defaults to move-in day. | #510 |
| `RoomAllocationResponse` | Adds `stayStartDate`, `stayEndDate`, `expectedCheckInDate`, `checkInDate`. | #510 |
| `ApplicationResponse` | Adds company, year and intake, plus the returning and expired flags. | #509, #512 |
| `POST /api/admin/allocation/waiting-list/enqueue` | 404 for another company's application; refuses another student's application. | #492 |
| `GET /api/room-allocation/room/{roomId}/availability`, `/is-available` | 404 for another company's room. | #492 |
| `POST /api/student` | Profile owner decided from the caller (403, 409, 404 as above). | #491 |

New email templates: `application-expired`, `applications-withdrawn`, `document-expiry-reminder`,
`place-released`, `reconfirm-request`, `reconfirm-reminder`, `stay-extended`. The
`application-approved` email names the application's year and intake; its "Semester" label is now
"Intake".

### Configuration, environment and infrastructure

- **No new environment variables** and no change to `application.yml` in this range.
- **nginx (#504).** `frontend/nginx.conf` has a new exact-match location for
  `/api/notifications/stream`. It ships in the frontend image. The config was not parsed with
  `nginx -t` in the PR. Caddy on the host sits in front of nginx on NONPROD and PROD and is not
  changed here.
- **NONPROD fixed addresses (#502).** `docker-compose.deploy.yml` pins `db` to `172.28.0.20` and
  `minio` to `172.28.0.30` on the app network, with no published ports. `docs/NONPROD_RUNBOOK.md`
  ("Developer access from your laptop") has the SSH tunnel commands. On PROD, db and minio are
  scaled to zero, so this has no effect there.
- **PROD runbook (#506).** `docs/PROD_RUNBOOK.md` gains "When an alert fires", "Reading logs" and
  "Rotating the other PROD secrets". References to the missing `CREDENTIAL_ROTATION.md` were
  replaced in `docs/BACKUP_AND_RESTORE.md`, `docs/OPERATOR_RUNBOOK.md` and
  `docker-compose.prod.yml`. `CLAUDE.md` still mentions `CREDENTIAL_ROTATION.md`.
- **CI coverage gate (#505, #507).** The backend CI job now runs
  `./gradlew jacocoTestCoverageVerification`. No runtime effect.
- **Per-company deployments (#501).** Documentation only: `docs/adr/ADR-010-client-branded-sites.md`
  and `docs/CLIENT_SITES_PLAN.md` propose one deployment per company. Both are proposed, not
  accepted, and nothing is built or deployed by this release.

### Before deploy

1. Take a fresh database backup or note the restore point. Three migrations change data and one
   drops columns.
2. Optionally, on a copy of PROD data, run the migrations and read the Flyway log for the
   `V2026_11_10_1710` warnings, so the outcome is known in advance.
3. Tell company admins that their academic years and intakes become their own, and that a company
   with no year cannot take applications.

### After deploy

1. Read the backend startup log for `RAISE WARNING` lines from `V2026_11_10_1710` and run the
   checks above. If a column was left nullable, raise it with the product owner.
2. Ask each company admin to review `/admin/academic-years`: years copied by `V2026_11_10_1300`,
   intakes, and windows. A company created after this deploy must set up a year before students
   can apply to it.
3. Mark expiring document types (for example proof of registration) where companies want them.
4. Check the PROD log for nginx `upstream timed out` on `/api/notifications/stream`; the PR notes
   the deployed config may have differed from the file.
5. On NONPROD, after the first deploy, confirm the fixed addresses with the `docker inspect` command
   in `docs/NONPROD_RUNBOOK.md` and try both tunnels.
6. The next day, confirm the three new jobs logged `job=intakeExpiry`, `job=reconfirmation` and
   `job=documentExpiryReminder` with `action=complete`.

### Rollback

`hbm2ddl.auto` is `validate` and there are no down migrations. The previous release maps the
dropped `academic_year` columns, so its backend will not start against the migrated schema.
Rolling back means restoring the database to a point before the deploy together with the previous
images (`docs/PROD_RUNBOOK.md`, "Rolling back"; `docs/BACKUP_AND_RESTORE.md`). Otherwise fix
forward.

## Breaking changes and behaviour changes

- `AcademicYearRequest` no longer takes `applicationOpenDate`, `applicationCloseDate` or
  `isCurrent`; it takes `institutionId` (optional), `name`, `startDate` and `endDate`. Windows
  belong to intakes.
- `POST /api/academic-years/{id}/set-current` is gone; the current year follows the dates.
- `GET /api/academic-years` returns a page, not a list, and only the caller's company's years.
- PLATFORM_STAFF can no longer create, edit or delete academic years; TENANT_ADMIN now can.
- `POST /api/applications/submit` returns a list. A submission with buildings from several
  companies creates several applications instead of being refused.
- A company takes applications only for its own academic years. The platform-year fallback still
  applies to `GET /api/academic-years/current`, but not to applying or to document expiry.
- `AcceptOfferRequest.expectedCheckInDate` is optional, and the check-in deadline counts from
  move-in (default 7 days after it, `app.allocation.check-in-expiry-days`), not from acceptance.
- Room capacity is counted by OFFERED, ACCEPTED and ACTIVE allocations whose planned stay
  overlaps the stay asked for. The end date is exclusive. Semester 1 and Semester 2 share a bed,
  an Annual stay blocks both, and a 2026 and a 2027 stay never clash.
- Capacity counts overlapping allocations, not the peak at any one time: in a two-bed room with
  one Semester 1 and one Semester 2 student, an Annual applicant is refused although one bed is
  free all year.
- Rooms with status FULL are no longer skipped by the scoring strategies; only MAINTENANCE is.
- A student can hold two live allocations (for example a 2026 room and a 2027 place).
  `GET /api/room-allocation/student/{id}/active` returns the one that needs them first: an open
  offer, then a place waiting for "Still coming?", then the room they live in.
- "Already has an active allocation" applies only to overlapping stays, so a 2026 resident is
  eligible for 2027.
- `ApplicationStatus` gains PENDING, UNDER_REVIEW, CHANGES_REQUESTED and APPROVED to EXPIRED, and
  UNDER_REVIEW to CANCELLED.
- An `EXPIRED` application status change sends the new `application-expired` email; it used to
  fall through to the rejection email.
- `IllegalArgumentException` still maps to 409, so "choose an intake" and "missing documents" on
  submit come back as 409.
- Forms reserve space under every field for error and hint text (#503).

## Known limitations and open decisions

Decisions recorded in the pull requests that a person should confirm, in short form:

**#508, academic years and intakes**

1. When a company has no year, `GET /current` falls back to platform years; company admins do not
   see platform years in their list.
2. Platform roles see every company's years; a year they create belongs to the platform.
3. "Close applications now" closes the window at the end of today and is refused before the
   window opens.
4. Pattern split: Semesters and Trimesters divide the year into equal parts with no gap; admins
   edit the dates that differ.
5. Backfill on odd data: a back-to-front window is swapped; a year that ends before it starts gets
   no intake.

**#509, applying for an intake**

1. No platform-year fallback for applying. The repair copies unended platform years into every
   company with none; a company created later must set up a year first.
2. The repair reuses a same-named year in the company rather than copying; a copied year is not
   checked for overlaps.
3. A year with no Annual intake: the repair picks the intake whose window held the application
   date, else the first by move-in.
4. A student with no institution can still submit; only "All institutions" years count.
5. "Specific institution wins" applies to intakes too.
6. An unknown intake id is refused the same way as another company's.
7. Duplicate buildings in one submission count once.
8. The unique index uses `NULLS NOT DISTINCT`.
9. The onboarding flow (`/application-submission` to `/application-review`) is not rebuilt; where
   several intakes are open, only the dashboard form can choose.
10. Intake counts and delete checks still use the intake's year.
11. The approval email label changed to "Intake"; the rejection email's next-period guess is
    unchanged.
12. `IllegalArgumentException` still maps to 409.

**#510, stay dates and reconfirmation**

1. NOT NULL on application year and intake is applied only where the data allows it (warning
   otherwise).
2. The stay end date is exclusive; no offer is made on a stay's last day.
3. Capacity counts overlapping allocations, not the peak (conservative, never double-books).
4. Occupancy today counts every checked-in student, including overdue residents.
5. Views that ask about no stay count beds promised from today on.
6. Check-in refused while occupied compares checked-in students with capacity; moves too.
7. Several live allocations: the order shown above.
8. Reconfirmation: places accepted before this release (no `accepted_at`) are never asked;
   reminders every 7 days; release only after being asked at least a day earlier; a released place
   cancels its application.
9. Two migration replay tests were removed; `ApplicationYearConstraintMigrationIntegrationTest`
   covers 1700 and 1710.
10. Integration fixtures use a 2090 platform year.

**#511, intake expiry and withdrawal**

1. An unfinished application with an OFFERED or ACCEPTED allocation is not expired; offer and
   check-in expiry end it. Should the job cancel those offers itself?
2. Expiry cut-off is the intake's stay end before today in SAST; no intake falls back to the
   year's end.
3. The expiry email's "Apply Now" goes to `/applications/new`; no deep link to an intake.
4. ALLOCATED applications at other companies are not withdrawn.
5. No stay dates, no withdrawal.
6. Companies are told through in-app notifications to TENANT_ADMIN users, not email; building
   staff are not notified.
7. Withdrawal runs synchronously after commit; the emails are async. Should the withdrawal be
   async?
8. No separate status-change email per withdrawn application.

**#512, expiring documents, stay extensions, returning residents**

1. Document expiry uses company years only, with no platform fallback.
2. A document is valid through its expiry date.
3. Reminder cadence is worked out from the dates; a past resident whose only link is an old
   `CHECKED_IN` application is not reminded.
4. The 30 day reminder period is also the window in which an approved document can be replaced.
5. Only required expiring types hold offers and check-in; reminders and the badge cover all
   expiring types.
6. The upload path is relaxed; deleting an approved document during a live application still
   needs an admin.
7. A stay extension needs every required document approved.
8. The room row lock is taken on the extension path only.
9. Extensions chain (Trimester 1 to 2 to 3); an ACCEPTED place can be extended before move-in.
10. No new admin screen for ending an extended stay.
11. `is_returning` is not backfilled.
12. One extra follow-up commit in the PR for a missing frontend type.

**Other known limitations**

- Waiting-list rescoring for a tenant admin still counts other companies' students. This has not
  been filed as an issue yet.
- None of the new flows has been run by hand in a browser. The PRs were checked with unit and
  integration tests and builds only. The UAT plan is the first manual run.
- No unique constraint on `student.user_id` yet (#491 left it for its own change, since duplicate
  rows may already exist), so two simultaneous profile creations can still race.
- #502: the NONPROD addresses, GUI client, MinIO console and port checks still need doing on
  NONPROD after deploy.
- #503: the spacing change was not checked by eye at desktop or phone width.
- #504: `nginx -t` was not run; the stream has no backend heartbeat and still carries the JWT in
  its query string (both out of scope).
- #506: whether a deploy recreates Alloy when only `PROMETHEUS_SCRAPE_PASSWORD` changes is not
  confirmed; UptimeRobot has no runbook entry yet (#340).
- #501: per-company deployments are a proposal only.

## Included pull requests

In merge order on `develop`.

| PR | Title | Linked issues |
|----|-------|---------------|
| [#501](https://github.com/beezhub/student-accommodation-system/pull/501) | docs(adr): propose one deployment per company | Closes #500 |
| [#491](https://github.com/beezhub/student-accommodation-system/pull/491) | fix(security): stop students creating a profile on another account | Closes #489 |
| [#492](https://github.com/beezhub/student-accommodation-system/pull/492) | fix(allocation): check the company on enqueue and room availability | Closes #490 |
| [#502](https://github.com/beezhub/student-accommodation-system/pull/502) | feat(deploy): give NONPROD db and minio fixed addresses for tunnels | Closes #264 |
| [#503](https://github.com/beezhub/student-accommodation-system/pull/503) | fix(frontend): show form field errors and hints again | Closes #434 |
| [#504](https://github.com/beezhub/student-accommodation-system/pull/504) | fix(frontend): keep the notification stream open through nginx | Closes #368 |
| [#505](https://github.com/beezhub/student-accommodation-system/pull/505) | fix(ci): enforce the 70% coverage minimum and restore two services | Closes #421 |
| [#507](https://github.com/beezhub/student-accommodation-system/pull/507) | test: cover listing and deactivating tenant admins | Refs #421 |
| [#508](https://github.com/beezhub/student-accommodation-system/pull/508) | feat: academic years per company and institution, with intakes | Closes #494, #439; Refs #493 |
| [#506](https://github.com/beezhub/student-accommodation-system/pull/506) | docs: add alert responses, log reading and secret rotation to PROD runbook | Closes #409 |
| [#509](https://github.com/beezhub/student-accommodation-system/pull/509) | feat: students apply for an intake of their institution | Closes #495; Refs #493 |
| [#510](https://github.com/beezhub/student-accommodation-system/pull/510) | feat: plan room stays by dates and reconfirm early places | Closes #496; Refs #493 |
| [#511](https://github.com/beezhub/student-accommodation-system/pull/511) | feat: expire applications with their intake, withdraw on acceptance | Closes #497; Refs #493 |
| [#512](https://github.com/beezhub/student-accommodation-system/pull/512) | feat: expiring documents, stay extensions and returning residents | Closes #498, #499; Refs #493 |
