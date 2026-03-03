# Senior Business Analyst - Requirements Analysis

## 1. Problem Statement

**What problem are we solving?**
The current Angular 21 frontend of the Student Accommodation System has structural design inconsistencies and incomplete implementation of some critical UX patterns. Specifically, there are matching design files (`_variables.scss` and `_tokens.scss`) with conflicting values. The `ModernStatusBadge` component only supports half of the 24 statuses required by the state machine backends, resulting in generic fallbacks for important application states (e.g. `UNDER_REVIEW`, `CHANGES_REQUESTED`). There is also a lack of proper admin sidebar navigation (rendering 17 routes un-navigable via the UI), and the app overuses generic spinners instead of modern skeleton loaders for data fetching states.

**Background Context:**
The application is a functioning SPA with a baseline slate/purple aesthetic, but this technical debt in the styling layer blocks scale. Resolving this will provide a single source of truth for the styling tokens, complete the required state representations, and deliver the final layer of UX polish for a production-ready application. This primarily affects the frontend layer of the application.

---

## 2. Stakeholder Analysis

**Primary Stakeholders:**
| Role | Needs | Success Criteria |
|------|-------|------------------|
| Students | Intuitive UI, clear status indications, smooth application and profile workflows | Can easily understand the status of their application, smooth loading transitions |
| Administrators | Direct access to all management modules, visual differentiation of application/allocation statuses | Can navigate to all 17 admin routes via a persistent sidebar, immediate recognition of required actions |
| Development Team | Single source of truth for design tokens, reusable UI components | `_tokens.scss` removed, all styles reference `_variables.scss`, shared components for Skeleton and Empty States available |

**Secondary Stakeholders:**
- University Management (needs the system to appear professional, accessible, and polished)

---

## 3. Functional Requirements

### 3.1 Core Functionality

**FR-1: Token System Consolidation**
- **Priority**: Must Have
- **Description**: The system shall use `_variables.scss` exclusively as the source of truth for design tokens. `_tokens.scss` shall be removed, and all references to its variables updated to the canonical scale.
- **Business Rules**: 
  - Neutral and brand colours must map accurately to the canonical palette defined in the UI Spec.
  - Spacing variables must be multiples of the 4px base unit.
- **Validation Rules**:
  - Compiling the SCSS should result in no reference errors.

**FR-2: Comprehensive Status Badging**
- **Priority**: Must Have
- **Description**: The `ModernStatusBadge` component shall support all 24 required statuses across application, allocation, document, and room domains.
- **Business Rules**: 
  - Every status must pair a colour, an icon, and a human-readable text label.
  - Badges must have appropriate ARIA `role="status"` and accessible labels.
- **Validation Rules**:
  - `status-info`, `status-neutral`, and `status-changes` CSS classes must be present.

**FR-3: Admin Sidebar Navigation**
- **Priority**: Must Have
- **Description**: The application shall present a persistent sidebar navigation menu for users with the Admin role, providing access to all 17 admin routes.
- **Business Rules**: 
  - Sidebar is permanent on large screens (`>= 1024px`), collapsible icon-rail on medium screens, and hidden behind a hamburger menu on small screens.
  - Sidebar must reflect the domain hierarchy (Overview, Applications, Buildings & Rooms, etc.).

**FR-4: Skeleton Loaders**
- **Priority**: Should Have
- **Description**: A shared `SkeletonLoaderComponent` shall be implemented to replace `mat-spinner` for primary data-fetching loading states (tables, cards, text blocks).
- **Business Rules**: 
  - Must respect `prefers-reduced-motion` CSS media query by removing the shimmer animation.

**FR-5: Empty State Component**
- **Priority**: Should Have
- **Description**: A shared `EmptyStateComponent` shall be implemented for data-empty scenarios.
- **Business Rules**: 
  - Component requires an icon, title, description, and optional Call To Action (CTA).

### 3.2 User Workflows

**Workflow 1: Admin Navigation**
```
Admin Role:
1. Logs into the system.
2. Views the dashboard and persistent sidebar navigation on the left.
3. Clicks on "Applications" to view the application list without needing to know the direct URL.
```

---

## 4. User Stories with Acceptance Criteria

### Story 1: Admin Navigation Sidebar

**As an** Admin
**I want to** use a persistent sidebar menu
**So that** I can easily access all management modules without memorizing URLs.

**Acceptance Criteria:**

```gherkin
Given I am logged in as an Admin
  And I am viewing the application on a desktop browser (>= 1024px)
When I look at the application shell
Then I should see a persistent sidebar navigation
  And the sidebar should contain links to Dashboard, Applications, Documents, Staged Allocations, Waiting List, Buildings, and Rooms
```

**Edge Cases:**
- **EC-1**: Screen width is reduced below 1024px -> Sidebar transitions to a collapsible icon rail or hamburger menu trigger.

