## 1. Database Migrations

- [ ] 1.1 Create Flyway migration: `building_favorite` table (`id BIGSERIAL PK, tenant_id BIGINT FK, student_id BIGINT FK, building_id BIGINT FK, created_at TIMESTAMPTZ DEFAULT NOW()`) with unique constraint on `(tenant_id, student_id, building_id)`
- [ ] 1.2 Create Flyway migration: `availability_subscription` table (`id BIGSERIAL PK, tenant_id BIGINT FK, student_id BIGINT FK, building_id BIGINT FK, created_at TIMESTAMPTZ DEFAULT NOW(), notified_at TIMESTAMPTZ NULL`) with unique constraint on `(tenant_id, student_id, building_id)`
- [ ] 1.3 Create Flyway migration: add `read_at TIMESTAMPTZ NULL` column to `user_notification` table
- [ ] 1.4 Create Flyway migration: add `google_subject_id VARCHAR(255) NULL` column to `app_user` table with unique index
- [ ] 1.5 Create Flyway migration: add `profile_photo_url VARCHAR(512) NULL` column to `app_user` table
- [ ] 1.6 Seed `SOCIAL_LOGIN_ENABLED` system setting with default value `false` for platform tenant (tenant_id=0)

## 2. Student Waitlist View (Backend)

- [ ] 2.1 Create `WaitlistController` with `GET /api/waitlist/position` endpoint (authenticated, student role)
- [ ] 2.2 Create `WaitlistService` with `getStudentWaitlistPosition()` using `ROW_NUMBER() OVER (ORDER BY created_at ASC)` ranked query scoped to tenant and academic year
- [ ] 2.3 Create `WaitlistPositionDto` with fields: `position`, `totalInQueue`, `estimatedWaitDays` (nullable)
- [ ] 2.4 Implement estimated wait time calculation based on average allocations per day over last 30 days; return null if fewer than 5 allocations exist
- [ ] 2.5 Return 404 when student has no active waitlist entry

## 3. Student Waitlist View (Frontend)

- [ ] 3.1 Create `WaitlistStatusComponent` standalone component with position indicator ("#N of M"), estimated wait, and notification promise text
- [ ] 3.2 Create `WaitlistService` Angular service calling `GET /api/waitlist/position`
- [ ] 3.3 Integrate `WaitlistStatusComponent` into student dashboard, displayed conditionally when application status is `WAITING_LIST`
- [ ] 3.4 Handle 404 response gracefully (hide waitlist card)

## 4. Save/Favorite Buildings (Backend)

- [ ] 4.1 Create `BuildingFavorite` JPA entity with tenant-aware mapping
- [ ] 4.2 Create `BuildingFavoriteRepository` with `findByTenantIdAndStudentId()` and `existsByTenantIdAndStudentIdAndBuildingId()`
- [ ] 4.3 Create `FavoriteController` with `POST /api/favorites/{buildingId}`, `DELETE /api/favorites/{buildingId}`, `GET /api/favorites`
- [ ] 4.4 Create `POST /api/favorites/sync` endpoint accepting JSON array of building IDs with upsert strategy
- [ ] 4.5 Create `FavoriteService` with add, remove, list, and sync methods
- [ ] 4.6 Return building summaries (id, name, address, primaryImage, minMonthlyRate) from `GET /api/favorites`

## 5. Save/Favorite Buildings (Frontend)

- [ ] 5.1 Create `FavoriteService` Angular service handling localStorage for anonymous users and backend API for authenticated users
- [ ] 5.2 Implement `syncFavoritesOnLogin()` method that POSTs localStorage favorites to `/api/favorites/sync` and clears localStorage on success
- [ ] 5.3 Call `syncFavoritesOnLogin()` in the auth login/signup success handler
- [ ] 5.4 Add favorite toggle icon (`favorite_border`/`favorite`) to building browse card template
- [ ] 5.5 Add favorite toggle button to building detail page header
- [ ] 5.6 Create `/favorites` route and `FavoritesListComponent` displaying favorited buildings as browse-style cards with unfavorite action and empty state
- [ ] 5.7 Pre-populate application building preference selector with favorites when student starts application wizard

