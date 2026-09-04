## 1. Create OfferExpiryComponent files

- [x] 1.1 Create `frontend/src/app/shared/components/offer-expiry/offer-expiry.component.ts` — standalone component with `@Input() expiryDate: string | null`, `@Input() status: AllocationStatus`, urgency computation method, `setInterval` countdown, `OnDestroy` cleanup, and `aria-live="polite"` binding
- [x] 1.2 Create `frontend/src/app/shared/components/offer-expiry/offer-expiry.component.html` — template with `@switch` or `@if` blocks for each urgency level (normal, warning, critical, alarm, expired)
- [x] 1.3 Create `frontend/src/app/shared/components/offer-expiry/offer-expiry.component.scss` — urgency chip/banner styles using design tokens from `_variables.scss`; include `@keyframes pulse` and `@media (prefers-reduced-motion: reduce)` suppression

## 2. Integrate into dashboard

- [x] 2.1 In `dashboard.component.ts`, import `OfferExpiryComponent` and add to `imports` array
- [x] 2.2 In `dashboard.component.html`, replace the inline `offer-expiry-notice` div (with hardcoded `style=""` amber colour) with `<app-offer-expiry [expiryDate]="roomAllocation.offerExpiryDate" [status]="roomAllocation.status">`

## 3. Verification

- [x] 3.1 Run `npm run build` in the worktree frontend and confirm zero new errors
