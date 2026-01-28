# Student Accommodation System - Design Style Guide

## 1. Color Palette

The design system uses a **Slate** neutral scale with **module-specific accent colors** for visual distinction between different admin sections.

### Module Color Themes

| Module | Primary Gradient | Accent Color | Usage |
|--------|------------------|--------------|-------|
| **Dashboard** | `#8b5cf6` → `#7c3aed` | Purple | Main admin overview |
| **Applications** | `#3b82f6` → `#1d4ed8` | Blue | Application management |
| **Documents** | `#3b82f6` → `#1d4ed8` | Blue | Document types & verification |
| **Users** | `#8b5cf6` → `#7c3aed` | Purple | User management |
| **Buildings** | `#f59e0b` → `#d97706` | Orange | Building management |
| **Rooms** | `#06b6d4` → `#0891b2` | Cyan | Room management |
| **Allocations** | `#8b5cf6` → `#7c3aed` | Purple | Room allocations |
| **Institutions** | `#8b5cf6` → `#7c3aed` | Purple | Educational institutions |
| **Year of Study** | `#10b981` → `#059669` | Green | Academic years |
| **Activity Logs** | `#64748b` → `#475569` | Gray | System logs |
| **Settings** | `#64748b` → `#475569` | Gray | System settings |

### Neutrals (Slate Scale)

| Color | Hex | Usage |
|-------|-----|-------|
| **Slate 50** | `#f8fafc` | Page backgrounds, card fills |
| **Slate 100** | `#f1f5f9` | Table headers, dividers, hover states |
| **Slate 200** | `#e2e8f0` | Borders, card outlines |
| **Slate 300** | `#cbd5e1` | Scrollbar thumbs, disabled borders |
| **Slate 400** | `#94a3b8` | Placeholder text, disabled icons |
| **Slate 500** | `#64748b` | Secondary text, labels, hints |
| **Slate 600** | `#475569` | Body text, descriptions |
| **Slate 700** | `#334155` | Table headers, strong labels |
| **Slate 800** | `#1e293b` | Primary text, headings |
| **Slate 900** | `#0f172a` | Deep emphasis text |

### Functional Status Colors

| Status | Text/Icon | Background Gradient | Border |
|--------|-----------|---------------------|--------|
| **Success** | `#059669` | `#ecfdf5` → `#d1fae5` | `#a7f3d0` |
| **Warning/Pending** | `#d97706` | `#fffbeb` → `#fef3c7` | `#fcd34d` |
| **Error/Danger** | `#dc2626` | `#fef2f2` → `#fee2e2` | `#fca5a5` |
| **Info/Primary** | `#1d4ed8` | `#eff6ff` → `#dbeafe` | `#93c5fd` |
| **Neutral** | `#64748b` | `#f8fafc` → `#f1f5f9` | `#e2e8f0` |

---

## 2. Typography

### Font Families
- **Primary:** System UI, `-apple-system`, `BlinkMacSystemFont`, `Segoe UI`, `Roboto`, `sans-serif`
- **Monospace:** `'SF Mono'`, `'Fira Code'`, `monospace` (IDs, codes, IP addresses)

### Type Scale

| Element | Size | Weight | Additional Properties |
|---------|------|--------|----------------------|
| **Page Title (H1)** | 32px | 700 | Gradient text with `-webkit-background-clip: text` |
| **Dialog Title (H2)** | 22px | 700 | Color: Slate 800 |
| **Section Header** | 20px | 700 | With icon, Slate 800 |
| **Card Title** | 16-18px | 600 | Slate 800 |
| **Table Header** | 12px | 700 | Uppercase, `letter-spacing: 0.05em`, Slate 700 |
| **Body Text** | 14px | 400 | Slate 600 |
| **Field Labels** | 12px | 600 | Uppercase, `letter-spacing: 0.03em`, Slate 500 |
| **Hints/Captions** | 12-13px | 500 | Slate 400 |
| **Badge Text** | 11px | 600 | Uppercase, `letter-spacing: 0.03em` |
| **Stat Values** | 24-32px | 700 | Module accent color |

---

## 3. Page Layout Pattern

### Container
```css
.container {
  padding: 24px;
  max-width: 1400px; /* or 1600px for wide tables */
  margin: 0 auto;
}
```

