# PROD runbook

How PROD is deployed and operated. The environment itself is `infra/terraform/prod` (#335) and its
design is `docs/PROD_ENVIRONMENT_PLAN.md`. Releases go out through the Deploy PROD workflow (#338),
which runs the same scripts the first deploy ran by hand (#336).

| What | Where |
|---|---|
| Site | `https://accommodation.beezhub.co.za` (A record at SADomains until #344) |
| Server | EC2 `i-0275831c4e2972f2a`, Elastic IP `15.240.189.31`, `af-south-1` |
| Database | RDS `sas-prod`, PostgreSQL 16, private; password managed by RDS in Secrets Manager |
| Documents | S3 `beezhub-sas-prod-documents`, through the instance role |
| Images | ECR `student-accommodation-{backend,frontend}`, pulled by digest |
| Settings | SSM Parameter Store under `/sas-prod/`; the server's `/opt/sas/.env` is generated from them |
| Email | SES API in `af-south-1` (`MAIL_TRANSPORT=ses-api`, #360), from `no-reply@beezhub.co.za` |
| Logs and metrics | Grafana Cloud stack `friendlysamosa508` (EU), through Alloy |
| Alarms | Grafana Cloud rules (`infra/terraform/prod-grafana`) for memory, swap, disk, backend up and 5xx rate; CloudWatch through SNS topic `sas-prod-alarms` for the server checks, CPU credits, RDS and the backup; the `sas-prod-data-out` AWS budget. What each one means: **When an alert fires** |
| Backups | Daily dump plus RDS point-in-time recovery; drills and recovery times in `docs/BACKUP_AND_RESTORE.md` |

## Access

There is no SSH; port 22 is closed. Everything on the server goes through SSM:

- **One-off commands:** `tools/prod/ssmrun.sh <script>` runs a local bash script on the server as
  root and prints its output.
- **A shell:** `aws ssm start-session --region af-south-1 --target i-0275831c4e2972f2a` (needs the
  Session Manager plugin for the AWS CLI).

Credentials on the operator's machine come from `aws login`.

## Deploy a release

PROD runs exactly what NONPROD has already run. Every merge to `main` builds both images on
GitHub's ARM runners and deploys them to NONPROD (`deploy.yml`, #337); the run summary lists the
two digests and the run id to promote.

### With the workflow (the normal way)

1. Merge `develop` into `main` through a pull request. NONPROD deploys it; check it there.
2. Copy the run id from the green **Deploy NONPROD** run's summary ("to promote: ...").
3. **Actions > Deploy PROD > Run workflow**, on `main`, with that run id as `nonprod_run`.

The workflow (`deploy-prod.yml`, #338) checks the run is a successful NONPROD deploy of `main`,
reads its commit and digests from the run's `nonprod-release` artifact, and does steps 2 to 5 below
through SSM. Its summary shows the Flyway version before and after, which decides how to roll back.
Starting the workflow is the release gate: anyone who can run it can deploy.

To roll back, run it again with the previous release's NONPROD run id, if the rules under
**Rolling back** allow it. Artifacts are kept 90 days.

### By hand (when GitHub Actions is unavailable)

1. **Pick the release.** From a green NONPROD deploy run, note the commit and the two image digests.
2. **Upload that commit's tree** (compose files, `tools/`):

   ```bash
   sha=$(git rev-parse --short <commit>)
   git archive --format=tar <commit> | aws s3 cp - "s3://beezhub-sas-prod-backups/deploy/$sha.tar"
   ```

3. **Write the tree and `.env` on the server:**

   ```bash
   VARS="SHA=$sha BACKEND_IMAGE=<registry>/student-accommodation-backend@sha256:<digest> FRONTEND_IMAGE=<registry>/student-accommodation-frontend@sha256:<digest>" \
     tools/prod/ssmrun.sh tools/prod/write-env.sh
   ```

   It prints key names only and ends with `compose config: OK with the four PROD files`. If
   Parameter Store lacks a key the release's compose files need, it stops before changing anything
   and names the key.

4. **Pull and start:** `tools/prod/ssmrun.sh tools/prod/start.sh`. It prints `schema_before=`,
   pulls (three tries, and stops before `up` if all fail), restarts any container whose mounted
   config file changed (#411), waits for readiness, prints `schema_after=`, and ends with the running
   digests, the error count and memory.

5. **Check from outside:** `curl -sI https://accommodation.beezhub.co.za/` returns 200, and the
   `Content-Security-Policy` header matches the release.

`up -d` replaces containers in place, so expect a short outage (#246). There is no automatic
rollback.

### Rolling back

Redeploying the previous commit and digests is safe **only if the release applied no Flyway
migration**: `hbm2ddl.auto` is `validate` and there are no down-migrations, so an older image against
a newer schema will not start. If a migration ran, restore RDS to a point in time before the deploy
together with the previous images. The steps and the measured times are in `docs/BACKUP_AND_RESTORE.md`.

## Restarting

**Actions > Restart PROD > Run workflow**, on `main`, and pick what to restart: `backend`,
`frontend`, `redis`, `alloy` or `all`. The workflow (`restart-prod.yml`) runs `tools/prod/restart.sh`
through SSM, waits for the backend's readiness, then checks the site from outside. By hand:
`VARS='TARGET=backend' tools/prod/ssmrun.sh tools/prod/restart.sh`.

A restart keeps the images and `.env` the last deploy left, so it deploys nothing and needs no
rollback. It also means a changed setting or secret is **not** picked up: redeploy for that. Expect a
short outage while the backend starts. The database is RDS and is not restarted.

## Changing a setting

Settings live in SSM, not in the server's `.env`, which is regenerated on every deploy:

```bash
MSYS_NO_PATHCONV=1 aws ssm put-parameter --region af-south-1 --name /sas-prod/<NAME> \
  --type String --value '<value>' --overwrite
```

Use `--type SecureString` for anything secret, and enter secrets through a prompt rather than on the
command line (see Grafana token below). Then redeploy the current release: run Deploy PROD with the same NONPROD run id (or steps 3 and 4
by hand).

The database login is not in SSM: `write-env.sh` reads it from the RDS-managed secret on every
deploy. Only on a deploy, though, which is what the next section is about.

## The database password

RDS keeps the master password in Secrets Manager and rotates it **once a year**
(`aws_secretsmanager_secret_rotation.db_master` in `infra/terraform/prod/database.tf`). The backend
does not read the secret itself: it runs on the copy `write-env.sh` put in `/opt/sas/.env` at the
last deploy. After a rotation, the connection pool replaces its connections within 10 minutes and
each new one is refused, so **the site goes down about 10 minutes after any rotation until PROD is
redeployed** (#407). While RDS used its default 7-day schedule, that would have happened every week.

The daily backup is not affected: `backup-db.sh` reads the secret on every run.

**When it rotates next:**

```bash
MSYS_NO_PATHCONV=1 aws secretsmanager describe-secret --region af-south-1 \
  --secret-id "$(MSYS_NO_PATHCONV=1 aws rds describe-db-instances --region af-south-1 \
    --db-instance-identifier sas-prod --query 'DBInstances[0].MasterUserSecret.SecretArn' --output text)" \
  --query '{Last:LastRotatedDate, Next:NextRotationDate, Rules:RotationRules}'
```

**Rotate on purpose, before that date,** at a quiet time, so the outage is a minute you chose rather
than ten minutes at night:

1. Have the NONPROD run id of the release PROD runs now (the last green Deploy PROD run's summary).
2. Rotate, with the secret ARN from the command above:

   ```bash
   MSYS_NO_PATHCONV=1 aws secretsmanager rotate-secret --region af-south-1 --secret-id '<SecretArn>'
   ```

   It takes a few seconds. Run the "when it rotates next" command again until `Last` shows the time
   you rotated; a deploy before that would copy the old password.

3. Straight away, run **Deploy PROD** with that same run id. `write-env.sh` picks up the new
   password and `start.sh` restarts the backend with it.
4. Check the site and `start.sh`'s `errors since start: 0`. The next rotation is now a year away.

**If the site is already down** and the backend log shows `password authentication failed for user
"sas_admin"`, a rotation happened: do step 3.

A restored database keeps the password it had at that time (`docs/BACKUP_AND_RESTORE.md`), so after a
restore the same redeploy applies.

## When an alert fires

Alerts reach the alarm inbox from three places. Each entry says what it means and the first thing
to look at. To run a command on the server, put it in a file and send it with
`tools/prod/ssmrun.sh` (see Access), for example:

```bash
echo 'free -m; docker stats --no-stream' > /tmp/look.sh && tools/prod/ssmrun.sh /tmp/look.sh
```

Grafana Cloud repeats an alert every 4 hours until it clears. CloudWatch and the budget email once
when the state changes, and CloudWatch emails again when it recovers.

| Alert | Source | What it means and the first look |
|---|---|---|
| Server memory almost full | Grafana Cloud, critical | Under 10% of memory has been free for 10 minutes; containers are close to being killed. Run `free -m` and `docker stats --no-stream` to see which container is large. `dmesg \| grep -i oom` shows whether something was already killed. |
| Server using swap | Grafana Cloud, warning | Over half of the 2 GB swap is in use, so memory has run out in practice and the site is slow. Same look as above. If the backend (limit 1 GB) is the large one, check its log for a burst of work before restarting it. |
| Server disk almost full | Grafana Cloud, warning | Under 15% of the root disk is free. `df -h /` and `docker system df` show where it went. Old images are the usual cause: `docker image prune -a -f` removes images no container uses, and the running ones stay. |
| Backend not answering | Grafana Cloud, critical | Alloy has not scraped the backend for 3 minutes, **or no metrics reach Grafana Cloud at all**. First check the site from outside (`curl -sI https://accommodation.beezhub.co.za/`). If it answers, the fault is Alloy: `docker logs --tail 50 student-accommodation-alloy`. If it does not, `docker ps` and the backend log, then the last `start.sh` output. |
| Backend 5xx error rate above 1% | Grafana Cloud, critical | More than 1% of real requests failed with a 5xx for 5 minutes. Read the backend errors (see Reading logs). A bad release shows as errors starting at the deploy time; roll back under **Rolling back**. |
| `sas-prod-ec2-system-check` | CloudWatch | AWS hardware or network fault under the server. AWS recovers the instance on its own, keeping its ID, Elastic IP and disk, and it reboots. Wait for the recovery email, then check the site and that `docker ps` shows every container up. |
| `sas-prod-ec2-instance-check` | CloudWatch | The server's own operating system is failing its check (kernel, full disk, memory). Try an SSM session. If it does not connect, reboot from the EC2 console, then check the site and `docker ps`. |
| `sas-prod-ec2-cpu-credits` | CloudWatch | Under 100 CPU credits left. At zero the server is held to 20% CPU and the site just gets slow. `docker stats --no-stream` and `top` show what is using CPU. Switching to unlimited credits stops the slowdown but adds charges, and `infra/terraform/prod/compute.tf` says `standard`, so record any change there. |
| `sas-prod-rds-cpu` | CloudWatch | Database CPU above 80% for 15 minutes. Open the RDS instance's monitoring graphs for the start time, then look for a matching burst of requests or a slow query in the backend log. |
| `sas-prod-rds-free-storage` | CloudWatch | Under 4 GB of database storage free. It grows on its own from 20 GB to a 40 GB ceiling (`max_allocated_storage`); raising the ceiling is a Terraform change. Find what is growing before it gets there. |
| `sas-prod-rds-free-memory` | CloudWatch | Under 100 MB of freeable memory for 15 minutes on a 1 GB class, so the database is swapping and a bigger class is due (#334). Choosing the class is a decision for a person. |
| `sas-prod-db-backup-missing` | CloudWatch | No successful daily dump for 2 days. On the server run `journalctl -u sas-backup-db -n 50` and `systemctl list-timers sas-backup-db.timer`. `systemctl start sas-backup-db.service` runs one now. See `docs/BACKUP_AND_RESTORE.md`. |
| `sas-prod-data-out` | AWS Budgets | Internet data out has passed 80 GB this month (actual) or is forecast to pass 100 GB, the free allowance for the whole account. Cost Explorer, grouped by usage type, shows whether it is the server or S3 document downloads. |

## Reading logs

Logs are in Grafana Cloud **Explore**, on the Loki data source. Alloy gives every line these labels:
`env` (`prod`), `service`, `container`, `level`, `unit` (host units only) and, on web requests,
`status_class` (`logs.alloy` lists them all). The backend's service is `student-accommodation-back-end`.

```
{env="prod", service="student-accommodation-back-end", level="error"}
```

Narrow it by the backend's `action=` names, which are structured metadata, with
`| action="verificationEmailSent"`. Failed web requests at the front door:
`{env="prod", service="caddy", status_class="5xx"}`. Host units are `{env="prod", unit="docker.service"}`
and `unit="caddy.service"`. The backup log is not shipped; read it with `journalctl -u sas-backup-db`.

When Grafana Cloud is the thing that is down, read the container directly:

```bash
echo 'docker logs --since 30m student-accommodation-back-end 2>&1 | grep "\"level\":\"ERROR\"" | tail -50' \
  > /tmp/errors.sh && tools/prod/ssmrun.sh /tmp/errors.sh
```

## Rotating the other PROD secrets

Each secret lives in SSM under `/sas-prod/`. Rotating one is always the same three steps: put the new
value (a SecureString, entered through a prompt as in "First deploy only"), run **Deploy PROD** with
the current release's NONPROD run id, and check the site and `errors since start: 0`. What differs is
what the change touches. The database password is separate (see above, #407).

| Secret | What it affects | Notes |
|---|---|---|
| `JWT_SECRET` | Every signed-in user is logged out at once, and must sign in again. | Generate with `openssl rand -base64 64`. Choose a quiet hour. |
| `REDIS_PASSWORD` | Redis and the backend both read it and are recreated by the deploy. | Brief errors while the two restart. The token blacklist and rate-limit counters are kept in Redis's volume. |
| `PROMETHEUS_SCRAPE_PASSWORD` | The backend checks it and Alloy presents it, so both must change together. | Alloy reads it as a compose secret. It is not confirmed that a deploy recreates Alloy for a changed secret, so after the deploy run `docker restart student-accommodation-alloy` through `ssmrun.sh`, then confirm "Backend not answering" stays quiet for 5 minutes. |
| `GRAFANA_CLOUD_API_KEY` | Alloy only; a bad token stops logs and metrics, and "Backend not answering" fires on no data. | Create the new token in Grafana Cloud with `logs:write` and `metrics:write` only, deploy, confirm data arrives, then revoke the old token. |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | Nothing today: with `MAIL_TRANSPORT=ses-api` the backend sends through the instance role and ignores them (#360). | They are an SES SMTP key that Terraform made (`aws_iam_access_key.smtp` in `infra/terraform/prod/email.tf`). To rotate, run `terraform apply -replace=aws_iam_access_key.smtp` in `infra/terraform/prod`, which rewrites both parameters, then deploy. Whether to delete the key instead is a person's call. |

The instance role needs no rotation. The S3 documents bucket is reached through it, with no access key.

## First deploy only

Done on 2026-11-03; kept here to rebuild PROD from nothing.

1. `terraform apply` in `infra/terraform/bootstrap`, then `infra/terraform/prod` (its README).
2. The A record for the hostname, pointing at the Elastic IP. **Check the digits:** the first
   attempt pointed at `5.240.189.31`, someone else's server.
3. The `/sas-prod/` settings: generated secrets (`JWT_SECRET`, `REDIS_PASSWORD`,
   `PROMETHEUS_SCRAPE_PASSWORD`, `GRAFANA_ADMIN_PASSWORD`, `ADMIN_BOOTSTRAP_PASSWORD`), the
   Grafana Cloud values, and the plain settings, including `EXAMPLE_DATA=false` (#365). The mail
   parameters come from Terraform.
4. The Grafana Cloud token, entered through a prompt so it never reaches the screen or a transcript:

   ```bash
   read -rsp "Paste Grafana token: " GTOKEN && echo && MSYS_NO_PATHCONV=1 aws ssm put-parameter \
     --region af-south-1 --name /sas-prod/GRAFANA_CLOUD_API_KEY --type SecureString --value "$GTOKEN"; unset GTOKEN
   ```

   Its access policy has `logs:write` and `metrics:write` only.
5. A release (above), then `tools/prod/ssmrun.sh tools/prod/install-caddy.sh` for the certificate.
6. The first `PLATFORM_ADMIN`: the backend creates it from `ADMIN_BOOTSTRAP_EMAIL` and
   `ADMIN_BOOTSTRAP_PASSWORD` and forces a password change at first login. After that login, delete
   both parameters and redeploy:

   ```bash
   MSYS_NO_PATHCONV=1 aws ssm delete-parameters --region af-south-1 \
     --names /sas-prod/ADMIN_BOOTSTRAP_EMAIL /sas-prod/ADMIN_BOOTSTRAP_PASSWORD
   ```

## Removing the example data (#365, once)

The baseline seed put two example institutions, a "2026" academic year and the locked demo
administrator on PROD. `V2026_11_04_1000__remove_example_data.sql` removes them when `EXAMPLE_DATA`
is `false`. It runs **once**, so the setting comes first:

1. Before deploying the first release that carries that migration:

   ```bash
   MSYS_NO_PATHCONV=1 aws ssm put-parameter --region af-south-1 --name /sas-prod/EXAMPLE_DATA \
     --type String --value 'false'
   ```

   Without it the deploy stops at `write-env.sh` with "need keys that /sas-prod/ does not have:
   EXAMPLE_DATA", and nothing on the server changes.
2. Deploy. In the backend's startup log, Flyway applies `2026.11.04.1000`. A line with
   `action=exampleDataKept` names a row it left because something real points at it; look at that
   row before removing it by hand.
3. In the platform admin UI, rename tenant 1 ("Default Tenant") to the real first tenant, and add
   the real institutions and academic year.

## The time zone (#461)

The backend runs in South African time (`TZ=Africa/Johannesburg`). Timestamp columns have no zone and
the API sends them with no offset, so the browser reads them as its own local time: a server in UTC
shows every time 2 hours early. The compose default is enough, and no key is required, but a stale
value wins over the default:

```bash
MSYS_NO_PATHCONV=1 aws ssm get-parameter --region af-south-1 --name /sas-prod/APP_TIMEZONE \
  --query Parameter.Value --output text
```

`ParameterNotFound` or `Africa/Johannesburg` is right. If it says `UTC`, overwrite it (see "Changing a
setting"). NONPROD's `/opt/sas/.env` is edited by hand: check it for the same line. After a deploy,
`docker exec student-accommodation-back-end printenv TZ` shows what the backend really got.

**Rows written while the server was on UTC stay as they are.** They now read as South African time, 2
hours earlier than they really happened. Old timestamps look the same as they always did, because the
browser was already reading them that way. Deadlines already stored now pass 2 hours sooner than
before, at the time the UI was showing all along: offers, check-ins, password reset and email
verification links, and account lockouts. Going back to UTC later would do the reverse to every row
written in South African time, including after a rollback to a release from before #461, so set
`APP_TIMEZONE=Africa/Johannesburg` in SSM before rolling back that far.

Before the first release with this change, count the offers and check-ins that would expire at once:

```sql
SELECT count(*) FROM room_allocation
WHERE (status = 'OFFERED' AND offer_expiry_date < (now() AT TIME ZONE 'UTC') + interval '2 hours')
   OR (status = 'ACCEPTED' AND check_in_expiry_date < (now() AT TIME ZONE 'UTC') + interval '2 hours');
```

## Checks after any deploy

- `start.sh` reports readiness and `errors since start: 0`.
- Register a student with an address you can read: the verification email arrives from
  `no-reply@beezhub.co.za` and the backend logs `action=verificationEmailSent`.
- An upload and a download of a document work (S3 through the instance role). Use a file over 1 MB,
  so the upload also proves nginx's limit (#450).
- `docker logs student-accommodation-back-end` shows `action=bucketCreationSkipped` at startup.

## Known gaps

- Restores were drilled on 2026-11-03 (#339); `docs/BACKUP_AND_RESTORE.md` has the steps and times. A copy of the dumps in a second account or provider is still missing.
- #340: Grafana Cloud alerts and dashboards are in `infra/terraform/prod-grafana` (applied by hand: `infra/terraform/README.md`, "Grafana Cloud alerts and dashboards"), and data out is the `sas-prod-data-out` budget in `prod/monitoring.tf`; UptimeRobot is not set up yet, so nothing outside AWS and Grafana Cloud checks the site; add it to **When an alert fires** once #340 sets it up.
- #344: DNS is still at the SADomains email hosting package.
