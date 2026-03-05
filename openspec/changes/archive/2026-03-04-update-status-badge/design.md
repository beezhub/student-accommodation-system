## Context

Angular's `ViewEncapsulation.Emulated` (the default) adds attribute selectors to component-defined styles so they don't leak out, but global styles still cascade in normally. This means `.status-warning` defined in `styles.scss` applies to elements inside `ModernStatusBadge` today. However, self-contained components should not rely on global styles for their own visual appearance — that creates invisible coupling between the component and the host application's stylesheet.

The four missing classes and their correct values (taken directly from `styles.scss` and UISpec.md section 2.1.3):

| Class | Background | Text colour | Border |
|-------|-----------|-------------|--------|
| `status-warning` | `#fef3c7` (warning-bg) | `#b45309` (warning) | `#fcd34d` (warning-border) |
| `status-info` | `#e0f2fe` (info-bg) | `#0369a1` (info) | `#7dd3fc` (info-border) |
| `status-neutral` | `#f1f5f9` (slate-100) | `#475569` (slate-600) | `#e2e8f0` (slate-200) |
| `status-changes` | `#fff7ed` | `#c2410c` | `#fb923c` |

## Goals / Non-Goals

**Goals:**
- All status colour classes used by the component are defined within the component
- Labels render with correct capitalisation without relying on CSS transforms

**Non-Goals:**
- Changing any label text, icon, or config mapping
- Moving the component from inline styles to a separate `.scss` file
- Removing the global `.status-*` classes from `styles.scss` (they may be used elsewhere)

## Decisions

### Keep global classes in styles.scss too

The global `status-info`, `status-neutral`, `status-changes`, `status-warning` classes in `styles.scss` should remain — they may be used by other components (e.g., a plain `<span class="status-info">` outside the badge). Adding them to the component does not create duplication problems; it only ensures the component is self-sufficient.

### Use CSS custom property fallbacks (consistent with existing styles)

The new class definitions will use the same `var(--token, fallback)` pattern already used in the component. This means if `:root` custom properties are ever defined, the component will automatically pick them up.

## Risks / Trade-offs

**[Risk] Slight values divergence if global styles.scss classes are updated but component styles are not**
→ Mitigation: The values are simple functional colours unlikely to change. When the design system evolves, both locations should be updated — a comment in the component pointing to UISpec.md section 2.1.3 makes this clear.

**[Risk] `text-transform: capitalize` removal could affect a status whose label is lowercase in the config**
→ Mitigation: Every config entry already uses title-case labels ("Under Review", "Changes Requested", etc.). Verified by reading the full config map.
