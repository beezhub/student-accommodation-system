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

**Standing constraint: the domain and its email hosting already exist, at SADomains, and are already
paid for.** Two consequences run through everything below:

- The apex DNS zone is not ours to move or rewrite — it holds working `MX`, `SPF` and `DKIM` records
  for real mailboxes. DigitalOcean is delegated the two host subdomains only (step 2). Any
  instruction elsewhere in the programme to point the domain's nameservers at DigitalOcean is
  superseded by step 2 here.
- The application sends through **Brevo's free tier** (step 4), while every human mailbox stays on
  SADomains. Brevo's DNS records are apex records, so they are added at SADomains too — and the
  existing `SPF` record is edited, never duplicated.

GH-244's cost table lists SMTP at $0.00, and that holds: Brevo free adds nothing to the mailbox
hosting already being paid for. What it adds instead is a **hard 300 messages/day ceiling**, which
becomes the binding constraint on the system. Step 4.5 covers what happens on a day that exceeds it,
and why the SADomains mailbox is kept as the fallback rather than retired.

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
| `APP_CONTACT_PHONE` | every email footer, hidden when blank | the real office number |
| First platform admin email | `ADMIN_BOOTSTRAP_EMAIL`, the first login | your address |

`ALLOWED_ORIGINS`, `APP_BASE_URL`, `STORAGE_PUBLIC_ORIGIN` and `GRAFANA_ROOT_URL` are all derived
from the hostname, so deciding the hostname decides them. `APP_SECURITY_TRUSTED_PROXIES` is decided
by the Terraform module (GH-247), not here.

`APP_SUPPORT_EMAIL` must be a **real mailbox on the existing email hosting**, not a send-only
address. It is the reply-to on every message the application sends, so a student hitting reply on a
rejection or an offer expiry has to reach a person. Create it before the first deploy.

## 0.2 Generate the application secrets

Separate from the provider credentials in steps 2–5: these are the application's own secrets, one
distinct set per environment, and nothing else in the programme generates them for you. The backend
refuses to start without `JWT_SECRET`, `PASSWORD_DB`, `USER_DB`, `S3_ACCESS_KEY`,
`S3_SECRET_KEY` and `ALLOWED_ORIGINS`; `docker-compose.prod.yml` additionally fail-fasts on
`REDIS_PASSWORD`, `GRAFANA_ADMIN_PASSWORD` and `APP_SECURITY_TRUSTED_PROXIES`.

```
openssl rand -base64 64   # JWT_SECRET  — must be >= 64 characters, one per environment
openssl rand -base64 32   # PASSWORD_DB
openssl rand -base64 32   # S3_SECRET_KEY
openssl rand -base64 32   # REDIS_PASSWORD
openssl rand -base64 32   # GRAFANA_ADMIN_PASSWORD
openssl rand -base64 32   # ADMIN_BOOTSTRAP_PASSWORD — you change it at first login
```

`USER_DB` and `S3_ACCESS_KEY` are names rather than secrets, but pick non-default ones. Never
reuse a NONPROD value in PROD: NONPROD is destroyed and recreated, and its secrets pass through more
hands.

`ADMIN_BOOTSTRAP_EMAIL` and `ADMIN_BOOTSTRAP_PASSWORD` can stay set in `.env` after the admin has
signed in: the bootstrap is idempotent and leaves an account alone once its password has been
changed, so a routine redeploy will not reset it. Set `ADMIN_BOOTSTRAP_FORCE=true` only to
deliberately reset a lost platform admin password on the next startup, then clear it again.

`.env.example` documents every variable the application reads, including the ones not listed here.

> `docker-compose.prod.yml` and `CLAUDE.md` both point at a `CREDENTIAL_ROTATION.md` that is not in
> the repository. Until it exists there is no written rotation procedure; treat every value above as
> rotatable only by redeploying with a new one.

---

## 1. Domain

**Already done.** The domain is registered with SADomains, and it carries live email hosting on the
same provider.

That second fact governs everything about DNS below. The apex zone holds working `MX`, `SPF` and
`DKIM` records for real mailboxes. Moving that zone wholesale to DigitalOcean means recreating every
one of those records by hand before the delegation takes effect, and finding out you missed one when
mail stops arriving hours later. So the zone does not move.

## 2. DigitalOcean account, and delegating only the host subdomains

**The apex zone stays at SADomains.** DigitalOcean is given authority over two subdomains and
nothing else, so Terraform can still create and destroy the host records while mail is never at
risk.

