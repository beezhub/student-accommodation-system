# ADR-010: One Deployment Per Company, In The Company's Own AWS Account

- **Status:** Proposed. Waiting for buy-in from the current client.
- **Date:** 2026-11-05
- **Task:** #500. Work slices are not filed as issues yet.
- **Deciders:** Boitumelo Tshehla
- **Plan:** [CLIENT_SITES_PLAN.md](../CLIENT_SITES_PLAN.md)

---

## In short

A client wants a site for their company only, so they can do their own marketing and students see
only their buildings, with no competitors.

**Each company gets its own deployment in its own AWS account.** The codebase stays the same, and
settings provide the branding and company details. Each company pays its own AWS bill and keeps its
own data. An agency can run several companies on one deployment, under one brand.

We push each new version to the client's ECR (AWS's store for app images). The client upgrades when
they choose, using our release notes.

The current client uses PROD today and is our only real use case. Nothing moves until they agree.

---

## Context

The platform was designed as one shared marketplace:

- Public visitors and students are never limited by company. `TenantFilterAspect` skips the tenant
  filter when the tenant is null (no login) or 0 (students).
- `GET /api/building` and the other building routes return every company's buildings.
- One email means one account across the platform, and a student's documents are reused across
  companies.

So hiding competitors inside one shared database means limiting every student route and redesigning
accounts. Putting each company in its own deployment avoids that work, because competitors' data is
never in the same database.

---

## Decision Drivers

1. **No competitors on a client's site.** This is the actual request.
2. **The client can market it as their own.** Own address, name, logo and colour.
3. **Each company keeps its own data and pays its own bill.**
4. **One codebase.** No copy of the code per client.
5. **One person can run it.**
6. **Agencies can support several companies.**

---

## Options Considered

| Option | No competitors because | Who pays AWS | Main cost |
|---|---|---|---|
| **Own deployment in the company's AWS account** | Their data is alone in that database | The company | Release tools, and clients on different versions |
| **Own deployment in our AWS account** | Same | Us, then we charge them | We carry the bills, and handing over data is extra work |
| **One shared deployment, one site per address (option A)** | Code limits each request | Us | Limits on every student route, plus new accounts |
| **Link with a filter** (`/buildings?company=5`) | Nothing: students can remove it | Us | Doesn't meet the request |
| **Microservices** | Same as option A | Us | Same as option A, plus splitting the app |

Option A was chosen first, then replaced. It is still the right design if we ever need many brands on
one deployment. Microservices and a separate config service don't solve the problem. Keeping companies
apart is about who owns data, and settings only need one small endpoint.

---

## Decision

**Proposed: own deployment in the company's own AWS account.** Multi-company code stays in the app,
because agencies use it.

### What we decided

| Question | Answer |
|---|---|
| What do settings cover? | Branding and company details: name, logo, colour, support contact, email sender, web address |
| Does login change? | No. Identity means company details, and login stays email and password |
| Who owns the AWS account? | The client. They give us a limited IAM role (a set of permissions in their account) |
| How do updates arrive? | We push images to their ECR. They upgrade on demand, using release notes |
| Which versions do we support? | The latest two. Security fixes must be applied within 14 days, or the contract says we no longer carry that risk |
| What about agencies? | One deployment and one brand. Students see all the agency's companies together |
| Who runs each deployment? | The client, using our runbook. Clients without AWS skills can buy a support plan, which adds a wider role they can switch on or off |

The 14 days is a starting suggestion and needs to be set in the contract.

### How branding works

Most settings are already environment variables, so one image can serve every client:

- **Already settings:** `APP_BASE_URL`, `MAIL_FROM`, `APP_UNIVERSITY_NAME`, `ALLOWED_ORIGINS`, and
  `APP_HOSTNAME` for Caddy.
- **New settings:** `APP_BRAND_*` for name, logo, main colour and support email.
- **New public endpoint:** `GET /api/site` returns these values, and Angular loads them at startup.

```json
GET /api/site
{ "name": "Client Rooms", "logoUrl": "...", "primaryColour": "#0a7d5a", "supportEmail": "..." }
```

The logo goes in the bucket under a `site/` key prefix. `OrphanFileCleanupJob` deletes under the
`{role}/` prefixes, so new writers must stay out of those.

### How releases work

- **Version numbers.** Each release gets a fixed version, and images in ECR can't be overwritten.
  Today's `infra/terraform/prod/registry.tf` allows overwriting (`MUTABLE`), so client accounts need
  this set to `IMMUTABLE`.
