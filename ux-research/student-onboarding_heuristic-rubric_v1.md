# Heuristic Evaluation Rubric: BeeZhub Student-Facing Experience

**Project:** BeeZhub Student Accommodation Platform
**Evaluator:** Prism (UX Research Agent)
**Date:** 2026-04-02
**Instrument Type:** Expert Heuristic Evaluation

---

## Evaluation Framework

### Primary Heuristics (Nielsen, 1994)

| ID | Heuristic | Definition |
|----|-----------|------------|
| H1 | Visibility of system status | The system should always keep users informed about what is going on, through appropriate feedback within reasonable time. |
| H2 | Match between system and real world | The system should speak the users' language, with words, phrases and concepts familiar to the user, rather than system-oriented terms. |
| H3 | User control and freedom | Users often choose system functions by mistake and will need a clearly marked "emergency exit" to leave the unwanted state. |
| H4 | Consistency and standards | Users should not have to wonder whether different words, situations, or actions mean the same thing. Follow platform conventions. |
| H5 | Error prevention | Even better than good error messages is a careful design which prevents a problem from occurring in the first place. |
| H6 | Recognition rather than recall | Minimize the user's memory load by making objects, actions, and options visible. |
| H7 | Flexibility and efficiency of use | Accelerators — unseen by the novice user — may speed up the interaction for the expert user. |
| H8 | Aesthetic and minimalist design | Dialogues should not contain information which is irrelevant or rarely needed. |
| H9 | Help users recognize, diagnose, and recover from errors | Error messages should be expressed in plain language, precisely indicate the problem, and constructively suggest a solution. |
| H10 | Help and documentation | Even though it is better if the system can be used without documentation, it may be necessary to provide help and documentation. |

### Supplementary Heuristics

| ID | Heuristic | Source | Applied To |
|----|-----------|--------|------------|
| S1 | Progressive disclosure | Tidwell (2020) | Wizard flow — information should be requested only when needed |
| S2 | Trust and credibility | Fogg (2003) | Home page, building detail, signup — elements that build platform trustworthiness |
| S3 | Appropriate information density | Shneiderman (2016) | Building browse/detail — showing enough information for decision-making |
| S4 | Reduce anxiety | Wroblewski (2008) | Post-submission — status visibility reduces user anxiety |
| S5 | Accessible by default | WCAG 2.1 AA | All flows — keyboard navigation, screen reader support, color contrast |

### Severity Scale

| Rating | Label | Definition | Priority |
|--------|-------|------------|----------|
| 0 | Not a problem | No usability issue identified | — |
| 1 | Cosmetic | Slight issue noticed, fix only if time allows | Low |
| 2 | Minor | Minor usability problem; fixing should be given low priority | Medium |
| 3 | Major | Major usability problem; important to fix, top priority | High |
| 4 | Catastrophe | Usability catastrophe; imperative to fix before release | Critical |

---

## Evaluation Scope

### Flow 1: Pre-Auth Conversion Funnel
**Path:** Home → Building Browse → Building Detail → Signup CTA

| Touchpoint | Evaluate For |
|------------|-------------|
| Home page | Trust signals (S2), value proposition clarity (H2), CTA hierarchy (H6), information scent (H1) |
| Building browse | Information density (S3), search/filter adequacy (H7), decision-enabling data (H6) |
| Building detail | Information completeness (S3), conversion path clarity (H6), trust indicators (S2) |
| Browse-to-signup transition | Progressive commitment (S1), motivation continuity (H3) |

### Flow 2: Onboarding Wizard
**Path:** Signup → Email Verification → Profile Setup → Application → Document Upload → Review → Confirmation

| Touchpoint | Evaluate For |
|------------|-------------|
| Signup form | Field necessity (S1), error prevention (H5), error recovery (H9), password rules (H2) |
| Email verification | System status (H1), what to do while waiting (H10), re-send option (H3) |
| Profile setup | Progressive disclosure (S1), field labeling (H2), dropdown loading (H1), back navigation (H3) |
| Application submission | Building picker usability (H6), document readiness clarity (H1), step continuity (H4) |
| Document upload | File requirements clarity (H6), upload feedback (H1), error recovery (H9), skip option (H3) |
| Application review | Summary completeness (H6), edit capability (H3), terms clarity (H2), submit behavior (H4) |
| Wizard overall | Step count justification (S1), progress visibility (H1), save/resume (H3), flow continuity (H4) |

### Flow 3: Post-Submission Experience
**Path:** Dashboard → Status Tracking → Offer → Accept/Decline

| Touchpoint | Evaluate For |
|------------|-------------|
| Dashboard | Status visibility (H1), anxiety reduction (S4), information hierarchy (H8), real-time updates (H1) |
| Journey stepper | Status completeness (H4), failure states (H9), edge case handling (H5) |
| Offer flow | Urgency communication (H1), decision support (H6), error recovery (H9) |
| Notifications | Actionability (H6), information density (H8), completeness (H4) |
| Waitlist view | Transparency (S4), position visibility (H1), expectation setting (H2) |

### Flow 4: Notification System
**Path:** Bell icon → Notification list → Actions

| Touchpoint | Evaluate For |
|------------|-------------|
| Notification bell | Badge visibility (H1), unread indicator (H6) |
| Notification list | Type differentiation (H4), actionability (H6), bulk actions (H7) |
| Notification detail | Rich context (S3), navigation to relevant screen (H3) |

---

## Finding Template

Each finding will be documented using this structure:

```
### [FINDING-ID]: [Short Title]

**Flow:** [Flow 1/2/3/4]
**Touchpoint:** [Specific screen/interaction]
**Severity:** [0-4]
**Heuristic(s) Violated:** [H1-H10, S1-S5]

**Observation:**
[What was observed in the codebase/UI]

**Evidence:**
[Specific file, line number, or template content that demonstrates the issue]

**Impact:**
[How this affects the user experience — behavioral prediction, not user-tested]

**Recommendation:**
[Specific, actionable fix]

**Confidence:** [Expert Assessment | Literature-Backed | Competitive Pattern]
```

---

## Evaluation Protocol

1. For each flow, walk through every touchpoint in code (template + component logic)
2. At each touchpoint, evaluate against all applicable heuristics
3. Document findings immediately — do not batch
4. Rate severity independently from frequency (a catastrophe that happens once is still a catastrophe)
5. For each finding, check: "Would this be caught by a competitor audit?" — if yes, cross-reference
6. After evaluation, sort findings by severity (4 → 0), then by flow order
7. Count findings per heuristic to identify systemic patterns
