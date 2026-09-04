## Why

Admin page components use 12+ variations of inline `.no-data` div blocks with inconsistent structure (some have `.no-data-icon`, some just a bare `mat-icon`, some have h3+p, some have only a `p`). A shared `EmptyStateComponent` already exists at `shared/components/empty-state/` but is used in only one page. Unifying these eliminates duplicated markup and guarantees visual consistency across all admin pages.

## What Changes

- Replace inline `.no-data` div blocks in **11 admin page components** with `<app-empty-state>` using the existing `EmptyStateComponent`
- Add `EmptyStateComponent` to the `imports` array of each affected component's TypeScript file
- No changes to `EmptyStateComponent` itself — it already covers the required API

**Already migrated (skip):** `user-management`
**No matching block (skip):** `allocation-rule-management` (uses inline text only, not a `.no-data` div)

## Capabilities

### New Capabilities

*(none — EmptyStateComponent already exists)*

### Modified Capabilities

*(no spec-level changes)*

## Impact

- Modified HTML: 11 admin page templates
- Modified TS: 11 admin page component classes (add `EmptyStateComponent` import)
- No API, route, or backend changes
