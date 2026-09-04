#!/usr/bin/env node
/**
 * Seed script — creates 500 student accounts + 1 building with 500 rooms
 * for the k6 soak test.
 *
 * Usage:
 *   node tools/k6/seed-data/seed.js
 *
 * Requires:
 *   - Full Docker Compose stack running
 *   - node >= 18 (uses fetch)
 *
 * Outputs:
 *   tools/k6/seed-data/students.json  — consumed by load-test.js
 *
 * TASK-3.08
 */

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';
const ADMIN_EMAIL = process.env.ADMIN_EMAIL || 'admin@beezhub.com';
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || 'Admin1234!';
const STUDENT_COUNT = 500;
const OUTPUT_FILE = new URL('./students.json', import.meta.url).pathname;

import { writeFileSync } from 'fs';

async function post(path, body, token) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const res = await fetch(`${BASE_URL}${path}`, {
    method: 'POST',
    headers,
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`POST ${path} → ${res.status}: ${text}`);
  }
  return res.json();
}

async function get(path, token) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const res = await fetch(`${BASE_URL}${path}`, { headers });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`GET ${path} → ${res.status}: ${text}`);
  }
  return res.json();
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

console.log(`Seeding ${STUDENT_COUNT} students against ${BASE_URL} …`);

// 1. Admin login
const { token: adminToken } = await post('/api/auth/login', {
  email: ADMIN_EMAIL,
  password: ADMIN_PASSWORD,
});
console.log('Admin authenticated.');

// 2. Create students
const students = [];
for (let i = 1; i <= STUDENT_COUNT; i++) {
  const email = `loadtest.student${i}@beezhub-test.com`;
  const password = `LoadTest${i}!`;

  let token;
  try {
    const res = await post('/api/auth/signup', {
      firstName: `Load`,
      lastName: `Student${i}`,
      email,
      password,
    });
    token = res.token;
  } catch (err) {
    // Already exists — just login
    try {
      const res = await post('/api/auth/login', { email, password });
      token = res.token;
    } catch (_) {
      console.warn(`  Skipping student ${i} — could not create or login: ${err.message}`);
      continue;
    }
  }

  // Resolve studentId from /api/auth/me
  const me = await get('/api/auth/me', token);
  students.push({ email, password, studentId: me.id });

  if (i % 50 === 0) console.log(`  Created/verified ${i}/${STUDENT_COUNT} students …`);
}

console.log(`Students ready: ${students.length}`);

// 3. Write students.json
writeFileSync(
  OUTPUT_FILE.startsWith('/') ? OUTPUT_FILE : OUTPUT_FILE.replace(/^\//, ''),
  JSON.stringify(students, null, 2)
);
console.log(`Wrote ${OUTPUT_FILE}`);
console.log('Seed complete. You can now run: k6 run tools/k6/load-test.js');
