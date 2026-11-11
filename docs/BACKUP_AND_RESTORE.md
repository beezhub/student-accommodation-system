# Backup and restore runbook

Covers the two stores that hold state you cannot rebuild: the PostgreSQL database and the MinIO
bucket. It also records what the system does about document retention, and why the bucket has no
lifecycle rule expiring documents.

GH-235. **PROD on AWS** (#335, #336) keeps its state in RDS and S3 rather than in containers, and is
covered first, in the PROD section. Sections 1 to 6 after it describe a compose deployment with
PostgreSQL and MinIO in containers. NONPROD keeps its PostgreSQL in a container, so section 2 applies
to it, but its documents are in S3 (#382), so section 3 does not.

---

## PROD (AWS Cape Town)

### What protects what

| Store | Backup | Kept | Who runs it |
| --- | --- | --- | --- |
| RDS `sas-prod` | Automated backups with point-in-time recovery to any second | 7 days | AWS |
| RDS, outside RDS | Daily `pg_dump -Fc` to `s3://beezhub-sas-prod-backups/postgres/` at 01:30 UTC (`sas-backup-db.timer`, `tools/prod/backup-db.sh`) | 35 days | The PROD server |
| S3 `beezhub-sas-prod-documents` | Versioning: a deleted or overwritten object keeps its previous version | 35 days | AWS |

The daily dump exists because RDS's own backups live with the instance: deleting the instance, or
losing the account, can take them too. After each upload it reports the CloudWatch metric
`SAS/Backups BackupSucceeded`; the alarm `sas-prod-db-backup-missing` emails `sas-prod-alarms` when
none arrives for two days. Its log is `journalctl -u sas-backup-db` on the server.

Redis holds only the token blacklist and rate-limit counters and is not backed up (section 1).

### Recovery times, from the drills on 2026-11-03 (#339)

| Restore | Measured | Notes |
| --- | --- | --- |
| A deleted document, from its previous version | 6 s | From detecting the deletion to the identical file back in place |
| The daily dump into an empty PostgreSQL 16, then Flyway `validate` | 34 s | 200 KB dump, 43 tables, 51 migrations validated; grows with the data |
| RDS to a point in time, as a new instance | 16 min | Restore started to the copy answering queries; RDS creating the instance is nearly all of it. Switching PROD over (SSM and a redeploy) adds about 2 min |

### Restore the database to a point in time

RDS always restores into a **new** instance; `sas-prod` is not touched.

```bash
aws rds restore-db-instance-to-point-in-time --region af-south-1 \
  --source-db-instance-identifier sas-prod --target-db-instance-identifier sas-prod-restore \
  --restore-time <UTC, e.g. 2026-11-03T14:46:25Z> \
  --db-instance-class db.t4g.micro --db-subnet-group-name sas-prod \
  --vpc-security-group-ids <sas-prod-db security group> --availability-zone af-south-1a \
  --db-parameter-group-name sas-prod-postgres16 --no-publicly-accessible --no-multi-az
```

The restored instance keeps the master password it had at that time, which is the one in the
RDS-managed secret unless it has been rotated since.

To switch PROD over: point `/sas-prod/DB_URL` in SSM at the new endpoint and redeploy
(`docs/PROD_RUNBOOK.md`, steps 3 and 4). Then either keep the new instance and import it into
Terraform, or rename the instances so the restored one is `sas-prod`. Check what a rollback of the
schema means first: an older schema with a newer image will not start (`docs/PROD_RUNBOOK.md`,
"Rolling back").

### Restore from a daily dump

For when RDS's own backups are gone. On the PROD server (`tools/prod/ssmrun.sh`), into a database
that is empty:

```bash
aws s3 cp s3://beezhub-sas-prod-backups/postgres/<file>.dump /var/tmp/restore.dump
pg_restore "host=<endpoint> dbname=studentaccommodation sslmode=require" --no-owner --no-privileges \
  /var/tmp/restore.dump
```

The drill restored into a throwaway `postgres:16.2-alpine` container on the server, so the data
never left AWS, and ran `flyway/flyway:11.7.2 validate` against `backend/src/main/resources/db/migration`
with `-placeholders.demoSeed=false`.

### Restore a deleted or overwritten document

```bash
aws s3api list-object-versions --bucket beezhub-sas-prod-documents --prefix <key>
aws s3api copy-object --bucket beezhub-sas-prod-documents --key <key> \
  --copy-source "beezhub-sas-prod-documents/<key>?versionId=<previous version id>"
```

Copying the previous version over the current one keeps the history; nothing is deleted.

### Database and documents together

The rule in section 5 holds on PROD too: a database restored to an earlier time has no rows for
documents uploaded after that time, and `OrphanFileCleanupJob` (daily, 03:00 UTC) deletes stored files
without a row once they are older than the grace period. Those files are still recoverable as old
versions for 35 days, but restore the database first and check the documents it references exist in
the bucket before the next cleanup run.

### Not covered

- A stolen or deleted AWS account takes RDS, its snapshots and both buckets with it. Copying the
  daily dumps to a second account or provider is the remaining gap.
- Self-service account deletion (`AuthService.deleteAccount`) removes a student's rows but leaves
  their stored files, which `OrphanFileCleanupJob` then removes as orphans. Until then, and for 35
  days afterwards as old versions, they are recoverable. Seen on PROD's first day, when a test
  student was deleted.

---

## NONPROD and local (compose)

Everything below assumes the compose deployment described in the repository root:

```
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

Adapt the container names if you run these services another way. The credentials these commands
need are the ones in your `.env`; `.env.example` documents every variable.

> Rotating a credential used below: for PROD see `docs/PROD_RUNBOOK.md`, "Rotating the other PROD
> secrets" and "The database password". For NONPROD and local, change the value in `.env` and
> redeploy.

---

## 1. What has to be backed up

| Store | Holds | Rebuildable? |
| --- | --- | --- |
| PostgreSQL (`postgres_data`) | every application, allocation, user, audit row | No |
| MinIO (`minio_data`) | student document objects, building images | No |
| Redis (`redis_data`) | token blacklist, rate-limit counters | Yes — losing it un-blacklists logged-out tokens until they expire, and resets rate limits |
| Postgres certificate (`postgres_tls`) | NONPROD's self-signed TLS certificate | Yes: `db-tls-cert` makes a new one on the next start, and `sslmode=require` does not check it |
| Grafana (`grafana_data`) | dashboards | Yes — provisioned from `tools/grafana/` |
| Prometheus | metrics history | Yes, and not worth backing up |
| Loki (`loki_data`) | two weeks of container and Caddy logs | Yes, and not worth backing up |

So: **the database and the bucket**. Redis, Prometheus and Grafana are all reconstructible, and a
restore that omits them is a complete restore.

The two that matter must be backed up **together and close in time**. A `document` row points at an
object key; a database restored ahead of the bucket has rows referencing objects that do not exist
yet, and a bucket restored ahead of the database has objects the orphan cleanup job will delete on
its next run (03:00 UTC by default). Section 5 covers the ordering that avoids both.

---

## 2. PostgreSQL

### 2.1 Nightly logical backup (the baseline)

`pg_dump` in custom format, which restores selectively and compresses:

```bash
docker exec student-accommodation-local-db \
  pg_dump -U "$USER_DB" -d studentaccommodationlocaldb -Fc \
  > "sas-db-$(date -u +%Y%m%dT%H%M%SZ).dump"
```

Verify it is not a truncated file before you trust it — a dump that failed half way still exits
with a file on disk:

```bash
pg_restore --list sas-db-*.dump | tail -5
```

Keep these off the machine running the database. A backup on the same volume survives exactly the
failures that were never going to lose you anything.

### 2.2 Point-in-time recovery (when a nightly dump is not enough)

A logical dump loses everything since it was taken. If losing a day of applications is unacceptable,
add WAL archiving, which lets you roll forward to any moment:

```yaml
# docker-compose.prod.yml, under services.db
    command:
      - postgres
      - -c
      - archive_mode=on
      - -c
      - archive_command=test ! -f /wal/%f && cp %p /wal/%f
      - -c
      - wal_level=replica
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - /srv/backup/wal:/wal
```

On NONPROD, `docker-compose.deploy.yml` already sets `command` to require TLS (#380). `command` is
replaced rather than merged, so copy its `-c` lines into yours or TLS turns off.

Then take a base backup periodically:

```bash
docker exec student-accommodation-local-db \
  pg_basebackup -U "$USER_DB" -D - -Ft -Xf -z > "sas-base-$(date -u +%Y%m%d).tar.gz"
```

Recovery is the base backup plus the WAL segments after it, with a `recovery_target_time` in
`postgresql.auto.conf`. This is more machinery than a dump and it is the only way to lose less than
a day; decide which of those you need before setting it up, not during an incident.

### 2.3 Volume snapshot

If the host gives you block-level snapshots, snapshotting `postgres_data` is the cheapest full
backup available. One condition: a snapshot taken while PostgreSQL is running is a *crash-consistent*
image, and recovers the way an unclean shutdown recovers. That is supported and it works, but it is
not a substitute for a dump you have actually test-restored — see 2.1.

### 2.4 Restore

```bash
# Stop the application first, or Flyway and the running app will race the restore.
docker compose stop student-accommodation-back-end

docker exec -i student-accommodation-local-db \
  pg_restore -U "$USER_DB" -d studentaccommodationlocaldb --clean --if-exists \
  < sas-db-20260906T030000Z.dump

docker compose start student-accommodation-back-end
```

On startup Flyway validates the restored schema against the migrations
(`validate-on-migrate: true`) and Hibernate validates the entities against it
(`hbm2ddl.auto: validate`). A dump from an older schema version will be migrated forward
automatically; a dump from a *newer* one will fail startup, which is the correct outcome — restore
onto a matching or older build.

---

## 3. MinIO

### 3.1 Backup

`mc mirror` copies the bucket to another location. Set an alias once:

```bash
mc alias set sas http://localhost:9000 "$S3_ACCESS_KEY" "$S3_SECRET_KEY"
mc mirror --overwrite sas/student-accommodation-bucket /srv/backup/minio/
```

For a real deployment, mirror to a second MinIO or an S3 bucket in another failure domain rather
than to a directory on the same host. Do **not** add `--remove` to the nightly job: it makes the
mirror follow deletions, so a bad retention run or an accidental bulk delete propagates to the
backup before anyone notices.

Snapshotting the `minio_data` volume works too and is faster, but it is opaque — you cannot pull one
student's document out of it without restoring the whole thing.

### 3.2 Restore

```bash
mc mirror --overwrite /srv/backup/minio/ sas/student-accommodation-bucket
```

Restoring a single document, given the `document_path` from the database:

```bash
mc cp "/srv/backup/minio/STUDENT/42/ID_DOCUMENT/passport.pdf" \
      "sas/student-accommodation-bucket/STUDENT/42/ID_DOCUMENT/passport.pdf"
```

### 3.3 Object versioning

Versioning is off. Turning it on gives you an undo for a bad delete without going to backups:

```bash
mc version enable sas/student-accommodation-bucket
```

It is worth doing before the first non-dry-run of the retention job (section 4). Note the cost: with
versioning on, deletes leave delete markers and the bucket grows until noncurrent versions are
expired, so pair it with a rule that expires them:

```bash
mc ilm rule add --noncurrent-expire-days 30 sas/student-accommodation-bucket
```

---

## 4. Document retention

### 4.1 The rule

**A student's document pack is deleted twelve months after their last application closes and
their last tenancy ends.**

The clock is the student's history, not the upload date. A pack is reused across applications to
different companies, so a student with anything still running anywhere keeps everything. Two things
count as still running:

- an **application** in `PENDING`, `UNDER_REVIEW`, `CHANGES_REQUESTED`, `APPROVED`,
  `AWAITING_ALLOCATION` or `ALLOCATED`;
- an **allocation** in `OFFERED`, `ACCEPTED` or `ACTIVE` — an offer out, an offer accepted, or a
  tenancy running.

`CHECKED_IN` is deliberately not in the first list. It is terminal in the application state
machine: nothing moves an application out of it, not even the student moving out. Keying retention
on it would exempt every student who ever took up a room, which is most of the ones the rule is
about. Residency is tracked by the allocation instead, which becomes `COMPLETED` or `TERMINATED`
when they leave — and that is when the twelve months start for a former resident.

A student who uploaded documents and never applied at all has no clock to read, so their upload
date is used instead. That is the only case where the upload date decides anything.

This is a default, not legal advice. It is set by `DOCUMENT_RETENTION_MONTHS` and is the number to
change if your jurisdiction or your own privacy notice says otherwise.

### 4.2 Why there is no bucket lifecycle rule for documents

The issue asked for a MinIO lifecycle policy. A lifecycle rule expires objects by **age, prefix or
tag** — all properties of the object itself. "Twelve months after the application closes" is a
property of the database, and the object may still be supporting a live application at another
company.

Any age-based rule over the document prefixes therefore deletes evidence that students are still
relying on. So the document prefixes carry no expiry rule, and the decision is made in
`DocumentRetentionJob`, where the applications are.

The one lifecycle rule that *is* safe here cleans up failed uploads, which are pure waste and
reference nothing:

```bash
mc ilm rule add --expire-delete-marker --noncurrent-expire-days 30 \
   sas/student-accommodation-bucket
```

### 4.3 Turning the job on

The job ships **reporting-only**. Deletion is irreversible, and the first run on an existing
deployment acts on the entire backlog at once. So:

1. Leave `DOCUMENT_RETENTION_DRY_RUN=true` and let a nightly run happen. It logs one
   `action=wouldDelete documentId=… studentId=…` line per candidate and a final
   `action=dryRun wouldDelete=N`.
2. Read `N`. If it is larger than you expect, the retention period or the application data is not
   what you assumed — find out which before going further.
3. Take a database dump and a bucket mirror (sections 2.1 and 3.1). Consider enabling versioning
   (3.3).
4. Set `DOCUMENT_RETENTION_DRY_RUN=false` and restart the backend.

The job then runs nightly at `DOCUMENT_RETENTION_CRON` (03:30 UTC by default, half an hour after the
orphan sweep). It deletes the `document` row and publishes the same after-commit event every other
deletion path uses, so the object goes only once the row deletion has committed. Anything the event
misses is an orphan, which the 03:00 UTC sweep collects the next night.

Watch `sas_jobs_documentRetention_processed_total` and `sas_jobs_documentRetention_failures_total`
in Prometheus. A sudden jump in the first is worth looking at before the next night's run.

---

## 5. Restoring both stores together

Order matters, because the orphan cleanup job deletes objects with no `document` row.

1. **Stop the backend.** `docker compose stop student-accommodation-back-end`. This stops both
   scheduled jobs, so neither can act on a half-restored state.
2. **Restore MinIO first** (3.2). Extra objects are harmless while the app is down.
3. **Restore PostgreSQL** (2.4).
4. **Start the backend.** Flyway migrates and Hibernate validates.
5. **Check before the next 03:00 UTC.** Any object whose row did not come back is now an orphan and
   will be deleted tonight. If the two backups were not taken close together, set
   `STORAGE_CLEANUP_CRON` to something far off until you have reconciled them.

Sanity check after a restore, run against the restored database:

```sql
SELECT count(*) FROM document;
SELECT count(*) FROM application WHERE status NOT IN ('REJECTED','EXPIRED','CANCELLED','CHECKED_IN');
```

Compare with `mc ls --recursive sas/student-accommodation-bucket | wc -l`. The object count should
be at least the document count; it is normally higher, because building images share the bucket
under `buildings/`.

---

## 6. Test the restore

A backup nobody has restored is a hypothesis. Once a quarter, restore the most recent dump and
mirror into a scratch environment, start the backend against them, and check that it comes up, that
a student's documents download, and that the counts in section 5 agree. Write down how long it
took — during an incident that number is the one people ask for.
