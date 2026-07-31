## Why

The UX research study (`ux-research/student-onboarding_consolidated-findings_v1.md`) identified building browsing as a **catastrophe-level** issue (Finding F-1, Strong confidence). Building pages currently show name, address, photos, and floor count — no pricing, no rooms, no amenities, no availability. Every competitor studied (Amber Student, Student.com, HousingAnywhere, DigsConnect, Varsity Vibe, Studypal) surfaces pricing publicly. Published research confirms that sites hiding pricing are immediately ruled out by users. The browse-to-signup funnel cannot convert without information to support decision-making. Additionally, the browse page loads all buildings in a single API call with client-side filtering — this will not scale.

## What Changes

- **Add pricing to rooms:** The `room` table currently has no price/rate field. Add a `monthly_rate` column to the room entity (backend migration), admin room management UI, and API responses
- **Add pricing range to building browse cards:** Display "From R[min_price]/month" on each building card, sourced from the minimum room price across all rooms in that building
- **Add pricing range to building detail pages:** Add a room types table showing type (via room number pattern or capacity), price/month, availability status, and capacity per room
- **Add room availability summary to browse cards:** Show availability indicator (e.g., "3 rooms available") on browse cards
- **Add amenities/attributes to building detail pages:** Surface building attributes (accessibility features) and add a general-purpose amenities system
- **Add server-side pagination and filtering:** Replace `getAllBuildingsNoPagination()` with paginated endpoint supporting filters: price range, room availability, institution

## Capabilities

### New Capabilities
- `room-pricing`: Room entity pricing field, admin management, and API exposure
- `building-browse-cards-v2`: Enhanced building browse cards with pricing, availability, and room type indicators
- `building-detail-v2`: Enhanced building detail page with room types table, amenities, and pricing
- `building-browse-filtering`: Server-side pagination and filtering for the building browse page

### Modified Capabilities
- `public-building-browse`: Browse card display requirements change (additional fields rendered), detail page display requirements change (additional sections)

## Impact

- **Backend entity change:** `Room` entity gains `monthlyRate` (BigDecimal) column — Flyway migration required
- **Backend DTO changes:** `RoomResponse` includes `monthlyRate`; `BuildingResponse` includes `minMonthlyRate`, `availableRoomCount`; new `RoomSummaryResponse` for detail page room table
- **Backend API changes:** Building list endpoint includes pricing/availability summary fields; building detail endpoint includes room summaries; new/modified query with pagination and filter parameters
- **Admin UI change:** Room management form gains a price field
- **Frontend components modified:** `building-browse.component`, `building-detail.component`, `building.service`
- **New frontend components:** Filter panel for browse page
- **Database migration:** Add `monthly_rate` column to `room` table
