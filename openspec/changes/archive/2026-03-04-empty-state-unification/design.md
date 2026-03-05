## Context

`EmptyStateComponent` API:
```html
<app-empty-state
  [icon]="'inbox'"
  [title]="'No items found'"
  [description]="'Description text here.'"
  [ctaLabel]="'Add Item'"         <!-- optional -->
  [ctaRoute]="'/admin/items/new'" <!-- optional -->
></app-empty-state>
```
`icon`, `title`, and `description` are `@Input({ required: true })`. `ctaLabel` and `ctaRoute` are optional.

## Goals / Non-Goals

**Goals:**
- Replace every `.no-data` div block in the 11 target page components
- Map the existing icon/title/description text from each inline block to the component inputs
- None of the 11 targets have CTA buttons in their `.no-data` blocks — `ctaLabel`/`ctaRoute` will be omitted

**Non-Goals:**
- No changes to `EmptyStateComponent` itself
- No changes to dialog components
- No changes to student-facing components

## Decisions

**Direct text mapping**: Each `.no-data` block's `mat-icon` name → `[icon]`, `h3` or title text → `[title]`, `p` → `[description]`. For blocks with no `h3` (e.g., application-review, academic-year-management), infer a sensible title from the page context.

**Preserve surrounding `@if` guards**: Only the inner `.no-data` div is replaced. The `@if (items.length === 0)` wrapper remains unchanged.

## Risks / Trade-offs

- **application-review no-data**: Has only `<mat-icon>inbox</mat-icon><p>No applications found</p>` — no h3. Will use "No Applications Found" as title.
- **academic-year-management**: Same pattern — no h3. Will use "No Academic Years Found".
