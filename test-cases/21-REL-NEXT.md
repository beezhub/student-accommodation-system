# Release Next: UAT Test Cases

> **Module:** Release Next (academic years, intakes, stays, documents; release `77f80ecf..7c5d0c3e`)
> **Frontend Routes:** `/admin/academic-years`, `/admin/applications`, `/admin/room-allocations`, `/admin/document-types`, `/admin` (dashboard), student `/dashboard`, `/applications/new`, documents page
> **Email Verification:** MailHog at `http://localhost:8025`
> **Release notes:** [`docs/releases/RELEASE_NOTES_2026-11-11.md`](../docs/releases/RELEASE_NOTES_2026-11-11.md)
> **Total Test Cases:** 74

| Field | Value |
|-------|-------|
| **Version** | 1.0 |
| **Date** | 2026-11-11 |
| **Prepared By** | ________ |
| **Reviewed By** | ________ |
| **Approved By** | ________ |

---

## 1. Scope

This plan covers what changed between the current `main` (equal to `develop` at `77f80ecf`) and
`develop` at `7c5d0c3e`: pull requests #491, #492, #501 to #512. It is run in addition to the
regression set in the Regression section, not instead of the full plan in `00-INDEX.md`.

In scope:

- Academic years per company and institution, with Annual, Semester and Trimester intakes (#508).
- Applying for an intake at up to three companies on one form; one active application per
  company per year (#509).
- Planned stay dates, capacity by overlapping stays, check-in rules, reconfirmation before
  move-in, admin filters and overdue check-outs (#510).
- Intake expiry and withdrawal of overlapping applications on acceptance (#511).
- Expiring documents, stay extensions and the returning-resident badge (#512).
- Migrations on PROD-like data.
- Security fixes: student profile ownership (#491), company checks by id (#492).
- Form errors (#503), notification stream through nginx (#504), NONPROD tunnels (#502), PROD
  runbook and alerts (#506), per-company deployment proposal (#501).

Out of scope:

- The CI coverage gate (#505, #507): CI only, no user-visible behaviour.
- Building a per-company deployment: #501 is a proposal, not built.
- The onboarding flow (`/application-submission` to `/application-review`), which was not rebuilt
  (PR #509 decision 9), except where the regression set covers it.
- Load and performance testing (`tools/k6/`).
- Waiting-list rescoring for a tenant admin, which still counts other companies' students; it is a
  known gap, not part of this release.

---

## 2. Test Environment

### 2.1 Local

Use the environment in [`00-INDEX.md`](00-INDEX.md), section 2 (Frontend `http://localhost:4200`,
Backend `http://localhost:8080`, Swagger, MinIO console, MailHog).

The scheduled-job cases need the backend run from source so its cron properties can be changed
(section 2.4): start the backing services with `docker-compose up db minio mailhog redis`, then
`cd backend && ./gradlew bootRun` and `cd frontend && npm start`.

### 2.2 NONPROD

TC-REL-071 and TC-REL-072 (section M) run on NONPROD, which serves the frontend through
nginx. Database access on NONPROD is by SSH tunnel to `172.28.0.20`, local port 15432, SSL mode
`require`, as documented in `docs/NONPROD_RUNBOOK.md`, "Developer access from your laptop".
Credentials come from `/opt/sas/.env` on the server. The runbook says not to change the schema by
hand; the data updates in section 2.5 are data only, and each needs a reason on NONPROD.

### 2.3 Clock and dates

- Every date check in the app uses South African time (`Africa/Johannesburg`). The three new jobs
  are pinned to UTC: intake expiry 05:00, reconfirmation 06:00, document expiry reminders 06:30.
- The dates below assume execution in November 2026. Company A's 2026 year ends 30 Nov 2026, so a
  Proof of Enrollment uploaded in 2026 expires that day at Company A. If you run this plan later,
  move every year, intake and expiry date forward by the same amount.
- `CURRENT_DATE` in PostgreSQL follows the database time zone (UTC in the local container). Run the
  SQL in section 2.5 between 02:00 and 22:00 South African time, so the database and the app
  agree on "today".

### 2.4 Triggering the scheduled jobs

There is no admin endpoint to run the new jobs. Each cron can be overridden by a property, and the
job then runs at the time given. Spring cron has six fields (second minute hour day month
weekday), in UTC.

| Job | Property | Environment variable | Default |
|-----|----------|---------------------|---------|
| Intake expiry | `app.application.intake-expiry-cron` | `APP_APPLICATION_INTAKE_EXPIRY_CRON` | `0 0 5 * * *` |
| Reconfirmation | `app.allocation.reconfirm-cron` | `APP_ALLOCATION_RECONFIRM_CRON` | `0 0 6 * * *` |
| Document expiry reminders | `app.document.expiry-reminder-cron` | `APP_DOCUMENT_EXPIRY_REMINDER_CRON` | `0 30 6 * * *` |

To run a job once, set its cron a few minutes ahead in UTC and restart the backend, for example
for 09:40 UTC (11:40 SAST):

```bash
export APP_ALLOCATION_RECONFIRM_CRON='0 40 9 * * *'
cd backend && ./gradlew bootRun
```

Confirm the run in the backend log: `job=reconfirmation action=start` and `action=complete`. The
local `docker-compose.yml` passes only the variables it lists to the backend container, so this
works when the backend runs from source, not in the compose backend. Do not use a cron that fires
every minute for the document reminder job: its cadence is worked out from dates, and every run on
a due day sends the reminder again.

### 2.5 SQL to simulate dates

Connect with `psql` to the test database (local: `docker exec -it student-accommodation-local-db
psql -U "$USER_DB" -d studentaccommodationlocaldb`). Table and column names were checked against
the migrations in this release. Replace `:id` placeholders with the ids found by the lookups.

Lookups:

```sql
-- Company ids
SELECT id, name FROM tenant ORDER BY id;

-- A student's applications, intakes and allocations
SELECT u.email, s.id AS student_id, a.id AS application_id, a.tenant_id, a.status,
       i.id AS intake_id, i.name AS intake, ra.id AS allocation_id, ra.status AS allocation_status,
       ra.stay_start_date, ra.stay_end_date, ra.accepted_at, ra.reconfirm_requested_at,
       ra.reconfirmed_at, ra.expected_check_in_date, ra.check_in_expiry_date
FROM app_user u
JOIN student s ON s.user_id = u.id
LEFT JOIN application a ON a.student_id = s.id
LEFT JOIN intake i ON i.id = a.intake_id
LEFT JOIN room_allocation ra ON ra.application_id = a.id
WHERE u.email = 'thandi.uat@test.com';

-- Every intake
SELECT i.id, t.name AS company, y.name AS year, i.name, i.intake_type, i.stay_start_date,
       i.stay_end_date, i.application_open_date, i.application_close_date
FROM intake i
JOIN academic_year y ON y.id = i.academic_year_id
JOIN tenant t ON t.id = i.tenant_id
ORDER BY t.name, i.stay_start_date;

-- A student's documents
SELECT d.id, dt.type_name, dt.is_expiring, d.document_status, d.created_at
FROM document d
JOIN document_type dt ON dt.id = d.document_type_id
JOIN student s ON s.id = d.student_id
JOIN app_user u ON u.id = s.user_id
WHERE u.email = 'thandi.uat@test.com';
```

Reconfirmation (an ACCEPTED allocation):

```sql
-- Ask point: move-in 20 days away, accepted 60 days ago (more than 42 days before move-in)
UPDATE room_allocation
SET stay_start_date = CURRENT_DATE + 20, accepted_at = NOW() - INTERVAL '60 days'
WHERE id = :id AND status = 'ACCEPTED';

-- Reminder: asked 7 days ago, not reminded yet
UPDATE room_allocation
SET reconfirm_requested_at = NOW() - INTERVAL '7 days', reconfirm_reminded_at = NULL
WHERE id = :id;

-- Release point: move-in 10 days away, asked 2 days ago
UPDATE room_allocation
SET stay_start_date = CURRENT_DATE + 10, reconfirm_requested_at = NOW() - INTERVAL '2 days'
WHERE id = :id AND status = 'ACCEPTED';

-- Restore the intake's move-in after each case, so later capacity cases are not skewed
UPDATE room_allocation SET stay_start_date = :intake_stay_start WHERE id = :id;
```

Intake that has ended (intake expiry, stay ended). The intake must still fall inside its year,
which a deferred trigger checks at commit, so only use it on an intake that started before
yesterday (Company A's 2026 Annual):

```sql
UPDATE intake SET stay_end_date = CURRENT_DATE - 1 WHERE id = :intake_id;
-- restore
UPDATE intake SET stay_end_date = DATE '2026-11-30' WHERE id = :intake_id;
```

Overdue check-out (an ACTIVE allocation):

```sql
UPDATE room_allocation SET stay_end_date = CURRENT_DATE - 3 WHERE id = :id AND status = 'ACTIVE';
-- restore
UPDATE room_allocation SET stay_end_date = DATE '2026-11-30' WHERE id = :id;
```

Expired document. Expiry is not stored: it is the end of the company's first academic year, for
the student's institution, that ends on or after the document's `created_at`. Moving `created_at`
into Company A's 2025 year makes it expire on 30 Nov 2025 at Company A:

```sql
UPDATE document SET created_at = TIMESTAMP '2025-06-15 10:00:00' WHERE id = :doc_id;
-- restore
UPDATE document SET created_at = NOW() WHERE id = :doc_id;
```

Company setting override (reminder period, reconfirmation lead times, window lead times):

```sql
INSERT INTO system_settings (setting_key, setting_value, description, tenant_id)
VALUES ('document_expiry_reminder_days', (DATE '2026-11-30' - CURRENT_DATE)::text, 'UAT override', :tenant_id)
ON CONFLICT (tenant_id, setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value;
-- remove
DELETE FROM system_settings WHERE tenant_id = :tenant_id AND setting_key = 'document_expiry_reminder_days';
```

Use the same statement with `reconfirm_ask_days_before`, `reconfirm_min_lead_days`,
`reconfirm_release_days_before`, `intake_window_open_days_before` or
`intake_window_close_days_before` and a plain number. The tenant settings screen may also allow
these overrides; that path was not checked for this plan.

---

## 3. Test Data Setup

Create the data through the UI (12-TNT, 11-REF, 05-BLD, 06-ROM, 13-USR) unless a step says SQL.

### 3.1 Companies and admins

| Company | Admin | Password | Notes |
|---------|-------|----------|-------|
| Company A, "Alpha Residences" | `admin.a@test.com` (Admin A) | Admin@12345 | Main company |
| Company B, "Beta Living" | `admin.b@test.com` (Admin B) | Admin@12345 | Second company |
| Company C, "Gamma Stay" | `admin.c@test.com` (Admin C) | Admin@12345 | Third company, withdrawal |
| Company D, "Delta Homes" | `admin.d@test.com` | Admin@12345 | Has a building and no academic year |
| Platform | `platformadmin@test.com` | Admin@12345 | As in 00-INDEX |

### 3.2 Institutions, buildings and rooms

- Institutions: University of Cape Town (UCT), Stellenbosch University (SU).
- Alpha House (Company A), serves UCT only: A-101, A-103, A-104, A-106, A-107 (capacity 1),
  A-102 and A-105 (capacity 2).
- Alpha Court (Company A), lists no institutions: two rooms of capacity 2.
- Beta Lodge (Company B), lists no institutions: B-201 (capacity 2).
- Gamma Hall (Company C), lists no institutions: C-301 (capacity 2).
- Delta Lofts (Company D), lists no institutions: one room.

### 3.3 Academic years and intakes

| Company | Year | Institution | Dates | Intakes |
|---------|------|-------------|-------|---------|
| A | 2025 | All institutions | 1 Feb 2025 to 30 Nov 2025 | Annual |
| A | 2026 | All institutions | 1 Feb 2026 to 30 Nov 2026 | Annual |
| A | UCT 2027 | UCT | 1 Feb 2027 to 30 Nov 2027 | Annual (1 Feb to 30 Nov), Semester 1 (1 Feb to 30 Jun), Semester 2 (15 Jul to 30 Nov) |
| B | 2027 | All institutions | 1 Feb 2027 to 30 Nov 2027 | Annual, Semester 1, Semester 2 (same dates as Company A) |
| C | 2027 | All institutions | 1 Feb 2027 to 30 Nov 2027 | Annual, Semester 1, Semester 2 (same dates) |
| D | none | | | |

UCT 2027 is created in TC-REL-001; create the rest before section B. With the default lead times,
Annual and Semester 1 windows run from 4 Oct 2026 to 18 Jan 2027 (open in November 2026) and
Semester 2 opens 17 Mar 2027. After TC-REL-001, edit every Semester 2 window to
open today. Company A's 2026 Annual window is opened to today only
while the 2026 residents below apply, then set back to a past window (for example 1 Oct 2025 to
15 Jan 2026).

### 3.4 Document types

- Platform types from the seed: ID Document, Proof of Address, Proof of Enrollment (required),
  Financial Aid Application, Medical Certificate (optional).
- Proof of Enrollment is ticked "Expires with the academic year" by the platform admin in
  TC-REL-048. Until then no type expires.
- Company A adds its own optional type "Residence Rules" in TC-REL-048.

### 3.5 Students

All passwords `Student@123`. "Documents in order" means ID Document, Proof of Address and Proof of
Enrollment uploaded in November 2026 and approved, by the platform admin or by an admin of a
company they applied to.

| Student | Email | Institution | Purpose |
|---------|-------|-------------|---------|
| Thandi | `thandi.uat@test.com` | UCT | Main applicant: two companies, Semester 1, reconfirmation, expiry notice |
| Sipho | `sipho.uat@test.com` | UCT | Semester 2 in the same bed as Thandi; reminder and release |
| Lerato | `lerato.uat@test.com` | UCT | Annual applicant refused a shared bed; closed window |
| Mpho | `mpho.uat@test.com` | UCT | Withdrawal across companies; stay extension |
| Ayanda | `ayanda.uat@test.com` | UCT | Returning resident: ACTIVE in A-101 for 2026 (Annual) |
| Kabelo | `kabelo.uat@test.com` | UCT | Resident ACTIVE in A-106 for 2026; overdue check-out |
| Sello | `sello.uat@test.com` | UCT | Resident ACTIVE in A-107 for 2026 (room FULL today) |
| Lungi | `lungi.uat@test.com` | UCT | 2027 place in A-106 while Kabelo is still in |
| Naledi | `naledi.uat@test.com` | none on profile | Institution prompt; missing documents |
| Pieter | `pieter.uat@test.com` | SU | Institution filtering |
| Zanele, Bongani, Karabo | `zanele.uat@test.com`, `bongani.uat@test.com`, `karabo.uat@test.com` | UCT | Concurrent stay extension |
| Spare students | `spare1.uat@test.com` and up | UCT | Expired-document and batch cases |

The 2026 residents (Ayanda, Kabelo, Sello) apply for Company A's 2026 Annual intake while its
window is open, are approved, offered their room, accept and are checked in, so their
allocations are ACTIVE with stay 1 Feb 2026 to 30 Nov 2026.

---

## 4. Roles Needed

| Role | Code | Used for |
|------|------|----------|
| Student | `STUDENT` | Applying, offers, "Still coming?", documents, stay extension |
| Company admin | `TENANT_ADMIN` | Years and intakes, reviews, offers, check-in, filters, badges (one per company) |
| Platform admin | `PLATFORM_ADMIN` | Platform settings, platform document types, cross-company views |
| Platform staff | `PLATFORM_STAFF` | One negative case, if such an account exists |
| Operator | none | Migrations, NONPROD tunnels, nginx, PROD alerts |

---


## A. Academic years and intakes (company admin)

---

### TC-REL-001: Create a year from the Semesters pattern

| Field | Value |
|-------|-------|
| **ID** | TC-REL-001 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | Admin A logged in. Company A has no year named UCT 2027. |
| **Test Data** | Institution UCT, name UCT 2027, 1 Feb 2027 to 30 Nov 2027, pattern Annual and Semesters |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/academic-years` | Years are grouped by institution. No "set current" button anywhere on the page |
| 2 | Click "Create year", pick UCT, enter the name and dates, tick Annual and Semesters, save | Year saved under the UCT group |
| 3 | Read the intakes shown inside the year | Annual, Semester 1 and Semester 2 are listed in plain words with move-in date and application window, for example "Semester 2, 2027. Move in ... Applications ... to ..." |
| 4 | Check each intake's window | Opens 120 days and closes 14 days before its move-in date (platform defaults) |
| 5 | Edit Semester 1 to end 30 Jun 2027 and Semester 2 to run 15 Jul 2027 to 30 Nov 2027 inline | Saved. The pattern split equal parts with no gap; only the edited dates change |

| Field | Value |
|-------|-------|
| **Postconditions** | UCT 2027 exists at Company A with three intakes. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | If the dialog does not allow Annual and Semesters together, create with Semesters and add the Annual intake with the add-intake action (`POST /api/academic-years/{id}/intakes`). Record which. |

---

### TC-REL-002: Copy last year moves every date on by a year

| Field | Value |
|-------|-------|
| **ID** | TC-REL-002 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | Admin A logged in. UCT 2027 exists (TC-REL-001). |
| **Test Data** | Copy UCT 2027 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Copy last year" on the UCT group | A new year is created, named with the next year (UCT 2028) |
| 2 | Compare the copy's year and intake dates with UCT 2027 | Every stay date and window date is one year later; intake names and types are the same |
| 3 | Delete the copy (it has no applications) | Deleted |

| Field | Value |
|-------|-------|
| **Postconditions** | Only UCT 2027 remains for UCT. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | A name with no year in it is refused with "Give the copy a name ..." (400). |

---

### TC-REL-003: Overlapping year for the same institution is refused

| Field | Value |
|-------|-------|
| **ID** | TC-REL-003 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | Admin A logged in. UCT 2027 exists. |
| **Test Data** | Institution UCT, name UCT 2027b, 1 Jun 2027 to 31 Dec 2027 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Create the year | Refused (409). The message names UCT 2027 and its dates |
| 2 | Create the same dates with institution "All institutions" while Company A has another "All institutions" year covering them | Refused, naming that year |
| 3 | Create an "All institutions" year 1 Dec 2026 to 31 Jan 2027 that overlaps no other "All institutions" year | Saved; a specific institution's year may overlap an "All institutions" one. Delete it afterwards |

| Field | Value |
|-------|-------|
| **Postconditions** | No overlapping year saved. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-004: Year names are per company and institution (#439)

| Field | Value |
|-------|-------|
| **ID** | TC-REL-004 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | Admins A and B logged in (two browsers). Company B has a year named 2027. |
| **Test Data** | Name 2027 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | As Admin A, create an "All institutions" year named 2027 with dates that overlap nothing at Company A | Saved, although Company B has a year with the same name |
| 2 | As Admin A, create a second "All institutions" year named 2027 with other dates | Refused (409): "There is already a year called '2027' ..." |
| 3 | Delete Admin A's test year | Deleted |

| Field | Value |
|-------|-------|
| **Postconditions** | Company A's years unchanged. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-005: Impossible dates are refused

| Field | Value |
|-------|-------|
| **ID** | TC-REL-005 |
| **Module** | Release Next |
| **Priority** | Medium |
| **Preconditions** | Admin A logged in. |
| **Test Data** | Year start 30 Nov 2027, end 1 Feb 2027; an intake moved outside its year |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Create a year whose end is before its start | Refused (400): "The year must start before it ends" |
| 2 | Edit Semester 2 of UCT 2027 to end 15 Dec 2027 (after the year end) | Refused (400): the intake must fall inside the year |
| 3 | Edit an intake so the window closes before it opens | Refused (400) |

| Field | Value |
|-------|-------|
| **Postconditions** | No change saved. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-006: Editing dates shows how many students are affected

| Field | Value |
|-------|-------|
| **ID** | TC-REL-006 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | Admin A logged in. At least one application and one allocation exist for UCT 2027 Semester 1 (after section B). |
| **Test Data** | UCT 2027 Semester 1 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Edit the Semester 1 application close date | Before or after saving, the screen shows the application and allocation counts for the intake |
| 2 | Save | Saved. Existing applications keep their intake; existing allocations keep their stay dates (check one allocation's stay dates are unchanged) |

| Field | Value |
|-------|-------|
| **Postconditions** | Window changed; existing rows untouched. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Counts are those of the intake's year until intake-level counts are added (PR #508 decision 4, PR #509 decision 10). |

---

### TC-REL-007: Delete with applications is refused; close applications now

| Field | Value |
|-------|-------|
| **ID** | TC-REL-007 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | Admin A logged in. UCT 2027 Semester 1 has applications. A test intake "Late intake" (type SEMESTER) has been added to UCT 2027 with its window opening next month. |
| **Test Data** | UCT 2027 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Delete Semester 1 | Refused (409): it has applications and cannot be deleted; the message points at closing applications |
| 2 | Delete the year UCT 2027 | Refused (409) |
| 3 | Click "Close applications" on Semester 1 | Window close date becomes today; applications are still accepted until the end of today. Activity log row written |
| 4 | Click "Close applications" on an intake whose window has not opened | Refused (400): applications open on a later date |
| 5 | Re-open Semester 1 by editing its close date back, and delete "Late intake" | Saved; deleted (it has no applications) |

| Field | Value |
|-------|-------|
| **Postconditions** | Semester 1 window restored. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-008: Company admin cannot read or change another company's years

| Field | Value |
|-------|-------|
| **ID** | TC-REL-008 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | Admin A token. Company B's year id and intake id known (from Admin B or SQL). |
| **Test Data** | B year id, B intake id |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | As Admin A, `GET /api/academic-years` | Only Company A's years, as a page (`content`, paging fields). No Company B or platform years |
| 2 | `GET /api/academic-years/{B year id}` | 404 |
| 3 | `PUT /api/academic-years/{B year id}` and `DELETE` it | 404 for both; nothing changes at Company B |
| 4 | `PUT /api/intakes/{B intake id}`, `DELETE /api/intakes/{B intake id}`, `POST /api/intakes/{B intake id}/close-applications` | 404 for each |
| 5 | `POST /api/academic-years/{B year id}/copy` and `/intakes` | 404 |

| Field | Value |
|-------|-------|
| **Postconditions** | Company B unchanged (check as Admin B). |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-009: Current year follows the dates

| Field | Value |
|-------|-------|
| **ID** | TC-REL-009 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | Company A has 2026 (ends 30 Nov 2026) and UCT 2027. |
| **Test Data** | `GET /api/academic-years/current?institutionId={UCT id}` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Call the endpoint as Admin A on a day inside 2026 | Returns the 2026 "All institutions" year, or UCT 2027 once 2026 has ended |
| 2 | Call `POST /api/academic-years/{id}/set-current` | 404 or 405: the endpoint no longer exists |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-010: Window lead times follow the company override

| Field | Value |
|-------|-------|
| **ID** | TC-REL-010 |
| **Module** | Release Next |
| **Priority** | Medium |
| **Preconditions** | Admin A logged in. Platform admin can edit Company A settings or SQL access. |
| **Test Data** | intake_window_open_days_before = 90 for Company A |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Set a Company A override of `intake_window_open_days_before` to 90 (settings screen or SQL in section 2.5) | Saved |
| 2 | Create a throw-away year from a pattern | Windows open 90 days before move-in; close still 14 |
| 3 | Delete the throw-away year and the override | Platform default 120 applies again |

| Field | Value |
|-------|-------|
| **Postconditions** | No override left. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-011: Platform admin sees every company's years

| Field | Value |
|-------|-------|
| **ID** | TC-REL-011 |
| **Module** | Release Next |
| **Priority** | Medium |
| **Preconditions** | Platform admin logged in. |
| **Test Data** | None |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open `/admin/academic-years` | Years of every company are listed |
| 2 | Create a year | It belongs to the platform (tenant 0), not to a company |
| 3 | Edit a Company B year's name and change it back | Saved |
| 4 | Delete the platform test year | Deleted |

| Field | Value |
|-------|-------|
| **Postconditions** | No platform test year left. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | PR #508 decision 5: platform roles cannot create a year for a specific company. |

---

### TC-REL-012: PLATFORM_STAFF can no longer change years

| Field | Value |
|-------|-------|
| **ID** | TC-REL-012 |
| **Module** | Release Next |
| **Priority** | Low |
| **Preconditions** | A PLATFORM_STAFF user exists, if the environment has one. |
| **Test Data** | None |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | As PLATFORM_STAFF, `POST /api/academic-years` | 403 |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Skip and mark N/A if no PLATFORM_STAFF account exists. |

---

## B. Applying for an intake (student)

---

### TC-REL-013: Building picker lists only buildings that can take the student

| Field | Value |
|-------|-------|
| **ID** | TC-REL-013 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | Setup complete. Student Thandi (UCT) and Pieter (SU) logged in (two browsers). |
| **Test Data** | Alpha House (UCT only), Alpha Court (no institutions), Beta Lodge, Gamma Hall, Delta Lofts (company with no year) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | As Thandi, open the new application form | Alpha House, Alpha Court, Beta Lodge and Gamma Hall are offered. Delta Lofts is not |
| 2 | As Pieter, open the form | Alpha House is not offered (UCT only). Delta Lofts is not offered |
| 3 | As Thandi, `GET /api/apply/buildings` | Same list as the screen |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Public `/buildings` pages are unchanged and still list every building. |

---

### TC-REL-014: Institution is asked inline when missing

| Field | Value |
|-------|-------|
| **ID** | TC-REL-014 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | Student Naledi logged in; her profile has no institution. |
| **Test Data** | UCT |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open the new application form | The form asks for the institution before listing buildings |
| 2 | Pick UCT | Buildings for UCT are listed; the profile now shows UCT |
| 3 | Reload the form | The institution is not asked again |

| Field | Value |
|-------|-------|
| **Postconditions** | Naledi's profile has institution UCT. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-015: One form, two companies, two applications

| Field | Value |
|-------|-------|
| **ID** | TC-REL-015 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | Thandi logged in, all required documents approved, no 2027 application anywhere. |
| **Test Data** | Preferences in order: Beta Lodge, Alpha House, Alpha Court |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Pick the three buildings in that order | Each company is checked as its buildings are picked; no problems shown |
| 2 | Choose Semester 1 for Company A and Annual for Company B | An intake is asked for only for companies with more than one open intake |
| 3 | Submit | Message like "2 applications sent: Company A (Alpha House, Alpha Court) and Company B (Beta Lodge)." |
| 4 | Open My Applications | Two cards, each showing company, year and intake |
| 5 | As Admin A, open the application | Preferences Alpha House (1), Alpha Court (2). No Beta Lodge |
| 6 | As Admin B, open the application list | Only Company B's application; Company A's is not visible |
| 7 | Activity logs (platform admin) | One submit row per application |

| Field | Value |
|-------|-------|
| **Postconditions** | Thandi has one 2027 application at A (Semester 1) and one at B (Annual). |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-016: Choosing an intake when several are open

| Field | Value |
|-------|-------|
| **ID** | TC-REL-016 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | Sipho logged in. Company A's UCT 2027 Annual, Semester 1 and Semester 2 windows are open. |
| **Test Data** | Alpha House only |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Pick Alpha House | The form asks which intake, listing the open ones in plain words |
| 2 | Submit without choosing (API: omit `intakeIds` for Company A) | Refused (409) asking to choose an intake; nothing saved |
| 3 | Choose Semester 2 and submit | One application for Semester 2 |

| Field | Value |
|-------|-------|
| **Postconditions** | Sipho has a Semester 2 application at Company A. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-017: Closed window refuses the whole submission

| Field | Value |
|-------|-------|
| **ID** | TC-REL-017 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | Lerato logged in. Admin C has moved Company C's 2027 Annual window to open next month (restore after). |
| **Test Data** | Alpha Court and Gamma Hall |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Pick Alpha Court and Gamma Hall | Gamma Hall shows "Company C opens applications on ..." with a button to remove Company C |
| 2 | Submit anyway through the API with both buildings | Refused (409), naming Company C and the reason. No application created at Company A either |
| 3 | Remove Company C on the form, choose the Annual intake for Company A and submit | One application at Company A for UCT 2027 Annual; the form kept the typed values after the first refusal |

| Field | Value |
|-------|-------|
| **Postconditions** | Lerato has one Company A application. Restore Company C's window. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-018: Missing documents refuse that company

| Field | Value |
|-------|-------|
| **ID** | TC-REL-018 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | Naledi logged in with no documents uploaded. |
| **Test Data** | Alpha Court |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Pick Alpha Court | The missing required documents for Company A are listed |
| 2 | Submit | Refused (409) naming Company A and the missing documents; nothing saved |

| Field | Value |
|-------|-------|
| **Postconditions** | No application for Naledi. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-019: One active application per company per year

| Field | Value |
|-------|-------|
| **ID** | TC-REL-019 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | Thandi has an active Company A application for 2027 (TC-REL-015). Ayanda is a 2026 resident at Company A (setup). |
| **Test Data** | Alpha Court again |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | As Thandi, pick Alpha Court | "You already applied for 2027 here (Semester 1, ...). View application." with a link to it |
| 2 | Submit through the API for the same intake | Refused (409): "You already applied to Company A for 2027." |
| 3 | As Ayanda, apply to Alpha House for UCT 2027 Annual | Accepted: a different year from her 2026 application at the same company |
| 4 | Fire 3 identical submits at once for one new student (for example with curl in parallel) | Exactly one application is created; the others get 409 |

| Field | Value |
|-------|-------|
| **Postconditions** | Ayanda has a 2026 and a 2027 application at Company A. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-020: Intake of another company or institution is refused

| Field | Value |
|-------|-------|
| **ID** | TC-REL-020 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | Pieter (SU) token. Ids of Company B's intake and Company A's UCT 2027 intake known. |
| **Test Data** | `POST /api/applications/submit` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Submit for Alpha Court with `intakeIds` mapping Company A to Company B's intake id | Refused (409): not one of Company A's intakes |
| 2 | Submit for Alpha Court with Company A mapped to the UCT 2027 intake | Refused: that intake is not for SU |
| 3 | Submit with an unknown intake id | Refused the same way as another company's, so ids cannot be probed |

| Field | Value |
|-------|-------|
| **Postconditions** | No application created. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-021: At most three buildings; duplicates count once

| Field | Value |
|-------|-------|
| **ID** | TC-REL-021 |
| **Module** | Release Next |
| **Priority** | Medium |
| **Preconditions** | Thandi logged in. |
| **Test Data** | Four buildings |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Try to pick a fourth building | The form does not allow more than three |
| 2 | Submit through the API with the same building twice | It counts once |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-022: Open windows banner, cards and approval email

| Field | Value |
|-------|-------|
| **ID** | TC-REL-022 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | Thandi has a Company A application. Admin A can approve it. |
| **Test Data** | None |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open the student dashboard | A banner lists companies with open windows (`GET /api/apply/open-windows`) |
| 2 | As Admin A, approve Thandi's application | Approved |
| 3 | Open the approval email in MailHog | Names the application's year and intake; the label reads "Intake", not "Semester" |

| Field | Value |
|-------|-------|
| **Postconditions** | Thandi's Company A application is APPROVED. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

## C. Stay dates and room capacity

---

### TC-REL-023: Semester 1 and Semester 2 share a bed

| Field | Value |
|-------|-------|
| **ID** | TC-REL-023 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | Room A-103 (capacity 1) empty for 2027. Thandi (Semester 1) and Sipho (Semester 2) applications are ready for allocation at Company A. |
| **Test Data** | Room A-103 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | As Admin A, offer A-103 to Thandi | OFFERED; allocation shows stay 1 Feb 2027 to 30 Jun 2027 |
| 2 | Offer A-103 to Sipho | OFFERED; stay 15 Jul 2027 to 30 Nov 2027. No capacity error |
| 3 | Check `GET /api/room-allocation/{id}` for both | `stayStartDate` and `stayEndDate` are the intakes' dates |

| Field | Value |
|-------|-------|
| **Postconditions** | A-103 holds two 2027 allocations that do not overlap. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-024: An Annual stay is refused when either semester holds the bed

| Field | Value |
|-------|-------|
| **ID** | TC-REL-024 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | TC-REL-023 done. Lerato has a UCT 2027 Annual application at Company A. |
| **Test Data** | Room A-103 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Offer A-103 to Lerato | Refused (409): the room has no free bed for the stay |
| 2 | Offer A-102 (capacity 2, empty for 2027) to Lerato | OFFERED |

| Field | Value |
|-------|-------|
| **Postconditions** | Lerato holds A-102 for the Annual stay. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Capacity counts overlapping allocations, not the peak (PR #510 decision 3). |

---

### TC-REL-025: A 2026 resident is offered a 2027 room

| Field | Value |
|-------|-------|
| **ID** | TC-REL-025 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | Ayanda is ACTIVE in A-101 for 2026 and has a 2027 Annual application (TC-REL-019). |
| **Test Data** | Room A-101 for 2027 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Check eligibility (`GET /api/room-allocation/student/{id}/eligibility`) | Eligible for 2027 |
| 2 | Offer A-101 to Ayanda for 2027 | OFFERED. The 2026 allocation stays ACTIVE |
| 3 | As Ayanda, open the dashboard | Both the current room and the 2027 offer are visible; the active-allocation card shows the open offer first |

| Field | Value |
|-------|-------|
| **Postconditions** | Ayanda holds a 2026 ACTIVE and a 2027 OFFERED allocation. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-026: A student cannot hold two beds for overlapping stays

| Field | Value |
|-------|-------|
| **ID** | TC-REL-026 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | Thandi holds A-103 for Semester 1 2027 (OFFERED or ACCEPTED). |
| **Test Data** | Room A-104 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Offer A-104 to Thandi for the same application | Refused (409): she already holds a bed for those dates |

| Field | Value |
|-------|-------|
| **Postconditions** | No second allocation. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-027: Accepting without a check-in date; deadline from move-in

| Field | Value |
|-------|-------|
| **ID** | TC-REL-027 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | Thandi has an OFFERED allocation for Semester 1 2027. |
| **Test Data** | Accept dialog |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open the accept dialog | The date picker starts on move-in day (1 Feb 2027) |
| 2 | Accept through the API with no `expectedCheckInDate` | ACCEPTED; `expectedCheckInDate` is move-in day |
| 3 | Read `check_in_expiry_date` (SQL in 2.5 or the allocation detail) | Move-in plus 7 days (8 Feb 2027), not acceptance plus 7 days |

| Field | Value |
|-------|-------|
| **Postconditions** | Thandi's place is ACCEPTED. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Thandi's Company B 2027 Annual application overlaps this stay and is withdrawn by this accept (section G). Accept through the dialog for Thandi and through the API for another student if you want both paths. |

---

### TC-REL-028: Check-in is refused while the room is occupied

| Field | Value |
|-------|-------|
| **ID** | TC-REL-028 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | Kabelo is ACTIVE in A-106 (capacity 1) for 2026. Lungi has been offered A-106 for UCT 2027 Semester 1 and has accepted; the offer is allowed because the planned stays do not overlap. |
| **Test Data** | Room A-106 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | As Admin A, check Lungi in | Refused (409): the room is still occupied by a checked-in student. Lungi's place stays ACCEPTED |
| 2 | Run TC-REL-041 (overdue check-outs) now if it has not been run, while Kabelo is still in | See TC-REL-041 |
| 3 | Check Kabelo out | COMPLETED |
| 4 | Check Lungi in again | ACTIVE |

| Field | Value |
|-------|-------|
| **Postconditions** | Lungi ACTIVE in A-106. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Checking in before move-in day is not blocked by this release; the refusal comes only from the occupied bed. |

---

### TC-REL-029: A move into an occupied room is refused

| Field | Value |
|-------|-------|
| **ID** | TC-REL-029 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | Run before TC-REL-028 step 3: Kabelo still checked in to A-106. Ayanda ACTIVE in A-101. |
| **Test Data** | `POST /api/admin/allocation/{Ayanda's 2026 allocation id}/move` to A-106 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Move Ayanda into A-106 | Refused (409): room still occupied |

| Field | Value |
|-------|-------|
| **Postconditions** | No move. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-030: A stay that has ended is never offered

| Field | Value |
|-------|-------|
| **ID** | TC-REL-030 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | An application for Company A's 2026 Annual intake in AWAITING_ALLOCATION, with the intake's stay end moved to yesterday (SQL in 2.5). |
| **Test Data** | Any free room |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Offer a room for that application | Refused (409): "This stay ended on ..." |
| 2 | Restore the intake's stay end date | Restored |

| Field | Value |
|-------|-------|
| **Postconditions** | No offer created. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-031: Rooms that are FULL today are still scored for next year

| Field | Value |
|-------|-------|
| **ID** | TC-REL-031 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | A-107 (capacity 1) shows status FULL today because Sello lives there for 2026, but it is free for 2027. A spare UCT student's 2027 application is AWAITING_ALLOCATION at Company A with no offer. Other rooms may be set to MAINTENANCE for the run so A-107 is the obvious choice. |
| **Test Data** | Staged allocation |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Run "Propose batch" (staged allocation, 08-STG) | A-107 can be proposed for the 2027 application |
| 2 | Set a room to MAINTENANCE and propose again | That room is never proposed |

| Field | Value |
|-------|-------|
| **Postconditions** | Reject the proposals if not needed. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

## D. Reconfirmation before move-in

---

### TC-REL-032: A place accepted close to move-in is never asked

| Field | Value |
|-------|-------|
| **ID** | TC-REL-032 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | An ACCEPTED place whose `accepted_at` is less than 42 days before its move-in. |
| **Test Data** | Job `reconfirmation` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Run the reconfirmation job (section 2.4) | No "Still coming?" card, email or change to `reconfirm_requested_at` |
| 2 | Check a place accepted before this release (`accepted_at` empty) | Never asked |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-033: "Still coming?" is asked 28 days before move-in

| Field | Value |
|-------|-------|
| **ID** | TC-REL-033 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | Thandi's ACCEPTED Semester 1 place. SQL in 2.5 sets move-in to 20 days from today and `accepted_at` to 60 days ago. |
| **Test Data** | Job `reconfirmation` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Run the job | Log `action=reconfirmationRequested ... reminder=false` |
| 2 | Open MailHog | A reconfirm request email to Thandi with the release date |
| 3 | Open Thandi's dashboard | "Still coming?" card with the move-in day and an "I'm still coming" button |

| Field | Value |
|-------|-------|
| **Postconditions** | `reconfirm_requested_at` set. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-034: Confirming keeps the place

| Field | Value |
|-------|-------|
| **ID** | TC-REL-034 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | TC-REL-033 done. |
| **Test Data** | None |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "I'm still coming" | Success; the card goes away; `reconfirmed_at` set |
| 2 | Run the job again | Nothing happens to this place |

| Field | Value |
|-------|-------|
| **Postconditions** | Place still ACCEPTED. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-035: Reminder every 7 days

| Field | Value |
|-------|-------|
| **ID** | TC-REL-035 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | Sipho has accepted his A-103 Semester 2 offer. Apply the ask-point SQL in 2.5 to his allocation, then the reminder SQL (asked 7 days ago). |
| **Test Data** | Job `reconfirmation` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Run the job | A reminder email; `reconfirm_reminded_at` set |
| 2 | Run it again the same day | No second reminder |

| Field | Value |
|-------|-------|
| **Postconditions** | Place still ACCEPTED. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-036: Unconfirmed place is released 14 days before move-in

| Field | Value |
|-------|-------|
| **ID** | TC-REL-036 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | Sipho's place asked at least a day ago and not confirmed; SQL sets move-in to 10 days from today. Another student is waiting for that intake at Company A. |
| **Test Data** | Job `reconfirmation` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Run the job | Allocation CANCELLED; application CANCELLED with remark "Place released: not reconfirmed before move-in."; waiting-list entry closed |
| 2 | Open MailHog | "Place released" email to Sipho |
| 3 | Check the room and the waiting list | Occupancy synced; the next waiting student is offered a bed (refill, 09-WL) |
| 4 | Activity / allocation history | Status change ACCEPTED to CANCELLED recorded |

| Field | Value |
|-------|-------|
| **Postconditions** | Sipho's place released. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | A place never asked before the release point is asked first and released on a later run (PR #510 decision 8). |

---

### TC-REL-037: Another student cannot reconfirm someone's place

| Field | Value |
|-------|-------|
| **ID** | TC-REL-037 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | Thandi's ACCEPTED place id known. Lerato's token. |
| **Test Data** | `POST /api/room-allocation/{Thandi's id}/reconfirm` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Call as Lerato | 403; Thandi's `reconfirmed_at` unchanged |
| 2 | Call as Admin A (TENANT_ADMIN) | 403 (endpoint is STUDENT and PLATFORM_ADMIN only) |
| 3 | Call as Thandi on a place that is not ACCEPTED | 409 |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-038: Company override of reconfirmation lead times

| Field | Value |
|-------|-------|
| **ID** | TC-REL-038 |
| **Module** | Release Next |
| **Priority** | Medium |
| **Preconditions** | SQL access. |
| **Test Data** | Company A: reconfirm_ask_days_before 35 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Insert the override (2.5) and set a place's move-in to 33 days from today, accepted 90 days ago | Prepared |
| 2 | Run the job | The place is asked (it would not be with the platform default 28) |
| 3 | Delete the override | Default applies again |

| Field | Value |
|-------|-------|
| **Postconditions** | No override left. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

## E. Admin filters and overdue check-outs

---

### TC-REL-039: Filter applications by institution and intake

| Field | Value |
|-------|-------|
| **ID** | TC-REL-039 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | Company A has applications for UCT 2027 Annual, Semester 1 and Semester 2 and the 2026 year. |
| **Test Data** | Filter UCT, then "2027 · Semester 2" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open `/admin/applications`, pick institution UCT | The intake list narrows to UCT and "All institutions" years |
| 2 | Pick Semester 2 of 2027 | Only Semester 2 applications listed |
| 3 | Repeat with `GET /api/admin/applications/search?institutionId=&intakeId=` | Same rows |
| 4 | As Admin B, pass Company A's intake id | No Company A rows |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-040: Filter allocations by institution and intake

| Field | Value |
|-------|-------|
| **ID** | TC-REL-040 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | Allocations exist for several intakes at Company A. |
| **Test Data** | `/admin/room-allocations` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Filter by UCT and Semester 1 | Only allocations whose application is for Semester 1 |
| 2 | `GET /api/room-allocation/search?intakeId=` | Same rows |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-041: Overdue check-outs on the dashboard

| Field | Value |
|-------|-------|
| **ID** | TC-REL-041 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | Kabelo is ACTIVE in A-106; SQL in 2.5 moves his planned stay end to 3 days ago. For the isolation check, a Company B resident with a past stay end if one exists. |
| **Test Data** | Admin dashboard |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | As Admin A, open the dashboard | "Overdue check-outs (n)" lists Company A's overdue resident, longest overdue first |
| 2 | `GET /api/admin/stats/overdue-check-outs` as Admin A | Company A rows only |
| 3 | As Admin B | No Company A rows |
| 4 | After TC-REL-028 checks Kabelo out | He is no longer listed |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

## F. Applications expire with their intake

---

### TC-REL-042: Unfinished applications expire the day after the stay ends

| Field | Value |
|-------|-------|
| **ID** | TC-REL-042 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | Company A's 2026 Annual intake has applications in PENDING, UNDER_REVIEW, CHANGES_REQUESTED, APPROVED and AWAITING_ALLOCATION (one each, or as many as practical), with waiting-list entries. SQL sets the intake's stay end to yesterday. |
| **Test Data** | Job `intakeExpiry` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Run the job | Log `action=applicationExpired` per application; each is EXPIRED with remark "Expired: the stay applied for ended on ..." |
| 2 | Check the waiting list | Their entries are closed |
| 3 | Open MailHog | One application-expired email per student, naming the intake and the earliest intake open today at Company A for their institution (for example UCT 2027 Semester 1), with an "Apply Now" link to `/applications/new` |

| Field | Value |
|-------|-------|
| **Postconditions** | Applications EXPIRED. Restore the intake's end date. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-043: Running intakes and outstanding offers are left alone

| Field | Value |
|-------|-------|
| **ID** | TC-REL-043 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | A UCT 2027 PENDING application. A 2026 application with an OFFERED allocation on the intake whose end is moved to yesterday. |
| **Test Data** | Job `intakeExpiry` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Run the job | The 2027 application is unchanged |
| 2 | Check the application with the open offer | Unchanged; log `reason=offerOutstanding` |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | PR #511 decision 1: offer expiry and check-in expiry end those. |

---

### TC-REL-044: Expiry email when no intake is open

| Field | Value |
|-------|-------|
| **ID** | TC-REL-044 |
| **Module** | Release Next |
| **Priority** | Medium |
| **Preconditions** | A student with an expiring application at a company with no open intake for their institution. |
| **Test Data** | Job `intakeExpiry` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Run the job | The email says no intake is open |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

## G. Withdrawal on acceptance across companies

---

### TC-REL-045: Accepting one place withdraws overlapping applications elsewhere

| Field | Value |
|-------|-------|
| **ID** | TC-REL-045 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | Mpho applied on one form to Alpha House (UCT 2027 Semester 1), Beta Lodge (2027 Annual) and Gamma Hall (2027 Semester 2). Admin A offered A-104 and Admin B offered B-201; the Company C application is PENDING. Admins B and C logged in. |
| **Test Data** | Accept at Company A |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | As Mpho, accept Company A's offer | ACCEPTED; the response returns normally |
| 2 | Check Company B's application and offer | Application CANCELLED; offer CANCELLED; bed back in the pool and refilled if someone waits |
| 3 | Check Company C's Semester 2 application | Unchanged: Semester 2 does not overlap Semester 1 |
| 4 | Open MailHog | One applications-withdrawn email listing the Company B application only |
| 5 | As Admin B, open notifications | An in-app notice about Company B's own application. Company A is not named |
| 6 | As Admin C | No notice |

| Field | Value |
|-------|-------|
| **Postconditions** | Mpho holds Company A's place and the Company C application. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-046: Another student's applications are never touched

| Field | Value |
|-------|-------|
| **ID** | TC-REL-046 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | Another student has an application at Company B overlapping Mpho's accepted stay, made before the accept in the previous case. |
| **Test Data** | None |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Check that student's applications | Unchanged |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-047: No withdrawal without stay dates; rollback withdraws nothing

| Field | Value |
|-------|-------|
| **ID** | TC-REL-047 |
| **Module** | Release Next |
| **Priority** | Medium |
| **Preconditions** | An accepted place with no planned stay dates (an allocation made without an application), if one exists from migrated data. |
| **Test Data** | None |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Accept such a place | No application elsewhere is withdrawn |
| 2 | Try to accept an offer that fails (for example already expired) | Nothing withdrawn elsewhere |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Mark N/A if no undated allocation exists. |

---

## H. Expiring documents

---

### TC-REL-048: Mark a document type as expiring

| Field | Value |
|-------|-------|
| **ID** | TC-REL-048 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | Platform admin and Admin A logged in. |
| **Test Data** | Proof of Enrollment (platform type); Company A type "Residence Rules" (optional) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | As platform admin, edit Proof of Enrollment, tick "Expires with the academic year", save | Saved; `is_expiring` true |
| 2 | As Admin A, create "Residence Rules" (not required) and tick expiring | Saved |
| 3 | As Admin A, `PATCH /api/document-type/{Proof of Enrollment id}/expiring?expiring=false` | 403: tenant admins cannot change platform types |
| 4 | As Admin B, PATCH Company A's "Residence Rules" | 404 |

| Field | Value |
|-------|-------|
| **Postconditions** | Proof of Enrollment expiring at every company. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-049: Student sees the expiry date

| Field | Value |
|-------|-------|
| **ID** | TC-REL-049 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | Thandi's Proof of Enrollment uploaded during Company A's 2026 year and approved; she has a live Company A application. |
| **Test Data** | Documents page |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open the documents page | "Your proof of enrollment expires on 30 November. Upload the new one." with an upload action |
| 2 | `GET /api/documents/my/expiry` | The expiry date per company |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-050: Two companies give different expiry dates

| Field | Value |
|-------|-------|
| **ID** | TC-REL-050 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | Thandi also has a live Company B application. Company B has no 2026 year; its first year ending after the upload is 2027. |
| **Test Data** | `GET /api/documents/my/expiry` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Read the response | Company A: 30 Nov 2026. Company B: 30 Nov 2027 (end of its 2027 year) |
| 2 | Company D (no years) where Thandi has no application | No expiry listed for it |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-051: Reminder at the start of the reminder period

| Field | Value |
|-------|-------|
| **ID** | TC-REL-051 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | SQL override: Company A `document_expiry_reminder_days` = days from today to the expiry date (2.5). |
| **Test Data** | Job `documentExpiryReminder` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Run the job once | A document-expiry-reminder email and an in-app notification for Thandi |
| 2 | Delete the override | Default 30 applies |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | The cadence is worked out from dates: running the job twice on the same day sends the reminder twice. |

---

### TC-REL-052: Reminder the day after expiry

| Field | Value |
|-------|-------|
| **ID** | TC-REL-052 |
| **Module** | Release Next |
| **Priority** | Medium |
| **Preconditions** | Company C has a year "2026" ending yesterday. Pieter has a live Company C application and an approved Proof of Enrollment whose `created_at` is inside that year (SQL 2.5). |
| **Test Data** | Job `documentExpiryReminder` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Run the job | An expired-document reminder for Pieter |
| 2 | As Admin C, open Pieter's application | Expired badge |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-053: Replace an expired document during a live application

| Field | Value |
|-------|-------|
| **ID** | TC-REL-053 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | Thandi's Proof of Enrollment is approved and expired (SQL sets `created_at` into Company A's 2025 year). Her National ID is approved and current. She has a live application. |
| **Test Data** | New PDF |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Upload a new Proof of Enrollment | Accepted; the document is PENDING |
| 2 | Try to replace the current approved ID Document | Refused (409); a current approved document stays locked while an application is live |
| 3 | As Admin A, approve the new Proof of Enrollment | APPROVED; expiry follows the new upload date |

| Field | Value |
|-------|-------|
| **Postconditions** | Thandi's documents current. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-054: Expired required document holds back offers

| Field | Value |
|-------|-------|
| **ID** | TC-REL-054 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | A spare UCT student whose required Proof of Enrollment is approved but expired (SQL in 2.5). Their 2027 application is AWAITING_ALLOCATION at Company A with a waiting-list entry. |
| **Test Data** | Manual, staged and batch allocation |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Admin A offers the student a room | Refused (409) naming the document |
| 2 | Propose batch (staged) | The student is not proposed |
| 3 | Let the batch allocation job run (feature flag `auto-allocation` on) | The student is skipped; the waiting-list entry stays WAITING |
| 4 | Mark only an optional expiring type as expired | Offers are not held back |

| Field | Value |
|-------|-------|
| **Postconditions** | No offer for the student. Restore the document's `created_at`. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-055: Expired required document refuses check-in

| Field | Value |
|-------|-------|
| **ID** | TC-REL-055 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | A spare UCT student with an ACCEPTED place in an empty room whose Proof of Enrollment is expired (SQL in 2.5). |
| **Test Data** | Check-in |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Admin A checks Lungi in | Refused (409) |
| 2 | Replace and approve the document, check in again | ACTIVE |

| Field | Value |
|-------|-------|
| **Postconditions** | Student ACTIVE. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-056: Expired badge for admins

| Field | Value |
|-------|-------|
| **ID** | TC-REL-056 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | At least one application with an expired expiring document. |
| **Test Data** | `/admin/applications` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open the application list | Expired badge on that row |
| 2 | Open the detail | Badge, and each expiring document's expiry date |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

## I. Stay extensions

---

### TC-REL-057: Extend Semester 1 into Semester 2

| Field | Value |
|-------|-------|
| **ID** | TC-REL-057 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | Run after section G. Mpho ACCEPTED in A-104 (capacity 1) for UCT 2027 Semester 1; nobody holds A-104 for Semester 2; Mpho's required documents approved and not expired. |
| **Test Data** | "Stay for Semester 2" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open the dashboard room card | "Stay for Semester 2" shown |
| 2 | Tap it | Confirmed at once; stay end becomes 30 Nov 2027; same allocation, room and application |
| 3 | Open MailHog and Admin A's notifications | Stay-extended email to Mpho; in-app notice to Company A admins |
| 4 | Activity logs | `EXTEND_STAY` row |
| 5 | Offer A-104 to another student for Semester 2 | Refused: the bed is taken, including the gap between 30 Jun and 15 Jul |

| Field | Value |
|-------|-------|
| **Postconditions** | Mpho's stay ends 30 Nov 2027. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-058: Refused when the bed is taken

| Field | Value |
|-------|-------|
| **ID** | TC-REL-058 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | Run before the release case in section D, or give A-103 Semester 2 to a spare student first. Thandi ACCEPTED in A-103 for Semester 1; Sipho holds A-103 for Semester 2. |
| **Test Data** | Room card |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open the dashboard | "Apply for Semester 2" shown instead of "Stay for" |
| 2 | `POST /api/room-allocation/{id}/stay-extension` | 409 with the reason; stay end unchanged |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-059: Refused with an expired document

| Field | Value |
|-------|-------|
| **ID** | TC-REL-059 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | Student's required expiring document expired (SQL); bed free for the next intake. |
| **Test Data** | Room card |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open the dashboard | No "Stay for" button |
| 2 | POST the extension | 409 |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-060: An Annual stay cannot be extended

| Field | Value |
|-------|-------|
| **ID** | TC-REL-060 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | Lerato holds an Annual 2027 place. |
| **Test Data** | `GET /api/room-allocation/{id}/stay-extension` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Call GET | Not allowed |
| 2 | Call POST | 409 |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-061: Another student gets 403

| Field | Value |
|-------|-------|
| **ID** | TC-REL-061 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | Thandi's allocation id. Lerato's token. |
| **Test Data** | GET and POST stay-extension |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Call both as Lerato | 403 for both; nothing changes |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-062: Two requests for the last bed

| Field | Value |
|-------|-------|
| **ID** | TC-REL-062 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | A-105 (capacity 2): Zanele and Bongani hold it for UCT 2027 Semester 1 (ACCEPTED), both with documents in order; Karabo holds one Semester 2 bed, so one Semester 2 bed is left. |
| **Test Data** | Two parallel POSTs |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Send both extension requests at the same moment (curl in parallel) | One 200 and one 409 |

| Field | Value |
|-------|-------|
| **Postconditions** | One extension. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

## J. Returning residents

---

### TC-REL-063: Returning badge

| Field | Value |
|-------|-------|
| **ID** | TC-REL-063 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | Ayanda (2026 resident at A) applied for 2027 at A (TC-REL-019). Lerato (first time). A past resident of Company B applies to Company A. |
| **Test Data** | `/admin/applications` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Admin A opens Ayanda's 2027 application | "Returning" badge on the list and detail |
| 2 | Lerato's application | No badge |
| 3 | The Company B past resident's Company A application | No badge (returning is per company) |
| 4 | Applications submitted before this release | No badge (no backfill) |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

## K. Migrations on PROD-like data

---

### TC-REL-064: Migrations run on a copy of PROD-like data

| Field | Value |
|-------|-------|
| **ID** | TC-REL-064 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | A database restored from a recent NONPROD or anonymised PROD dump, at the previous release's schema. Backend of this release. |
| **Test Data** | Flyway log |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Start the backend | All 13 migrations from `V2026_11_10_1000` to `V2026_11_11_1100` apply; the app starts (`hbm2ddl validate` passes) |
| 2 | Read the log for `RAISE WARNING` from `V2026_11_10_1710` | Record any `left nullable` warning and its count |
| 3 | Run the nullability and count queries in the release notes | Matches the warnings: NOT NULL where no row was empty |
| 4 | `SELECT is_current FROM academic_year LIMIT 1;` | Error: column does not exist (also `application_open_date`, `application_close_date`) |

| Field | Value |
|-------|-------|
| **Postconditions** | Migrated copy for the next cases. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-065: Existing data still loads and is repaired

| Field | Value |
|-------|-------|
| **ID** | TC-REL-065 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | TC-REL-064 done. |
| **Test Data** | Existing applications, allocations, years |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open `/admin/applications` and several old applications as a company admin | Load without error; each shows company, year and intake |
| 2 | Check each company's `/admin/academic-years` | Years that rows pointed at in another company now exist as the company's own copy; companies with no year got the platform years that had not ended |
| 3 | `SELECT COUNT(*) FROM academic_year y WHERE NOT EXISTS (SELECT 1 FROM intake i WHERE i.academic_year_id = y.id);` | 0, unless a year ends before it starts |
| 4 | `SELECT COUNT(*) FROM room_allocation WHERE stay_start_date IS NULL;` | Only allocations with no application and no usable year |
| 5 | Open existing allocations and the student dashboard of an existing resident | Load without error |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-066: Settings seeded

| Field | Value |
|-------|-------|
| **ID** | TC-REL-066 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | Migrated database. |
| **Test Data** | `system_settings` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | `SELECT setting_key, setting_value FROM system_settings WHERE tenant_id = 0 AND setting_key IN ('intake_window_open_days_before','intake_window_close_days_before','reconfirm_min_lead_days','reconfirm_ask_days_before','reconfirm_release_days_before','document_expiry_reminder_days');` | 120, 14, 42, 28, 14, 30 |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

## L. Security

---

### TC-REL-067: Student profile ownership (#491)

| Field | Value |
|-------|-------|
| **ID** | TC-REL-067 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | Two student accounts: Naledi (no profile) and Thandi (has one). Platform admin token. |
| **Test Data** | `POST /api/student` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | As Naledi, create a profile naming Thandi's user id | 403; nothing created |
| 2 | As Naledi, create a profile with no `userId` | 201; owned by Naledi |
| 3 | As Naledi, create a second profile | 409 |
| 4 | As platform admin, create a profile for an unknown user id | 404 |
| 5 | As platform admin, for a user who is not a student | Refused (409); nothing created |

| Field | Value |
|-------|-------|
| **Postconditions** | Naledi has one profile. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-068: Company ownership checks by id (#492)

| Field | Value |
|-------|-------|
| **ID** | TC-REL-068 |
| **Module** | Release Next |
| **Priority** | Critical |
| **Preconditions** | Admin A token. A Company B application id and room id. A Company A application of another student. |
| **Test Data** | Enqueue and room availability |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | `POST /api/admin/allocation/waiting-list/enqueue` with the Company B application | 404 |
| 2 | Enqueue a Company A student on another student's Company A application | Refused |
| 3 | `GET /api/room-allocation/room/{B room id}/availability` and `/is-available` | 404 for both |
| 4 | Repeat with Company A's own rows | 200 |

| Field | Value |
|-------|-------|
| **Postconditions** | No queue change. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-069: New endpoints respect roles

| Field | Value |
|-------|-------|
| **ID** | TC-REL-069 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | Tokens for a student, Admin A and platform admin. |
| **Test Data** | New endpoints |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | As Admin A, call `GET /api/apply/buildings`, `/api/apply/open-windows`, `/api/documents/my/expiry` | 403 (STUDENT only) |
| 2 | As a student, call `POST /api/academic-years/from-pattern`, `GET /api/admin/stats/overdue-check-outs`, `PATCH /api/document-type/{id}/expiring` | 403 |
| 3 | As Admin A, `POST /api/room-allocation/{id}/stay-extension` | 403 |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

## M. Fixes and operations

---

### TC-REL-070: Form errors and hints show again (#503)

| Field | Value |
|-------|-------|
| **ID** | TC-REL-070 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | Logged out, then admin. |
| **Test Data** | Login, signup, change password, application form, create-user dialog, building and room forms |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Submit each form with a required field empty | The field's error text appears under it |
| 2 | Open the create-user dialog | The hint about signing in is visible |
| 3 | Check each form at desktop and phone width (375 px) | No overlapping or clipped fields; record spacing issues |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-071: Notification stream stays open through nginx (#504)

| Field | Value |
|-------|-------|
| **ID** | TC-REL-071 |
| **Module** | Release Next |
| **Priority** | High |
| **Preconditions** | Frontend served by the nginx image (NONPROD, or `docker run` of the frontend image), a logged-in user. |
| **Test Data** | `/api/notifications/stream` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open DevTools Network, filter on `stream`, leave the page idle for 5 minutes | One open request; no reconnect every minute or two |
| 2 | Trigger a notification (for example approve an application) | It arrives on the open stream |
| 3 | Read the nginx error log | No `upstream timed out` for the stream |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | The backend ends a stream after 30 minutes; one reconnect per 30 minutes is expected. |

---

### TC-REL-072: NONPROD database and MinIO tunnels (#502)

| Field | Value |
|-------|-------|
| **ID** | TC-REL-072 |
| **Module** | Release Next |
| **Priority** | Medium |
| **Preconditions** | Developer with NONPROD SSH access. After a NONPROD deploy. |
| **Test Data** | `docs/NONPROD_RUNBOOK.md`, Developer access from your laptop |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Run the `docker inspect` command from the runbook on the server | db prints `172.28.0.20`; minio is on `172.28.0.30` |
| 2 | Open the Postgres tunnel on local port 15432 and connect with SSL mode require | Connected |
| 3 | Open the MinIO tunnel and `http://localhost:9001` | Console loads |
| 4 | Scan the server's public ports | Neither 5432 nor 9000/9001 is published |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-073: PROD runbook and alerts walkthrough (#506)

| Field | Value |
|-------|-------|
| **ID** | TC-REL-073 |
| **Module** | Release Next |
| **Priority** | Medium |
| **Preconditions** | Operator with access to `docs/PROD_RUNBOOK.md` and Grafana Cloud. |
| **Test Data** | "When an alert fires", "Reading logs" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | For each alert in the table, find the rule in Grafana Cloud or CloudWatch | Names and thresholds match |
| 2 | Run the Loki query for backend errors in Explore | Returns lines with the documented labels |
| 3 | After the deploy, check that no new alert fired for 24 hours | None, or each explained |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

### TC-REL-074: Per-company deployment is a proposal only (#501)

| Field | Value |
|-------|-------|
| **ID** | TC-REL-074 |
| **Module** | Release Next |
| **Priority** | Low |
| **Preconditions** | None. |
| **Test Data** | `docs/adr/ADR-010-client-branded-sites.md` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Read the ADR status | Proposed |
| 2 | Confirm the release has no per-company build or deploy change | No compose, workflow or app change from #501 |

| Field | Value |
|-------|-------|
| **Postconditions** | None. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** |  |

---

## 5. Regression

Run these existing files on the release build, after the migrations, on the same data. "Full"
means every case; "Part" lists the sections that touch changed code.

| File | Scope | Why |
|------|-------|-----|
| [01-AUTH.md](01-AUTH.md) | Part: login, signup, change password | Form error text and spacing changed (#503) |
| [02-ONB.md](02-ONB.md) | Part: profile creation and the application submission and review flow | Profile ownership (#491); submit now returns a list and needs an open intake (#509) |
| [03-APP.md](03-APP.md) | Full | Submit, statuses, cancel and search all changed (#509, #510, #511) |
| [04-DOC.md](04-DOC.md) | Full | Upload lock rules and document types changed (#512) |
| [05-BLD.md](05-BLD.md) | Part: building forms and the public building pages | Form spacing (#503); public pages count beds by date (#510) |
| [06-ROM.md](06-ROM.md) | Part: availability, occupancy, room forms, delete | Capacity by stay dates; availability 404 across companies (#492, #510) |
| [07-ALC.md](07-ALC.md) | Full | Every allocation path counts by stay dates; accept, check-in and move changed (#510, #512) |
| [08-STG.md](08-STG.md) | Full | Staged and batch allocation use the bed ledger and document checks (#510, #512) |
| [09-WL.md](09-WL.md) | Full | Enqueue checks, waiting-list position per year, refill after release (#492, #510, #511) |
| [10-NTF.md](10-NTF.md) | Full | Seven new email templates, new in-app notices, live stream through nginx (#504, #510, #511, #512) |
| [11-REF.md](11-REF.md) | Part: academic years, document types, institutions | Academic years rebuilt; document types gain the expiring flag (#508, #512) |
| [12-TNT.md](12-TNT.md) | Part: creating a company | A new company has no year and takes no applications until it adds one (#509) |
| [13-USR.md](13-USR.md) | Part: create-user dialog | Hint text visible again (#503) |
| [14-SET.md](14-SET.md) | Full | Six new settings in the cascade (#508, #510, #512) |
| [16-LOG.md](16-LOG.md) | Part: activity log rows | New logged actions: year and intake changes, `EXTEND_STAY`, submit per application |
| [17-DSH.md](17-DSH.md) | Full | Overdue check-outs card; occupancy counts (#510) |
| [18-JOB.md](18-JOB.md) | Full | Offer and check-in expiry interact with intake expiry; check-in deadline from move-in (#510, #511) |
| [19-SEC.md](19-SEC.md) | Full | New endpoints, tenant isolation, student ownership (#491, #492) |
| [20-XFW.md](20-XFW.md) | Full | End-to-end flows now run through intakes |

15-FF (feature flags) is not affected beyond the `auto-allocation` flag used in TC-REL-054; run it
only if time allows.

---

## 6. Summary

| Section | Test Cases | IDs |
|---------|-----------|-----|
| A. Academic years and intakes (company admin) | 12 | TC-REL-001 to TC-REL-012 |
| B. Applying for an intake (student) | 10 | TC-REL-013 to TC-REL-022 |
| C. Stay dates and room capacity | 9 | TC-REL-023 to TC-REL-031 |
| D. Reconfirmation before move-in | 7 | TC-REL-032 to TC-REL-038 |
| E. Admin filters and overdue check-outs | 3 | TC-REL-039 to TC-REL-041 |
| F. Applications expire with their intake | 3 | TC-REL-042 to TC-REL-044 |
| G. Withdrawal on acceptance across companies | 3 | TC-REL-045 to TC-REL-047 |
| H. Expiring documents | 9 | TC-REL-048 to TC-REL-056 |
| I. Stay extensions | 6 | TC-REL-057 to TC-REL-062 |
| J. Returning residents | 1 | TC-REL-063 to TC-REL-063 |
| K. Migrations on PROD-like data | 3 | TC-REL-064 to TC-REL-066 |
| L. Security | 3 | TC-REL-067 to TC-REL-069 |
| M. Fixes and operations | 5 | TC-REL-070 to TC-REL-074 |
| **Total** | **74** | |

---

## 7. Exit Criteria

The release may go to PROD when all of these hold:

1. Every Critical case in this file has passed.
2. Every High case has passed, or has a filed issue that the product owner accepts for this
   release in writing.
3. The regression files marked Full have no Critical or High failure that is new in this release.
4. TC-REL-064 to TC-REL-066 have run on a copy of PROD-like data, and any `V2026_11_10_1710`
   warning has a decision from a person.
5. The "Decisions to confirm" in the release notes have been read by the product owner, and any
   one they reject has an issue.
6. No open defect allows one company to see or change another company's data, or one student to
   act on another student's data.
7. The deploy and rollback steps in the release notes have been read by the operator who will run
   the deploy.

---

## 8. Sign-Off

By signing below, I confirm that the cases in this file and the regression set have been executed
and reviewed, and the results accurately reflect the release's readiness for production.

| Role | Name | Signature | Date |
|------|------|-----------|------|
| **QA Lead** | _________________ | _________________ | _________ |
| **Product Owner** | _________________ | _________________ | _________ |
| **Operator (deploy)** | _________________ | _________________ | _________ |
| **UAT Lead** | _________________ | _________________ | _________ |
