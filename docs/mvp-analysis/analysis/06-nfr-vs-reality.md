# Analysis 6: Non-Functional Requirements vs. Reality

## Scope

Compare the 27 documented NFRs in `BUSINESS_REQUIREMENTS.md` against the actual implementation, focusing on security posture, performance readiness, accessibility, and compliance.

---

## Finding 32: No Rate Limiting Anywhere (NFR-008)

**Category:** MISSING-CRITICAL
**Priority:** P0
**Affected Area:** Backend / Security Infrastructure

### Description
NFR-008 requires "protection against common vulnerabilities." The system has no rate limiting on any endpoint. Public endpoints (`/api/auth/login`, `/api/auth/signup`, `/api/auth/forgot-password`, `/api/auth/resend-verification`) are fully exposed to abuse.

### Impact
- Brute force password attacks are uncapped.
- `forgot-password` and `resend-verification` can be used to flood a user's inbox (email bombing).
- `signup` can be used to mass-create garbage accounts.
- For SaaS: one abusive client could degrade the platform for all tenants.

### Evidence
- No `Bucket4j`, `RateLimiter`, or Spring Cloud Gateway rate limiting configuration found.
- No `@RateLimited` annotations or filter-based throttling.
- `SecurityConfig.java`: public endpoints are wide open.

### Recommendation
Implement rate limiting using `Bucket4j` with Spring Boot Starter:
- `/api/auth/login`: 5 attempts per minute per IP.
- `/api/auth/signup`: 10 per hour per IP.
- `/api/auth/forgot-password`: 3 per hour per email.
- `/api/auth/resend-verification`: 3 per hour per email.
- Global: 100 requests per minute per authenticated user.

---

## Finding 33: No Virus / Malware Scanning on File Uploads (NFR-009)

**Category:** MISSING-IMPORTANT
**Priority:** P2
**Affected Area:** Backend / Security

### Description
NFR-009 requires "File uploads shall be scanned for malware." Documents are uploaded to MinIO without any content scanning. The system validates file extension and size but not file content.

### Impact
- Malicious files could be stored in MinIO and served to admins via presigned URLs.
- A crafted PDF could exploit viewer vulnerabilities.

### Evidence
- `DocumentService.uploadDocuments()`: validates file size and extension via `DocumentService` / frontend `validateFile()`.
- No ClamAV, VirusTotal, or any scanning integration.

### Recommendation
For MVP: validate that uploaded file content matches its declared MIME type (e.g., a `.pdf` actually contains a PDF header). For post-MVP: integrate ClamAV or a cloud scanning service.

---

## Finding 34: No WCAG 2.1 AA Compliance (NFR-017)

**Category:** MISSING-IMPORTANT
**Priority:** P2
**Affected Area:** Frontend / Accessibility

### Description
NFR-017 requires WCAG 2.1 AA compliance. The frontend styling audit report (`STYLING_AUDIT_REPORT.md`) scored the system 5.5/10 and identified 12 critical issues, 31 major issues, and 47 minor issues. There is no evidence of:
- Accessibility testing (automated or manual).
- ARIA labels on interactive components.
- Keyboard navigation testing.
- Screen reader compatibility.

### Impact
- Students with disabilities cannot use the system effectively.
- Legal compliance risk (disability discrimination laws).
- For SaaS: institutional clients may have accessibility mandates.

### Evidence
- `frontend/docs/STYLING_AUDIT_REPORT.md`: overall score 5.5/10.
- No `axe`, `pa11y`, or `lighthouse` configuration in the project.
- No `.a11y` test files found.

### Recommendation
For MVP: run an automated accessibility audit (Lighthouse, axe-core) and fix critical issues (form labels, color contrast, keyboard navigation). Full WCAG AA compliance can be a Phase 2 goal.

---

## Finding 35: No Internationalization / Multi-Language (NFR-018)

**Category:** IMPROVEMENT
**Priority:** P2
**Affected Area:** Frontend

### Description
NFR-018 requires "Multi-language support." The Angular frontend has no i18n configuration. All UI text is hardcoded in English within component templates.

### Impact
- Cannot serve institutions in non-English regions.
- For SaaS: limits addressable market.

### Evidence
- No `@angular/localize` or `ngx-translate` dependency.
- No `.xlf`, `.json` translation files.
- All component templates use hardcoded English strings.

### Recommendation
Defer to Phase 2. For MVP, ensure all user-facing strings are in template files (not buried in TypeScript logic) so extraction is easy when i18n is implemented.

---

## Finding 36: No API Versioning (NFR-023)

**Category:** MISSING-IMPORTANT
**Priority:** P1
**Affected Area:** Backend / SaaS Architecture

