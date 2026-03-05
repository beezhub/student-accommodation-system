## Why

Admin table action buttons (edit, delete, view, download, etc.) use `mat-icon-button` with only a `matTooltip` for labeling. `matTooltip` is not read by screen readers — these buttons are completely unlabeled for assistive technology users. UISpec §2.8 requires all icon-only interactive elements to have an `aria-label`.

## What Changes

- Add `aria-label` to every `mat-icon-button` in admin table rows and dialogs that currently lacks one
- Scope: 12 admin component HTML files where icon-only buttons are missing `aria-label`

## Capabilities

### New Capabilities
- `admin-button-accessibility`: Icon-only action buttons in admin tables carry descriptive `aria-label` attributes

### Modified Capabilities
<!-- none -->

## Impact

- 12 HTML files in `frontend/src/app/features/admin/`
- No TypeScript changes required
- No visual change for sighted users
- No new dependencies
