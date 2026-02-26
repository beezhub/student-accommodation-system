# ADR-007: HttpOnly Cookie JWT Storage Evaluation

- **Status:** Decision — Defer to Phase 4
- **Date:** 2026-02-26
- **Task:** TASK-3.12
- **Architecture Reference:** [ARCH 2.7 SEC-006], [ARCH 3.5]
- **Requirement Reference:** [REQ AUTH-001]
- **Deciders:** Tech Lead, Backend Lead, Frontend Lead

---

## Context

JWT tokens are currently stored in `localStorage` via `TokenStorageService`
(`frontend/src/app/core/services/token-storage.service.ts`). The token is
injected into every API request by `AuthInterceptor` as a `Bearer` header.

The `currentUser` object (which also contains the token) is additionally
persisted to `localStorage` by `AuthService`.

The classification "Too Risky to Touch Now" was applied during the Phase 1 MVP
because:

1. Single-tenant, single building owner — XSS attack surface was small.
2. Implementation required coordinated backend + frontend changes.
3. Time pressure prioritised feature delivery over hardening.

Multi-tenant / multi-building-owner is now live (Phase 2 complete). The XSS
attack surface has increased: a compromised Angular module (e.g. a malicious
third-party npm package) now has access to tokens for all tenants served from
the same origin.

---

## Decision Drivers

| Driver | Weight |
|--------|--------|
| XSS resilience (tokens inaccessible to JavaScript) | High |
| CSRF complexity introduced by cookie approach | High |
| Migration scope vs. current team velocity | Medium |
| Existing CSP header coverage | Medium |
| Current customer scale (number of active tenants) | Low |

---

## Options Considered

### Option A — Migrate to HttpOnly SameSite=Strict cookies now

**Backend changes required:**

1. New `/api/auth/token` cookie endpoint that sets:
   ```
   Set-Cookie: jwt=<token>; HttpOnly; Secure; SameSite=Strict; Path=/api
   ```
2. CSRF must be re-enabled in `SecurityConfig` (currently `csrf(AbstractHttpConfigurer::disable)`).
3. A CSRF token endpoint (e.g. `/api/auth/csrf`) must be added to provide the
   Angular app with a CSRF token on page load.
4. The `/api/auth/logout` endpoint must invalidate the cookie (set `Max-Age=0`).
5. Both `JWT_EXPIRATION` and `JWT_ADMIN_EXPIRATION` remain unchanged; the cookie
   expiry mirrors the JWT claim `exp`.

**Frontend changes required:**

1. `TokenStorageService` — remove all `localStorage` reads/writes; token is now
   sent automatically by the browser via the cookie.
2. `AuthInterceptor` — remove `Authorization: Bearer` header injection; instead
   attach the CSRF token header on every mutating request.
3. `AuthService` — remove `localStorage.setItem('currentUser', ...)` for the
   token field; retain user metadata (name, role) in memory / `sessionStorage`.
4. `ErrorInterceptor` / `TokenExpiryInterceptor` — adapt to handle 401 responses
   from cookie expiry (no change in semantics, but `localStorage.removeItem`
   calls must be removed).
5. All four Angular auth guards remain logically identical; they check role, not
   token presence.
6. Logout flow must call the backend endpoint to clear the cookie server-side.

**Estimate:** 8–12 hours implementation + 4 hours integration testing.

**Pros:**
- Tokens completely inaccessible to JavaScript — eliminates XSS token theft.
- Industry standard for server-rendered and SPA authentication.

**Cons:**
- CSRF re-enablement adds complexity for every state-changing request.
- SameSite=Strict breaks OAuth redirect flows if added later.
- Mobile app clients (if added) cannot use HttpOnly cookies without custom
  handling.
- Requires coordinated backend + frontend release.

---

### Option B — Strengthen CSP headers and defer cookie migration to Phase 4

**Changes required now:**

