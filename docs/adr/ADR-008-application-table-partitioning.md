# ADR-008: application Table Partitioning Plan

- **Status:** Decision — Defer implementation; monitor row count
- **Date:** 2026-02-26
- **Task:** TASK-3.13
- **Architecture Reference:** [ARCH 6.2]
- **Requirement Reference:** [REQ scalability NFR — 50,000 applications per year]
- **Deciders:** Tech Lead, Backend Lead, DBA

---

## Context

The `application` table is the central entity of the system. It currently has no
partitioning. [ARCH 6.2] mandates: *"Plan range partitioning on
`application(created_at)` at 100,000 rows."*

At the current production scale, the table is well below this threshold. This ADR
documents the plan and the trigger criteria so that implementation can begin
without additional research when the threshold is approached.

### Reference pattern: `activity_logs`

The `activity_logs` table is already range-partitioned on `timestamp` using
yearly partitions. This is the established pattern in the schema and must be
followed for `application`:

```sql
-- From V2026_02_19_2100__init_full_schema.sql
CREATE TABLE activity_logs (...) PARTITION BY RANGE (timestamp);
CREATE TABLE activity_logs_2024 PARTITION OF activity_logs
    FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');
CREATE TABLE activity_logs_2025 PARTITION OF activity_logs
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
CREATE TABLE activity_logs_2026 PARTITION OF activity_logs
    FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');
CREATE TABLE activity_logs_2027 PARTITION OF activity_logs
    FOR VALUES FROM ('2027-01-01') TO ('2028-01-01');
CREATE TABLE activity_logs_default PARTITION OF activity_logs DEFAULT;
```

---

## Growth Projection

| Year | Cumulative rows (@ 50,000/yr) | Partitioning needed? |
|------|-------------------------------|----------------------|
| 1    | ~50,000                       | No                   |
| 2    | ~100,000                      | **Threshold reached** |
| 3    | ~150,000                      | Yes (already applied) |

At the target growth rate of 50,000 applications per year, the 100,000-row
threshold will be crossed during **Year 2** of operation.

---

## Decision

**Defer implementation until the 100,000-row trigger is reached.** Partitioning
an already-populated table in PostgreSQL requires a full table rebuild
(`pg_partman` or manual `CREATE TABLE … PARTITION BY` + data migration). This
carries downtime risk and should not be performed speculatively.

**Monitor continuously** via the Grafana dashboard panel added in this task
(see [Monitoring](#monitoring-query--grafana-panel) below).

---

## Implementation Plan (execute when threshold is reached)

### Step 1 — Pre-migration checks

- [ ] Verify row count: `SELECT COUNT(*) FROM application;` exceeds 100,000
- [ ] Confirm a maintenance window is scheduled (DDL will lock the table briefly)
- [ ] Take a full PostgreSQL dump before proceeding

### Step 2 — Create the partitioned replacement table

```sql
CREATE TABLE application_partitioned (
    -- identical column definitions to current application table
    LIKE application INCLUDING ALL
) PARTITION BY RANGE (created_at);
```

> **Note:** All constraints, indexes, and foreign keys must be recreated on the
> partitioned table and its child partitions. Verify with
> `\d application` before migrating.

### Step 3 — Create year partitions

Follow the `activity_logs` yearly pattern. Create one partition per active year
plus a default partition:

```sql
CREATE TABLE application_2025 PARTITION OF application_partitioned
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');

CREATE TABLE application_2026 PARTITION OF application_partitioned
    FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');

CREATE TABLE application_2027 PARTITION OF application_partitioned
    FOR VALUES FROM ('2027-01-01') TO ('2028-01-01');

CREATE TABLE application_2028 PARTITION OF application_partitioned
    FOR VALUES FROM ('2028-01-01') TO ('2029-01-01');

CREATE TABLE application_default PARTITION OF application_partitioned DEFAULT;
```

### Step 4 — Migrate data

```sql
INSERT INTO application_partitioned SELECT * FROM application;
```

### Step 5 — Swap tables atomically

```sql
BEGIN;
ALTER TABLE application RENAME TO application_old;
ALTER TABLE application_partitioned RENAME TO application;
COMMIT;
```

### Step 6 — Verify and clean up

- [ ] Run `EXPLAIN SELECT * FROM application WHERE created_at >= '2026-01-01';`
  and confirm partition pruning (`Seq Scan on application_2026`)
- [ ] Run application smoke tests (full allocation flow)
- [ ] Drop `application_old` after a one-week stabilisation window

### Step 7 — Add the migration file

Create a Flyway migration following the naming convention:

```
backend/src/main/resources/db/migration/
  V{YYYY_MM_DD_HHMM}__partition_application_table.sql
```

This migration must be idempotent: wrap DDL in `DO $$ BEGIN … EXCEPTION WHEN … END $$`
blocks where appropriate.

### Step 8 — Annual partition maintenance

Add a new partition at the start of each calendar year. Track this as a
recurring operational task. Consider `pg_partman` to automate partition
creation if the operational burden becomes significant.

---

## Monitoring Query & Grafana Panel

A stat panel has been added to `tools/grafana/dashboards/backend-dashboard.json`
titled **"Application Table Row Count"**. It uses a PostgreSQL datasource with
the query:

```sql
SELECT COUNT(*) AS count FROM application;
```

The panel displays the current row count and uses the following thresholds:

| Range | Color |
|-------|-------|
| 0 – 75,000 | Green |
| 75,000 – 100,000 | Yellow (approaching threshold) |
| > 100,000 | Red (partitioning overdue) |

---

## Trigger Criteria

Initiate the implementation plan when **any** of the following conditions are
met:

1. `SELECT COUNT(*) FROM application;` exceeds **100,000 rows**, OR
2. Query planner shows sequential scans on `application` affecting response
   times > 500 ms for date-filtered queries, OR
3. The Grafana "Application Table Row Count" panel turns **red** for two
   consecutive weeks.

---

## Consequences

- **Positive:** Partitioning plan is fully documented; implementation can begin
  immediately when the threshold is reached.
- **Positive:** Grafana monitoring provides continuous visibility — no manual
  checks needed.
- **Positive:** Follows the established `activity_logs` pattern, minimising
  implementation risk.
- **Negative:** `application` table remains unpartitioned until the threshold;
  query performance will degrade gradually as the table grows past ~50,000 rows.
- **Neutral:** No code changes required by this ADR.