### Story 2: Complete Status Indicators

**As a** User (Student or Admin)
**I want to** see accurate, plain-language status badges for my applications and allocations
**So that** I instantly understand the current state and what action is required.

**Acceptance Criteria:**

```gherkin
Given an Application is in the "CHANGES_REQUESTED" state
When I view the application on the dashboard
Then the status badge should display the label "Changes Requested"
  And the badge should use an orange warning color scheme and the "edit_note" icon
  And the badge should include aria-labels for screen readers
```

### Story 3: Improved Data Loading Experience

**As a** User
**I want to** see skeleton schemas while data is loading
**So that** I understand the page layout before the data finishes fetching, without a jarring blank screen.

**Acceptance Criteria:**

```gherkin
Given I navigate to a page with data tables or cards
When the data is actively being fetched from the backend
Then I should see skeleton loader animations mimicking the expected layout
  And when data loading completes, the skeletons are replaced with the actual data
```

---

## 5. Non-Functional Requirements

### 5.1 Performance
- **NFR-P1**: Replacing `mat-spinner` with skeleton loaders should not negatively impact the Core Web Vitals (Cumulative Layout Shift should actually improve).

### 5.2 Accessibility
- **NFR-A1**: All new components (`SkeletonLoaderComponent`, `EmptyStateComponent`) and updated `ModernStatusBadge` must meet WCAG 2.1 AA accessiblity standards.
- **NFR-A2**: Implement `prefers-reduced-motion` globally to pause CSS animations for users who request it.
- **NFR-A3**: Icon-only buttons must contain proper `aria-label` attributes contextually.

### 5.3 Usability
- **NFR-U1**: Navigation must be fully responsive across `xs`, `sm`, `md`, `lg`, and `xl` breakpoints.

---

## 6. Data Requirements

### 6.1 Data Relationships
- No backend database changes are required for this pure UI/UX scope. 
- All ENUMs in the UI (Status strings) must exactly map to the existing backend representations for Applications, Allocations, and Documents.

---

## 7. Integration Points

### 7.1 Internal Integrations
- Components integrate with existing Angular feature modules (`features/admin`, `features/student`, `features/dashboard`).

---

## 8. UI/UX Requirements

This specification *is* implementing the UI/UX Requirements document (`UISpec.md`). The core updates affect:
- **Global Theme**: `styles.scss` and `_variables.scss`
- **Shell Layout**: `AppNavMenuComponent` or application shell
- **Shared Components**: `ModernStatusBadgeComponent`, `SkeletonLoaderComponent`, `EmptyStateComponent`

---

## 9. Business Rules and Constraints

**BR-1**: Single Design Token Source
- **Rule**: No new styles should introduce hard-coded colours or spacing. All must reference `_variables.scss`.
- **Rationale**: Eliminates technical debt and prevents UI inconsistencies.

**BR-2**: Badge Consistency
- **Rule**: A status indicator must never rely on colour alone.
- **Rationale**: Accessibility requirements (colour blindness) require both text and icon pairing.

---

## 10. Success Metrics

| Metric | Target | Measurement Method |
|--------|--------|-------------------|
| Token consistency | 0 | Number of SCSS files referencing `_tokens.scss` |
| Status coverage | 100% | All 24 statuses from the Requirements.md state machine correctly map to a visual badge |
| Accessbility Audits | 100% Pass | Lighthouse Accessibility score on updated pages |

---

## 11. Edge Cases and Error Scenarios

### 11.1 Edge Cases

**EC-1: Reduced Motion Preference**
- **Description**: User OS settings preferred reduced motion.
- **Expected Behavior**: Component animations (skeleton shimmer, card hover spring transformations) are disabled.
- **Frequency**: Occasional

**EC-2: Unrecognized Status Enum**
- **Description**: Backend returns a new or unmapped status string.
- **Expected Behavior**: `ModernStatusBadge` falls back to a neutral default style with the raw string capitalized, rather than failing/crashing.
- **Frequency**: Rare

---

## 12. Dependencies and Prerequisites

**Technical Dependencies:**
- Requires Angular 21 frontend and Material Design libraries to be fully functional.

**Feature Dependencies:**
- Depends on the existing auth layout and dashboard shells for rendering the admin sidebar.

---

## 13. Out of Scope

**Explicitly NOT included in this feature:**
- Modifying backend API endpoints or backend enum definitions.
- Changes to the authentication or authorization logic.
- Building a new backend capability.

---

## 14. Assumptions and Open Questions

**Assumptions:**
- Backend API is stable and properly providing the status strings as denoted by existing requirements.

---

## 15. Approval and Sign-off

**Business Owner**: UI/UX Designer
**Estimated Business Value**: Medium/High (Reduces debt, highly improves usability)
