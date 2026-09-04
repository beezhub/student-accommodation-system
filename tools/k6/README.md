# k6 Load Test — Student Accommodation System

TASK-3.08 — 500 VU soak test covering the primary student journey.

## NFRs Under Test

| Metric | Target |
|--------|--------|
| P95 response time (all endpoints) | < 500ms |
| Error rate | < 1% |
| HikariCP connection wait | < 30s |

---

## Prerequisites

1. **k6 installed** — https://k6.io/docs/getting-started/installation/
   ```bash
   # macOS
   brew install k6

   # Windows (winget)
   winget install k6

   # Docker
   docker pull grafana/k6
   ```

2. **Full Docker Compose stack running**
   ```bash
   docker-compose up --build -d
   ```
   Wait until `http://localhost:8080/actuator/health` returns `{"status":"UP"}`.

3. **Node >= 18** (for the seed script)

---

## Step 1 — Seed Test Data

The load test reads student credentials from `tools/k6/seed-data/students.json`.
Run the seed script once before the first test run:

```bash
node tools/k6/seed-data/seed.js
```

**Environment variables (optional overrides):**

| Variable | Default | Description |
|----------|---------|-------------|
| `BASE_URL` | `http://localhost:8080` | Backend base URL |
| `ADMIN_EMAIL` | `admin@beezhub.com` | Platform admin email |
| `ADMIN_PASSWORD` | `Admin1234!` | Platform admin password |

Example:
```bash
BASE_URL=http://staging.example.com ADMIN_EMAIL=admin@example.com node tools/k6/seed-data/seed.js
```

The seed script:
- Creates 500 student accounts (`loadtest.student1@beezhub-test.com` … `loadtest.student500@beezhub-test.com`)
- Skips already-existing accounts (safe to re-run)
- Writes `tools/k6/seed-data/students.json` consumed by `load-test.js`

---

## Step 2 — Run the Soak Test

```bash
k6 run tools/k6/load-test.js
```

**Override base URL:**
```bash
k6 run -e BASE_URL=http://staging.example.com tools/k6/load-test.js
```

**Run via Docker (no local k6 install):**
```bash
docker run --rm -i --network host \
  -v "$(pwd)/tools/k6:/scripts" \
  grafana/k6 run /scripts/load-test.js
```

The test runs **500 VUs for 30 minutes** (constant-vus executor).

---

## Scenarios Covered

| Step | Endpoint | Method |
|------|----------|--------|
| 1 | `/api/auth/login` | POST |
| 2 | `/api/applications/my-applications` | GET |
| 3 | `/api/applications/submit` | POST |
| 4 | `/api/notifications` | GET |
| 5 | `/api/room-allocation/{id}/accept` | POST |

Steps 3 and 5 are skipped gracefully when the student already has an application / no pending offer, keeping the error rate clean without masking real failures.

---

## Interpreting Results

k6 prints a summary at the end. Key lines to check:

```
http_req_duration............: avg=XXms  p(95)=XXms   ← must be < 500ms
http_req_failed..............: X.XX%               ← must be < 1%
login_duration...............: p(95)=XXms
apps_duration................: p(95)=XXms
submit_duration..............: p(95)=XXms
notif_duration...............: p(95)=XXms
accept_duration..............: p(95)=XXms
```

A ✓ or ✗ is printed next to each threshold.

### HikariCP connection wait

During the test, query the Prometheus metric exposed by the backend:

```
http://localhost:8080/actuator/prometheus
```

Search for:
```
hikaricp_connections_timeout_total
hikaricp_connections_pending
```

Or view in Grafana → **SAS — Backend Dashboard** → HikariCP panel.

---

## Updating Results

After a test run, document the results in this file under the section below.

---

## Test Results

> Replace this section after each soak test run.

| Run date | VUs | Duration | P95 (global) | Error rate | Pass? |
|----------|-----|----------|--------------|------------|-------|
| _not yet run_ | 500 | 30m | — | — | — |

### Per-endpoint P95 (last run)

| Endpoint | P95 |
|----------|-----|
| POST /api/auth/login | — |
| GET /api/applications/my-applications | — |
| POST /api/applications/submit | — |
| GET /api/notifications | — |
| POST /api/room-allocation/{id}/accept | — |