1. Create a DigitalOcean account. Enable two-factor authentication — this account holds production.
2. **API → Tokens → Generate New Token**, read *and* write, no expiry. This is Terraform's
   credential. Store it in a password manager; you will paste it once into `terraform.tfvars`
   (which is gitignored).
3. **Networking → Domains → Add Domain**, *twice*, entering the full hostnames as the domains:
   `nonprod.<domain>` and `app.<domain>`. Not the apex. DigitalOcean treats each as its own zone.
   It offers to attach a droplet — decline; Terraform creates the records.
4. In the **SADomains** DNS editor for your domain, add three `NS` records per hostname — six
   records in total:

   | Name | Type | Value |
   | --- | --- | --- |
   | `nonprod` | NS | `ns1.digitalocean.com` |
   | `nonprod` | NS | `ns2.digitalocean.com` |
   | `nonprod` | NS | `ns3.digitalocean.com` |
   | `app` | NS | `ns1.digitalocean.com` |
   | `app` | NS | `ns2.digitalocean.com` |
   | `app` | NS | `ns3.digitalocean.com` |

   Touch nothing else. In particular do not alter the `MX` records or the existing `SPF` record.
5. Confirm the delegation before going further:

   ```
   dig NS nonprod.<domain> +short     # expects the three DigitalOcean nameservers
   dig MX <domain> +short             # expects your email host, unchanged
   ```

   Propagation is usually minutes for a fresh subdomain, since nothing was cached.

Terraform creates the A record *inside* each delegated zone — an `@` record in the
`nonprod.<domain>` zone, not a `nonprod` record in the apex zone. Certificate issuance is
unaffected: Caddy solves the HTTP-01 challenge over port 80 and never touches DNS.

> **Note for GH-247.** The module's `digitalocean_domain` is the full hostname and the record name
> is `@`. Writing it the other way — zone `<domain>`, name `nonprod` — makes Terraform demand
> authority over the apex zone, which it does not have and must not be given.

Mail records are added in step 4, and they go to **SADomains**, not DigitalOcean.

## 3. Cloudflare R2 (backup destination)

1. Create a Cloudflare account. Under **R2**, create a bucket named
   `student-accommodation-backups`. Location: automatic or EU.
2. **R2 → Manage R2 API Tokens → Create API Token**, permission *Object Read & Write*, scoped to
   that bucket. Copy the **Access Key ID**, **Secret Access Key** and the **S3 endpoint URL** (of
   the form `https://<account-id>.r2.cloudflarestorage.com`). Password manager. These become
   `BACKUP_S3_*` values in GH-248.

## 4. SMTP (Brevo) — **blocks PROD entirely**

Without working SMTP the backend refuses to start (`docker-compose.prod.yml` fail-fasts on
`MAIL_HOST`) and no student can register: MailHog is scaled to zero outside development, and email
verification gates application submission.

**Brevo sends only the application's mail; the SADomains hosting keeps every human mailbox.** The
two coexist and do not compete: `MX` governs mail *arriving* and is not touched at any point in this
step, while `SPF` and `DKIM` govern mail *leaving* and are only extended. `APP_SUPPORT_EMAIL` stays
a real, human-read mailbox on SADomains — Brevo sends, that address receives the replies.

The account is free, so this adds no monthly cost to the mailbox hosting already being paid for.
What it does add is a **hard 300 messages/day ceiling**, which is now the binding constraint on the
whole system. Section 4.5 is not optional reading.

### 4.1 Sender identity

In Brevo, add `noreply@<domain>` as the sending address.

Do not send as a human mailbox. The credentials end up in deployment configuration, and the address
appears on every automated message — a student replying to an offer expiry should reach
`APP_SUPPORT_EMAIL`, not a personal inbox.

### 4.2 Authenticate the domain — records go to SADomains, not DigitalOcean

**Senders & IP → Domains → Add a domain**: enter your domain. Brevo shows a set of DNS records —
typically a DKIM `TXT` at `brevo._domainkey`, a `TXT` verification code at the apex, and an SPF
`include:spf.brevo.com`.

Add them in the **SADomains DNS editor**. These are apex-zone records, and step 2 deliberately left
the apex zone at SADomains; only `nonprod` and `app` were delegated to DigitalOcean. Two rules:

- **An SPF record already exists**, because the domain already sends mail. Do not add a second one
  — a domain with two `TXT` SPF records fails SPF entirely, which breaks the **existing mailboxes**
  as well as Brevo. Edit the record in place and add the include before the `~all` or `-all`:

  ```
  v=spf1 include:<existing-host> include:spf.brevo.com ~all
  ```