### Page Header
```css
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-content h1 {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 8px 0;
  background: linear-gradient(135deg, #1e293b 0%, [ACCENT_COLOR] 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.subtitle {
  color: #64748b;
  font-size: 15px;
  font-weight: 500;
}
```

### Header Actions (Stats + Button)
```css
.header-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 24px;
  background: linear-gradient(135deg, [LIGHT_ACCENT] 0%, [LIGHTER_ACCENT] 100%);
  border-radius: 12px;
  border: 1px solid [ACCENT_BORDER];
}

.add-btn {
  height: 48px;
  border-radius: 12px;
  padding: 0 24px;
  font-weight: 600;
}
```

---

## 4. Component Patterns

### Filter Card
```css
.filter-card {
  margin-bottom: 24px;
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.filter-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 14px;
  font-weight: 600;
  color: #475569;
}

.filter-form {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  align-items: start;
}

.filter-actions button {
  height: 56px;
  border-radius: 12px;
  font-weight: 600;
}
```

### Table Card
```css
.table-card {
  border-radius: 24px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

/* Table Header */
.table th {
  font-weight: 700;
  color: #334155;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 18px 16px;
  background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
  border-bottom: 2px solid #e2e8f0;
}

/* Table Cell */
.table td {
  padding: 14-16px;
  font-size: 14px;
  color: #475569;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
}

/* Row Hover */
.table tbody tr:hover {
  background: linear-gradient(90deg, [LIGHT_ACCENT] 0%, [LIGHTER_ACCENT] 50%, [LIGHT_ACCENT] 100%);
}
```

### Cell Patterns

#### Avatar Cell (Users, Students)
```css
.avatar {
  width: 36-40px;
  height: 36-40px;
  border-radius: 50%;
  background: linear-gradient(135deg, [ACCENT] 0%, [ACCENT_DARK] 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13-14px;
  font-weight: 700;
  color: #ffffff;
}
```

#### Icon Cell (Buildings, Rooms, Documents)
```css
.icon-wrapper {
  width: 36-44px;
  height: 36-44px;
  background: linear-gradient(135deg, [LIGHT_ACCENT] 0%, [LIGHTER_ACCENT] 100%);
  border-radius: 8-12px;
  border: 1px solid [ACCENT_BORDER];
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-wrapper mat-icon {
  font-size: 18-22px;
  color: [ACCENT];
}
```

#### Date Cell
```css
.date-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #64748b;
  font-size: 13px;
}

.date-cell mat-icon {
  font-size: 16px;
  color: #94a3b8;
}
```

### Status Badges
```css
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.status-badge mat-icon {
  font-size: 14px;
  width: 14px;
  height: 14px;
}

/* Status Variants */
.status-badge.success {
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  color: #059669;
  border: 1px solid #a7f3d0;
}

.status-badge.warning {
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
  color: #d97706;
  border: 1px solid #fcd34d;
}

.status-badge.error {
  background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
  color: #dc2626;
  border: 1px solid #fca5a5;
}

.status-badge.info {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  color: #1d4ed8;
  border: 1px solid #93c5fd;
}

.status-badge.neutral {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  color: #64748b;
  border: 1px solid #e2e8f0;
}
```

### Action Buttons
```css
.action-btn {
  transition: all 0.2s ease;
  border-radius: 10px;
  color: #94a3b8;
}

.action-btn:hover {
  transform: scale(1.1);
}

.action-btn.edit:hover {
  background: linear-gradient(135deg, [LIGHT_ACCENT] 0%, [LIGHTER_ACCENT] 100%);
  color: [ACCENT];
}

.action-btn.delete:hover {
  background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
  color: #dc2626;
}
```

### Empty State
```css
.no-data {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}

.no-data-icon {
  width: 100px;
  height: 100px;
  background: linear-gradient(135deg, [LIGHT_ACCENT] 0%, [LIGHTER_ACCENT] 100%);
  border-radius: 50%;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.no-data-icon mat-icon {
  font-size: 48px;
  color: [ACCENT];
}

.no-data h3 {
  font-size: 18px;
  font-weight: 600;
  color: #475569;
  margin: 0 0 8px 0;
}

.no-data p {
  font-size: 14px;
  color: #94a3b8;
  margin: 0 0 20px 0;
}
```

---

## 5. Dialog Patterns

