# Research Plan: Building Owner Discovery Study

## Objectives

1. **Validate or invalidate** the priority ranking of missing capabilities (financials, leases, NSFAS, maintenance, occupancy)
2. **Understand current workflows** — how do building owners currently manage operations without integrated software?
3. **Identify willingness to pay** and feature-to-tier mapping for pricing decisions
4. **Discover unmet needs** not on our assumption list

## Method Selection

| Method | Purpose | Why This Method |
|---|---|---|
| **Semi-structured interviews** (Primary) | Behavioral understanding of owner workflows, pain points, willingness to pay | "Why" questions require qualitative depth. Surveys would measure frequency but not motivation. |
| **Competitive UX audit** (Secondary) | Benchmark against NSFAS portal and Livinsoft | Heuristic evaluation of competing admin experiences to identify usability gaps |
| **Jobs-to-Be-Done mapping** (Synthesis) | Connect owner needs to product capabilities | JTBD framework maps "hire" decisions to specific platform features |

**Triangulation:** Interviews (attitudinal) + competitive audit (evaluative) + JTBD mapping (generative) = three complementary lenses.

## Participant Criteria

### Must-Have
- Owns or manages private student accommodation in South Africa
- Currently has students living in the property (not vacant/planned)
- Makes or influences technology purchase decisions for the property

### Segment Quotas (Minimum 6, Target 8)
| Segment | Target | Rationale |
|---|---|---|
| Small owner (1-2 buildings, <80 beds) | 3 | Majority of market |
| Mid/large operator (3+ buildings, 100+ beds) | 2 | High-value segment, different needs |
| NSFAS-accredited provider | 2 (can overlap with above) | Critical for NSFAS feature validation |
| Non-NSFAS private-only provider | 1 (can overlap) | Control group for NSFAS assumption |

### Disqualification Criteria
- University-owned/managed residences (different governance model)
- Currently using BeezhHub (biased toward confirming existing features)
- Property management company employees who don't make purchase decisions

## Sample Size Rationale

6-8 participants across 2-3 segments. Per Nielsen's diminishing returns research, 5 participants per segment surface ~80% of usability issues. For attitudinal research with heterogeneous populations (different building sizes, NSFAS vs private), we need at least 2-3 per segment to detect pattern vs outlier. 8 total balances insight quality against recruitment feasibility.

**Limitation:** This sample cannot quantify market-wide preferences. Findings will be directional. A follow-up survey (n=50+) would be needed to validate frequency/priority rankings.

## Timeline

| Week | Activity |
|---|---|
| 1 | Recruit participants (screener below), schedule sessions |
| 2-3 | Conduct 6-8 interviews (60 min each, remote via Zoom/Teams) |
| 3-4 | Analysis: coding, theming, JTBD mapping |
| 4 | Findings report + roadmap recommendations |

## Risk / Limitations

1. **Recruitment bias:** Owners willing to do interviews may be more tech-savvy than average — findings may overindex on sophisticated needs
2. **Social desirability:** Owners may overstate willingness to pay in interviews — validate with behavioral anchoring ("what do you currently pay for X?")
3. **Small sample:** Cannot generalize to market — label all findings with confidence qualifiers
4. **No direct observation:** Remote interviews capture self-reported behavior, not actual behavior — recommend follow-up contextual inquiry for top 2-3 owners

---

# Recruitment Screener

## Screening Questions

**S1.** Do you own or manage private student accommodation in South Africa?
- Yes → Continue
- No → Disqualify

**S2.** How many student accommodation buildings do you currently manage?
- 1-2 → Tag: Small owner
- 3-10 → Tag: Mid operator
- 11+ → Tag: Portfolio operator

**S3.** Approximately how many student beds do you manage in total?
- [Open numeric field] → Record for segmentation

**S4.** Are any of your properties NSFAS-accredited?
- Yes, all → Tag: NSFAS provider
- Yes, some → Tag: Mixed provider
- No → Tag: Private-only
- Not sure → Flag for follow-up

**S5.** What tools do you currently use to manage your accommodation business? (Select all that apply)
- Spreadsheets (Excel/Google Sheets)
- WhatsApp groups
- Paper-based records
- Property management software (specify: _______)
- NSFAS Student Connect portal
- Accounting software (specify: _______)
- Other: _______
- None — I manage everything manually

**S6.** Are you the person who makes or influences decisions about which software/tools to purchase for the business?
- Yes, I make the final decision → Continue
- Yes, I influence the decision → Continue
- No, someone else decides → Disqualify

**S7.** Would you be available for a 60-minute remote interview in the next 2 weeks?
- Yes → Schedule
- No → Waitlist

---

# Interview Discussion Guide

**Session Duration:** 60 minutes
**Format:** Remote video call (Zoom/Teams)
**Recording:** Audio + video with participant consent

---

## Section 1: Warm-Up & Context (8 min)

**Goal:** Build rapport, understand their business at a high level.

