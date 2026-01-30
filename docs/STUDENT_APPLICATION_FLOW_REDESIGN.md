# Plan: Redesign Student Application Flow

## Summary

Align the student application flow with the restrictions-based allocation system:
- **Remove unused preferences** (preferredRoomType - not used in allocation)
- **Admin-configurable accommodation types** (new master data entity)
- **Explicit accessibility checkbox** (replaces text parsing)
- **Use profile gender** for room matching (no separate preference)

---

## Current State → Target State

| Aspect | Current | Target |
|--------|---------|--------|
| Accommodation Types | Hardcoded (SINGLE, DOUBLE, STUDIO, ENSUITE) | Admin-configurable master data |
| Room Type | Hardcoded (STANDARD, PREMIUM, etc.) - NOT USED | **Remove entirely** |
| Accessibility | Parsed from specialRequirements text | Explicit checkbox field |
| Gender preference | N/A | Use student profile gender |
| Building selection | Filtered by institution | Keep as-is |

---

## Implementation Steps

### Phase 1: Backend - AccommodationType Entity & CRUD

**New files to create:**

1. **Entity:** `backend/src/main/java/com/beezhub/student_accommodation/model/entity/AccommodationType.java`
   - Fields: `id`, `name` (unique), `description`, `isActive`, `createdAt`, `updatedAt`

2. **DTOs:**
   - `model/dto/AccommodationTypeRequest.java` - name (required), description, isActive
   - `model/dto/AccommodationTypeResponse.java` - all fields

3. **Repository:** `repository/AccommodationTypeRepository.java`
   - `findByIsActiveTrue()` - for student form
   - `existsByName()` - for validation

4. **Mapper:** `mapper/AccommodationTypeMapper.java`

5. **Service:** `service/AccommodationTypeService.java`
   - `getAllActive()`, `getAll(pageable)`, `getById()`, `create()`, `update()`, `delete()`

6. **Controller:** `controller/AccommodationTypeController.java`
   - `GET /api/accommodation-types/active` - public, for student forms
   - `GET /api/accommodation-types` - admin only, paginated
   - `POST/PUT/DELETE` - admin only

7. **Migration:** `db/migration/V{date}__create_accommodation_type.sql`
   ```sql
   CREATE TABLE accommodation_type (
       id BIGSERIAL PRIMARY KEY,
       name VARCHAR(100) NOT NULL UNIQUE,
       description VARCHAR(500),
       is_active BOOLEAN NOT NULL DEFAULT TRUE,
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
   );

   -- Seed defaults
   INSERT INTO accommodation_type (name, description, is_active) VALUES
       ('Single Room', 'Private room for one occupant', TRUE),
       ('Double Room', 'Shared room for two occupants', TRUE),
       ('Studio Apartment', 'Self-contained studio with kitchenette', TRUE),
       ('En-suite Room', 'Private room with attached bathroom', TRUE);
   ```

---

### Phase 2: Backend - Application Entity Changes

**File:** `backend/src/main/java/com/beezhub/student_accommodation/model/entity/Application.java`
- Add: `@Column(name = "requires_accessibility") private Boolean requiresAccessibility = false;`
- Keep: `preferredAccommodationType` as String (stores type name)
- Deprecate/Remove: `preferredRoomType` field

**File:** `model/dto/ApplicationRequest.java`
- Add: `private Boolean requiresAccessibility;`
- Remove: `preferredRoomType`

**File:** `model/dto/ApplicationResponse.java`
- Add: `private Boolean requiresAccessibility;`
- Remove: `preferredRoomType`

**File:** `service/ApplicationService.java`
- Update `submitApplication()` to set `requiresAccessibility`
- Remove `preferredRoomType` handling

**Migration:**
```sql
ALTER TABLE application ADD COLUMN requires_accessibility BOOLEAN DEFAULT FALSE;
-- Migrate existing data
UPDATE application SET requires_accessibility = TRUE
WHERE preferred_room_type = 'ACCESSIBLE';
```

---

### Phase 3: Backend - Eligibility Filter Update

**File:** `backend/src/main/java/com/beezhub/student_accommodation/service/AllocationScoringService.java`

Update `hasAccessibilityRequirements()` method:
```java
private boolean hasAccessibilityRequirements(Student student, Application application) {
    // Primary: check application's explicit accessibility flag
    if (application != null && Boolean.TRUE.equals(application.getRequiresAccessibility())) {
        return true;
    }
    // Fallback: parse student's specialRequirements text (legacy)
    // ... existing keyword parsing logic
}
```

Update `isAccessibilityCompliant()` to accept Application parameter.

---

### Phase 4: Frontend - Accommodation Type Service & Model

