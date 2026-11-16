# Student Accommodation System — UAT Test Plan

| Field | Value |
|-------|-------|
| **Version** | 1.0 |
| **Date** | 2026-05-17 |
| **Prepared By** | ________ |
| **Reviewed By** | ________ |
| **Approved By** | ________ |

---

## 1. Purpose

This document is the master index for the **exhaustive manual User Acceptance Testing (UAT) test plan** for the Student Accommodation System. It covers every user-facing feature across all roles and workflows, and is intended to serve as the formal sign-off gate before go-live.

Each linked test-case file contains detailed, step-by-step test cases with preconditions, expected results, and pass/fail checkboxes. All test cases must be executed, reviewed, and signed off by the QA Lead and Product Owner before the system is approved for production deployment.

---

## 2. Test Environment

| Component | URL / Command |
|-----------|---------------|
| **Frontend** | http://localhost:4200 |
| **Backend API** | http://localhost:8080 |
| **Swagger UI** | http://localhost:8080/swagger-ui.html |
| **MinIO Console** | http://localhost:9001 |
| **MailHog (email testing)** | http://localhost:8025 |

**Setup:**

```bash
docker-compose up --build
```

Ensure all five services (backend, frontend, PostgreSQL, MinIO, MailHog) are healthy before starting test execution. Verify by loading the Frontend URL and confirming the login page renders.

---

## 3. Test Data Setup

### 3.1 User Accounts

| Role | Email | Password |
|------|-------|----------|
| Platform Admin | platformadmin@test.com | Admin@12345 |
| Tenant Admin | tenantadmin@test.com | Admin@12345 |
| Student 1 | john.doe@test.com | Student@123 |
| Student 2 | jane.smith@test.com | Student@123 |
| Student 3 | alex.jones@test.com | Student@123 |

### 3.2 Reference Data

The following reference data must be seeded (via Flyway migrations or manual setup) before test execution:

- **Institutions**: University of Cape Town, Stellenbosch University, University of the Witwatersrand
- **Years of Study**: 1st Year, 2nd Year, 3rd Year, Postgraduate
- **Academic Years**: 2025, 2026
- **Buildings**: Residence A (mixed, 50 rooms), Residence B (female-only, 30 rooms), Residence C (postgraduate, 20 rooms)
- **Rooms**: Single rooms, double rooms, accessible rooms across each building with varying capacities and pricing
- **Document Types**: National ID, Proof of Registration, Proof of Income, Medical Certificate, Disability Certificate, Bursary Letter
- **Allocation Rules**: Distance-based scoring, year-of-study priority, disability priority, financial need weighting
- **System Settings**: Platform defaults for offer expiry days, auto-allocation toggle, batch interval

---

## 4. User Roles

| Role | Code | Description |
|------|------|-------------|
| Student | `STUDENT` | Submits accommodation applications, uploads documents, accepts/declines offers, views allocation status. |
| Platform Admin | `PLATFORM_ADMIN` | Manages platform-wide settings, tenants, feature flags, and system defaults. Has access to all tenants. |
| Tenant Admin | `TENANT_ADMIN` | Manages buildings, rooms, applications, allocations, documents, reference data, and users within their tenant. |
| Building Staff | `BUILDING_STAFF` | Views building-specific information, manages check-ins, and handles day-to-day residence operations. |

---

## 5. Test Case Format

Every test case in the linked files follows this standard template:

```
### TC-{MODULE}-{NUMBER}: {Title}

| Field | Value |
|-------|-------|
| **ID** | TC-{MODULE}-{NUMBER} |
| **Module** | {Module} |
| **Priority** | Critical / High / Medium / Low |
| **Preconditions** | {preconditions} |
| **Test Data** | {data} |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | {action} | {result} |
| 2 | {action} | {result} |

| Postconditions | {state after test} |
| Pass/Fail | ☐ |
| Tester | _________ |
| Date | _________ |
| Notes | |
```

**Priority definitions:**
- **Critical** — Blocks go-live if failing. Core business flows (login, apply, allocate).
- **High** — Major feature broken but workaround exists.
- **Medium** — Minor feature or cosmetic issue with limited impact.
- **Low** — Edge case or nice-to-have behaviour.

