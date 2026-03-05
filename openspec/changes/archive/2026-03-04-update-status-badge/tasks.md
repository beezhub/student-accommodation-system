## 1. Add missing colour classes to component inline styles

- [x] 1.1 Added `.status-warning` to the warning group selector (alongside `.status-pending`)
- [x] 1.2 Added `.status-info` to the info group selector (alongside `.status-available`)
- [x] 1.3 Added `.status-neutral` to the neutral group selector (alongside `.status-maintenance`)
- [x] 1.4 Added `.status-changes` as its own block after `.status-neutral`

## 2. Remove text-transform from badge-label

- [x] 2.1 Removed `text-transform: capitalize;` from `.badge-label`

## 3. Verify

- [x] 3.1 `ng build --configuration development` — zero errors
- [x] 3.2 All 4 new colour classes present in component styles
- [x] 3.3 `.badge-label` has no `text-transform` property