**New file:** `frontend/src/app/core/services/accommodation-type.service.ts`
```typescript
@Injectable({ providedIn: 'root' })
export class AccommodationTypeService {
  private apiUrl = `${environment.apiUrl}/accommodation-types`;

  getActiveTypes(): Observable<AccommodationTypeResponse[]>
  getAll(filters): Observable<PageResponse<AccommodationTypeResponse>>
  create(request): Observable<AccommodationTypeResponse>
  update(id, request): Observable<AccommodationTypeResponse>
  delete(id): Observable<void>
}
```

**New file:** `frontend/src/app/core/models/accommodation-type.model.ts`

---

### Phase 5: Frontend - Accommodation Type Management Page

**New directory:** `frontend/src/app/features/admin/accommodation-type-management/`

Follow pattern from `institution-management/`:
- `accommodation-type-management.component.ts/html/css` - table with CRUD
- `accommodation-type-form-dialog/` - create/edit dialog

**Form fields:**
- Name (required)
- Description (optional)
- Is Active (toggle)

**Add route:** `frontend/src/app/app-routing.module.ts`
```typescript
{ path: 'accommodation-types', loadComponent: () => ... }
```

---

### Phase 6: Frontend - Application Submission Changes

**File:** `frontend/src/app/features/auth/application-submission/application-submission.component.ts`

Changes:
1. Inject `AccommodationTypeService`
2. Add `accommodationTypes: AccommodationTypeResponse[] = []`
3. Remove hardcoded `accommodationTypes` and `roomTypeOptions` arrays
4. Call `loadAccommodationTypes()` in `ngOnInit()`
5. Update form:
   ```typescript
   this.applicationForm = this.fb.group({
     preferredAccommodationType: ['', Validators.required],
     preferredMoveInDate: ['', Validators.required],
     preferredDurationMonths: ['', [Validators.required, Validators.min(1), Validators.max(12)]],
     // REMOVED: preferredRoomType
     requiresAccessibility: [false],  // NEW
     preferredBuildingId: [''],
     specialRequirements: [''],
     courseName: ['', Validators.required],
   });
   ```
6. Update `onSubmit()` to include `requiresAccessibility`

**File:** `frontend/src/app/features/auth/application-submission/application-submission.component.html`

Changes:
1. Update accommodation type dropdown to use dynamic `accommodationTypes`
2. **Remove** room type dropdown entirely (lines ~121-136)
3. Add accessibility checkbox:
   ```html
   <mat-checkbox formControlName="requiresAccessibility" color="primary">
     I require an accessible room
   </mat-checkbox>
   ```
4. Add `MatCheckboxModule` to imports

---

### Phase 7: Frontend - Admin Dashboard Link

**File:** `frontend/src/app/features/admin/admin-dashboard/admin-dashboard.component.ts`

Add to `quickLinks` array:
```typescript
{
  title: 'Accommodation Types',
  description: 'Manage accommodation type options',
  icon: 'hotel',
  route: '/admin/accommodation-types',
}
```

---

## Critical Files Summary

### Backend (New)
- `model/entity/AccommodationType.java`
- `model/dto/AccommodationTypeRequest.java`
- `model/dto/AccommodationTypeResponse.java`
- `repository/AccommodationTypeRepository.java`
- `mapper/AccommodationTypeMapper.java`
- `service/AccommodationTypeService.java`
- `controller/AccommodationTypeController.java`

### Backend (Modify)
- `model/entity/Application.java` - add requiresAccessibility
- `model/dto/ApplicationRequest.java` - add requiresAccessibility, remove preferredRoomType
- `model/dto/ApplicationResponse.java` - add requiresAccessibility, remove preferredRoomType
- `service/ApplicationService.java` - handle new field
- `service/AllocationScoringService.java` - use Application.requiresAccessibility

### Frontend (New)
- `core/services/accommodation-type.service.ts`
- `core/models/accommodation-type.model.ts`
- `features/admin/accommodation-type-management/` (component + dialog)

### Frontend (Modify)
- `features/auth/application-submission/application-submission.component.ts` - form changes
- `features/auth/application-submission/application-submission.component.html` - UI changes
- `features/admin/admin-dashboard/admin-dashboard.component.ts` - add link
- `app-routing.module.ts` - add route
- `core/models/application.model.ts` - update interface

---

## Verification Steps

1. **Accommodation Type Management:**
   - Admin can create/edit/delete accommodation types
   - Toggle active/inactive status
   - Student form only shows active types

2. **Application Submission:**
   - Student sees admin-configured accommodation types (not hardcoded)
   - Room type field no longer appears
   - Accessibility checkbox works and saves
   - Form validates correctly

3. **Allocation Eligibility:**
   - Student with `requiresAccessibility=true` only sees accessible rooms
   - Student with `requiresAccessibility=false` sees all rooms
   - Legacy text parsing still works as fallback

4. **Data Migration:**
   - Existing applications with `preferredRoomType=ACCESSIBLE` have `requiresAccessibility=true`
   - New applications have correct field values

---

## Out of Scope

- Changing how building preference works (already filtered by institution)
- Student profile changes (gender already captured)
- Room browsing for students
