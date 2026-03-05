## Why

Every admin page shows a blank white card with a spinning `mat-spinner` while data loads. UISpec section 3.9 identifies this as UX-008 (HIGH priority): skeleton loaders are the required replacement because they match the real layout, dramatically reduce perceived load time, and eliminate the jarring blank-page flash.

## What Changes

- **New shared component** `SkeletonLoaderComponent` at `app/shared/skeleton-loader/` with three types: `card`, `table-row`, `text-block`; a `count` input; shimmer animation; `prefers-reduced-motion` support
- **Replace page-level mat-spinner** in 11 admin page components: `admin-dashboard`, `application-review`, `building-management`, `room-management`, `room-allocation`, `user-management`, `document-verification`, `activity-logs`, `waiting-list-dashboard`, `staged-allocation`, `allocation-rule-management`
- Dialog spinners and button-loading spinners are **not changed** (UISpec §3.9 explicitly excludes them)

## Capabilities

### New Capabilities

- `skeleton-loader`: Shared shimmer skeleton component with card, table-row, and text-block types

### Modified Capabilities

*(none — this is purely a new component + usage swap)*

## Impact

- New files: `app/shared/skeleton-loader/skeleton-loader.component.ts`
- Modified HTML: loading-container sections in 11 admin page templates
- Modified TS: add `SkeletonLoaderComponent` to imports in 11 admin page components
- No API changes, no route changes, no backend changes
