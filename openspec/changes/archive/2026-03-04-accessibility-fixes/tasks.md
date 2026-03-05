## Add aria-label to icon-only buttons in admin components

For each task, add `aria-label="<label>"` to the listed `<button mat-icon-button>` elements.
Where a button already has `matTooltip`, use the same text as the aria-label.

---

### 1. academic-year-management

- [x] 1.1 `academic-year-management.component.html` — add `aria-label` to 3 buttons at lines ~67, 75, 84:
  - Edit button → `aria-label="Edit"`
  - Set as Current button → `aria-label="Set as Current"`
  - Delete button → `aria-label="Delete"`

---

### 2. building-management

- [x] 2.1 `building-management.component.html` — desktop table view (lines ~85, 88, 91) missing aria-label:
  - Edit button → `aria-label="Edit Building"`
  - Delete button → `aria-label="Delete Building"`
  - Rules button → `aria-label="Allocation Rules"`
  - (Line ~82 Manage Images already has matTooltip — add matching `aria-label="Manage Images"`)

---

### 3. room-management

- [x] 3.1 `room-management.component.html` — desktop table view (lines ~123, 127, 131) missing aria-label:
  - Attributes button → `aria-label="Room Attributes"`
  - Edit button → `aria-label="Edit Room"`
  - Delete button → `aria-label="Delete Room"`

---

### 4. allocation-rule-management

- [x] 4.1 `allocation-rule-management.component.html` — card view (lines ~107, 110) missing aria-label:
  - Edit button → `aria-label="Edit Rule"`
  - Delete button → `aria-label="Delete Rule"`
  - (Lines ~215, 218 already have matTooltip — add matching `aria-label` for each)

---

### 5. application-review

- [x] 5.1 `application-review.component.html` — mobile view (lines ~166, 169, 172) missing aria-label:
  - Review button → `aria-label="Review Application"`
  - Preferences button → `aria-label="View Preferences"`
  - More actions button → `aria-label="More Actions"`
  - (Lines ~288, 292, 304 already have matTooltip — add matching `aria-label` for each)
  - Line ~296 button — check icon content and add appropriate label (e.g. `aria-label="Allocate"`)

---

### 6. room-allocation

- [x] 6.1 `room-allocation.component.html` — line ~151 missing aria-label:
  - Delete button → `aria-label="Delete Allocation"`
  - Lines ~237, 245, 253, 261 — read context and add appropriate labels (Confirm Check-In, Confirm Check-Out, Move Student, Delete Allocation)

---

### 7. document-verification

- [x] 7.1 `document-verification.component.html` — mobile view (lines ~118, 121) missing aria-label:
  - View button → `aria-label="View Document"`
  - Download button → `aria-label="Download"`

---

### 8. accommodation-type-management

- [x] 8.1 `accommodation-type-management.component.html` — lines ~135, 138 have matTooltip but no aria-label:
  - Edit button → `aria-label="Edit"`
  - Delete button → `aria-label="Delete"`

---

### 9. document-type-management

- [x] 9.1 `document-type-management.component.html` — lines ~178, 184, 190 missing aria-label:
  - Edit → `aria-label="Edit"`, Toggle → dynamic `[attr.aria-label]`, Delete → `aria-label="Delete"`

---

### 10. institution-management

- [x] 10.1 `institution-management.component.html` — lines ~185, 191 missing aria-label:
  - Edit → `aria-label="Edit"`, Delete → `aria-label="Delete"`

---

### 11. user-management

- [x] 11.1 `user-management.component.html` — line ~93 missing aria-label:
  - Delete button → `aria-label="Delete User"` (mobile + desktop)

---

### 12. building-image-dialog

- [x] 12.1 `building-image-dialog.component.html` — lines ~53, 62 missing aria-label:
  - Set as Primary → dynamic `[attr.aria-label]`, Delete Image → `aria-label="Delete Image"`

---

### 13. Verification

- [x] 13.1 Run `npm run build` in the worktree frontend and confirm zero new errors