1. Add a strict Content Security Policy header in `SecurityConfig`:
   ```
   Content-Security-Policy:
     default-src 'self';
     script-src 'self';
     object-src 'none';
     base-uri 'self';
     frame-ancestors 'none';
   ```
2. Add `X-Content-Type-Options: nosniff` and `X-Frame-Options: DENY` if not
   already set (verify via Spring Security headers defaults).
3. Document the residual XSS risk in this ADR and set Phase 4 trigger criteria.

**Estimate:** 1–2 hours.

**Pros:**
- Eliminates the most common XSS vector (inline script injection) without
  touching the auth flow.
- Preserves team velocity for Phase 3 feature delivery.
- Low risk of regression.

**Cons:**
- Token remains in `localStorage` — a malicious script that bypasses CSP can
  still read it.
- Defers the hardening work, accumulating security debt.

---

## Current Risk Assessment

| Factor | Current State | Risk Level |
|--------|--------------|------------|
| Inline script injection | No inline scripts in production bundle | Low |
| Third-party npm packages | 47 direct dependencies; audited in CI | Medium |
| CSP header | Not explicitly configured (Spring Security defaults: `X-Frame-Options`, `X-XSS-Protection` only) | **High** |
| Active tenants | < 10 building owners in production | Low |
| Token lifetime | Student: 10 h; Admin: 4 h | Medium |

The absence of a CSP header is the highest-risk finding. A strict CSP closes
the most realistic XSS vector independently of token storage location.

---

## Decision

**Defer full cookie migration to Phase 4.** Implement Option B immediately
(CSP hardening), which eliminates the primary attack vector at low cost.

**Rationale:**
- Current tenant scale does not justify the 12-hour coordinated migration.
- CSP hardening delivers 80% of the security improvement for 10% of the effort.
- The cookie migration plan is fully documented here; no research is needed when
  Phase 4 starts.

### Immediate action (Phase 3, this PR)

- Add strict CSP header in `SecurityConfig.java`:
  `Content-Security-Policy: default-src 'self'; script-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none';`

  > **Note:** This is tracked as a follow-up sub-task and should be implemented
  > in the same Phase 3 sprint. It is not blocked by this ADR — only the
  > full cookie migration is deferred.

### Phase 4 trigger criteria

Revisit the full cookie migration when **any** of the following conditions are
met:

1. Active tenant count exceeds **50 building owners**, OR
2. A penetration test or third-party security audit identifies a realistic XSS
   vector, OR
3. A mobile application client is added (re-evaluate SameSite strategy), OR
4. A compliance requirement (e.g. ISO 27001, SOC 2) mandates HttpOnly cookie
   storage explicitly.

---

## Migration Checklist (for Phase 4 implementer)

- [ ] Backend: add cookie-based `/api/auth/login` endpoint
- [ ] Backend: re-enable CSRF in `SecurityConfig`, add CSRF token endpoint
- [ ] Backend: add `/api/auth/logout` cookie-clearing endpoint
- [ ] Frontend: refactor `TokenStorageService` — remove `localStorage`
- [ ] Frontend: refactor `AuthInterceptor` — replace Bearer header with CSRF header
- [ ] Frontend: refactor `AuthService` — separate user metadata from token storage
- [ ] Frontend: update `ErrorInterceptor` and `TokenExpiryInterceptor`
- [ ] Test: `AuthServiceSpec.login_storesTokenInHttpOnlyCookie_notLocalStorage()`
- [ ] Test: `AuthInterceptorSpec.request_includesCsrfHeader_notBearerToken()`
- [ ] Integration test: full login → API call → logout flow
- [ ] Verify mobile clients (if any) handle cookie-based auth correctly

---

## Consequences

- **Positive:** CSP header reduces XSS risk immediately at low cost.
- **Positive:** Full migration plan is documented and ready for Phase 4.
- **Negative:** `localStorage` token storage remains in production until Phase 4.
- **Neutral:** No changes to existing auth tests required by this ADR.
