# PROD runbook

How PROD is deployed and operated by hand, as done for the first deploy (#336). The environment
itself is `infra/terraform/prod` (#335) and its design is `docs/PROD_ENVIRONMENT_PLAN.md`. #338
automates this procedure; until then, this is how PROD changes.

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

PROD runs exactly what NONPROD has already run. Every merge to `develop` builds both images on
GitHub's ARM runners and deploys them to NONPROD (`deploy.yml`, #337); the run summary lists the
two digests.

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

   It prints key names only and ends with `compose config: OK with the four PROD files`.

4. **Pull and start:** `tools/prod/ssmrun.sh tools/prod/start.sh`. It waits for readiness and ends
   with the running digests, the error count and memory.

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
command line (see Grafana token below). Then redeploy the current release (steps 3 and 4).

The database login is not in SSM: `write-env.sh` reads it from the RDS-managed secret every time, so
it always matches what RDS holds.

## First deploy only

Done on 2026-11-03; kept here to rebuild PROD from nothing.

1. `terraform apply` in `infra/terraform/bootstrap`, then `infra/terraform/prod` (its README).
2. The A record for the hostname, pointing at the Elastic IP. **Check the digits:** the first
   attempt pointed at `5.240.189.31`, someone else's server.
3. The `/sas-prod/` settings: generated secrets (`JWT_SECRET`, `REDIS_PASSWORD`,
   `PROMETHEUS_SCRAPE_PASSWORD`, `GRAFANA_ADMIN_PASSWORD`, `ADMIN_BOOTSTRAP_PASSWORD`), the
   Grafana Cloud values, and the plain settings. The mail parameters come from Terraform.
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

## Checks after any deploy

- `start.sh` reports readiness and `errors since start: 0`.
- Register a student with an address you can read: the verification email arrives from
  `no-reply@beezhub.co.za` and the backend logs `action=verificationEmailSent`.
- An upload and a download of a document work (S3 through the instance role).
- `docker logs student-accommodation-back-end` shows `action=bucketCreationSkipped` at startup.

## Known gaps

- #338: this procedure is manual.
- #339: no tested restore yet; RDS point-in-time recovery exists but has not been drilled.
- #340: Grafana Cloud dashboards and alerts are not built yet.
- #344: DNS is still at the SADomains email hosting package.