- **Do not change the `MX` records.** If Brevo's wizard offers to route mail to it, decline. You are
  authenticating a sending domain, not migrating a mailbox.

Then click **Authenticate** in Brevo and wait for every record to show green. If they are not green
within an hour, a record name or type is wrong — SADomains may append the domain to record names
automatically, so check you have not created `brevo._domainkey.<domain>.<domain>`.

### 4.3 Collect the settings

**SMTP & API → SMTP**, then generate an SMTP key:

| Variable | Value |
| --- | --- |
| `MAIL_HOST` | `smtp-relay.brevo.com` |
| `MAIL_PORT` | `587` |
| `MAIL_USERNAME` | your Brevo SMTP login |
| `MAIL_PASSWORD` | the generated SMTP key, not your account password |

`docker-compose.prod.yml` already sets `MAIL_SMTP_AUTH=true` and `MAIL_STARTTLS_ENABLE=true`, which
matches port 587.

### 4.4 Verify from outside, both directions

The Brevo dashboard showing green is not evidence that mail arrives. Check from a real inbox:

1. Send one message from the application's sender to a Gmail address. Confirm it lands in the
   **inbox, not spam**, then open it, choose **Show original**, and confirm `SPF: PASS` and
   `DKIM: PASS`.
2. **Then send from an existing SADomains mailbox** to the same Gmail address and check `SPF: PASS`
   on that one too. A malformed SPF merge in 4.2 breaks your existing mail, and this is the only
   check that catches it.

Step 2 left `MX` alone, so incoming mail should be unaffected — confirm with `dig MX <domain>`
returning what it always did.

### 4.5 The 300/day ceiling, and what happens when you hit it

Free Brevo is capped at **300 messages per day**. Measure the two bursts this application produces
against it:

| Burst | Size | Bounded by |
| --- | --- | --- |
| Scheduled batch allocation | **50 per run** | `app.allocation.batch-size`, `BATCH_SIZE`, default 50 |
| Registration on the day the portal opens | **one verification email per student who signs up** | nothing — student behaviour |

The allocation burst is bounded by configuration and is comfortably inside the cap. The registration
burst is not bounded at all: it is roughly the number of students who sign up on the busiest day,
plus every document, application and allocation email generated the same day. A cohort of 300+
signing up in one window exceeds the free tier on its own.

**Why the ceiling matters more than a rate limit would.** Failed sends are persisted and retried with
exponential backoff (`NotificationRetryService`, up to 3 attempts, 200 rows a pass), so a brief
throttle drains harmlessly over the following passes. A *daily quota* behaves differently: once it
is exhausted mid-morning, all three retries fail before the quota resets at midnight, the rows reach
`PERMANENTLY_FAILED`, and the retry job never looks at them again. Those students never receive a
verification link, cannot submit an application, and **nothing surfaces it** — no error reaches them
and no alert fires.

So, before any day you expect to exceed 300:

- **Estimate the peak.** If expected signups plus routine mail approach 300, act in advance.
- **Either** upgrade the Brevo tier before that day — not during it, when the quota is already gone
  and the affected rows are already terminal.
- **Or** fail over to the SADomains mailbox SMTP, which is already paid for and has a different
  limit. That is a configuration change only — `MAIL_HOST=mail.<domain>`, `MAIL_USERNAME` the full
  `noreply@<domain>` address, `MAIL_PASSWORD` its mailbox password — with no code change and no DNS
  change, since the existing SPF already authorises the host. Keeping that mailbox in existence is
  what makes this escape hatch available, so create it even though Brevo is the primary.

Watch `sas.async.queue.size` and the notification failure count (GH-249) around those days; a spike
in `PERMANENTLY_FAILED` rows is the quota being hit, and it is recoverable only by resending
manually.

## 5. Free monitoring accounts

Sign up only; configuration happens in GH-248 and GH-249.

- **Healthchecks.io** — create one check named `nightly-backup-prod`, schedule daily, grace period
  2 hours. Copy its ping URL.
- **UptimeRobot** (or Better Stack) — create the account now. The monitor itself is added the day
  PROD answers on its hostname, to the settings in 5.1 below.

### 5.1 The PROD uptime monitor

Everything else in this platform watches itself from inside the box it runs on. Prometheus,
Grafana, Loki and Alloy are all containers on the same host as the application, so when that host
dies they die with it and nobody is told. This monitor is the only component that survives the
host, which makes it the highest value thing in the whole observability section and the one piece
that cannot be committed to the repository.

