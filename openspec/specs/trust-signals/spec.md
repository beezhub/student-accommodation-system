## Requirements

### Requirement: Home page displays an enhanced "How it Works" section
The home/landing page SHALL display a "How it Works" section with at least 3 steps, each with a specific description (not generic), a visual icon, and concrete language. The section SHALL include a footer note about data handling (e.g., "Your data is encrypted and never shared with third parties").

#### Scenario: Visitor views the How it Works section
- **WHEN** an unauthenticated visitor loads the home page
- **THEN** the page displays a "How it Works" section with 3+ steps, each containing an icon, step title, and descriptive text
- **AND** a data handling assurance note is visible below the steps

#### Scenario: Step descriptions use concrete language
- **WHEN** the "How it Works" section is rendered
- **THEN** each step description SHALL reference specific actions (e.g., "Browse properties with transparent pricing", "Apply with your details", "Get matched within X days") rather than vague statements

---

### Requirement: Home page displays testimonials or institutional credibility section
The home page SHALL display a testimonials section with 2-3 student testimonials. If real testimonials are not yet available, the section SHALL display institutional credibility content: platform description, founding context, and partnership information. The section SHALL be structured with clear content slots to facilitate easy content updates.

#### Scenario: Testimonials section renders with content
- **WHEN** an unauthenticated visitor loads the home page
- **THEN** the page displays a testimonials or credibility section below the "How it Works" section
- **AND** each testimonial (or credibility item) includes a title/name, descriptive text, and optional attribution

#### Scenario: Testimonial content is easily replaceable
- **WHEN** a developer needs to update testimonial content
- **THEN** the content is defined in a clearly marked data structure (array of objects or configuration) separate from the template layout

---

### Requirement: Building detail pages display a security assurance section
Each building detail page SHALL display a "How We Protect Your Data" or equivalent security assurance section. The section SHALL explain document handling (how uploaded IDs are stored), and privacy commitments. The section SHALL be positioned below the main building content.

#### Scenario: Security assurance on building detail page
- **WHEN** a visitor views a building detail page
- **THEN** a security assurance section is visible explaining how documents are handled and data is protected

#### Scenario: Security assurance does not require authentication
- **WHEN** an unauthenticated visitor views a building detail page
- **THEN** the security assurance section is visible (no login required)

---

### Requirement: Home page displays a visible support contact
The home page SHALL display a support contact section with the support email address, and optionally business hours or alternative contact methods. The contact information SHALL be surfaced prominently, not buried in a footer link.

#### Scenario: Support contact is visible on home page
- **WHEN** a visitor loads the home page
- **THEN** a support contact section is visible with at least the support email address