---

## 6. Table of Contents

| # | File | Module | Target TCs | Status |
|---|------|--------|------------|--------|
| 1 | [01-AUTH.md](01-AUTH.md) | Authentication & Account Management | 52 | ☐ |
| 2 | [02-ONB.md](02-ONB.md) | Student Onboarding | 35 | ☐ |
| 3 | [03-APP.md](03-APP.md) | Application Management | 63 | ☐ |
| 4 | [04-DOC.md](04-DOC.md) | Document Management | 44 | ☐ |
| 5 | [05-BLD.md](05-BLD.md) | Building Management | 37 | ☐ |
| 6 | [06-ROM.md](06-ROM.md) | Room Management | 43 | ☐ |
| 7 | [07-ALC.md](07-ALC.md) | Allocation System | 66 | ☐ |
| 8 | [08-STG.md](08-STG.md) | Staged/Batch Allocation | 25 | ☐ |
| 9 | [09-WL.md](09-WL.md) | Waiting List | 15 | ☐ |
| 10 | [10-NTF.md](10-NTF.md) | Notifications | 35 | ☐ |
| 11 | [11-REF.md](11-REF.md) | Reference Data Management | 49 | ☐ |
| 12 | [12-TNT.md](12-TNT.md) | Tenant Management | 20 | ☐ |
| 13 | [13-USR.md](13-USR.md) | User Management | 17 | ☐ |
| 14 | [14-SET.md](14-SET.md) | System Settings | 15 | ☐ |
| 15 | [15-FF.md](15-FF.md) | Feature Flags | 10 | ☐ |
| 16 | [16-LOG.md](16-LOG.md) | Activity Logs | 19 | ☐ |
| 17 | [17-DSH.md](17-DSH.md) | Admin Dashboard | 12 | ☐ |
| 18 | [18-JOB.md](18-JOB.md) | Scheduled Jobs | 15 | ☐ |
| 19 | [19-SEC.md](19-SEC.md) | Security & Authorization | 30 | ☐ |
| 20 | [20-XFW.md](20-XFW.md) | Cross-Feature Workflows | 25 | ☐ |
| 21 | [21-REL-NEXT.md](21-REL-NEXT.md) | Release Next: Academic Years, Intakes and Stays | 74 | ☐ |

**Total: 701 test cases**

---

## 7. Test Execution Summary

| Module | Total | Passed | Failed | Blocked | Not Run |
|--------|-------|--------|--------|---------|---------|
| Authentication & Account Management | 52 | | | | 52 |
| Student Onboarding | 35 | | | | 35 |
| Application Management | 63 | | | | 63 |
| Document Management | 44 | | | | 44 |
| Building Management | 37 | | | | 37 |
| Room Management | 43 | | | | 43 |
| Allocation System | 66 | | | | 66 |
| Staged/Batch Allocation | 25 | | | | 25 |
| Waiting List | 15 | | | | 15 |
| Notifications | 35 | | | | 35 |
| Reference Data Management | 49 | | | | 49 |
| Tenant Management | 20 | | | | 20 |
| User Management | 17 | | | | 17 |
| System Settings | 15 | | | | 15 |
| Feature Flags | 10 | | | | 10 |
| Activity Logs | 19 | | | | 19 |
| Admin Dashboard | 12 | | | | 12 |
| Scheduled Jobs | 15 | | | | 15 |
| Security & Authorization | 30 | | | | 30 |
| Cross-Feature Workflows | 25 | | | | 25 |
| Release Next: Academic Years, Intakes and Stays | 74 | | | | 74 |
| **TOTALS** | **701** | **0** | **0** | **0** | **701** |

---

## 8. Sign-Off

By signing below, I confirm that all test cases have been executed and reviewed, and the results accurately reflect the system's readiness for production deployment.

| Role | Name | Signature | Date |
|------|------|-----------|------|
| **QA Lead** | _________________ | _________________ | _________ |
| **Project Manager** | _________________ | _________________ | _________ |
| **Product Owner** | _________________ | _________________ | _________ |
| **UAT Lead** | _________________ | _________________ | _________ |
