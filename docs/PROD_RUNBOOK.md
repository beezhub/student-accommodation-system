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
| Settings | SSM Parameter Store under `/sas-prod/` (24 parameters); the server's `/opt/sas/.env` is generated from them |
| Email | SES API in `af-south-1` (`MAIL_TRANSPORT=ses-api`, #360), from `no-reply@beezhub.co.za` |
| Logs and metrics | Grafana Cloud stack `friendlysamosa508` (EU), through Alloy |
| Alarms | CloudWatch, emailed through SNS topic `sas-prod-alarms` |

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
(#339) together with the previous images.

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

## Checks after any deploy

- `start.sh` reports readiness and `errors since start: 0`.
- Register a student with an address you can read: the verification email arrives from
  `no-reply@beezhub.co.za` and the backend logs `action=verificationEmailSent`.
- An upload and a download of a document work (S3 through the instance role). Use a file over 1 MB,
  so the upload also proves nginx's limit (#450).
- `docker logs student-accommodation-back-end` shows `action=bucketCreationSkipped` at startup.

## Known gaps

- #339: no tested restore yet; RDS point-in-time recovery exists but has not been drilled.
- #340: Grafana Cloud alerts and dashboards are in `infra/terraform/prod-grafana` (applied by hand: `infra/terraform/README.md`, "Grafana Cloud alerts and dashboards"), and data out is the `sas-prod-data-out` budget in `prod/monitoring.tf`; UptimeRobot is not set up yet.
- #344: DNS is still at the SADomains email hosting package.
