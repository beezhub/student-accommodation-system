## Context

With Sprint 1 (broken UX fixes), Sprint 2 (information density), and Sprint 3 (trust infrastructure) complete, BeeZhub has a functional baseline. The platform still lacks engagement and transparency features that competitors offer and that the UX research identified as roadmap priorities (Findings F-4, F-6, F-3b). Sprint 4+ covers 8 capabilities spanning 3 sub-sprints: waitlist + favorites + social login (Sprint 4), map + wizard persistence + notifications (Sprint 5), real-time updates + notify-me (Sprint 6).

The backend uses Spring Boot 3.5.7, Java 17, PostgreSQL 16.2 with Flyway, Spring Security with JWT, and an existing Google Maps integration (`GoogleMapsService`, `GOOGLE_MAPS_API_KEY`). The frontend uses Angular 21 with Material Design. The existing `WaitingListEntry` entity tracks waitlist state but has no student-visible position. The `UserNotification` entity tracks sent notifications but has no read/unread status. Building geocoding infrastructure exists (`BuildingController` has a `/maps-config` endpoint).

## Goals / Non-Goals

**Goals:**
- Give students visibility into their waitlist position and expected wait time
- Allow students to save/favorite buildings before and after authentication
- Add a map view with distance-to-campus for building browse
- Persist wizard state across sessions so students can resume later
- Offer Google Sign-In as a low-friction registration alternative
- Replace polling with SSE for real-time offer expiry updates
- Build a full notification center with type-specific rendering and read tracking
- Let students subscribe to availability notifications for full buildings

**Non-Goals:**
- In-app messaging (R-20) — Sprint 5-6 scope but deferred to a dedicated change due to architectural complexity (WebSocket infrastructure, message persistence, admin inbox UI). Estimated as 5-effort item in research.
- Push notifications (browser/mobile) — in-app notification center is first; push channels are a separate concern
- Third-party review integration (R-23, Trustpilot/Google Reviews) — depends on user base reaching critical mass
- Custom notification preferences (per-type opt-in/opt-out) — future enhancement after notification center is stable
- Mobile-native app — all features are responsive web

## Decisions

### D1: Waitlist position calculated dynamically, not stored

**Decision:** Waitlist position is computed at query time via a ranked query (`ROW_NUMBER() OVER (ORDER BY created_at ASC)` within the same tenant and academic year) rather than stored as a column that must be maintained on every insert/delete.

**Rationale:** Storing position requires recalculating and updating N rows every time a student leaves the waitlist. A computed position is always correct and simpler. At current scale (hundreds, not millions, of entries) the query cost is negligible.

**Alternative considered:** Denormalized `position` column updated by a service method on every waitlist change. Rejected — write amplification outweighs read cost at this scale.

### D2: Save/favorite uses localStorage for anonymous, backend table for authenticated

**Decision:** Anonymous users get localStorage-based favorites (key: `beezhub_favorites`, value: JSON array of building IDs). On login/signup, a sync method POSTs the localStorage list to `POST /api/favorites/sync`, which merges with any existing server-side favorites. Authenticated users always read from the backend. New table: `building_favorite (id, tenant_id, student_id, building_id, created_at)` with unique constraint on `(tenant_id, student_id, building_id)`.

**Rationale:** Anonymous favorites are critical because the browse page is public — requiring login to save a building breaks the browsing flow. Backend persistence after login ensures favorites survive across devices. The sync-on-login pattern is a common e-commerce approach.

**Alternative considered:** Backend-only favorites requiring login. Rejected — contradicts the public browse experience established in Sprint 2.

### D3: Google Maps JavaScript API for map view with existing geocoding

**Decision:** Use the Google Maps JavaScript API (already available — the platform has `GOOGLE_MAPS_FRONTEND_API_KEY`) to render an interactive map on the building browse page. Building coordinates come from the existing `Building.latitude`/`Building.longitude` fields populated by `GoogleMapsService` geocoding. Distance-to-campus is calculated client-side using the Haversine formula from the student's institution coordinates (institution entity needs lat/lng if not present, or use a hardcoded campus center as fallback).

**Rationale:** Reuses existing infrastructure. No new API key or geocoding service needed. Client-side distance calculation avoids backend roundtrips for what is essentially a presentation concern.

**Alternative considered:** Mapbox GL JS (better free tier). Rejected — Google Maps is already integrated and paid for.

### D4: Wizard state persisted to localStorage with schema versioning

**Decision:** Each wizard step component auto-saves its form state to localStorage on every field change (debounced 500ms). Keys: `beezhub_wizard_{step}_{userId|'anon'}`. A `schemaVersion` number is included so stale saved state from an older wizard version is discarded rather than causing form errors. State is cleared on successful submission completion.

**Rationale:** localStorage is immediate, requires no backend changes, and works offline. Schema versioning prevents issues when form fields change between deployments.

**Alternative considered:** Backend draft endpoint (`POST /api/application/draft`). Rejected for Sprint 4 — adds API complexity. Can be added later for cross-device resume.

### D5: Google OAuth2 via Spring Security OAuth2 Login

**Decision:** Add Google as an OAuth2 login provider using Spring Security's built-in OAuth2 Login support. On first Google login, create an `AppUser` with role `STUDENT` if no account with that email exists. If an account exists, link the Google subject ID. JWT tokens are still issued after OAuth2 callback — the existing JWT infrastructure remains the session mechanism. Frontend adds a "Sign in with Google" button that redirects to `/oauth2/authorization/google`.

**Rationale:** Spring Security's OAuth2 Login is mature and well-documented. JWT issuance after OAuth2 callback keeps the existing auth flow intact — no session-based auth needed.

