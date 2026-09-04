## Context

The student dashboard displays room allocation offers with a static expiry date string. When an offer is in `OFFERED` status, `offerExpiryDate` (ISO string) is returned by the backend. Currently `dashboard.component.html` renders this as a plain `date` pipe output inside an inline `offer-expiry-notice` div with hardcoded amber `style=""` attributes. The student has no visual cue as the deadline approaches.

The new `OfferExpiryComponent` lives in `shared/components/offer-expiry/` and encapsulates all urgency logic, making it reusable for any future screen that shows allocation offers.

## Goals / Non-Goals

**Goals:**
- Four urgency levels mapped to distinct chip/banner treatments and icons per UISpec §3.14
- Live countdown updating every 60 seconds (e.g. "Expires in 6 hours")
- `aria-live="polite"` so screen readers announce urgency changes
- Pulse animation for the critical level, disabled via `@media (prefers-reduced-motion: reduce)`
- Standalone Angular 21 component, zero module boilerplate
- Replaces the inline `offer-expiry-notice` block in `dashboard.component.html`

**Non-Goals:**
- Sub-minute countdown precision (every 60 s is sufficient UX)
- Push notifications or server-sent events for expiry
- Displaying urgency on the admin Room Allocation table (separate change)

## Decisions

### D1: setInterval vs RxJS timer
**Decision:** Use `setInterval` inside `ngOnInit`, store the handle, clear in `ngOnDestroy`.

**Rationale:** Keeps the component dependency-free (no RxJS import needed just for a tick). A 60-second interval is low-frequency enough that zone.js overhead is negligible. The component only lives while the dashboard is mounted.

**Alternative:** `interval(60_000)` from RxJS with `takeUntilDestroyed()`. Valid but adds unnecessary pipe complexity for a single tick operation.

### D2: Urgency computed in component vs pipe
**Decision:** Compute urgency level in the component class (`getUrgencyLevel()` method returning a string enum), bind CSS class and template blocks to it.

**Rationale:** The urgency level drives multiple things (icon, label text, CSS class, aria label) — a pipe would need to be called multiple times or return an object. A component method computed on each change-detection cycle (called ~once per minute) is cleaner and testable.

### D3: Input type for expiryDate
**Decision:** Accept `expiryDate` as `string | null` (ISO 8601), parse with `new Date()` inside the component.

**Rationale:** `RoomAllocationResponse.offerExpiryDate` is typed as `string | undefined`. The component guards against null/undefined and shows nothing when absent, consistent with how the dashboard already conditionally renders it.

### D4: Placement in shared vs dashboard-specific
**Decision:** Place in `shared/components/offer-expiry/` (not inside `features/dashboard/`).

**Rationale:** The component has no dashboard-specific dependencies. Admin views (staged allocations, room allocations table) may want to show the same urgency chip in future. Shared placement is consistent with `EmptyStateComponent`, `SkeletonLoaderComponent`, and `ModernStatusBadgeComponent` patterns.

## Risks / Trade-offs

- **Clock drift:** `setInterval` is not precision-guaranteed. For a 60-second visual update, this is acceptable. If the tab is backgrounded, the browser may throttle; on resume the next paint will re-evaluate.
  → Mitigation: call `updateUrgency()` immediately on `ngOnInit` before the first interval tick, so the initial render is always correct.

- **Stale view if component stays mounted:** If a student leaves the tab open for hours, the countdown may hit zero while the interval is still running. The `EXPIRED` level handles this gracefully.
  → Mitigation: the urgency method checks `remainingMs < 0` and returns `'expired'`.

## Open Questions

None — UISpec §3.14 is prescriptive. All urgency thresholds and labels are specified.
