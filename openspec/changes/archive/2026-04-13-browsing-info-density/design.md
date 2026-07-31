## Context

The UX research (Finding F-1, Strong confidence) identified zero information density as a catastrophe-level issue: building pages show no pricing, no rooms, no amenities, no availability. The `room` table has no price column — this is not a frontend-only change. The browse page currently loads all buildings in one API call (`getAllBuildingsNoPagination`) with client-side text search only.

The backend uses Spring Boot 3.5.7 with Spring Data JPA. The frontend uses Angular 21 with Material Design. Building browse and detail are public (unauthenticated) pages governed by the `public-building-browse` spec.

## Goals / Non-Goals

**Goals:**
- Add a `monthly_rate` column to the `room` table so pricing can be stored per room
- Expose pricing summary (min price, room count, available room count) in the building list API response
- Expose room-level detail (price, capacity, status) in the building detail API response
- Display pricing range and availability on building browse cards
- Display a room types table with pricing on the building detail page
- Replace the client-side-only browse with server-side pagination and filtering (price range, availability, institution)
- Add a price input field to the admin room management form

**Non-Goals:**
- No general-purpose amenities/facilities system in this sprint (the only amenity-adjacent field is `building_attributes.accessibility_features` JSONB — surfacing it is in-scope, but building a full amenities CRUD system is not)
- No map view or distance calculation (Sprint 4+, R-22)
- No save/favorite functionality (Sprint 4+, R-21)
- No room type taxonomy — rooms are identified by capacity and room number, not a separate type entity
- No real-time availability updates (Sprint 4+, R-26)

## Decisions

### D1: Add `monthly_rate` as BigDecimal column on `room` table

**Decision:** Add a nullable `monthly_rate NUMERIC(10,2)` column to the `room` table via Flyway migration. The field is nullable because existing rooms have no pricing data — admin must populate prices.

**Rationale:** Pricing belongs on the room (not the building) because different rooms in the same building can have different prices. BigDecimal/NUMERIC avoids floating-point rounding issues for currency.

**Alternative considered:** A separate `room_pricing` table with effective dates for time-varying pricing. Rejected as premature — a simple column covers the current use case. Can be evolved later.

### D2: Building list API returns pricing and availability summary

**Decision:** Modify the `BuildingResponse` DTO (used by `GET /api/building/all` and `GET /api/building`) to include three new computed fields: `minMonthlyRate` (minimum across all rooms), `maxMonthlyRate` (maximum across all rooms), and `availableRoomCount` (count of rooms with status AVAILABLE). These are computed at query time via JPA projections or `@Formula`.

**Rationale:** Computing these per-building at query time avoids denormalization and keeps data fresh. For the current scale the performance cost is negligible. If scale demands it, a materialized view or denormalized column can be added later.

**Alternative considered:** Denormalize into `building` table columns updated by a trigger or event. Rejected — adds complexity for no performance benefit at current scale.

### D3: Building detail API returns room summaries

**Decision:** The building detail endpoint (`GET /api/building/{id}`) response will include a `rooms` array containing room summaries: `roomNumber`, `capacity`, `monthlyRate`, `status`, `currentOccupancy`. This reuses the existing `Room` to `RoomResponse` mapping with the new `monthlyRate` field.

**Rationale:** Room data is already associated with buildings in the schema. The detail endpoint just needs to include it.

### D4: Server-side pagination with Spring Data Pageable

**Decision:** Add a new paginated endpoint `GET /api/building/browse` that accepts: `page`, `size`, `sort`, `minPrice`, `maxPrice`, `available` (boolean), `institutionId`. Implemented using JPA `Specification` pattern (already used elsewhere in the codebase — see `specification/` package). The existing `GET /api/building/all` endpoint remains unchanged for backwards compatibility.

**Rationale:** The `Specification` pattern is already established in the codebase. A new endpoint avoids breaking existing admin consumers of `GET /api/building/all`.

**Alternative considered:** Add filter parameters to the existing `/all` endpoint. Rejected — changing the unpaginated endpoint to paginated is a breaking change.

### D5: Browse card layout uses existing Material card pattern

**Decision:** Extend the existing `mat-card` layout in `building-browse.component.html` to include price range, availability badge, and room count. No new component — the card template is modified in place.

**Rationale:** The existing card structure is suitable. Adding fields is simpler than creating a new component.

### D6: Detail page adds a "Rooms & Pricing" section

**Decision:** Add a new section below the existing description on the building detail page with a Material table showing room information grouped by capacity. Columns: capacity label (e.g., "Single", "Double" for capacity 1, 2), price range within that capacity group, available/total count.

**Rationale:** Grouping by capacity is the closest analog to "room type" without introducing a type taxonomy. Students care about "how many people share the room" more than room numbers.

### D7: Admin room management form gains price field

**Decision:** Add a `monthly_rate` input field (type number, min 0, step 0.01) to the existing admin room create/edit form. The field is optional — admin can leave it blank for rooms where pricing is not yet determined.

**Rationale:** The admin UI is the data entry point. Without it, pricing data cannot be populated.

## Risks / Trade-offs

- **[Existing rooms have no pricing data]** → The `monthly_rate` column is nullable. Browse cards will show "Price on request" when no rooms have prices. Admin must populate prices for the feature to be useful. Migration includes no default values.
- **[New paginated endpoint duplicates some browse logic]** → Acceptable trade-off for backwards compatibility. The old unpaginated endpoint can be deprecated later.
- **[No room type entity]** → Grouping rooms by capacity is an approximation. If the platform later needs named room types (e.g., "Deluxe Single"), a `room_type` entity will be needed. This design does not preclude that evolution.
- **[Price filtering requires rooms to have prices]** → The filter treats rooms with `null` price as excluded from price range filtering. Buildings with no priced rooms will not appear in price-filtered results.
