# Accessibility Audit Report (WCAG 2.1 AA)

**Date:** 2026-02-19
**Target:** WCAG 2.1 Level AA
**Tools:** Lighthouse, axe-core, Manual Review

## Executive Summary
This document tracks accessibility issues identified during the audit of the Student Accommodation System frontend.

## Baseline Scores (Lighthouse)
*Run 1 - Before Fixes*
- **Login:** [Score]
- **Dashboard:** [Score]
- **Application Form:** [Score]
- **Admin Tables:** [Score]

## Identified Issues

### 1. Color Contrast
| Location | Element | Current Ratio | Required | Status |
|----------|---------|---------------|----------|--------|
| Global | Primary Text | | 4.5:1 | Open |
| Login | Sign In Button (Disabled) | 2.4:1 | 4.5:1 | Fixed (Custom disabled style) |
| Change Password | Submit Button | Visual Check OK | 4.5:1 | Pass |

### 2. Form Labels & Inputs
| Location | Field | Issue | Remediation | Status |
|----------|-------|-------|-------------|--------|
| Login | Password | None | N/A | Pass |
| Change Password | All Inputs | Label not associated | Add for/id attributes | Fixed |

### 3. Keyboard Navigation & Focus
| Location | Interaction | Issue | Remediation | Status |
|----------|-------------|-------|-------------|--------|
| Login | Sign In Button | Skipped (disabled attribute) | Use aria-disabled="true" | Fixed |
| Dashboard | Charts | Not focusable/readable | Add role="img" & aria-label | Fixed |

### 4. Semantic Structure
| Location | Element | Issue | Remediation | Status |
|----------|---------|-------|-------------|--------|
| Dashboard | Charts | Divs used for charts | Add role="img" | Fixed |
| Application Form | Building Gallery | Not focusable | Add tabindex="0" & key events | Fixed |
| Application Form | Validation | Errors not announced | Add role="alert" | Fixed |
| Admin Tables | Icon-only Buttons | Missing aria-label | Add aria-label | Fixed |
| Admin Dialogs | Close Buttons | Missing aria-label | Add aria-label | Fixed |

### 5. Color Contrast
| Location | Element | Current Ratio | Required | Status |
|----------|---------|---------------|----------|---------|
| Login | Sign In Button (Disabled) | 2.4:1 | 4.5:1 | Fixed |
| App Form | Submit Button (Disabled) | Low | 4.5:1 | Fixed |

## Remediation Log
- [x] Fix color contrast on disabled buttons
- [x] Add missing form labels (Change Password page)
- [x] Add role="img" and aria-label to Dashboard charts
- [x] Add keyboard support and ARIA roles to Building Gallery
- [x] Add role="alert" to form validation messages
- [x] Add aria-label to all icon-only buttons across Admin tables
- [x] Add aria-label to dialog close buttons
