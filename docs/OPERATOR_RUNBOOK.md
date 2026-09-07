# Operator runbook

Every account, domain, DNS record, credential and GitHub setting the deployment needs from a
person. GH-245, part of the deployment programme in GH-244.

This is the one part of the programme that is not code. Everything a human has to do is collected
here in the order it is needed, so nothing hides inside an implementation issue.

- **Section 0 and steps 1–6** block the first NONPROD deploy.
- **Step 7** is needed before CI (GH-243).
- **Step 8** is needed before PROD (GH-251).

Work top to bottom. Several steps wait on DNS or domain verification, so start section 0 and steps
1–2 early even if you are not deploying yet.

## What you do NOT do

Terraform (GH-247) creates the droplets, the managed databases, the VPC, the firewall and the DNS
records for the hosts. Do not create any of those by hand — a hand-made resource is exactly what
breaks "destroy then apply reproduces it".

---

## 0.1 Decide these values first

Write them down; several steps below need them.

| Value | Used for | Example |
| --- | --- | --- |
| Domain | everything | `example.co.za` |
| NONPROD hostname | Caddy, `APP_BASE_URL` | `nonprod.example.co.za` |
| PROD hostname | same | `app.example.co.za` |
| `APP_UNIVERSITY_NAME` | every email template | `Example University` |
| `APP_SUPPORT_EMAIL` | every email template, reply-to | `accommodation@example.co.za` |
| First platform admin email | `ADMIN_BOOTSTRAP_EMAIL`, the first login | your address |

`ALLOWED_ORIGINS`, `APP_BASE_URL`, `STORAGE_PUBLIC_ORIGIN` and `GRAFANA_ROOT_URL` are all derived
from the hostname, so deciding the hostname decides them. `APP_SECURITY_TRUSTED_PROXIES` is decided
by the Terraform module (GH-247), not here.

## 0.2 Generate the application secrets

Separate from the provider credentials in steps 2–5: these are the application's own secrets, one
distinct set per environment, and nothing else in the programme generates them for you. The backend
refuses to start without `JWT_SECRET`, `PASSWORD_DB`, `USER_DB`, `MINIO_ACCESS_KEY`,
`MINIO_SECRET_KEY` and `ALLOWED_ORIGINS`; `docker-compose.prod.yml` additionally fail-fasts on
`REDIS_PASSWORD`, `GRAFANA_ADMIN_PASSWORD` and `APP_SECURITY_TRUSTED_PROXIES`.

```
openssl rand -base64 64   # JWT_SECRET  — must be >= 64 characters, one per environment
openssl rand -base64 32   # PASSWORD_DB
openssl rand -base64 32   # MINIO_SECRET_KEY
openssl rand -base64 32   # REDIS_PASSWORD
openssl rand -base64 32   # GRAFANA_ADMIN_PASSWORD
openssl rand -base64 32   # ADMIN_BOOTSTRAP_PASSWORD — you change it at first login
```

`USER_DB` and `MINIO_ACCESS_KEY` are names rather than secrets, but pick non-default ones. Never
reuse a NONPROD value in PROD: NONPROD is destroyed and recreated, and its secrets pass through more
hands.

`.env.example` documents every variable the application reads, including the ones not listed here.

> `docker-compose.prod.yml` and `CLAUDE.md` both point at a `CREDENTIAL_ROTATION.md` that is not in
> the repository. Until it exists there is no written rotation procedure; treat every value above as
> rotatable only by redeploying with a new one.

---

## 1. Domain

Register the domain with any registrar (~$12/year). You will point its nameservers at DigitalOcean
in step 2, so the registrar's own DNS is not used.

## 2. DigitalOcean account and DNS

1. Create a DigitalOcean account. Enable two-factor authentication — this account holds production.
2. **API → Tokens → Generate New Token**, read *and* write, no expiry. This is Terraform's
   credential. Store it in a password manager; you will paste it once into `terraform.tfvars`
   (which is gitignored).
3. **Networking → Domains → Add Domain**: add the domain from step 1. Do not add any records yet.
4. At your registrar, set the domain's nameservers to `ns1.digitalocean.com`,
   `ns2.digitalocean.com`, `ns3.digitalocean.com`. Propagation takes up to a day; `dig NS <domain>`
   shows when it is done.

Terraform will create the `nonprod` and `app` A records. Mail records are added in step 4.

## 3. Cloudflare R2 (backup destination)

1. Create a Cloudflare account. Under **R2**, create a bucket named
   `student-accommodation-backups`. Location: automatic or EU.
2. **R2 → Manage R2 API Tokens → Create API Token**, permission *Object Read & Write*, scoped to
   that bucket. Copy the **Access Key ID**, **Secret Access Key** and the **S3 endpoint URL** (of
   the form `https://<account-id>.r2.cloudflarestorage.com`). Password manager. These become
   `BACKUP_S3_*` values in GH-248.

