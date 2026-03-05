## Context

`mat-icon-button` renders a button with only a `<mat-icon>` inside. The Material tooltip (`matTooltip`) is visually helpful but is not wired to the button's accessible name — screen readers announce the button without any label. The fix is simply adding `aria-label="<action description>"` directly on the `<button>` element.

## Goals / Non-Goals

**Goals:**
- Every `mat-icon-button` in admin table rows/dialogs has a descriptive `aria-label`
- Labels match the existing `matTooltip` text where available, or are derived from the icon name

**Non-Goals:**
- Adding tooltips to buttons that only have `aria-label` (out of scope)
- Modifying any TypeScript files
- Touching non-admin components

## Decisions

### D1: Use `aria-label` on the `<button>` element directly
Angular Material `mat-icon-button` does not automatically derive an accessible name from child `<mat-icon>` content. Adding `aria-label` on the `<button>` element is the correct, minimal fix.

### D2: Match `matTooltip` text where it exists
Where a button already has `matTooltip="Edit Building"`, the `aria-label` should be the same string to ensure consistency between visual tooltip and screen reader announcement.

### D3: Derive label from icon name where no tooltip exists
For buttons without a tooltip, derive the label from the icon name (e.g., `edit` → `"Edit"`, `delete` → `"Delete"`, `more_vert` → `"More actions"`).

### D4: Include row context in labels where needed
For table row buttons that repeat on every row, include context where helpful (e.g., `"Edit"` is sufficient — the screen reader will already be in row context).

## Component Inventory

Files requiring `aria-label` additions:

| Component | Buttons missing `aria-label` |
|-----------|------------------------------|
| academic-year-management | Edit, Delete, Toggle active (3 buttons) |
| accommodation-type-management | Edit, Delete (2 buttons) |
| allocation-rule-management | Edit Rule, Delete Rule × 2 (desktop + card view) |
| application-review | Review, Preferences, More actions (mobile view) |
| building-management | Edit, Delete, Allocation Rules (desktop view) |
| building-image-dialog | Set Primary, Delete Image (2 buttons) |
| document-type-management | Edit, Delete, Toggle (3 buttons) |
| document-verification | View Document, Download (mobile view) |
| institution-management | Edit, Delete (2 buttons) |
| room-allocation | Delete, View, Accept, Decline, More actions |
| room-management | Edit, Delete, Attributes (where missing) |
| user-management | Edit, Delete (where missing) |
