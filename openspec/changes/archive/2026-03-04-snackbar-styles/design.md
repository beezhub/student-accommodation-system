## Context

`MatSnackBar.open()` applies `panelClass` strings as CSS classes on the snackbar overlay container. Global styles (in `styles.scss`) can target the MDC surface element inside those containers. The existing codebase uses `error-snackbar`, `success-snackbar`, `warning-snackbar`, `info-snackbar`; UISpec §3.8 specifies `snack-error`, `snack-success`, `snack-warning`, `snack-info`. Both naming conventions are supported by adding CSS for both in `styles.scss`.

ARIA politeness is passed via the `MatSnackBarConfig.politeness` option (`'polite' | 'assertive' | 'off'`). It maps directly to the `aria-live` attribute on the snackbar live region.

## Goals / Non-Goals

**Goals:**
- Colour-coded snackbars matching UISpec §3.8 colour values
- `politeness: 'assertive'` on errors; `politeness: 'polite'` on success/warning/info
- Both existing class names and UISpec class names work

**Non-Goals:**
- Replacing `MatSnackBar` with a custom notification service
- Updating every individual component that calls `snackBar.open()` directly with inline panelClass — only the centralised `error-handling.service.ts` and `error.interceptor.ts` are in scope

## Decisions

### D1: Add both naming conventions in CSS
Both `snack-error` (UISpec) and `error-snackbar` (existing codebase) are added as separate rules pointing to the same colour values. This avoids a sweeping find-and-replace across all components and keeps the change minimal.

### D2: Target `.mat-mdc-snackbar-surface`
MDC-based Material snackbars render `<div class="mat-mdc-snackbar-surface">` inside the overlay. This is the correct element to override `background` and `color` on — confirmed in UISpec §3.8.

### D3: Politeness only on centralised service calls
Only `error-handling.service.ts` and `error.interceptor.ts` are updated. Inline `snackBar.open()` calls in individual components remain unchanged — they are lower-priority, non-standardised calls that are out of scope for this change.

## Risks / Trade-offs

- **CSS specificity:** The `.mat-mdc-snackbar-surface` override may need `!important` if Material's own styles win. Check during implementation.
  → Mitigation: test after build; add `!important` only if needed.
- **Class name drift:** Future developers may use either naming convention.
  → Mitigation: both conventions are documented in the UISpec and in the CSS comments.
