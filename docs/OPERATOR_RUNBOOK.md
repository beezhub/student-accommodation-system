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
- The application sends through that existing mailbox hosting (step 4), not a separate transactional
  provider. No mail-related DNS record changes at all.

GH-244's cost table lists SMTP at $0.00 on a free transactional tier. That figure does not change —
mailbox SMTP is $0.00 too, being already bought — so this is a decision about vendor count and
sending limits, not about monthly spend. Step 4.2 is where the limit gets checked.

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

`APP_SUPPORT_EMAIL` must be a **real mailbox on the existing email hosting**, not a send-only
address. It is the reply-to on every message the application sends, so a student hitting reply on a
rejection or an offer expiry has to reach a person. Create it before the first deploy.

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

## 4. SMTP — the existing SADomains mailbox hosting — **blocks PROD entirely**

Without working SMTP the backend refuses to start (`docker-compose.prod.yml` fail-fasts on
`MAIL_HOST`) and no student can register: MailHog is scaled to zero outside development, and email
verification gates application submission.

**Use the email hosting already being paid for.** A separate transactional provider would be a
second vendor for a capability already bought, and it buys less than it looks like it does — the
free tiers that make it sound costless cap at around 300 messages a day, which is the same order as
a shared-hosting cap. At any volume large enough to break mailbox SMTP, the free tier breaks too and
the alternative is a paid plan on top of the hosting bill. No DNS changes are needed either: the
domain's `SPF` already authorises its own host, so step 2's decision to leave the apex zone alone
holds here too.

The sizing check in 4.2 is what decides whether that stays true. Do it before PROD, not after.

### 4.1 Create a dedicated sending mailbox

In the SADomains control panel create a mailbox for the application — `noreply@<domain>` — with its
own password.

Do not reuse a human mailbox. Its password ends up in the deployment configuration, so rotating a
person's password would silently stop every application email, and a compromise of the application
host would hand over a real person's inbox.

`APP_SUPPORT_EMAIL` from section 0.1 stays a *separate*, human-read mailbox. `noreply@` sends;
`APP_SUPPORT_EMAIL` receives the replies.

### 4.2 Find the sending limit, and check it against the peak

Get the outbound limit from the control panel or from SADomains support, per hour and per day, and
write it down next to the other decided values. Shared mailbox hosting is commonly 200–500 an hour.

Then compare it against the two bursts this application actually produces:

| Burst | Size | Bounded by |
| --- | --- | --- |
| Scheduled batch allocation | **50 per run** | `app.allocation.batch-size`, `BATCH_SIZE`, default 50 |
| Registration on the day the portal opens | **one verification email per student who signs up** | nothing — student behaviour |

The allocation burst is bounded by configuration and fits comfortably under any plausible cap. The
registration burst is the one to size: it is roughly the number of students you expect to sign up on
the busiest day, and it is not throttled by the application.

The application absorbs a short overrun on its own. Failed sends are persisted and retried with
exponential backoff (`NotificationRetryService`, up to 3 attempts, 200 rows a pass), so brief
throttling drains over the following passes rather than losing mail. What it does not survive is a
*daily* cap hit mid-morning: three retries all fail before midnight, the rows reach
`PERMANENTLY_FAILED`, and those students never receive a verification link and cannot apply.

So: **if the expected peak day exceeds the daily limit, this decision has to be revisited before
that day**, not during it. Compare paid transactional tiers at that point — the free ones will not
help at that volume either.

### 4.3 Collect the settings

From the control panel's mail configuration, typically:

| Variable | Value |
| --- | --- |
| `MAIL_HOST` | `mail.<domain>` — confirm the exact hostname in the panel |
| `MAIL_PORT` | `587` |
| `MAIL_USERNAME` | the **full address**, `noreply@<domain>`, not the local part |
| `MAIL_PASSWORD` | that mailbox's password |

`docker-compose.prod.yml` already sets `MAIL_SMTP_AUTH=true` and `MAIL_STARTTLS_ENABLE=true`, which
matches port 587. If the panel offers only implicit TLS on 465, say so — that needs a different
property and is not what the overlay is configured for.

### 4.4 Confirm DKIM is on

Shared hosts frequently leave DKIM off by default. Find the DKIM or "email authentication" section
of the control panel and enable it for the domain; the host publishes its own selector record. Then
verify from the outside rather than trusting the panel:

- Send one message from `noreply@<domain>` to a Gmail address.
- Open it, **Show original**, and confirm `SPF: PASS` and `DKIM: PASS`.
- Confirm it arrived in the inbox, not spam.

If DKIM cannot be enabled, stop and reconsider the provider. Verification mail that lands in spam
fails silently and looks exactly like an application bug.

### 4.5 Know the one risk you are accepting

Shared mailbox hosting sends from an IP shared with the host's other customers, so their sending
behaviour affects your deliverability and you cannot see or control it. The signal that this has
become a problem is verification emails starting to land in spam without any change on your side.
`SmtpHealthIndicator` covers connectivity, not reputation, so this needs the periodic Gmail check in
4.4 rather than a dashboard.

That risk is worth taking to avoid a second vendor at this scale. It stops being worth it if
students begin reporting missing verification emails — at which point a dedicated sending domain,
not just a dedicated provider, is the fix.

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

- [ ] `dig NS nonprod.<domain>` and `dig NS app.<domain>` return DigitalOcean nameservers, and
      `dig NS <domain>` still returns SADomains
- [ ] `dig MX <domain>` is unchanged from before any of this started
- [ ] a `noreply@<domain>` mailbox exists, separate from any human mailbox, and DKIM is enabled for
      the domain
- [ ] a test message from `noreply@<domain>` reached a Gmail **inbox** with `SPF: PASS` and
      `DKIM: PASS` on "Show original"
- [ ] the outbound sending limit is written down, and the expected busiest registration day fits
      under the daily figure
- [ ] `APP_SUPPORT_EMAIL` is a mailbox someone reads, not a send-only address
- [ ] the R2 bucket exists and its token is stored
- [ ] both GitHub environments exist, `production` has a required reviewer, and each holds its
      `SSH_PRIVATE_KEY`, `DEPLOY_HOST` and `DEPLOY_USER`
- [ ] the DigitalOcean API token, R2 token, `noreply@` mailbox password and Healthchecks ping URL
      are in the password manager
- [ ] a full set of application secrets (section 0.2) exists per environment, NONPROD and PROD
      sharing none of them

## Related

- [Backup and restore runbook](BACKUP_AND_RESTORE.md) — what GH-248 automates offsite.
- `.env.example` — every environment variable the application reads, with its default.
- `docker-compose.prod.yml` — the overlay whose fail-fast variables section 0.2 satisfies.
