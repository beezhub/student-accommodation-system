## ADDED Requirements

### Requirement: Skeleton loader component exists
A shared `SkeletonLoaderComponent` SHALL exist at `app/shared/skeleton-loader/` and be usable via the selector `app-skeleton-loader`.

#### Scenario: Component renders without errors
- **WHEN** `<app-skeleton-loader>` is placed in a template
- **THEN** it SHALL render without console errors

### Requirement: Three skeleton types
The component SHALL accept a `type` input with values `'card'`, `'table-row'`, and `'text-block'`. Default SHALL be `'table-row'`.

#### Scenario: Card type renders card skeletons
- **WHEN** `[type]="'card'"` is set
- **THEN** the component SHALL render tall rounded skeleton blocks (border-radius matching `$radius-xl`)

#### Scenario: Table-row type renders row skeletons
- **WHEN** `[type]="'table-row'"` is set
- **THEN** the component SHALL render short horizontal skeleton rows matching a table row height

#### Scenario: Text-block type renders text lines
- **WHEN** `[type]="'text-block'"` is set
- **THEN** the component SHALL render stacked short line skeletons

### Requirement: Count input controls repetition
The component SHALL accept a `count` input of type `number` (default 3) that controls how many skeleton items are rendered.

#### Scenario: Count of 8 renders 8 rows
- **WHEN** `[count]="8"` is set
- **THEN** the component SHALL render exactly 8 skeleton rows

### Requirement: Shimmer animation
Each skeleton element SHALL display a left-to-right shimmer animation using a CSS `linear-gradient` over a `$slate-100` base.

#### Scenario: Shimmer is visible on skeleton elements
- **WHEN** a skeleton loader is rendered in a standard browser environment
- **THEN** a shimmer highlight SHALL animate across the skeleton element at 1.5s intervals

### Requirement: prefers-reduced-motion support
The shimmer animation SHALL be disabled when the user's OS preference is `prefers-reduced-motion: reduce`.

#### Scenario: Static skeleton in reduced-motion mode
- **WHEN** the browser reports `prefers-reduced-motion: reduce`
- **THEN** skeleton elements SHALL display as static `$slate-100` blocks with no animation

### Requirement: Page-level loading states use skeleton loaders
All admin page components with page-level loading states SHALL use `<app-skeleton-loader>` instead of `<mat-spinner>` inside `.loading-container` divs.

#### Scenario: No page-level mat-spinner in admin pages
- **WHEN** any of the 11 target admin page components enter their loading state
- **THEN** `<app-skeleton-loader>` is shown, not `<mat-spinner>`