## 6. Building Map View (Frontend)

- [ ] 6.1 Add map/list toggle control to building browse page header; persist view mode in URL query parameter `view=map|list`
- [ ] 6.2 Create `BuildingMapComponent` using Google Maps JavaScript API (existing `GOOGLE_MAPS_FRONTEND_API_KEY`)
- [ ] 6.3 Plot building markers from latitude/longitude; exclude buildings without coordinates
- [ ] 6.4 Implement marker clustering using `@googlemaps/markerclusterer` library
- [ ] 6.5 Implement info window on marker click: building name, thumbnail, price range, "View Details" link
- [ ] 6.6 Implement distance-to-campus calculation using Haversine formula from student's institution coordinates
- [ ] 6.7 Display "N.N km from campus" on building cards and marker info windows for authenticated students with institution coordinates
- [ ] 6.8 Ensure map view respects active browse filters (price, availability, institution)

## 7. Wizard Save-and-Resume (Frontend)

- [ ] 7.1 Create `WizardPersistenceService` with `save(stepName, userId, formState)`, `restore(stepName, userId)`, `clear(stepName, userId)`, and `clearAll(userId)` methods using localStorage
- [ ] 7.2 Add `schemaVersion` number to saved state; discard and notify if version mismatch on restore
- [ ] 7.3 Integrate auto-save (debounced 500ms on `valueChanges`) into `profile-setup.component.ts`
- [ ] 7.4 Integrate auto-save into `application-submission.component.ts`
- [ ] 7.5 Integrate auto-save into `document-upload.component.ts` (save selected file names/types, not binary data)
- [ ] 7.6 Implement restore-on-load with snackbar notification ("We restored your previous progress")
- [ ] 7.7 Validate restored reference data (institution ID, year of study ID) against current server data; clear invalid values with helper text
- [ ] 7.8 Add "Start fresh" action to each step that clears saved state and resets form
- [ ] 7.9 Clear all wizard localStorage entries on successful submission completion

## 8. Social Login — Google (Backend)

- [ ] 8.1 Add Spring Security OAuth2 Client dependency to `build.gradle`
- [ ] 8.2 Configure Google OAuth2 provider in `application.yml` with `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` env vars
- [ ] 8.3 Create `OAuth2LoginSuccessHandler` that creates/links `AppUser` and issues JWT token
- [ ] 8.4 Implement account creation for new Google users (role=STUDENT, store google_subject_id, name from Google profile)
- [ ] 8.5 Implement account linking for existing users (match by email, store google_subject_id)
- [ ] 8.6 Redirect to frontend `/auth/callback#token={jwt}` after successful OAuth2 callback
- [ ] 8.7 Store Google profile photo URL in `profile_photo_url` column on account creation
- [ ] 8.8 Gate OAuth2 endpoints behind `SOCIAL_LOGIN_ENABLED` system setting (return 404 when disabled)

## 9. Social Login — Google (Frontend)

- [ ] 9.1 Create `/auth/callback` route and `AuthCallbackComponent` that extracts JWT from URL fragment and stores it
- [ ] 9.2 Add "Sign in with Google" button to login page with Google brand guidelines styling
- [ ] 9.3 Add "Sign up with Google" button to signup page
- [ ] 9.4 Conditionally render Google buttons based on `SOCIAL_LOGIN_ENABLED` setting (fetch on auth page load)
- [ ] 9.5 Wire Google buttons to redirect to `/oauth2/authorization/google`

## 10. Real-Time Offer Updates (Backend)

- [ ] 10.1 Create `SseController` with `GET /api/sse/offers` endpoint using `SseEmitter` (5-minute timeout)
- [ ] 10.2 Implement JWT authentication via query parameter for SSE endpoint
- [ ] 10.3 Implement 30-second keepalive comments on idle connections
- [ ] 10.4 Publish `offer-update` and `new-offer` events via `ApplicationEventPublisher` when allocation status changes
- [ ] 10.5 Subscribe SSE emitters to the event bus; deliver events scoped to the specific student

## 11. Real-Time Offer Updates (Frontend)

