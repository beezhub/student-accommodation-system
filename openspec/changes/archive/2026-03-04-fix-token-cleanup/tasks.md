## 1. Fix deprecated darken() calls in admin-dashboard.component.scss

- [x] 1.1 Add `@use 'sass:color';` as the first line of `admin-dashboard.component.scss` (before existing `@use` statements)
- [x] 1.2 Replace line 167: `darken(vars.$success, 10%)` → `color.scale(vars.$success, $lightness: -10%)`
- [x] 1.3 Replace line 174: `darken(vars.$success, 10%)` → `color.scale(vars.$success, $lightness: -10%)`
- [x] 1.4 Replace line 178: `darken(vars.$warning, 10%)` → `color.scale(vars.$warning, $lightness: -10%)`
- [x] 1.5 Replace line 182: `darken(vars.$error, 10%)` → `color.scale(vars.$error, $lightness: -10%)`
- [x] 1.6 Replace line 617: `darken(vars.$warning, 10%)` → `color.scale(vars.$warning, $lightness: -10%)`
- [x] 1.7 (discovered) Fix `darken(vars.$success, 10%)` in `activity-logs.component.scss:347` — same replacement + add `@use 'sass:color'`

## 2. Remove duplicate mat-icon font-family blocks from admin SCSS files

- [x] 2.1 `activity-logs.component.scss` — removed
- [x] 2.2 `admin-dashboard.component.scss` — removed
- [x] 2.3 `allocation-move-dialog.component.scss` — removed
- [x] 2.4 `allocation-rule-management.component.scss` — removed
- [x] 2.5 `building-management.component.scss` — removed
- [x] 2.6 `room-allocation.component.scss` — removed
- [x] 2.7 `room-management.component.scss` — removed
- [x] 2.8 `user-management.component.scss` — removed
- [x] 2.9 `waiting-list-dashboard.component.scss` — removed

## 3. Verify

- [x] 3.1 Run `ng build --configuration development` — zero `darken() is deprecated` warnings, zero errors
- [x] 3.2 Confirmed no `mat-icon { font-family` blocks remain in any admin component SCSS file
