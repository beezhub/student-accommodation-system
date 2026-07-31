# Recommendations: Feature Roadmap & Pricing Tier Strategy

## Executive Summary

1. **DigsConnect and Varsity Vibe are not SaaS competitors** — DigsConnect is a marketplace (R1,950 finder's fee), Varsity Vibe is a social media brand. The real competitive forces are the NSFAS Student Connect portal (government), Livinsoft (international, no SA localization), and spreadsheets/WhatsApp (the actual incumbent).

2. **NSFAS integration is the single highest-value differentiator** in the SA market. The NSFAS portal charges 5% of monthly revenue AND a per-bed registration fee. Building owners are already paying for a management platform — they just don't love it. A superior private alternative that also handles non-NSFAS students wins.

3. **The platform's current strength (application-to-allocation lifecycle) is table stakes**, not a differentiator. Owners won't pay for allocation alone. The missing financial, lease, and maintenance modules are the *actual* value proposition.

4. **The recommended build order is: Financials > Leases > NSFAS > Maintenance > Reporting** — this sequence maximizes owner value at each release while building toward the full platform.

5. **Three pricing tiers** map naturally to owner segments: Starter (small owners, allocation + basic financials), Professional (mid-scale, full operations), Enterprise (portfolio operators, cross-building analytics + NSFAS automation).

---

## Confidence Levels

| Recommendation | Confidence | Evidence Source |
|---|---|---|
| DigsConnect is marketplace, not SaaS competitor | **Strong** | Direct platform analysis |
| NSFAS integration is highest-value differentiator | **Moderate** | NSFAS portal analysis + market structure inference. Needs interview validation (A5). |
| Financials are #1 missing capability | **Moderate** | Industry pattern (every property management SaaS leads with payments). Needs interview validation (A2). |
| Proposed IA restructure | **Emerging** | Based on competitive patterns + codebase analysis. Needs owner feedback to validate navigation model. |
| Pricing tier structure | **Emerging** | Professional judgment based on SaaS patterns. Needs willingness-to-pay data from interviews (Q4.1-Q4.3). |

---

## Feature Roadmap: Recommended Build Sequence

### Quarter 1: Financial Foundation + Lease Management

**Theme:** "Owners can see their money"

| Feature | Scope | Entities/APIs Needed | Estimated Complexity |
|---|---|---|---|
| **Invoice Generation** | Create, send, track invoices per student per month | `Invoice`, `InvoiceLineItem`, `PaymentRecord` entities; `InvoiceController`; PDF generation | High |
| **Rent Tracking Dashboard** | View who has paid, who is in arrears, by building | `PaymentStatus` on allocation/lease; dashboard aggregation queries | Medium |
| **Lease Creation** | Generate lease from allocation, capture terms, store as document | `Lease` entity linking `RoomAllocation` + `Student` + terms; `LeaseController` | High |
| **Lease Lifecycle** | Active → expiring → renewed → terminated states | State machine on `Lease`; scheduled expiry notifications | Medium |
| **Basic Financial Report** | Monthly income summary per building, arrears aging | Reporting queries on `Invoice` + `PaymentRecord` | Medium |
| **Wire up AllocationRule** | Expose existing entity via REST + frontend | `AllocationRuleController`; admin UI component | Low |
| **Wire up IntakePeriod** | Expose existing entity via REST + frontend | `IntakePeriodController`; admin UI component | Low |

### Quarter 2: NSFAS Integration + Maintenance + Occupancy

**Theme:** "Owners can run their business from one place"

| Feature | Scope | Entities/APIs Needed | Estimated Complexity |
|---|---|---|---|
| **NSFAS Invoice Format** | Generate invoices matching NSFAS submission requirements | Template engine for NSFAS-specific format; `NsfasInvoiceService` | High |
| **NSFAS Payout Reconciliation** | Import NSFAS payment data, match to invoices, flag discrepancies | `NsfasPayment` entity; CSV/API import; reconciliation logic | High |
| **NSFAS Accreditation Tracker** | Track accreditation status, required documents, inspection dates per building | `AccreditationStatus` on `Building`; compliance checklist | Medium |
| **Maintenance Request System** | Students submit, admins assign, track to resolution, log costs | `MaintenanceRequest`, `MaintenanceAssignment` entities; student-facing form; admin queue | High |
| **Occupancy Dashboard** | Real-time occupancy by building, floor, room type; historical trends | Aggregation queries on `RoomAllocation` + `Room`; time-series data | Medium |
| **Building Switcher** | Top-bar building selector for multi-building owners | Frontend navigation component; scoped API queries | Medium |
| **Portfolio Analytics** | Cross-building comparison (occupancy, revenue, maintenance costs) | Aggregate reporting across buildings for tenant | Medium |

---

## Pricing Tier Strategy (Pending Interview Validation)

**Important caveat:** These tiers are based on competitive analysis and professional judgment. The interview guide (Q4.1-Q4.3) is designed to validate willingness-to-pay and preferred pricing model. Do NOT finalize pricing until interviews are complete.

### Pricing Model Options

| Model | Pros | Cons | Competitor Precedent |
|---|---|---|---|
| **Per-bed/month** | Scales with property size; fair for small owners | Complex for mixed-use buildings | NSFAS: R100-200/bed (once-off) |
| **% of revenue** | Aligns incentives; owners pay more when they earn more | Requires revenue visibility; owners may resist | NSFAS: 5% of monthly revenue |
| **Flat monthly + per-building** | Predictable; simple | May overprice small owners or underprice large ones | Livinsoft: ~US$1/user/month |
| **Tiered flat fee** | Clear value proposition per tier; SaaS standard | Requires clear tier boundaries | Most global PMS tools |

**Recommended:** Tiered flat fee with per-building add-on (e.g., base fee includes 1 building, R X per additional building). This is the SaaS standard, simplest to communicate, and allows owners to self-select into the right tier.

### Proposed Tiers

| Tier | Target Segment | Included Features | Differentiator |
|---|---|---|---|
| **Starter** | Small owner, 1-2 buildings, <50 beds | Applications, allocations, document verification, basic invoicing, basic occupancy stats, 1 building | Replaces spreadsheets |
| **Professional** | Mid-scale, 2-5 buildings, 50-200 beds | Everything in Starter + full lease management, maintenance tracking, financial reports, NSFAS invoice generation, 3 buildings included | Replaces NSFAS portal as primary tool |
| **Enterprise** | Portfolio operator, 5+ buildings, 200+ beds | Everything in Professional + NSFAS reconciliation automation, portfolio analytics, cross-building comparison, custom reports, API access, unlimited buildings, priority support | Full property management OS |

---

## What NOT to Build (Based on Current Evidence)

| Feature | Why Not Now |
|---|---|
| **Visitor management / access control** | Hardware dependency; different buyer; not core to accommodation management software |
| **Inventory / asset tracking** | Nice-to-have; no competitive pressure; owners don't switch platforms for this |
| **Student social features** | Distraction from owner value proposition; DigsConnect already owns the student side |
| **Mobile app (native)** | Responsive web is sufficient until feature set is proven; native app is a Q3+ investment |

---

## Next Steps

| # | Action | Owner | Timeline |
|---|---|---|---|
| 1 | **Recruit 6-8 building owners** using the screener in `03-research-plan-and-interview-guide.md` | Product/Growth team | Week 1 |
| 2 | **Conduct interviews** using the discussion guide | UX researcher | Weeks 2-3 |
| 3 | **Analyze and synthesize** — code interviews, map to JTBD, validate/invalidate assumptions A1-A7 | UX researcher | Week 3-4 |
| 4 | **Update this roadmap** based on interview findings — especially priority order and pricing model | Product team | Week 4 |
| 5 | **Begin Q1 build** — financial entities, invoice generation, lease management | Engineering team | Week 5+ |
| 6 | **Follow-up survey** (n=50+) to quantify priority rankings and willingness-to-pay across segments | UX researcher | Week 6-8 |

---

## Counter-Evidence & Limitations

1. **We have not spoken to a single building owner yet.** All recommendations are based on competitive analysis, codebase audit, and professional judgment. Confidence levels reflect this. The interview guide is designed to validate or invalidate these assumptions.

2. **NSFAS importance may be overstated** if the target market skews toward premium private-only residences (Thrive Student Living-type properties). The interviews must include at least one non-NSFAS operator.

3. **Willingness to pay is unknown.** South African accommodation owners may have very different price sensitivity than global SaaS benchmarks suggest. The R1,950 DigsConnect fee and NSFAS's 5% revenue model provide local anchors but are different value propositions.

4. **The competitive landscape was partially opaque.** Several DigsConnect pages returned 500/403 errors during scraping. There may be landlord dashboard features behind login that we couldn't observe.

5. **Livinsoft's actual feature depth is unknown** — only Capterra listing data was available, not a hands-on evaluation. A trial account would provide better competitive intelligence.