### Dialog Container
```css
.dialog-container {
  min-width: 440-520px;
}

.dialog-header {
  text-align: center;
  padding: 24px 24px 0 24px;
}

.header-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: linear-gradient(135deg, [ACCENT] 0%, [ACCENT_DARK] 100%);
  margin-bottom: 16px;
}

.header-icon mat-icon {
  font-size: 32px;
  color: #ffffff;
}

.dialog-header h2 {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 6px 0;
}

.dialog-header p {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}
```

### Dialog Content
```css
mat-dialog-content {
  padding: 24px;
  max-height: 60vh;
}

.form-field-wrapper {
  margin-bottom: 16-20px;
}

.field-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.field-label mat-icon {
  font-size: 14px;
  color: #64748b;
}
```

### Dialog Actions
```css
mat-dialog-actions {
  padding: 16px 24px 24px 24px;
  gap: 12px;
  justify-content: flex-end;
}

.cancel-btn {
  height: 42px;
  border-radius: 10px;
  font-weight: 500;
  color: #64748b;
  border-color: #e2e8f0;
  padding: 0 20px;
}

.submit-btn {
  height: 42px;
  border-radius: 10px;
  font-weight: 600;
  padding: 0 20px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.submit-btn mat-icon {
  font-size: 18px;
}
```

---

## 6. Loading States

```css
.loading-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 80px;
  gap: 16px;
}

.loading-container p {
  color: #64748b;
  font-weight: 500;
  margin: 0;
}
```

---

## 7. Scrollbar Styling

```css
.table-wrapper::-webkit-scrollbar {
  height: 6px;
}

.table-wrapper::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 3px;
}

.table-wrapper::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.table-wrapper::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
```

---

## 8. Responsive Breakpoints

| Breakpoint | Width | Adjustments |
|------------|-------|-------------|
| **Desktop** | > 1024px | Full layout, side-by-side elements |
| **Tablet** | 768px - 1024px | Stack header, 2-column grids |
| **Mobile** | < 768px | Single column, full-width buttons |
| **Small Mobile** | < 480px | Hide stats, compact padding |

### Responsive Patterns
```css
@media (max-width: 1024px) {
  .page-header {
    flex-direction: column;
    gap: 16px;
  }
  .header-actions {
    width: 100%;
    justify-content: space-between;
  }
}

@media (max-width: 768px) {
  .container {
    padding: 16px;
  }
  .page-header h1 {
    font-size: 24px;
  }
  .filter-form {
    grid-template-columns: 1fr;
  }
  .filter-actions {
    flex-direction: column;
    width: 100%;
  }
  .add-btn {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .stat-item {
    display: none;
  }
}
```

---

## 9. Animation & Transitions

### Standard Transitions
```css
/* Hover effects */
transition: all 0.2s ease;

/* Card hover */
.card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.1);
}

/* Button scale */
.action-btn:hover {
  transform: scale(1.1);
}

/* Row hover */
.table tbody tr {
  transition: all 0.2s ease;
}
```

---

## 10. Shadow Scale

| Level | Shadow | Usage |
|-------|--------|-------|
| **Subtle** | `0 4px 20px rgba(0, 0, 0, 0.04)` | Filter cards, info cards |
| **Medium** | `0 8px 32px rgba(0, 0, 0, 0.06)` | Table cards, main content |
| **Elevated** | `0 12px 40px rgba(0, 0, 0, 0.1)` | Hover states, dialogs |
| **Accent** | `0 4px 12px rgba([ACCENT], 0.3)` | Accent badges, buttons |

---

## 11. Border Radius Scale

| Size | Radius | Usage |
|------|--------|-------|
| **Small** | `6-8px` | Badges, small buttons |
| **Medium** | `10-12px` | Action buttons, input fields, icon wrappers |
| **Large** | `16-20px` | Cards, dialogs |
| **XL** | `24px` | Main table cards |
| **Pill** | `20px` or `999px` | Status badges |
| **Circle** | `50%` | Avatars, round icons |

---

## 12. Icon Sizing

| Context | Size | Usage |
|---------|------|-------|
| **Inline/Text** | 14-16px | Labels, hints, small buttons |
| **Cell/Card** | 18-22px | Table cells, card icons |
| **Header** | 24-28px | Section headers |
| **Page Title** | 36px | Main page headers |
| **Empty State** | 48px | No-data illustrations |
| **Dialog Header** | 32px | Dialog icons |
