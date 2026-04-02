# Research Plan: Student Onboarding & Post-Submission Experience

**Project:** BeeZhub Student Accommodation Platform
**Researcher:** Prism (UX Research Agent)
**Date:** 2026-04-02
**Status:** Phase 1 — Research Plan
**Depends on:** [Research Brief v1](./student-onboarding_research-brief_v1.md)

---

## 1. Research Objectives

| # | Objective | Maps to Research Question |
|---|---|---|
| O1 | Identify what property information students need *before* creating an account to convert from browser to applicant | RQ1 |
| O2 | Map friction points in the current onboarding wizard through expert heuristic evaluation | RQ2 |
| O3 | Determine industry-standard trust signals for private (non-university) accommodation platforms | RQ4 |
| O4 | Establish which post-submission transparency features are table-stakes vs. differentiators | RQ3, RQ5 |
| O5 | Assess information architecture and progressive disclosure patterns across competing platforms | RQ6 |
| O6 | Identify communication models (chat, messaging, community) used by competitors and their apparent maturity | RQ7 |

---

## 2. Methods & Rationale

Given the constraints (no user access, 2–3 weeks, no budget), three complementary methods are feasible. Using only one would produce fragile, single-perspective conclusions.

### Method 1: Expert Heuristic Evaluation

**What:** Systematic evaluation of BeeZhub's current student-facing flows against established usability heuristics.

**Why this method:** Heuristic evaluation is the highest-value method when no users are available. It surfaces likely usability issues grounded in decades of interface design research. It does *not* surface user preferences — that limitation is accepted and documented.

**Heuristic Framework:** Nielsen's 10 Usability Heuristics (1994), supplemented by:
- Shneiderman's 8 Golden Rules (for form design evaluation)
- Tidwell's "Designing Interfaces" patterns (for wizard flow evaluation)
- WebAIM WCAG 2.1 AA (for accessibility baseline)

**Scope:** The following flows will be evaluated:
1. Home page → Building browse → Building detail (pre-auth conversion funnel)
2. Signup → Email verification → Profile setup → Application → Document upload → Review (onboarding wizard)
3. Dashboard → Application status tracking → Offer accept/decline (post-submission experience)
4. Notification system (bell dropdown, notification types, actions)

**Evaluators:** Single expert evaluator (Prism). Acknowledged limitation: Nielsen (1994) recommends 3–5 independent evaluators for coverage. A single evaluator typically finds 35% of usability issues. This is offset by deep codebase access (we can inspect implementation, not just UI surface).

**Output:** Severity-rated findings using Nielsen's 0–4 scale:
- 0: Not a usability problem
- 1: Cosmetic problem only
- 2: Minor usability problem
- 3: Major usability problem — important to fix
- 4: Usability catastrophe — imperative to fix

### Method 2: Competitive Audit

**What:** Structured analysis of 6 competitor platforms, examining specific UX dimensions.

**Why this method:** Competitive analysis reveals industry norms and expectations. When students use BeeZhub, they bring expectations from other platforms. Understanding those baselines helps identify where BeeZhub is below standard (risk), at standard (parity), or above standard (differentiator).

**Competitor Set:**

| Platform | Type | Region | Why Selected |
|---|---|---|---|
| **Amber Student** (amberstudent.com) | Private marketplace | Global (1M+ beds) | Largest private student accommodation marketplace. Gold standard for pre-auth browsing, pricing transparency, and trust signals. |
| **Student.com** | Private marketplace | Global (2M+ beds, 400+ cities) | Major competitor with strong university partnerships. Relevant for trust-building via institutional association. |
| **HousingAnywhere** | Private rental platform | Europe + Global (400+ cities) | Strong trust model (Tenant Protection, escrow payments). Relevant for trust/safety pattern design. |
| **DigsConnect** (digsconnect.com) | Private marketplace | South Africa | Direct regional competitor. Most relevant for local student expectations and SA market norms. |
| **Varsity Vibe** (varsityvibe.co.za) | Private marketplace | South Africa | Regional competitor. SA-specific design patterns and feature expectations. |
| **Campus Living Villages** (mystudentvillage.com) | Operator (private) | AUS/UK/US | Non-university operator with strong brand trust. Relevant for how private operators build credibility without university branding. |

**Audit Dimensions:**

| Dimension | What We're Evaluating | Maps to Objective |
|---|---|---|
| **Pre-auth information density** | What property info is shown without login: pricing, rooms, amenities, availability, reviews, map | O1 |
| **Conversion funnel design** | How platforms move users from browsing to signup: CTAs, progressive commitment, intermediate engagement | O1, O5 |
| **Trust signals** | What trust elements appear: reviews/ratings, partner logos, security badges, Trustpilot, guarantees, "Featured in" press, verified listings | O3 |
| **Onboarding flow** | Number of steps, information requested at each step, progressive disclosure, back navigation, save-and-resume | O2, O5 |
| **Post-booking transparency** | Application/booking status tracking, timeline visibility, what students can see about their application state | O4 |
| **Communication channels** | In-app messaging, chat with landlord/admin, WhatsApp integration, support channels, community features | O6 |
| **Waitlist handling** | Whether waitlists exist, position visibility, ETA, alternative suggestions | O4 |

