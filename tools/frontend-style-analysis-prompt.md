# Frontend Styling & Responsiveness Analysis Prompt

You are an expert frontend developer and UI/UX auditor. Analyze this Angular application's frontend code for styling inconsistencies and responsiveness issues against the provided design system.

## Your Task

Systematically review all component styles (.scss/.css files) and templates (.html files) in the frontend to identify:
1. Deviations from the design system
2. Responsiveness problems across device sizes
3. Inconsistent implementations between similar components

---

## Design System Reference

### Module Color Themes

| Module | Primary Gradient | Accent Color |
|--------|------------------|--------------|
| Dashboard | `#8b5cf6` → `#7c3aed` | Purple |
| Applications | `#3b82f6` → `#1d4ed8` | Blue |
| Documents | `#3b82f6` → `#1d4ed8` | Blue |
| Users | `#8b5cf6` → `#7c3aed` | Purple |
| Buildings | `#f59e0b` → `#d97706` | Orange |
| Rooms | `#06b6d4` → `#0891b2` | Cyan |
| Allocations | `#8b5cf6` → `#7c3aed` | Purple |
| Institutions | `#8b5cf6` → `#7c3aed` | Purple |
| Year of Study | `#10b981` → `#059669` | Green |
| Activity Logs | `#64748b` → `#475569` | Gray |
| Settings | `#64748b` → `#475569` | Gray |

### Neutrals (Slate Scale)

| Token | Hex | Usage |
|-------|-----|-------|
| Slate 50 | `#f8fafc` | Page backgrounds, card fills |
| Slate 100 | `#f1f5f9` | Table headers, dividers, hover states |
| Slate 200 | `#e2e8f0` | Borders, card outlines |
| Slate 300 | `#cbd5e1` | Scrollbar thumbs, disabled borders |
| Slate 400 | `#94a3b8` | Placeholder text, disabled icons |
| Slate 500 | `#64748b` | Secondary text, labels, hints |
| Slate 600 | `#475569` | Body text, descriptions |
| Slate 700 | `#334155` | Table headers, strong labels |
| Slate 800 | `#1e293b` | Primary text, headings |
| Slate 900 | `#0f172a` | Deep emphasis text |

### Functional Status Colors

| Status | Text/Icon | Background Gradient | Border |
|--------|-----------|---------------------|--------|
| Success | `#059669` | `#ecfdf5` → `#d1fae5` | `#a7f3d0` |
| Warning/Pending | `#d97706` | `#fffbeb` → `#fef3c7` | `#fcd34d` |
| Error/Danger | `#dc2626` | `#fef2f2` → `#fee2e2` | `#fca5a5` |
| Info/Primary | `#1d4ed8` | `#eff6ff` → `#dbeafe` | `#93c5fd` |
| Neutral | `#64748b` | `#f8fafc` → `#f1f5f9` | `#e2e8f0` |

### Typography Scale

| Element | Size | Weight | Additional |
|---------|------|--------|------------|
| Page Title (H1) | 32px | 700 | Gradient text with `-webkit-background-clip: text` |
| Dialog Title (H2) | 22px | 700 | Color: Slate 800 (`#1e293b`) |
| Section Header | 20px | 700 | With icon, Slate 800 |
| Card Title | 16-18px | 600 | Slate 800 |
| Table Header | 12px | 700 | Uppercase, `letter-spacing: 0.05em`, Slate 700 |
| Body Text | 14px | 400 | Slate 600 (`#475569`) |
| Field Labels | 12px | 600 | Uppercase, `letter-spacing: 0.03em`, Slate 500 |
| Hints/Captions | 12-13px | 500 | Slate 400 |
| Badge Text | 11px | 600 | Uppercase, `letter-spacing: 0.03em` |
| Stat Values | 24-32px | 700 | Module accent color |

### Border Radius Scale

| Size | Radius | Usage |
|------|--------|-------|
| Small | 6-8px | Badges, small buttons |
| Medium | 10-12px | Action buttons, input fields, icon wrappers |
| Large | 16-20px | Cards, dialogs, filter cards |
| XL | 24px | Main table cards |
| Pill | 20px | Status badges |
| Circle | 50% | Avatars, round icons |

### Shadow Scale

| Level | Shadow | Usage |
|-------|--------|-------|
| Subtle | `0 4px 20px rgba(0, 0, 0, 0.04)` | Filter cards, info cards |
| Medium | `0 8px 32px rgba(0, 0, 0, 0.06)` | Table cards, main content |
| Elevated | `0 12px 40px rgba(0, 0, 0, 0.1)` | Hover states, dialogs |

### Icon Sizing

| Context | Size |
|---------|------|
| Inline/Text | 14-16px |
| Cell/Card | 18-22px |
| Header | 24-28px |
| Page Title | 36px |
| Empty State | 48px |
| Dialog Header | 32px |

