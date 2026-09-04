## 1. Add snackbar CSS classes to styles.scss

- [x] 1.1 Append 8 snackbar panel-class rules to `frontend/src/styles.scss` — 4 UISpec names (`snack-success`, `snack-error`, `snack-warning`, `snack-info`) and 4 existing aliases (`success-snackbar`, `error-snackbar`, `warning-snackbar`, `info-snackbar`), all targeting `.mat-mdc-snackbar-surface` with the colours from UISpec §3.8

## 2. Add ARIA politeness to centralised snackbar calls

- [x] 2.1 In `frontend/src/app/core/services/error-handling.service.ts`, add `politeness: 'assertive'` to `showErrorSnackbar()` and `politeness: 'polite'` to `showSuccess()`, `showWarning()`, and `showInfo()`
- [x] 2.2 In `frontend/src/app/core/interceptors/error.interceptor.ts`, add `politeness: 'assertive'` to the snackbar call

## 3. Verification

- [x] 3.1 Run `npm run build` in the worktree frontend and confirm zero new errors