- [ ] 11.1 Create `SseService` Angular service wrapping `EventSource` with JWT query param and exponential backoff reconnection (1s → 30s max)
- [ ] 11.2 Refactor `OfferExpiryComponent` to subscribe to SSE stream for offer-update events
- [ ] 11.3 Implement polling fallback: if SSE fails after 3 retries, activate 60-second polling
- [ ] 11.4 Clean up both SSE subscription and polling interval on component destroy

## 12. Notification Center (Backend)

- [ ] 12.1 Update `UserNotification` entity to include `readAt` (Timestamp, nullable) field
- [ ] 12.2 Create `NotificationController` with `GET /api/notifications` (paginated, supports `unreadOnly` filter)
- [ ] 12.3 Create `PUT /api/notifications/{id}/read` endpoint to mark single notification as read
- [ ] 12.4 Create `PUT /api/notifications/read-all` endpoint to mark all student's notifications as read
- [ ] 12.5 Create `NotificationResponseDto` with id, type, message, createdAt, readAt fields
- [ ] 12.6 Add unread count query to support bell badge

## 13. Notification Center (Frontend)

- [ ] 13.1 Create `/notifications` route and `NotificationCenterComponent` with paginated list (20 per page)
- [ ] 13.2 Implement type-specific rendering templates for top 5 notification types (OFFER_RECEIVED, APPLICATION_STATUS_CHANGE, DOCUMENT_VERIFIED, ALLOCATION_CONFIRMED, OFFER_EXPIRING) with specific icons, colors, and action buttons
- [ ] 13.3 Implement generic fallback template (bell icon, neutral color, message text) for remaining types
- [ ] 13.4 Implement read/unread visual treatment (light blue background + dot indicator for unread)
- [ ] 13.5 Implement click-to-mark-as-read behavior calling `PUT /api/notifications/{id}/read`
- [ ] 13.6 Add "Mark all as read" button calling `PUT /api/notifications/read-all`
- [ ] 13.7 Add unread count badge to existing notification bell icon
- [ ] 13.8 Add "View all notifications" link in bell dropdown navigating to `/notifications`

## 14. "Notify Me" Availability Subscription (Backend)

- [ ] 14.1 Create `AvailabilitySubscription` JPA entity with tenant-aware mapping
- [ ] 14.2 Create `AvailabilitySubscriptionRepository`
- [ ] 14.3 Create `AvailabilitySubscriptionController` with `POST`, `DELETE`, `GET /api/availability-subscriptions/{buildingId}`
- [ ] 14.4 Create `AvailabilitySubscriptionService` with subscribe, unsubscribe, and check-status methods
- [ ] 14.5 Implement room vacancy listener: when `Room.status` changes to `AVAILABLE` and building was at 0 available rooms, notify all subscribers and set `notified_at`
- [ ] 14.6 Send both email and in-app notification to subscribers on room vacancy

## 15. "Notify Me" Availability Subscription (Frontend)

- [ ] 15.1 Create `NotifyMeButtonComponent` with subscribe/unsubscribe toggle (outline bell → filled bell)
- [ ] 15.2 Fetch subscription status on component init via `GET /api/availability-subscriptions/{buildingId}`
- [ ] 15.3 Add `NotifyMeButtonComponent` to building detail page, visible only when `availableRoomCount === 0` and user is authenticated
- [ ] 15.4 Implement unsubscribe confirmation dialog

## 16. Modified Capability: Offer Lifecycle — Waitlist Feedback

- [ ] 16.1 Update `RoomAllocationService.declineOffer()` to create waitlist entry and return the new position
- [ ] 16.2 Display snackbar "You've been placed back on the waiting list at position #N" after decline action

## 17. Verification

- [ ] 17.1 Run backend build and tests (`./gradlew clean build`)
- [ ] 17.2 Run frontend lint (`npm run lint`)
- [ ] 17.3 Run frontend tests (`npm test`)
- [ ] 17.4 Verify all new database migrations apply cleanly (`./gradlew flywayMigrate` or application startup)
- [ ] 17.5 Verify feature flag `SOCIAL_LOGIN_ENABLED=false` hides all Google Sign-In UI and returns 404 on OAuth2 endpoints
