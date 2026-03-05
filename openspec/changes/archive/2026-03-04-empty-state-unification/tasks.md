## 1. application-review

- [x] 1.1 Replace `.no-data` block (icon: `inbox`, no h3, p: "No applications found") with `<app-empty-state [icon]="'inbox'" [title]="'No Applications Found'" [description]="'No applications match your current filters.'"></app-empty-state>`
- [x] 1.2 Add `EmptyStateComponent` import to `application-review.component.ts`

## 2. building-management

- [x] 2.1 Replace `.no-data` block (icon: from no-data-icon, h3: "No Buildings Found", p: "Get started by adding your first building to the system.") with `<app-empty-state>`
- [x] 2.2 Add `EmptyStateComponent` import to `building-management.component.ts`

## 3. room-management

- [x] 3.1 Replace `.no-data` block (h3: "No Rooms Found", p: "Get started by adding your first room to a building.") with `<app-empty-state>`
- [x] 3.2 Add `EmptyStateComponent` import to `room-management.component.ts`

## 4. room-allocation

- [x] 4.1 Replace `.no-data` block (h3: "No Allocations Found", p: "Start by allocating a room to a student.") with `<app-empty-state>`
- [x] 4.2 Add `EmptyStateComponent` import to `room-allocation.component.ts`

## 5. document-verification

- [x] 5.1 Replace `.no-data` block (h3: "No Documents Found", p: "There are no documents matching your current filters.") with `<app-empty-state>`
- [x] 5.2 Add `EmptyStateComponent` import to `document-verification.component.ts`

## 6. activity-logs

- [x] 6.1 Replace `.no-data` block (h3: "No Activity Logs Found", p: "There are no logs matching your current filters.") with `<app-empty-state>`
- [x] 6.2 Add `EmptyStateComponent` import to `activity-logs.component.ts`

## 7. waiting-list-dashboard

- [x] 7.1 Replace `.no-data` block (h3: "No Students in Waiting List", p: "The waiting list is currently empty or no students match the selected filter.") with `<app-empty-state>`
- [x] 7.2 Add `EmptyStateComponent` import to `waiting-list-dashboard.component.ts`

## 8. academic-year-management

- [x] 8.1 Replace `.no-data` block (icon: `event_note`, no h3, p: "No academic years found. Create one to get started.") with `<app-empty-state [icon]="'event_note'" [title]="'No Academic Years Found'" [description]="'Create one to get started.'"></app-empty-state>`
- [x] 8.2 Add `EmptyStateComponent` import to `academic-year-management.component.ts`

## 9. accommodation-type-management

- [x] 9.1 Replace `.no-data` block (h3: "No Accommodation Types Found", p: "Get started by adding your first accommodation type.") with `<app-empty-state>`
- [x] 9.2 Add `EmptyStateComponent` import to `accommodation-type-management.component.ts`

## 10. document-type-management

- [x] 10.1 Replace `.no-data` block (h3: "No Document Types Found", p: "Create your first document type to get started.") with `<app-empty-state>`
- [x] 10.2 Add `EmptyStateComponent` import to `document-type-management.component.ts`

## 11. institution-management

- [x] 11.1 Replace `.no-data` block (h3: "No Institutions Found", p: "Add your first educational institution to get started.") with `<app-empty-state>`
- [x] 11.2 Add `EmptyStateComponent` import to `institution-management.component.ts`

## 12. Verification

- [x] 12.1 Run `ng build --configuration development` in `.worktrees/angular-frontend-redesign/frontend` and confirm zero new errors
