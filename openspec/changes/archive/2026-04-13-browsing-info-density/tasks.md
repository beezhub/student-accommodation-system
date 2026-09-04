## 1. Database Migration and Room Entity

- [x] 1.1 Create Flyway migration to add `monthly_rate NUMERIC(10,2) NULL` column to `room` table
- [x] 1.2 Add `monthlyRate` (BigDecimal) field to `Room` entity with `@Column(name = "monthly_rate")`
- [x] 1.3 Add `@Min(0)` validation on `monthlyRate` field
- [x] 1.4 Add `monthlyRate` field to `RoomRequest` DTO (optional)
- [x] 1.5 Add `monthlyRate` field to `RoomResponse` DTO
- [x] 1.6 Update `RoomMapper` (MapStruct) to map `monthlyRate` between entity and DTOs
- [x] 1.7 Verify room CRUD operations work with the new field (create, update, read with and without pricing)

## 2. Building API - Pricing and Availability Summary

- [x] 2.1 Add `minMonthlyRate`, `maxMonthlyRate` (BigDecimal), and `availableRoomCount` (Integer) fields to `BuildingResponse` DTO
- [x] 2.2 Add computed pricing/availability logic in `BuildingService` or `BuildingMapper` — query min/max monthly_rate and count of AVAILABLE rooms per building
- [x] 2.3 Ensure `GET /api/building/all` and `GET /api/building/{id}` responses include the new summary fields
- [x] 2.4 Ensure `GET /api/building/{id}` response includes a `rooms` array with room summaries (roomNumber, capacity, monthlyRate, status, currentOccupancy)

## 3. Server-Side Paginated Browse Endpoint

- [x] 3.1 Create `BuildingBrowseSpecification` in `specification/` package implementing JPA `Specification<Building>` with filters: `minPrice`, `maxPrice`, `available`, `institutionId`
- [x] 3.2 Add `GET /api/building/browse` endpoint to `BuildingController` accepting `Pageable` + filter parameters, returning `Page<BuildingResponse>`
- [x] 3.3 Ensure the endpoint is publicly accessible (no authentication required) — update security config if needed
- [ ] 3.4 Write tests for the specification: price range filtering, availability filtering, institution filtering, combined filters

## 4. Admin Room Management - Price Field

- [x] 4.1 Add "Monthly Rate" input field to admin room create/edit form (numeric, optional, min 0, step 0.01)
- [x] 4.2 Ensure the room form sends `monthlyRate` in the request body on create and update
- [x] 4.3 Display `monthlyRate` in the admin room list/table

## 5. Frontend - Building Browse Cards

- [x] 5.1 Update `BuildingService` to add a `browseBuildingsPaginated()` method calling `GET /api/building/browse` with filter and pagination parameters
- [x] 5.2 Update `building-browse.component.ts` to use the paginated endpoint instead of `getAllBuildingsNoPagination()`
- [x] 5.3 Add pricing range display to browse card template: "From R[min]/month", "R[min] - R[max]/month", or "Price on request"
- [x] 5.4 Add availability indicator to browse card: room count with green/amber/grey colour coding
- [x] 5.5 Add room capacity type chips to browse card (Single, Double, etc.) derived from room data
- [x] 5.6 Implement "Load more" button or infinite scroll for pagination

## 6. Frontend - Browse Filtering

- [x] 6.1 Add filter panel component to the browse page with: price range inputs (min/max), availability toggle, institution dropdown
- [x] 6.2 Wire filter panel to update query parameters on the paginated API call
- [x] 6.3 Sync filter state to URL query parameters for deep-linking and browser navigation
- [x] 6.4 Load initial filter state from URL query parameters on page load
- [x] 6.5 Reset pagination to page 0 when filters change

## 7. Frontend - Building Detail Page

- [x] 7.1 Add "Rooms & Pricing" section to building detail template with a table grouped by room capacity
- [x] 7.2 Implement capacity-to-label mapping (1=Single, 2=Double, 3=Triple, 4+=Shared)
- [x] 7.3 Display price range per capacity group and available/total count per group
- [x] 7.4 Show "Price on request" for capacity groups with no priced rooms and "Room information not yet available" empty state
- [x] 7.5 Add availability summary near building name ("X of Y rooms available")
- [x] 7.6 Add "Accessibility" section displaying `accessibilityFeatures` from building attributes when present

## 8. Frontend Model Updates

- [x] 8.1 Update `BuildingResponse` TypeScript interface to include `minMonthlyRate`, `maxMonthlyRate`, `availableRoomCount`, and `rooms` array
- [x] 8.2 Update or create `RoomSummary` TypeScript interface with `roomNumber`, `capacity`, `monthlyRate`, `status`, `currentOccupancy`

## 9. Verification

- [x] 9.1 Run backend tests (`./gradlew test`) and fix any failures
- [x] 9.2 Run frontend lint (`npm run lint`) and fix any errors
- [x] 9.3 Run frontend tests (`npm test`) and fix any failures
- [x] 9.4 Verify browse cards show pricing and availability for buildings with priced rooms
- [x] 9.5 Verify browse cards show "Price on request" for buildings without priced rooms
- [x] 9.6 Verify pagination loads buildings in pages of 12
- [x] 9.7 Verify price range filter excludes buildings outside the range
- [x] 9.8 Verify availability filter excludes fully occupied buildings
- [x] 9.9 Verify building detail page shows rooms and pricing table
- [x] 9.10 Verify filter state persists in URL and loads correctly from URL
