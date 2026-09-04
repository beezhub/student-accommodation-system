## 1. Create SkeletonLoaderComponent

- [x] 1.1 Create `frontend/src/app/shared/skeleton-loader/skeleton-loader.component.ts` as a standalone inline-template component with `type` input (`'card' | 'table-row' | 'text-block'`, default `'table-row'`) and `count` input (`number`, default `3`); use `Array.from({ length: count })` for the repeat array and `@for (item of rows; track $index)` in the template
- [x] 1.2 Add inline styles: `.skeleton-item` base with `background: #f1f5f9`, shimmer `@keyframes` (`background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%); background-size: 200%; animation: shimmer 1.5s ease-in-out infinite`); card variant tall+rounded, table-row variant short, text-block variant stacked lines
- [x] 1.3 Add `@media (prefers-reduced-motion: reduce)` rule that sets `animation: none` on `.skeleton-item`

## 2. Replace mat-spinner in admin-dashboard

- [x] 2.1 In `admin-dashboard.component.html`, replace `<div class="loading-container"><mat-spinner …></mat-spinner></div>` (or equivalent) with `<app-skeleton-loader [type]="'card'" [count]="4"></app-skeleton-loader>`
- [x] 2.2 Add `SkeletonLoaderComponent` to `admin-dashboard.component.ts` imports array; remove `MatProgressSpinnerModule` if it is only used for the loading state

## 3. Replace mat-spinner in application-review

- [x] 3.1 In `application-review.component.html`, replace loading-container mat-spinner with `<app-skeleton-loader [type]="'table-row'" [count]="8"></app-skeleton-loader>`
- [x] 3.2 Add `SkeletonLoaderComponent` to `application-review.component.ts` imports

## 4. Replace mat-spinner in building-management

- [x] 4.1 In `building-management.component.html`, replace loading-container mat-spinner with `<app-skeleton-loader [type]="'card'" [count]="4"></app-skeleton-loader>`
- [x] 4.2 Add `SkeletonLoaderComponent` to `building-management.component.ts` imports

## 5. Replace mat-spinner in room-management

- [x] 5.1 In `room-management.component.html`, replace loading-container mat-spinner with `<app-skeleton-loader [type]="'table-row'" [count]="8"></app-skeleton-loader>`
- [x] 5.2 Add `SkeletonLoaderComponent` to `room-management.component.ts` imports

## 6. Replace mat-spinner in room-allocation

- [x] 6.1 In `room-allocation.component.html`, replace loading-container mat-spinner with `<app-skeleton-loader [type]="'table-row'" [count]="8"></app-skeleton-loader>`
- [x] 6.2 Add `SkeletonLoaderComponent` to `room-allocation.component.ts` imports

## 7. Replace mat-spinner in user-management

- [x] 7.1 In `user-management.component.html`, replace loading-container mat-spinner with `<app-skeleton-loader [type]="'table-row'" [count]="8"></app-skeleton-loader>`
- [x] 7.2 Add `SkeletonLoaderComponent` to `user-management.component.ts` imports

## 8. Replace mat-spinner in document-verification

- [x] 8.1 In `document-verification.component.html`, replace loading-container mat-spinner with `<app-skeleton-loader [type]="'card'" [count]="4"></app-skeleton-loader>`
- [x] 8.2 Add `SkeletonLoaderComponent` to `document-verification.component.ts` imports

## 9. Replace mat-spinner in activity-logs

- [x] 9.1 In `activity-logs.component.html`, replace loading-container mat-spinner with `<app-skeleton-loader [type]="'table-row'" [count]="8"></app-skeleton-loader>`
- [x] 9.2 Add `SkeletonLoaderComponent` to `activity-logs.component.ts` imports

## 10. Replace mat-spinner in waiting-list-dashboard

- [x] 10.1 In `waiting-list-dashboard.component.html`, replace loading-container mat-spinner with `<app-skeleton-loader [type]="'table-row'" [count]="6"></app-skeleton-loader>`
- [x] 10.2 Add `SkeletonLoaderComponent` to `waiting-list-dashboard.component.ts` imports

## 11. Replace mat-spinner in staged-allocation

- [x] 11.1 In `staged-allocation.component.html`, replace loading-container mat-spinner with `<app-skeleton-loader [type]="'table-row'" [count]="8"></app-skeleton-loader>`
- [x] 11.2 Add `SkeletonLoaderComponent` to `staged-allocation.component.ts` imports

## 12. Replace mat-spinner in allocation-rule-management

- [x] 12.1 In `allocation-rule-management.component.html`, replace loading-container mat-spinner with `<app-skeleton-loader [type]="'card'" [count]="4"></app-skeleton-loader>`
- [x] 12.2 Add `SkeletonLoaderComponent` to `allocation-rule-management.component.ts` imports

## 13. Verification

- [x] 13.1 Run `ng build --configuration development` in `.worktrees/angular-frontend-redesign/frontend` and confirm zero new errors