### Responsive Breakpoints

| Breakpoint | Width | Required Adjustments |
|------------|-------|----------------------|
| Desktop | >1024px | Full layout, side-by-side elements |
| Tablet | 768-1024px | Stack page header, 2-column grids |
| Mobile | <768px | Single column, full-width buttons, padding: 16px, title: 24px |
| Small Mobile | <480px | Hide stats, compact padding |

### Required Component Patterns

**Container:** `padding: 24px; max-width: 1400px; margin: 0 auto;`

**Filter Card:** `border-radius: 20px; border: 1px solid rgba(226, 232, 240, 0.8); box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04); background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);`

**Table Card:** `border-radius: 24px; border: 1px solid rgba(226, 232, 240, 0.6); box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06);`

**Status Badge:** `padding: 6px 12px; border-radius: 20px; font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.03em;`

**Action Buttons:** `border-radius: 10px; transition: all 0.2s ease;` with `transform: scale(1.1)` on hover

**Dialog:** `min-width: 440-520px;` with centered header, 64px icon wrapper with 16px border-radius

**Empty State:** Centered with 100px circular icon container, 80px padding

---

## Analysis Checklist

### 1. Color Audit
- [ ] Module pages use correct accent colors per theme table
- [ ] All neutral colors use Slate scale (no arbitrary grays)
- [ ] Status indicators use correct functional colors
- [ ] No hardcoded colors that should be tokens
- [ ] Gradients follow the 135deg or 180deg patterns

### 2. Typography Audit
- [ ] Page titles: 32px/700/gradient
- [ ] Table headers: 12px/700/uppercase/0.05em spacing
- [ ] Body text: 14px/400/Slate 600
- [ ] Labels: 12px/600/uppercase/0.03em spacing
- [ ] Badges: 11px/600/uppercase
- [ ] Consistent font-weight usage

### 3. Spacing & Layout Audit
- [ ] Container has proper padding (24px desktop, 16px mobile)
- [ ] Consistent gap values (8px, 12px, 16px, 20px, 24px)
- [ ] Filter forms use `grid-template-columns: repeat(auto-fit, minmax(180px, 1fr))`
- [ ] Proper margin-bottom between sections (24px)

### 4. Component Pattern Audit
- [ ] Filter cards: 20px border-radius, gradient background
- [ ] Table cards: 24px border-radius, correct shadow
- [ ] Status badges: pill shape, gradient backgrounds, borders
- [ ] Action buttons: 10px radius, hover scale effect
- [ ] Dialogs: proper min-width, centered header pattern
- [ ] Empty states: centered, 100px icon circle
- [ ] Avatar circles: gradient background, centered initials

### 5. Responsiveness Audit
- [ ] Media query at 1024px: header stacks vertically
- [ ] Media query at 768px: single column filters, full-width buttons
- [ ] Media query at 480px: stats hidden
- [ ] Tables horizontally scrollable on mobile
- [ ] Touch targets minimum 44px on mobile
- [ ] No horizontal overflow causing scroll
- [ ] Text doesn't overflow containers

### 6. Interaction States Audit
- [ ] All interactive elements have hover states
- [ ] Focus states for accessibility
- [ ] Transitions use `all 0.2s ease`
- [ ] Table rows have hover background
- [ ] Buttons scale or change on hover

### 7. Consistency Audit
- [ ] Similar components styled identically across modules
- [ ] Same patterns for tables, cards, filters across features
- [ ] Icons consistently sized per context
- [ ] Shadows consistently applied

---

## Output Format

For each issue found, report in this structured format:

```
### Issue #[N]: [Brief Title]

**File:** `path/to/file.scss` (lines X-Y)
**Severity:** Critical | Major | Minor
**Category:** Color | Typography | Spacing | Component | Responsiveness | Consistency

**Current Implementation:**
```css
[code snippet]
```

**Expected (per Style Guide):**
```css
[correct code]
```

**Impact:** [Describe the visual/UX impact]

**Fix:** [Specific change needed]
```

---

## Priority Levels

- **Critical:** Broken layouts, inaccessible elements, major visual bugs on any device
- **Major:** Significant deviations from design system, poor mobile experience
- **Minor:** Small inconsistencies, missing hover states, slight spacing issues

---

## Summary Requirements

After analysis, provide:

1. **Executive Summary:** Overall health score (1-10) with key findings
2. **Issues by Severity:** Count of Critical/Major/Minor issues
3. **Issues by Category:** Distribution across Color/Typography/Spacing/etc.
4. **Most Affected Files:** Top 5 files needing attention
5. **Quick Wins:** Easy fixes with high impact
6. **Recommendations:** Prioritized action plan

Begin your analysis by examining all `.scss`, `.css`, and `.html` files in the `frontend/src/app` directory structure.