Create it against PROD only. NONPROD is ephemeral by design (GH-247) and would alert every time it
is destroyed.

| Setting | Value |
|---|---|
| Type | HTTPS |
| URL | `https://app.<domain>/health/readiness` |
| Interval | 5 minutes (the free tier's floor) |
| Expected | HTTP 200, body contains `"status":"UP"` |
| Alert after | 2 consecutive failures, so one dropped packet is not a page |
| Contacts | the same mailbox as `APP_SUPPORT_EMAIL`, plus SMS or push if the tier allows |

**Probe that path and no other.** `/` and `/health` are both served by nginx out of its own
container and answer 200 with the backend and the database dead, so a monitor pointed at either
reports a healthy site through a total outage. `/health/readiness` is proxied to the backend's
readiness probe, so a 200 means Caddy, nginx, the application and the database are all answering.
It deliberately excludes SMTP, MinIO and Redis: none of them stops a page being served, and
including them would page you at 3am for a mail provider's outage (GH-147).

Check it does what you think on the day you create it. From any machine that is not the host:

```
curl -i https://app.<domain>/health/readiness      # 200, {"status":"UP"}
```

Then stop the database on the host and confirm the monitor goes red within two intervals:

```
docker compose stop db && sleep 90
curl -i https://app.<domain>/health/readiness      # 503, {"status":"DOWN"}
docker compose start db
```

A monitor nobody has ever seen turn red is not evidence of anything.

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

### 9.1 Keeping the host `.env` current

`/opt/sas/.env` is edited by hand on each host. The deploy syncs the repository tree but never
writes that file, so a key added to `.env.example` does not reach a running host until someone puts
it there. Compose resolves its whole configuration, secrets included, before it acts, so a missing
key does not degrade — it aborts `docker compose up -d` partway through and leaves the stack half
recreated (GH-274).

The `Preflight the host environment` step in `.github/workflows/deploy.yml` now catches this first,
before anything is synced or rebuilt, and names the missing keys. When a deploy fails there:

```bash
ssh <deploy-user>@<host>
${EDITOR:-nano} /opt/sas/.env   # add each key the run named, values per .env.example
```

Then re-run the workflow. Where `.env.example` says an empty value is a valid choice, the key still
has to be present — `KEY=` is a setting, an absent line is not.

---

## Done when

- [ ] `dig NS nonprod.<domain>` and `dig NS app.<domain>` return DigitalOcean nameservers, and
      `dig NS <domain>` still returns SADomains
- [ ] `dig MX <domain>` is unchanged from before any of this started
- [ ] Brevo shows the domain authenticated, sending as `noreply@<domain>`
- [ ] the existing `SPF` record was **edited**, not duplicated — `dig TXT <domain>` returns exactly
      one `v=spf1` record
- [ ] a test message from the application's sender reached a Gmail **inbox** with `SPF: PASS` and
      `DKIM: PASS` on "Show original"
- [ ] a message from an *existing* SADomains mailbox also still passes SPF — this is what catches a
      bad SPF merge
- [ ] the expected busiest registration day was estimated against Brevo's 300/day cap, and the
      SADomains `noreply@` mailbox exists as the documented fallback
- [ ] `APP_SUPPORT_EMAIL` is a mailbox someone reads, not a send-only address
- [ ] the R2 bucket exists and its token is stored
- [ ] both GitHub environments exist, `production` has a required reviewer, and each holds its
      `SSH_PRIVATE_KEY`, `DEPLOY_HOST` and `DEPLOY_USER`
- [ ] the DigitalOcean API token, R2 token, Brevo SMTP key, SADomains `noreply@` mailbox password
      and Healthchecks ping URL are in the password manager
- [ ] a full set of application secrets (section 0.2) exists per environment, NONPROD and PROD
      sharing none of them
- [ ] the uptime monitoring account exists, and once PROD is serving, its monitor probes
      `https://app.<domain>/health/readiness` (section 5.1), alerts a mailbox someone reads, and
      has been watched going red once with the database stopped

## Related

- [Backup and restore runbook](BACKUP_AND_RESTORE.md) — what GH-248 automates offsite.
- `.env.example` — every environment variable the application reads, with its default.
- `docker-compose.prod.yml` — the overlay whose fail-fast variables section 0.2 satisfies.
- [PROD runbook](PROD_RUNBOOK.md): deploying and operating PROD on AWS, which replaced the
  DigitalOcean plan this runbook was written for (docs/PROD_ENVIRONMENT_PLAN.md).