### Description
NFR-023 requires API versioning. All endpoints use `/api/...` without a version prefix. Once external consumers (mobile apps, institution integrations) depend on these APIs, breaking changes will be costly.

### Impact
- Any API change could break existing frontends or third-party integrations.
- For SaaS: critical. Multiple clients may be on different frontend versions.

### Evidence
- All controllers use `@RequestMapping("/api/...")` — no `/api/v1/` prefix.
- No `ApiVersion` annotation or header-based versioning.

### Recommendation
Add `/api/v1/` prefix to all endpoints now, before external consumers exist. This is a one-time migration that prevents future pain.

---

## Finding 37: No GDPR Data Export Endpoint (NFR-024/026)

**Category:** MISSING-IMPORTANT
**Priority:** P2
**Affected Area:** Backend / Compliance

### Description
NFR-024 requires GDPR compliance. NFR-026 requires "right to data portability and deletion." The system has `POST /api/auth/delete-account` for account deletion, but:
- No endpoint to export a user's complete data (applications, documents, allocations, notifications).
- It is unclear whether `deleteAccount` cascades to MinIO files (document blobs).

### Impact
- Cannot respond to GDPR Subject Access Requests (SARs).
- Potential legal non-compliance in EU or SA POPIA jurisdictions.

### Evidence
- `AuthService.deleteAccount()` exists but cascade behavior to MinIO is unclear.
- No `GET /api/me/data-export` or similar endpoint.

### Recommendation
For MVP: verify that `deleteAccount` removes all personal data including MinIO files. Add `GET /api/me/data-export` that returns a JSON bundle of all user data. This is a regulatory requirement, not optional.

---

## NFR Compliance Matrix

| NFR | Requirement | Status | Finding |
|---|---|---|---|
| NFR-001 | 10,000 concurrent users | **UNKNOWN** — no load testing, HikariCP pool=10 | — |
| NFR-002 | Page load < 3 seconds | **LIKELY OK** — pagination implemented | — |
| NFR-003 | Upload < 30 seconds for 10MB | **OK** — MinIO direct upload | — |
| NFR-004 | DB queries < 2 seconds | **OK** — optimized fetch joins, pagination | — |
| NFR-005 | HTTPS encryption | **DEPLOYMENT** — not app-level concern | — |
| NFR-006 | BCrypt passwords | **OK** — `BCryptPasswordEncoder(10)` | — |
| NFR-007 | JWT expiry after 10 hours | **OK** — configurable expiration | — |
| NFR-008 | XSS/CSRF/SQLi protection | **PARTIAL** — no rate limiting | [#32](#finding-32-no-rate-limiting-anywhere-nfr-008) |
| NFR-009 | Malware scanning | **NO** | [#33](#finding-33-no-virus--malware-scanning-on-file-uploads-nfr-009) |
| NFR-010 | Audit logs | **OK** — AOP-based `ActivityLogAspect` | — |
| NFR-015 | Mobile responsive | **PARTIAL** — styling scored 5.5/10 | — |
| NFR-016 | Material Design | **OK** — Angular Material throughout | — |
| NFR-017 | WCAG 2.1 AA | **NO** | [#34](#finding-34-no-wcag-21-aa-compliance-nfr-017) |
| NFR-018 | Multi-language | **NO** | [#35](#finding-35-no-internationalization--multi-language-nfr-018) |
| NFR-022 | RESTful APIs | **OK** — standard REST patterns | — |
| NFR-023 | API versioning | **NO** | [#36](#finding-36-no-api-versioning-nfr-023) |
| NFR-024 | GDPR compliance | **PARTIAL** — delete exists, export missing | [#37](#finding-37-no-gdpr-data-export-endpoint-nfr-024026) |
| NFR-025 | Encryption at rest/transit | **DEPLOYMENT** — PostgreSQL + HTTPS | — |
| NFR-026 | Data portability | **NO** — no export endpoint | [#37](#finding-37-no-gdpr-data-export-endpoint-nfr-024026) |
| NFR-027 | Processing audit trails | **OK** — `ActivityLog` + `AllocationAuditLog` | — |

## Summary

| # | Title | Category | Priority |
|---|---|---|---|
| 32 | No Rate Limiting | MISSING-CRITICAL | P0 |
| 33 | No Virus Scanning on Uploads | MISSING-IMPORTANT | P2 |
| 34 | No WCAG 2.1 AA Compliance | MISSING-IMPORTANT | P2 |
| 35 | No i18n / Multi-Language | IMPROVEMENT | P2 |
| 36 | No API Versioning | MISSING-IMPORTANT | P1 |
| 37 | No GDPR Data Export | MISSING-IMPORTANT | P2 |
