## Why

After fixing broken UX (Sprint 1), enriching browsing (Sprint 2), and building trust (Sprint 3), BeeZhub still lacks the engagement and transparency features that retain students post-application. The UX research identified 9 roadmap-level features (R-15, R-20, R-21, R-22, R-24, R-25, R-26, R-27, R-28) that address post-submission anxiety (Finding F-4, Strong confidence), engagement gaps (Finding F-6, Moderate confidence), and conversion barriers (Finding F-3b, Strong confidence). No competitor offers a student-facing waitlist view — this is BeeZhub's clearest differentiation opportunity. Save/favorite, map view, and wizard persistence address standard expectations. Social login and real-time updates reduce friction at both ends of the funnel.

## What Changes

- **Student waitlist view:** Surface waitlist position, queue depth, estimated wait time, and automatic notification promise on the student dashboard when application status is `WAITING_LIST` (R-15). No competitor does this — differentiator.
- **Save/favorite buildings:** Allow students to bookmark buildings from browse and detail pages using localStorage for anonymous users, persisted to backend after authentication (R-21). Favorites feed into application building preferences.
- **Map view with distance:** Add a map/list toggle to building browse with markers, cluster grouping, and distance-to-campus calculation from institution coordinates (R-22). Reuse existing Google Maps integration.
- **Wizard save-and-resume:** Persist wizard state (profile setup, application, document upload) to localStorage so students can close the browser and resume where they left off (R-24). Clear saved state on successful submission.
- **Social login (Google):** Add "Sign in with Google" as an alternative to email/password registration and login, reducing signup friction (R-25). Link to existing `app_user` accounts by email match.
- **Real-time offer updates:** Replace 60-second polling on offer expiry component with server-sent events (SSE) so students see offer status transitions and expiry countdown changes instantly (R-26).
- **Rich notification rendering:** Extend the notification dropdown and add a dedicated `/notifications` route with type-specific rendering for all 17 notification types — icons, action buttons, timestamps, and read/unread state (R-27).
- **"Notify me" for full buildings:** Allow students to subscribe to availability notifications for buildings at capacity. When a room opens, subscribed students receive an email and in-app notification (R-28).

## Capabilities

### New Capabilities
- `student-waitlist-view`: Student-facing waitlist transparency — position indicator, queue depth, estimated wait, notification promise, status-conditional dashboard rendering
- `save-favorite-buildings`: Building bookmarking with anonymous localStorage and authenticated persistence — browse card and detail page integration, favorites list page, application preference pre-fill
- `building-map-view`: Map/list toggle on building browse with Google Maps markers, clustering, distance-to-campus calculation, and marker-to-card interaction
- `wizard-save-resume`: Cross-session wizard state persistence via localStorage — auto-save on field changes, restore on return, stale-state detection, clear on completion
- `social-login-google`: Google OAuth2 sign-in integration — registration, login, account linking by email, profile photo import
- `real-time-offer-updates`: SSE-based push for offer lifecycle events — replaces polling, instant expiry transitions, reconnection with backoff
- `notification-center`: Dedicated notifications page and enhanced dropdown — type-specific rendering for all 17 types, read/unread tracking, action buttons, bulk operations
- `notify-me-availability`: Building availability subscription — subscribe/unsubscribe UI, backend subscription tracking, triggered notification on room vacancy

### Modified Capabilities
- `offer-lifecycle`: Waitlist re-queue behavior gains transparency requirements — student sees position after decline, waitlist entry is created with visible metadata
- `public-building-browse`: Browse page gains map/list toggle and favorite action on cards; detail page gains favorite action and "notify me" for full buildings
- `offer-expiry-urgency`: Polling mechanism replaced by SSE push; urgency tier model unchanged but data source shifts from timer-based fetch to server-push events

## Impact

- **Backend new:** SSE endpoint for offer events, Google OAuth2 security configuration, waitlist position query, favorites CRUD API, availability subscription API, notification read-status API
- **Backend modified:** `WaitingListEntry` entity gains `position` and `estimatedWait` fields; `Notification`/`UserNotification` gains `read` status tracking; `Room` availability changes trigger subscription notifications; Spring Security gains OAuth2 login provider
- **Database migrations:** New tables for `building_favorite` and `availability_subscription`; add columns to `waiting_list_entry` (`position`, `notified_at`); add `read_at` column to `user_notification`
- **Frontend new:** `WaitlistStatusComponent`, `FavoritesListComponent`, `BuildingMapComponent`, `NotificationCenterComponent`, `NotifyMeButtonComponent`, Google Sign-In button component
- **Frontend modified:** Building browse page (map toggle + favorite icons), building detail page (favorite + notify me), wizard step components (localStorage persistence), offer expiry component (SSE subscription), notification dropdown (enhanced rendering), auth pages (social login option), app routing (add `/notifications` route)
- **External dependencies:** Google OAuth2 client credentials (new), Google Maps JavaScript API (existing, extended usage)
- **Multi-sprint delivery:** Features are independent and can ship incrementally — waitlist + favorites + social login (Sprint 4), map + wizard persistence + notifications (Sprint 5), SSE + notify-me + messaging prep (Sprint 6)
