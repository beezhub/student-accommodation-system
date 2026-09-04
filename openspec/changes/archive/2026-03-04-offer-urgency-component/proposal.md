## Why

Offer expiry dates are currently displayed as plain text (`"Offer expires: Mar 6, 2026, 3:00 PM"`), giving students no sense of urgency until it's too late. Missed offer deadlines are a critical pain point — students lose allocated rooms and re-enter the waiting list. An urgency-aware display with escalating visual treatments and a live countdown addresses this directly.

## What Changes

- New `OfferExpiryComponent` (standalone) in `shared/components/offer-expiry/` with `@Input() expiryDate: string` and `@Input() status: AllocationStatus`
- Four urgency levels with distinct visual treatments: normal (> 3 days), warning (1–3 days), critical (< 24 hours), alarm (< 1 hour), and expired
- Live countdown timer updating every 60 seconds via `setInterval`, cleaned up `OnDestroy`
- `aria-live="polite"` on container for screen reader announcements
- Pulse animation disabled when `prefers-reduced-motion` is set
- Replace the inline `offer-expiry-notice` block in `dashboard.component.html` with `<app-offer-expiry>`

## Capabilities

### New Capabilities
- `offer-expiry-urgency`: Visual urgency escalation for offer expiry dates, including countdown timer, 4 urgency levels, animations, and ARIA live region

### Modified Capabilities
<!-- none -->

## Impact

- New files: `shared/components/offer-expiry/offer-expiry.component.ts`, `.html`, `.scss`
- Modified: `dashboard.component.html` (replace inline expiry block), `dashboard.component.ts` (import new component)
- No backend changes required — `offerExpiryDate` already returned as ISO string in `RoomAllocationResponse`
