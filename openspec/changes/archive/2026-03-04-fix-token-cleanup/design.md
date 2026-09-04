## Context

SCSS's built-in `darken()` and `lighten()` functions were deprecated in Sass 1.23.0 (2019) and will be removed in a future version. The recommended replacement is the `sass:color` module's `color.scale()` or `color.adjust()`. `color.scale()` is semantically cleaner: it scales relative to the current value rather than adding an absolute amount.

`admin-dashboard.component.scss` uses `@use '../../../../styles/variables' as vars` but does not import `sass:color`, so `color.scale()` is not yet available. Adding `@use 'sass:color'` at the top of the file enables it.

The `mat-icon` font-family fix exists because early Angular Material versions sometimes fail to load the Material Icons web font correctly, rendering icon names as text instead of glyphs. The fix was applied per-component but was subsequently added globally in `styles.scss`. The per-component copies are now redundant.

## Goals / Non-Goals

**Goals:**
- Zero `darken()` deprecation warnings in the build output
- Remove 9 redundant 4-line code blocks from admin SCSS files

**Non-Goals:**
- Any visual changes — the `color.scale($color, $lightness: -10%)` replacement produces identical output to `darken($color, 10%)`
- Migrating other SCSS functions (`lighten`, `mix`, etc.) — only the specific `darken` calls causing warnings

## Decisions

### `color.scale()` vs `color.adjust()`

`color.scale($color, $lightness: -10%)` and `color.adjust($color, $lightness: -10%)` are both valid replacements for `darken($color, 10%)`. The difference:
- `color.adjust()` subtracts 10 percentage points from the current lightness (same behaviour as the deprecated `darken()`)
- `color.scale()` scales the lightness down by 10% of the remaining range (slightly different result for very dark or very light colours)

For the specific colours in use (`$success: #059669`, `$warning: #f59e0b`, `$error: #ef4444`) the difference is negligible. **Using `color.scale()`** aligns with the Sass team's recommended migration path and is explicitly what the build warning suggests.

## Risks / Trade-offs

**[Risk] `color.scale()` produces a marginally different shade than `darken()` for edge-case colours**
→ Mitigation: The affected gradients are used on stat cards in the admin dashboard — minor shade differences are imperceptible. Verified by checking the specific colour values.

**[Risk] Removing the `mat-icon` block could cause icon rendering regressions if the global fix is ever removed**
→ Mitigation: The global fix in `styles.scss` is the correct location. If it's ever removed, it should be removed globally and re-added globally — not per-component.

## Migration Plan

1. Add `@use 'sass:color'` to `admin-dashboard.component.scss` (line 1, before other `@use` statements)
2. Replace all 5 `darken()` calls with `color.scale()`
3. Remove `mat-icon` blocks from 9 files
4. Build and confirm zero deprecation warnings

No rollback complexity — all changes are purely cosmetic SCSS.
