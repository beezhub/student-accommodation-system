## Why

The build emits SCSS deprecation warnings on every compile due to `darken()` calls in `admin-dashboard.component.scss`. These warnings pollute CI output and will become errors in a future Sass release. Separately, nine admin component SCSS files each contain an identical `mat-icon { font-family: 'Material Icons' !important }` block that is already covered by the global stylesheet — the duplicates are dead code creating maintenance noise.

Note: Several items originally scoped here were already resolved in the worktree prior to this change: `_tokens.scss` has already been deleted, `prefers-reduced-motion` is already in `styles.scss`, and the missing CSS custom properties (`--color-info-*`, `--color-neutral-*`) are already defined with fallback values in `styles.scss`.

## What Changes

- Replace all 5 `darken()` calls in `admin-dashboard.component.scss` with `color.scale()` (the Sass-recommended replacement)
- Remove the redundant `mat-icon { font-family: 'Material Icons' !important; font-weight: normal; font-style: normal; }` block from 9 admin component SCSS files (the fix is already global in `styles.scss`)
- Add `@use 'sass:color'` to `admin-dashboard.component.scss` to enable the `color.scale()` function

## Capabilities

### New Capabilities

### Modified Capabilities

## Impact

- `admin-dashboard.component.scss` — add `@use 'sass:color'`, replace 5× `darken()` with `color.scale($color, $lightness: -10%)`
- 9 admin SCSS files — remove 4-line `mat-icon` block: `activity-logs`, `admin-dashboard`, `allocation-move-dialog`, `allocation-rule-management`, `building-management`, `room-allocation`, `room-management`, `user-management`, `waiting-list-dashboard`
- No API changes, no template changes, no routing changes, no behaviour changes