**Alternative considered:** Firebase Authentication as a proxy. Rejected — adds unnecessary dependency when Spring Security handles it natively.

### D6: Server-Sent Events (SSE) for real-time offer updates

**Decision:** Add a `GET /api/sse/offers` endpoint using Spring's `SseEmitter`. When a student connects, the emitter subscribes to an in-memory event bus (`ApplicationEventPublisher`). Events are published when offer status changes (accepted, declined, expired, new offer). The `OfferExpiryComponent` subscribes to the SSE stream and updates its state from server-push events instead of polling. A reconnection strategy with exponential backoff (1s, 2s, 4s, max 30s) handles connection drops.

**Rationale:** SSE is unidirectional (server → client), simpler than WebSocket, natively supported by browsers via `EventSource`, and sufficient for this use case (no client → server push needed). Spring's `SseEmitter` is built-in.

**Alternative considered:** WebSocket via STOMP. Rejected — bidirectional is unnecessary for push-only events. WebSocket adds complexity (STOMP broker, session management) that is warranted only for messaging (deferred feature).

### D7: Notification center with read tracking via `read_at` column

**Decision:** Add a `read_at TIMESTAMP` nullable column to `user_notification`. A `GET /api/notifications` paginated endpoint returns the student's notifications with read/unread status. A `PUT /api/notifications/{id}/read` marks individual notifications as read. A `PUT /api/notifications/read-all` marks all as read. The frontend gets a new `/notifications` route with a `NotificationCenterComponent` that renders notifications using type-specific templates (icon, action button, color per notification type).

**Rationale:** The `user_notification` table already exists and tracks per-user notification delivery. Adding `read_at` is the minimal change for read tracking. A dedicated page addresses the gap identified in the frontend spec audit ("No notification center page").

**Alternative considered:** Separate `notification_read` join table. Rejected — `user_notification` is already per-user, so a column is simpler.

### D8: Availability subscription via new `availability_subscription` table

**Decision:** New table: `availability_subscription (id, tenant_id, student_id, building_id, created_at, notified_at)` with unique constraint on `(tenant_id, student_id, building_id)`. When `Room.status` changes to `AVAILABLE` and the building was previously at capacity, all subscribers for that building receive a notification (email + in-app) and `notified_at` is set. Subscriptions are one-shot: after notification, the student must re-subscribe if they want future alerts.

**Rationale:** One-shot prevents notification fatigue (a room briefly available then taken would otherwise spam subscribers). The subscription is building-level, not room-level, because students browse by building.

**Alternative considered:** Persistent subscription with cooldown period. Rejected — one-shot is simpler and sufficient for MVP.

## Risks / Trade-offs

- **[SSE connection scaling]** → Each connected student holds an open HTTP connection. At current scale (tens-hundreds of concurrent students) this is fine. If scale increases to thousands, move to a shared event bus (Redis pub/sub) to decouple SSE emitters from application instances. → Mitigation: SSE timeout of 5 minutes with client-side reconnect keeps connections short-lived.
- **[Google OAuth2 requires credential management]** → Google Client ID/Secret must be configured. Missing credentials will disable social login gracefully (button hidden). → Mitigation: Feature flag `SOCIAL_LOGIN_ENABLED` in system settings.
- **[localStorage wizard state could contain stale data]** → Schema versioning handles structural changes. For data staleness (e.g., institutions list changed), the restore flow validates against current reference data and discards invalid selections with a toast notification.
- **[Notification center for 17 types requires 17 rendering templates]** → Group types into rendering categories (success, warning, info, action-required) with a generic fallback. Type-specific templates are added incrementally — the initial release covers the 5 most common types with specific templates, rest use the category fallback.
- **[Anonymous favorites sync on login could create duplicates]** → The sync endpoint uses `ON CONFLICT DO NOTHING` (upsert with unique constraint). Duplicates are silently merged.
- **[Map view requires buildings to have geocoded coordinates]** → Buildings without lat/lng are excluded from the map but still visible in list view. A warning badge on the admin building page indicates "Not geocoded — will not appear on map."
- **[Institution coordinates for distance calculation may not exist]** → Fallback: if no institution coordinates, distance column is hidden. Adding institution lat/lng is part of this change.

## Migration Plan

1. **Sprint 4 (Weeks 1-2):** Deploy waitlist position query, favorites table + API, Google OAuth2 config. All are additive — no existing behavior changes. Feature flags: `SOCIAL_LOGIN_ENABLED`.
2. **Sprint 5 (Weeks 3-4):** Deploy notification center (`read_at` column migration), map view (frontend-only, uses existing data), wizard persistence (frontend-only). The `read_at` migration is backward-compatible (nullable column).
3. **Sprint 6 (Weeks 5-6):** Deploy SSE endpoint, availability subscription table. SSE replaces polling in the `OfferExpiryComponent` — the fallback is to keep the 60-second polling if SSE connection fails.

**Rollback:** Each sub-sprint's changes are independently deployable and reversible. Database migrations are additive (new columns, new tables). Frontend feature flags can disable new UI without backend changes.

## Open Questions

- **Institution coordinates:** Does the `institution` table have latitude/longitude fields? If not, a migration is needed. Alternatively, use the first building's coordinates as a campus center approximation.
- **Notification type enum completeness:** The system has "17 notification types" per the research. The exact enum values need to be catalogued before building type-specific templates.
- **SSE authentication:** SSE connections via `EventSource` cannot set custom headers. The JWT token must be passed as a query parameter (`/api/sse/offers?token=...`) or use cookie-based auth for the SSE endpoint specifically. Decision needed.