- **Push rights only.** The routine role can push images to the client's ECR and nothing else. It
  can't reach their database or documents.
- **Upgrade command.** The client runs one command with a version number. It takes an RDS snapshot
  (a full database backup) first, then deploys and checks health.
- **Release notes.** The repo is private, so notes must be published where clients can read them. Each
  one lists migrations, new required settings, and security fixes.

Going back after a migration is hard, because Flyway only moves forward. Going back means restoring
the snapshot, which loses anything saved since then. Skipping versions works, because Flyway applies
waiting migrations in order. A new required setting missing from the notes would stop the app starting.

---

## Risks

### The current client may say no

This model asks them to open an AWS account, pay AWS directly, and either run upgrades or buy support.
They may prefer one bill from us. If so, we could run their copy in an account we own and charge them.
That keeps most of this plan, but changes who holds the bill.

### Moving live data

The current client's students, applications and documents are on PROD now. Moving them is the riskiest
step in this plan. It needs a rehearsal, a short outage, and a redirect, because emails already sent
link to `accommodation.beezhub.co.za`. Login tokens won't carry over if `JWT_SECRET` changes, so
students would log in again. Old copies of their data must be removed from our account afterwards.

### Clients on different versions

A bug report means nothing without a version number, so the version must be easy to find. A client can
also fall behind on security fixes, which is why the contract needs the deadline.

### Control sits with the client

The client can remove our role, and can reach the images in their account, which contain our code. The
contract needs to cover licensing and what support applies if access is removed. If they stop paying
AWS, their site can go down, and we may still get the support call.

### Every new account has setup steps

SES production access needs a manual AWS review per account, so allow for the wait. Each account also
needs the `af-south-1` region turned on. The hostname and account are fixed in
`.github/workflows/deploy-prod.yml`, `tools/prod/install-caddy.sh` and
`infra/terraform/prod/variables.tf`. Monitoring today is one Grafana Cloud stack.

### Agencies bring shared-database problems back

Several companies in one database means these existing problems matter again. Fix them before the
first agency goes live:

- Any `TENANT_ADMIN` can list, search and delete all students (`StudentController`,
  `StudentService.getAllStudents`). This comes from reading the code, and it has not been run.
- Companies share one document pack, one status per document, and all comments
  (`DocumentCommentService:115`).
- The waiting list counts across all companies (`WaitingListService:262-264`).

### Fixed text and colours

BeeZhub's name is fixed across login, signup, verification and landing pages. The landing page has
fixed stats (`home.component.ts:24-28`) and testimonials. Email subjects and some wording are fixed
(`EmailSubjects.java`, `EmailDispatchService.java`). `src/app` has more than 1,100 hardcoded hex colours
across more than 70 files, so version 1 covers logo, name and main colour, not a full redesign.

### Marketing gaps

WhatsApp and Facebook previews don't run JavaScript, so they show the fixed title in `index.html`.
There is no `robots.txt`, sitemap or meta description. A client's analytics tag needs a change to the
Content Security Policy in `frontend/nginx-security-headers.conf`.

### POPIA

POPIA is South Africa's data privacy law. These need a lawyer's advice:

- `security-assurance.component.ts:129` promises data is "never sold or shared with third parties".
- Terms of Service and Privacy Policy link to `#` (`signup.component.html:129,131`).
- Consent is not saved anywhere.
- Moving data to the client's account, and our access under a support plan, both need contract terms.

### HSTS on a client's domain

`Caddyfile:49` sends `includeSubDomains`, so browsers refuse plain HTTP below that address for a year.
A client should use a subdomain like `rooms.client.co.za`, not `client.co.za`.

---

## Consequences

**What gets better:**

- One client's outage, breach or bill can't touch another.
- No competitor can appear through a bug in a single-company deployment.
- Routine releases don't give us access to student data.
- AWS costs sit with the company using them.

**What gets harder:**

- Each new account is one more thing to set up and support.
- Clients run different versions, so upgrades must stay safe across versions.
- Release notes and upgrade tools become part of the product.
- This is safer between clients, but it isn't automatically more reliable. Each deployment can still
  fail by itself, and fixing it depends on who has access.

### Revisit this decision when any of these happens

- The current client doesn't want their own AWS account.
- Enough clients sign that supporting separate accounts becomes too much for one person.
- An agency wants a different brand per company, which brings back option A's work.

---

## How much of this is confirmed

**Confirmed by reading the code on 2026-11-05.** No code was run for this document.

**Not yet checked:**

- What is on PROD now, and how large the data is.
- That a tenant admin really can list all students.
- That the current client agrees.
