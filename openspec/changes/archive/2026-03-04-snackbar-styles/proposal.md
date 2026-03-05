## Why

Snackbar notifications have no colour styling — they all render as the default Material dark surface regardless of whether they indicate success, error, warning, or info. Additionally, the ARIA `politeness` setting is not set on any snackbar call, meaning screen readers receive no guaranteed announcement timing. Error messages (which require immediate attention) need `politeness: 'assertive'`; non-urgent success/info messages need `politeness: 'polite'`.

## What Changes

- Add 4 snackbar colour classes to `styles.scss`: `snack-success`, `snack-error`, `snack-warning`, `snack-info` (UISpec naming) plus matching aliases for the existing `success-snackbar`, `error-snackbar`, `warning-snackbar`, `info-snackbar` class names already used in the codebase
- Update `error-handling.service.ts` to add `politeness: 'assertive'` on error snackbars and `politeness: 'polite'` on success/warning/info snackbars
- Update `error.interceptor.ts` to add `politeness: 'assertive'` on its error snackbar call

## Capabilities

### New Capabilities
- `snackbar-feedback`: Colour-coded and ARIA-politeness-aware snackbar notification system for success, error, warning, and info states

### Modified Capabilities
<!-- none -->

## Impact

- Modified: `frontend/src/styles.scss` (add 8 snackbar CSS rules)
- Modified: `frontend/src/app/core/services/error-handling.service.ts` (add `politeness` to 4 snackbar calls)
- Modified: `frontend/src/app/core/interceptors/error.interceptor.ts` (add `politeness: 'assertive'`)
- No new dependencies