**Q1.1** Tell me a bit about your accommodation business — how many buildings, where are they, how long have you been doing this?

**Q1.2** Walk me through a typical week for you as a building owner/manager. What takes up most of your time?
- *Probe:* What's the most tedious part of your week?
- *Probe:* What tasks do you wish you could hand off to someone or something?

**Q1.3** How many people help you manage the properties? What are their roles?
- *Probe:* Do any of them have access to any management systems, or is it mostly you?

---

## Section 2: Current Workflow Deep Dive (20 min)

**Goal:** Map actual behavior across the domains we're investigating. Behavioral anchoring — ask about last time, not hypotheticals.

### Financial Management

**Q2.1** Tell me about the last time you dealt with a late rent payment. What happened, step by step?
- *Probe:* How did you find out the payment was late?
- *Probe:* How do you track who has paid and who hasn't?
- *Probe:* What tools or systems are involved?

**Q2.2** How do you currently generate invoices for students (or for NSFAS)?
- *Probe:* What format? How often?
- *Probe:* How long does invoicing take you each month?

**Q2.3** [If NSFAS-accredited] Walk me through your last NSFAS payout cycle. How do you reconcile what NSFAS paid versus what was owed?
- *Probe:* Have you ever had discrepancies? What happened?
- *Probe:* How much time do you spend on NSFAS-related admin each month?

### Lease Management

**Q2.4** When a new student moves in, what does the lease process look like?
- *Probe:* Is it paper or digital?
- *Probe:* How do you handle lease renewals at the end of the year?
- *Probe:* What happens when a student needs to leave early?

### Maintenance

**Q2.5** Think about the last maintenance issue a student reported to you. How did that reach you, and what happened next?
- *Probe:* How do students report issues — phone, WhatsApp, in person?
- *Probe:* How do you track whether the issue was resolved?
- *Probe:* Do you track maintenance costs? How?

### Occupancy Tracking

**Q2.6** If I asked you right now, "What percentage of your beds are occupied?" — how quickly could you give me that number?
- *Probe:* How do you track move-ins and move-outs?
- *Probe:* Do you forecast occupancy for next semester/year?

---

## Section 3: Pain Points & Prioritization (15 min)

**Goal:** Identify highest-pain areas and relative priority.

**Q3.1** Of everything we've discussed — finances, leases, maintenance, occupancy, NSFAS — which one causes you the most stress or costs you the most time?
- *Probe:* Can you give me a specific example of when this went really wrong?
- *Probe:* What would it mean for your business if this area was handled perfectly?

**Q3.2** I'm going to read a list of capabilities. For each one, tell me: is this something you desperately need, would be nice to have, or doesn't matter much to you?
1. Automated rent invoicing and payment tracking
2. NSFAS invoice generation and payout reconciliation
3. Digital lease agreements with e-signatures
4. Maintenance request tracking (students submit, you assign, track resolution)
5. Real-time occupancy dashboard across all your buildings
6. Financial reports (income statements, arrears aging, revenue by building)
7. Student communication (announcements, messaging)
8. Move-in/move-out room inspections with photo evidence
9. Document management for compliance (B-BBEE, NSFAS accreditation docs)

**Q3.3** If you could only have THREE of those nine, which three would you choose? Why those?

---

## Section 4: Willingness to Pay & Switching (10 min)

**Goal:** Validate pricing assumptions and understand switching behavior.

**Q4.1** What software or tools do you currently pay for to manage your accommodation business?
- *Probe:* How much per month, roughly?
- *Probe:* What do you get for that money? Is it worth it?

**Q4.2** If a platform existed that handled [their top 3 from Q3.3], what would that be worth to you per month?
- *Probe:* Would you think about it as a fixed monthly fee, or as a percentage of revenue, or per bed?
- *Probe:* What would make you NOT sign up, even if the features were right?

**Q4.3** Have you tried and stopped using any property management tools? What happened?
- *Probe:* What made you abandon it?

---

## Section 5: Cool-Down & Open (7 min)

**Q5.1** Is there anything about managing your accommodation business that we haven't discussed but that keeps you up at night?

**Q5.2** If you could wave a magic wand and have one thing about your daily operations just *work* — what would it be?

**Q5.3** Is there anything else you'd like to share that might help us build better tools for accommodation owners like you?

---

## Moderator Notes

- **If the participant goes on tangents about student behavior issues:** Gently redirect with "That's really interesting context. Coming back to the management side of things..."
- **If the participant has never used any software:** Spend more time on Section 2 (understanding manual workflows) and less on Section 4 (switching behavior). These participants reveal the "job to be done" most clearly.
- **If the participant gets emotional about NSFAS:** Let them vent briefly (30 sec), validate ("That sounds really frustrating"), then refocus on the specific workflow: "Can you walk me through exactly what happens when...?"
- **Participant distress:** If a participant becomes visibly upset about financial or business difficulties, offer to pause or skip the topic. Research ethics > data completeness.