**Output:** Comparative matrix with screenshots/evidence, pattern frequency analysis, and gap identification.

### Method 3: Secondary Research Synthesis

**What:** Synthesis of published UX research, design pattern libraries, and behavioral science literature relevant to the three decision areas.

**Why this method:** Compensates for the lack of primary user research by drawing on published findings from researchers who *have* studied these populations. Evidence quality varies — peer-reviewed > industry reports > blog posts.

**Source Categories:**

| Category | Example Sources | Expected Contribution |
|---|---|---|
| **Nielsen Norman Group (NN/g)** | Articles on form design, wizard patterns, trust signals, progressive disclosure, mobile usability | Heuristic grounding for recommendations |
| **Baymard Institute** | Checkout/signup usability research (transferable to accommodation applications) | Quantitative benchmarks for form completion rates |
| **Academic literature** | Trust formation in online marketplaces (Gefen et al.), technology adoption in student populations | Theoretical grounding for trust signal recommendations |
| **Industry reports** | Student housing market reports, proptech UX benchmarks | Market context and user expectations |
| **Design pattern libraries** | UI-patterns.com, Mobbin, Page Flows | Implementation examples for recommended patterns |

**Output:** Annotated bibliography organized by theme, with confidence ratings per source (peer-reviewed, industry, anecdotal).

---

## 3. Timeline

| Week | Activities | Deliverables |
|---|---|---|
| **Week 1 (Days 1–5)** | Complete heuristic evaluation of all 4 scoped flows. Begin competitive audit (3 of 6 platforms). | Heuristic Evaluation Report (draft) |
| **Week 2 (Days 6–10)** | Complete competitive audit (remaining 3 platforms). Conduct secondary research synthesis. Cross-reference findings. | Competitive Audit Matrix. Secondary Research Summary. |
| **Week 3 (Days 11–14)** | Synthesize all three methods into unified findings. Prioritize recommendations. Prepare stakeholder deliverables. | Final Findings Report. Prioritized Recommendations. Stakeholder Brief. |

---

## 4. Synthesis Approach

Findings from all three methods will be triangulated using an **Opportunity-Solution Tree** framework:

```
                    Opportunity
                 (User need / pain point)
                /           |            \
     Evidence:          Evidence:        Evidence:
     Heuristic          Competitive      Secondary
     Evaluation         Audit            Research
        \                |               /
         \               |              /
          Confidence Rating (Strong / Moderate / Emerging)
                         |
                    Solution Space
                 (Recommendations)
                /        |        \
           Quick       Medium     Strategic
           Wins        Term       Initiatives
```

**Confidence rating rules:**
- **Strong** — Corroborated by 2+ methods, or by heuristic evaluation + strong secondary research
- **Moderate** — Supported by 1 method + directional evidence from another
- **Emerging** — Identified by 1 method only; flagged for future user validation

---

## 5. Risks & Limitations

| Risk | Severity | Mitigation |
|---|---|---|
| **Single-evaluator heuristic bias** | High | Document reasoning chain per finding. Apply structured heuristic framework (not ad hoc opinion). Acknowledge 35% coverage ceiling. |
| **Competitive audit surface-level limitation** | Medium | We can only evaluate what's publicly visible. Post-login experiences of competitors are inaccessible without creating accounts. |
| **No user validation** | High | Every finding will be labeled by source type. No finding will be presented as "what users want" — only "what expert assessment and competitive analysis suggest." |
| **South African competitor opacity** | Medium | DigsConnect and Varsity Vibe use JS-rendered SPAs that may limit automated content extraction. Manual analysis may be needed. |
| **Recency bias in secondary research** | Low | Prioritize sources from 2022–2026. Flag older sources where patterns may have shifted. |
| **Survivorship bias in competitive audit** | Medium | We're studying successful platforms. Their patterns may not be successful *because of* those patterns. Flag this in recommendations. |

---

## 6. Output Artifacts

| Artifact | Audience | Format |
|---|---|---|
| Heuristic Evaluation Report | Engineering, Design | Severity-rated finding cards with screenshots, heuristic violated, recommendation |
| Competitive Audit Matrix | PM, Design, Leadership | Comparative table with evidence links |
| Secondary Research Summary | All | Annotated themes with source credibility ratings |
| **Findings Report** (primary deliverable) | All stakeholders | Executive summary + detailed findings + prioritized recommendations |
| Stakeholder Brief: Engineering | Engineering | Behavioral requirements, error recovery patterns, performance expectations |
| Stakeholder Brief: Design | Design | Design implications, pattern recommendations, accessibility requirements |
| Stakeholder Brief: PM/Leadership | PM, Leadership | Decision brief — what to do, why, what happens if we don't |

---

## 7. Success Criteria

This research is successful if it:
1. Produces at least 3 **Strong-confidence** findings that directly inform a design decision
2. Identifies the top 5 friction points in the current onboarding flow with severity ratings
3. Provides a clear competitive baseline for pre-auth information density, trust signals, and post-submission features
4. Generates a prioritized recommendation list that engineering/design can action in the next sprint cycle
5. Explicitly documents what we *don't know* and what requires user research to validate

---

## Next Step

→ **Phase 2: Research Instruments** — Heuristic evaluation rubric, competitive audit scorecard, and secondary research protocol.
