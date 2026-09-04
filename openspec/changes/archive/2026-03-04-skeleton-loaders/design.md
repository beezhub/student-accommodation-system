## Context

Admin page templates follow a consistent pattern: `@if (loading)` shows a `.loading-container` div with `<mat-spinner diameter="40">`, else the data table/cards render. The new skeleton replaces the `.loading-container` block only — the surrounding `@if`/`@else` and the loaded state are untouched.

## Goals / Non-Goals

**Goals:**
- Single `SkeletonLoaderComponent` covering three layout types (card, table-row, text-block)
- Shimmer animation via CSS `@keyframes` on a `linear-gradient`
- `prefers-reduced-motion` media query disables shimmer (static slate-100 block)
- `count` input repeats skeleton rows/cards (default 3 for card, 8 for table-row)
- Drop-in replacement: swap `<div class="loading-container">…</div>` → `<app-skeleton-loader>`

**Non-Goals:**
- No skeleton inside dialogs
- No skeleton for button loading states
- No skeleton for form fields
- No dynamic column-width matching (table skeleton uses fixed proportional widths)

## Decisions

**Standalone inline-template component**
Single `.ts` file with inline template and styles — no separate HTML/SCSS files. Keeps the shared component self-contained and easy to inspect, consistent with `ModernStatusBadgeComponent`.

**`@for` repetition with `count` input**
`Array.from({ length: count })` in the component class provides the array for `@for (item of rows; track $index)` — avoids a pipe and keeps the template declarative.

**Three skeleton types as CSS classes**
The `type` input switches a CSS class on the container. Card skeletons are tall rectangles with border-radius `$radius-xl`; table-row skeletons are short horizontal bars; text-block is a single multi-line stack. No sub-components needed.

**Shimmer via `background-size: 200%` shift**
```css
@keyframes shimmer {
  0%   { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}
```
Gradient: `linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%)`.

## Risks / Trade-offs

- **Table column mismatch**: Skeleton table rows use fixed proportional widths, not the real column widths. Acceptable tradeoff — exact matching would require passing column config, over-engineering for this use case.
- **Count default**: Using `count=3` for cards, `count=8` for table rows as defaults; callers can override.
