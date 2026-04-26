# Research Brief: Optimal Admin Experience for Private Student Accommodation Owners

**Project:** BeezhHub Student Accommodation SaaS Platform
**Researcher:** Prism (UX Research Agent)
**Date:** 2026-04-03
**Status:** Phase 0 Complete — Research Brief + Competitive Intelligence + Recommendations

---

## Decision Context

This research will inform the **feature roadmap and pricing tier structure** for the next two quarters. The core decision: what capabilities must the admin/owner experience include to justify a paid SaaS subscription for private student accommodation owners in South Africa?

## Stakeholder Assumptions to Test

| # | Assumption | Risk Level | Source |
|---|---|---|---|
| A1 | Building owners need occupancy tracking beyond basic stats | High | Product team hypothesis |
| A2 | Financial reporting / rent collection is a must-have | High | Product team hypothesis |
| A3 | Lease management (contracts, e-signatures) is expected | Medium | Industry pattern |
| A4 | Maintenance/work order tracking is high-value | Medium | Industry pattern |
| A5 | NSFAS reconciliation is a differentiator in the SA market | High | Market-specific hypothesis |
| A6 | DigsConnect and Varsity Vibe are the primary SaaS competitors | High | Needs validation |
| A7 | Multi-building owners need portfolio-level dashboards | Medium | Architecture hypothesis |

## Riskiest Assumptions

**A6 is already invalidated** — competitive research reveals DigsConnect is a marketplace (not a management SaaS) and Varsity Vibe is a social media brand (not a platform). The actual competitive landscape is different from what was assumed.

**A5 (NSFAS reconciliation)** and **A2 (financial reporting)** are the highest-risk assumptions because they would drive the most engineering investment and are hardest to reverse if wrong.

## Target User Segments

| Segment | Description | Est. Market Size |
|---|---|---|
| **Small owner** | 1-2 buildings, 20-80 beds, owner-operated | Large (majority of SA private accom market) |
| **Mid-scale operator** | 3-10 buildings, 100-500 beds, small management team | Medium |
| **Portfolio operator** | 10+ buildings, 500+ beds, professional management company | Small but high-value |
| **NSFAS-accredited provider** | Any size, registered on NSFAS Student Connect portal | Cross-cutting segment, ~60%+ of market |

## Constraints

- **Time:** Roadmap decisions needed within 2 quarters
- **Budget:** No dedicated research budget stated — leveraging competitive intelligence + structured interview plan
- **Access:** Need to recruit 6-8 building owners across segments (not yet recruited)
- **Technical:** Existing codebase is strong on application-to-allocation lifecycle but has zero financial, maintenance, or lease infrastructure

## Prior Research / Data

- No prior user research exists for the admin/owner persona
- Codebase audit completed (34 entities, 27 controllers, ~80 services) — all focused on application lifecycle
- Competitive intelligence gathered via web research (DigsConnect, NSFAS platform, Livinsoft, SA proptech landscape)
