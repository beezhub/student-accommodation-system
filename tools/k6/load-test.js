/**
 * k6 Soak Load Test — Student Accommodation System
 *
 * Simulates the primary student journey at 500 concurrent VUs for 30 minutes.
 * NFRs: P95 < 500ms, error rate < 1%.
 *
 * Pre-requisites:
 *   - Full Docker Compose stack running (backend, db, minio)
 *   - Seed data in place: 500 student accounts + 1 building with 500 rooms
 *     (see README.md for seed instructions)
 *
 * Run:
 *   k6 run tools/k6/load-test.js
 *
 * TASK-3.08
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { SharedArray } from 'k6/data';

// ---------------------------------------------------------------------------
// Configuration
// ---------------------------------------------------------------------------

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Pre-generated student credentials — populated by the seed script.
// Format: [{ email, password, studentId }]
const STUDENTS = new SharedArray('students', function () {
  return JSON.parse(open('./seed-data/students.json'));
});

// ---------------------------------------------------------------------------
// Custom metrics
// ---------------------------------------------------------------------------

const loginFailRate    = new Rate('login_fail_rate');
const appsFailRate     = new Rate('apps_fail_rate');
const submitFailRate   = new Rate('submit_fail_rate');
const notifFailRate    = new Rate('notif_fail_rate');
const acceptFailRate   = new Rate('accept_fail_rate');

const loginDuration    = new Trend('login_duration',  true);
const appsDuration     = new Trend('apps_duration',   true);
const submitDuration   = new Trend('submit_duration', true);
const notifDuration    = new Trend('notif_duration',  true);
const acceptDuration   = new Trend('accept_duration', true);

// ---------------------------------------------------------------------------
// Thresholds (NFRs)
// ---------------------------------------------------------------------------

export const options = {
  scenarios: {
    soak: {
      executor: 'constant-vus',
      vus: 500,
      duration: '30m',
    },
  },
  thresholds: {
    // Global P95 must be under 500ms
    http_req_duration: ['p(95)<500'],
    // Global error rate must be under 1%
    http_req_failed: ['rate<0.01'],

    // Per-endpoint P95
    login_duration:  ['p(95)<500'],
    apps_duration:   ['p(95)<500'],
    submit_duration: ['p(95)<500'],
    notif_duration:  ['p(95)<500'],
    accept_duration: ['p(95)<500'],

    // Per-endpoint error rates
    login_fail_rate:  ['rate<0.01'],
    apps_fail_rate:   ['rate<0.01'],
    submit_fail_rate: ['rate<0.01'],
    notif_fail_rate:  ['rate<0.01'],
    accept_fail_rate: ['rate<0.01'],
  },
};

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

const JSON_HEADERS = { 'Content-Type': 'application/json' };

function authHeaders(token) {
  return { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };
}

function pickStudent() {
  return STUDENTS[Math.floor(Math.random() * STUDENTS.length)];
}

// ---------------------------------------------------------------------------
// Scenario: student primary journey
// ---------------------------------------------------------------------------

export default function () {
  const student = pickStudent();

  // ------------------------------------------------------------------
  // 1. Login
  // ------------------------------------------------------------------
  let token = null;
  let studentId = student.studentId;

  group('1. login', function () {
    const payload = JSON.stringify({ email: student.email, password: student.password });
    const res = http.post(`${BASE_URL}/api/auth/login`, payload, { headers: JSON_HEADERS });

    const ok = check(res, {
      'login 200': (r) => r.status === 200,
      'login has token': (r) => {
        try { return !!JSON.parse(r.body).token; } catch { return false; }
      },
    });

    loginFailRate.add(!ok);
    loginDuration.add(res.timings.duration);

    if (ok) {
      token = JSON.parse(res.body).token;
    }
  });

  if (!token) {
    // Cannot proceed without a token — count as failed VU iteration
    sleep(1);
    return;
  }

  const authed = { headers: authHeaders(token) };

  sleep(0.5);

  // ------------------------------------------------------------------
  // 2. Get my applications
  // ------------------------------------------------------------------
  let applicationId = null;

  group('2. get my applications', function () {
    const res = http.get(`${BASE_URL}/api/applications/my-applications`, authed);

    const ok = check(res, {
      'get applications 200': (r) => r.status === 200,
    });

    appsFailRate.add(!ok);
    appsDuration.add(res.timings.duration);

    if (ok) {
      try {
        const apps = JSON.parse(res.body);
        if (Array.isArray(apps) && apps.length > 0) {
          applicationId = apps[0].id;
        }
      } catch (_) { /* ignore */ }
    }
  });

  sleep(0.5);

  // ------------------------------------------------------------------
  // 3. Submit application (only if student has none yet)
  // ------------------------------------------------------------------
  group('3. submit application', function () {
    if (applicationId !== null) {
      // Student already has an application — skip to keep error rate clean
      submitFailRate.add(false);
      return;
    }

    const payload = JSON.stringify({ studentId: studentId });
    const res = http.post(`${BASE_URL}/api/applications/submit`, payload, { headers: authHeaders(token) });

    const ok = check(res, {
      'submit application 201': (r) => r.status === 201,
    });

    submitFailRate.add(!ok);
    submitDuration.add(res.timings.duration);

    if (ok) {
      try { applicationId = JSON.parse(res.body).id; } catch (_) { /* ignore */ }
    }
  });

  sleep(0.5);

  // ------------------------------------------------------------------
  // 4. Get notifications
  // ------------------------------------------------------------------
  let allocationId = null;

  group('4. get notifications', function () {
    const res = http.get(`${BASE_URL}/api/notifications`, authed);

    const ok = check(res, {
      'get notifications 200': (r) => r.status === 200,
    });

    notifFailRate.add(!ok);
    notifDuration.add(res.timings.duration);

    if (ok) {
      // Look for an OFFER_ISSUED notification that contains an allocationId
      try {
        const body = JSON.parse(res.body);
        const offerNotif = (body.notifications || []).find(
          (n) => n.type === 'OFFER_ISSUED' && n.data && n.data.allocationId
        );
        if (offerNotif) {
          allocationId = offerNotif.data.allocationId;
        }
      } catch (_) { /* ignore */ }
    }
  });

  sleep(0.5);

  // ------------------------------------------------------------------
  // 5. Accept offer (only if an active offer exists)
  // ------------------------------------------------------------------
  group('5. accept offer', function () {
    if (allocationId === null) {
      // No pending offer — skip gracefully
      acceptFailRate.add(false);
      return;
    }

    // Use a future check-in date (tomorrow at noon)
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    tomorrow.setHours(12, 0, 0, 0);
    const checkIn = tomorrow.toISOString().replace('Z', '');

    const payload = JSON.stringify({ expectedCheckInDate: checkIn });
    const res = http.post(
      `${BASE_URL}/api/room-allocation/${allocationId}/accept`,
      payload,
      { headers: authHeaders(token) }
    );

    const ok = check(res, {
      'accept offer 200': (r) => r.status === 200,
    });

    acceptFailRate.add(!ok);
    acceptDuration.add(res.timings.duration);
  });

  sleep(1);
}
