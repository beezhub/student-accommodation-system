# MVP Analysis — Master Index

This document is the entry point for the MVP readiness audit of the Student Accommodation Management System. The analysis covers 10 areas across workflow completeness, business rules, security, data model, UX, and more — producing 58 globally-numbered findings with concrete proposals and a prioritised action plan.

The system is designed as a **multi-tenant SaaS platform** serving multiple educational institutions.

---

## File Relationship Map

```mermaid
graph TD
    INDEX["00-INDEX.md\n(you are here)"]
    EXEC["01-executive-summary.md"]

    subgraph Analysis Files
        A01["01 Core Workflow\n(Findings 1–6)"]
        A02["02 Business Rules\n(Findings 7–13)"]
        A03["03 Role & Permissions\n(Findings 14–18)"]
        A04["04 Data Model\n(Findings 19–25)"]
        A05["05 Missing Features\n(Findings 26–31)"]
        A06["06 NFR vs Reality\n(Findings 32–37)"]
        A07["07 Security\n(Findings 38–42)"]
        A08["08 Error Handling\n(Findings 43–47)"]
        A09["09 UX Friction\n(Findings 48–52)"]
        A10["10 Contradictions\n(Findings 53–58)"]
    end

    subgraph Proposals
        P1["Corrected Application\nLifecycle"]
        P2["Data Model\nAdditions"]
        P3["New\nRequirements"]
    end

    subgraph Action Plan
        AP1["Critical Findings\n(P0 + P1)"]
        AP2["Top 10\nAction Items"]
        AP3["Deferred to\nPhase 2"]
    end

    subgraph Appendix
        REG["Finding Registry\n(all 58 findings)"]
    end

    INDEX --> EXEC
    INDEX --> A01 & A02 & A03 & A04 & A05
    INDEX --> A06 & A07 & A08 & A09 & A10

    A01 --> P1
    A01 --> P2
    A02 --> P3
    A04 --> P2
    A04 --> P3
    A05 --> P3
    A06 --> P3
    A07 --> P3
    A08 --> P3
    A10 --> P1
    A10 --> P2

    P1 --> AP2
    P2 --> AP2
    P3 --> AP2

    AP1 --> AP2
    AP2 --> AP3

    A01 & A02 & A03 & A04 & A05 --> REG
    A06 & A07 & A08 & A09 & A10 --> REG
```

---

## Table of Contents

### Overview

| File | Title | Summary |
|------|-------|---------|
| [00-INDEX.md](./00-INDEX.md) | Master Index | This file — entry point, file map, and finding counts. |
| [01-executive-summary.md](./01-executive-summary.md) | Executive Summary | MVP gap score, readiness radar, and critical findings overview. |

### Analysis (Findings 1–58)

| File | Title | Findings | Summary |
|------|-------|----------|---------|
| [01-core-workflow-completeness.md](./analysis/01-core-workflow-completeness.md) | Core Workflow Completeness | 1–6 | Missing academic year, offer acceptance, check-in/out, application periods, renewal, and post-allocation experience. |
| [02-business-rule-enforcement.md](./analysis/02-business-rule-enforcement.md) | Business Rule Enforcement | 7–13 | Weak duplicate check, no expiry task, untracked doc validity, gender bypass, unstructured special requirements, no first-year priority, contradictory "final" rule. |
| [03-role-permission-gaps.md](./analysis/03-role-permission-gaps.md) | Role & Permission Gaps | 14–18 | Empty Landlord role, no waiting list visibility, no bulk processing, no roommate info, confusing API identifiers. |
| [04-data-model-gaps.md](./analysis/04-data-model-gaps.md) | Data Model Gaps | 19–25 | Missing pricing, funding, lease, amenity, and accommodation type entities; missing floor number and notification preferences. |
| [05-missing-mvp-features.md](./analysis/05-missing-mvp-features.md) | Missing MVP Features | 26–31 | No bulk import, no broadcast announcements, no funding export, no support tickets, minimal reporting, no SaaS onboarding. |
| [06-nfr-vs-reality.md](./analysis/06-nfr-vs-reality.md) | NFR vs Reality | 32–37 | No rate limiting, no virus scanning, no WCAG compliance, no i18n, no API versioning, no GDPR export. |
| [07-security-concerns.md](./analysis/07-security-concerns.md) | Security Concerns | 38–42 | Error leakage, role hierarchy risk, no JWT revocation, no content-type validation, MinIO cleanup gap on deletion. |
| [08-error-handling-edge-cases.md](./analysis/08-error-handling-edge-cases.md) | Error Handling & Edge Cases | 43–47 | Unsafe institution/building/room deletion, zombie user state, email and MinIO failure handling. |
| [09-ux-workflow-friction.md](./analysis/09-ux-workflow-friction.md) | UX & Workflow Friction | 48–52 | Jargon status labels, no post-allocation guidance, unclear doc rejection, no room browsing, registration friction. |
| [10-contradictions.md](./analysis/10-contradictions.md) | Contradictions | 53–58 | Missing "Under Review" status, missing amenities, missing budget field, missing support tickets, unused Redis, unversioned APIs. |

