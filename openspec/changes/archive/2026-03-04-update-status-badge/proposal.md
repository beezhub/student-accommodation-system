## Why

`ModernStatusBadge` references four CSS colour classes (`status-warning`, `status-info`, `status-neutral`, `status-changes`) in its config map that are not defined inside the component's encapsulated styles. They exist in the global `styles.scss`, which means they currently render correctly due to Angular's ViewEncapsulation passing global styles through — but this is an implicit dependency that breaks the component's self-containment. If the global styles are ever reorganised, these statuses silently lose their colours with no build error. Additionally, `.badge-label` uses `text-transform: capitalize`, which corrupts multi-word labels — `under_review` becomes "under review" instead of "Under Review".

## What Changes

- Add `status-warning`, `status-info`, `status-neutral`, and `status-changes` CSS class definitions directly to the component's inline `styles` array
- Remove `text-transform: capitalize` from `.badge-label` — labels are already correctly capitalised in the config strings
- No config map changes — all 24 status entries are already present and correctly mapped

## Capabilities

### New Capabilities

### Modified Capabilities

## Impact

- `modern-status-badge.component.ts` — styles block only; zero template or logic changes
- No other files affected
- Visually: `under_review`, `changes_requested`, `offered`, `submitted`, `withdrawn`, `declined`, `completed` badges will now render correctly even if global styles are removed; `text-transform` removal has no visible effect since labels are already title-cased in the config
