## ADDED Requirements

### Requirement: Admin routes suppress the global student toolbar
The application shell SHALL NOT render the `mat-toolbar` when the active route URL begins with `/admin`.

#### Scenario: Admin navigates to the admin dashboard
- **WHEN** an authenticated admin user navigates to `/admin`
- **THEN** the `mat-toolbar` is not present in the DOM

#### Scenario: Admin navigates to a nested admin route
- **WHEN** an authenticated admin user navigates to `/admin/applications`
- **THEN** the `mat-toolbar` is not present in the DOM

#### Scenario: Student navigates to their dashboard
- **WHEN** an authenticated student user navigates to `/dashboard`
- **THEN** the `mat-toolbar` IS present in the DOM

#### Scenario: Unauthenticated user visits the home page
- **WHEN** a visitor navigates to `/home`
- **THEN** the `mat-toolbar` IS present in the DOM

### Requirement: All unauthenticated auth routes suppress the global toolbar
The application shell SHALL NOT render the `mat-toolbar` on the routes `/login`, `/signup`, `/verify-email`, `/forgot-password`, and `/reset-password`.

#### Scenario: User visits the signup page
- **WHEN** a visitor navigates to `/signup`
- **THEN** the `mat-toolbar` is not present in the DOM

#### Scenario: User visits the forgot password page
- **WHEN** a visitor navigates to `/forgot-password`
- **THEN** the `mat-toolbar` is not present in the DOM

### Requirement: Admin page containers do not depend on toolbar height offset
Admin page container components SHALL NOT use a hardcoded `margin-top` or `min-height` expression derived from the toolbar height (64px) in their SCSS.

#### Scenario: Admin dashboard renders without top gap
- **WHEN** an admin user views the admin dashboard
- **THEN** the page content begins at the top of the content area with standard page padding (`$space-6`, 24px), and no 64px gap appears between the sidebar header and the first page element