### Proposals

| File | Title | Summary |
|------|-------|---------|
| [corrected-application-lifecycle.md](./proposals/corrected-application-lifecycle.md) | Corrected Application Lifecycle | Revised status enums, offer acceptance flow, check-in/out, expiry — full state diagram. |
| [data-model-additions.md](./proposals/data-model-additions.md) | Data Model Additions | 5 new entities (AcademicYear, RoomPricing, StudentFunding, LeaseAgreement, Amenity) + modifications to existing entities. |
| [new-requirements.md](./proposals/new-requirements.md) | New Requirements | 17 new FRs, 4 new BRs, 5 new NFRs, and 2 updated BRs — ready to merge into BUSINESS_REQUIREMENTS.md. |

### Action Plan

| File | Title | Summary |
|------|-------|---------|
| [critical-findings.md](./action-plan/critical-findings.md) | Critical Findings (P0 + P1) | All 36 findings that must be resolved before MVP launch, with acceptance criteria. |
| [top-10-action-items.md](./action-plan/top-10-action-items.md) | Top 10 Action Items | Ordered implementation plan with dependency graph, work descriptions, and acceptance criteria. |
| [deferred-to-phase-2.md](./action-plan/deferred-to-phase-2.md) | Deferred to Phase 2 | 18 items that can wait until after MVP launch, with workarounds and rationale. |

### Appendix

| File | Title | Summary |
|------|-------|---------|
| [finding-registry.md](./appendix/finding-registry.md) | Finding Registry | Single flat table of all 58 findings with category, priority, and links to source and proposal files. |

---

## Finding Counts

### By Category

| Category | Count | Description |
|----------|-------|-------------|
| DEFECT | 9 | Existing code or logic that is incorrect |
| MISSING-CRITICAL | 8 | Absent functionality that blocks MVP launch |
| MISSING-IMPORTANT | 22 | Absent functionality needed before UAT |
| IMPROVEMENT | 7 | Working features that need enhancement |
| CONTRADICTION | 8 | Requirements doc says X, implementation does Y |
| **Total** | **54** | *4 findings appear under CONTRADICTION that overlap with MISSING/DEFECT categories* |

> Note: 58 unique findings total. The counts above reflect the primary category assignment per finding.

### By Priority

| Priority | Count | Meaning |
|----------|-------|---------|
| P0 — Showstopper | 8 | MVP cannot ship without this |
| P1 — Critical | 28 | Must fix before UAT |
| P2 — Important | 22 | Should fix, but has workaround |
| **Total** | **58** | |

### By Category x Priority

| Category | P0 | P1 | P2 | Total |
|----------|----|----|-----|-------|
| DEFECT | 1 | 8 | 0 | **9** |
| MISSING-CRITICAL | 7 | 1 | 0 | **8** |
| MISSING-IMPORTANT | 0 | 14 | 8 | **22** |
| IMPROVEMENT | 0 | 1 | 6 | **7** |
| CONTRADICTION | 0 | 4 | 4 | **8** |
| **Total** | **8** | **28** | **18** | **54** |