## 4. SMTP provider (Brevo) — **blocks PROD entirely**

Without this the backend refuses to start (`docker-compose.prod.yml` fail-fasts on `MAIL_HOST`) and
no student can register: MailHog is scaled to zero outside development, and email verification gates
application submission.

1. Sign up at Brevo, free tier (300 emails/day).
2. **Senders & IP → Domains → Add a domain**: enter your domain. Brevo shows you a set of DNS
   records to add — typically a DKIM record, a Brevo verification record, and an SPF `include`.
   **Add each of them, exactly as shown, in DigitalOcean DNS** (Networking → Domains → your domain
   → Create new record). Use the type and name Brevo gives; if an SPF record already exists, merge
   the `include:` into it rather than adding a second SPF record, which invalidates both.
3. Back in Brevo, click **Authenticate** and wait for all records to show green. If they do not
   within an hour, the record name or type is wrong.
4. **SMTP & API → SMTP**: note the server (`smtp-relay.brevo.com`), port (587), your login, and
   generate an SMTP key. These are `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`.
5. **Test it before moving on:** send one email from Brevo's interface to a Gmail address and
   confirm it arrives in the inbox, not spam. Then open the message, "Show original", and confirm
   `SPF: PASS` and `DKIM: PASS`.

## 5. Free monitoring accounts

Sign up only; configuration happens in GH-248 and GH-249.

- **Healthchecks.io** — create one check named `nightly-backup-prod`, schedule daily, grace period
  2 hours. Copy its ping URL.
- **UptimeRobot** (or Better Stack) — nothing to create yet; the PROD monitor is added once PROD
  exists.

## 6. Deploy SSH keys

One key pair per environment, generated on your machine, never reused:

```
ssh-keygen -t ed25519 -f deploy_nonprod -N "" -C deploy@nonprod
ssh-keygen -t ed25519 -f deploy_prod    -N "" -C deploy@prod
```

The **public** halves (`.pub`) go into Terraform's variables for cloud-init (GH-247). The
**private** halves go to GitHub in step 7 and nowhere else. Delete the private files from your
machine after step 7.

## 7. GitHub — before CI is wired (GH-243)

**Environments.** Settings → Environments → New environment, twice: `nonprod` and `production`.

- On `production`: enable **Required reviewers** and add yourself. This is the manual approval gate
  — a deploy to PROD stops here until someone clicks Approve.

**Per environment** (Settings → Environments → the environment):

| Kind | Name | Value |
| --- | --- | --- |
| Secret | `SSH_PRIVATE_KEY` | contents of `deploy_nonprod` / `deploy_prod` from step 6 |
| Variable | `DEPLOY_HOST` | the hostname from section 0.1 |
| Variable | `DEPLOY_USER` | `deploy` |
| Variable | `SSH_KNOWN_HOSTS` | *after* the host exists: run `ssh-keyscan -t ed25519 <hostname>` and paste the output. Re-do this every time NONPROD is recreated, because its host key changes |

**Repository variable** (Settings → Secrets and variables → Actions → Variables):
`NONPROD_ACTIVE` = `true` while NONPROD exists, `false` after you destroy it. CI skips the NONPROD
deploy when false.

## 8. GitHub — branch protection (before PROD, GH-251)

Settings → Branches → Add branch ruleset (or classic rule), once for `develop` and once for `main`:

- Require a pull request before merging.
- Require status checks to pass: `Backend Build & Test` and `Frontend Lint & Test` — these are the
  two job names in `.github/workflows/ci.yml`, and the names must match exactly or the rule waits
  forever on a check that never reports.
- On `main` only: restrict who can push, to nobody but the merge button.

`CLAUDE.md` already says never to commit directly to these branches; this is what enforces it.

## 9. Where the values go

Until GH-250 lands, keep everything above in a password manager. Once SOPS is set up, they move into
the encrypted `secrets.<env>.yaml` files in the repository and the password manager copy becomes the
recovery copy.

---

## Done when

- [ ] `dig NS <domain>` returns DigitalOcean nameservers
- [ ] Brevo shows the domain authenticated, and a test email arrived in a Gmail inbox with SPF and
      DKIM passing
- [ ] the R2 bucket exists and its token is stored
- [ ] both GitHub environments exist, `production` has a required reviewer, and each holds its
      `SSH_PRIVATE_KEY`, `DEPLOY_HOST` and `DEPLOY_USER`
- [ ] the DigitalOcean API token, R2 token, Brevo SMTP key and Healthchecks ping URL are in the
      password manager
- [ ] a full set of application secrets (section 0.2) exists per environment, NONPROD and PROD
      sharing none of them

## Related

- [Backup and restore runbook](BACKUP_AND_RESTORE.md) — what GH-248 automates offsite.
- `.env.example` — every environment variable the application reads, with its default.
- `docker-compose.prod.yml` — the overlay whose fail-fast variables section 0.2 satisfies.
