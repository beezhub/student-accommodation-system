# Angular Component Generation Template

Use this template when generating new Angular components with AI assistance.

## Prompt Template

```markdown
Create an Angular 18+ standalone component following these specifications:

## Component Context
- **Component Name**: [ComponentName]
- **Purpose**: [Brief description of component functionality]
- **Feature Module**: [Which feature module this belongs to]
- **Parent/Child Relationships**: [If applicable]

## Requirements
- Implement [specific functionality]
- Follow Single Responsibility Principle
- Use Angular Material components with Apple-inspired design
- Implement proper TypeScript types
- Include accessibility features (WCAG 2.1 AA)
- Support responsive design

## Technical Specifications
- Use standalone component architecture
- Implement OnPush change detection strategy
- Use reactive forms if form handling is required
- Include proper error handling
- Follow Angular style guide conventions
- Use RxJS for reactive programming where applicable

## Design Requirements
- Apply Apple-inspired design principles:
  - Clean, minimalist interface
  - Consistent spacing and typography
  - Smooth animations and transitions
  - Clear visual hierarchy
- Use Angular Material components:
  - [Specify required Material components]
- Implement responsive breakpoints:
  - Mobile: 576px and below
  - Tablet: 768px and below
  - Desktop: 992px and above

## Testing Requirements
- Generate comprehensive unit tests using Jasmine
- Include tests for:
  - Component initialization
  - User interactions
  - Input/Output properties
  - Edge cases and error scenarios
- Achieve minimum 80% code coverage
- Include accessibility tests with @angular/cdk/a11y

## File Structure
Generate these files:
- component-name.component.ts
- component-name.component.html
- component-name.component.scss
- component-name.component.spec.ts

## Code Style
- Use TypeScript strict mode
- Follow Angular naming conventions
- Include JSDoc comments for public methods
- Use consistent indentation (2 spaces)
- Apply Prettier formatting

## Example Usage
[Provide usage example if applicable]

## Dependencies
List any additional dependencies or imports required.
```

## Example Implementation Request

```markdown
Create an Angular component for displaying student application status with the following requirements:

## Component Context
- **Component Name**: ApplicationStatusComponent
- **Purpose**: Display current status of student accommodation applications with timeline
- **Feature Module**: Dashboard
- **Parent/Child Relationships**: Used in DashboardComponent and ApplicationDetailsComponent

## Requirements
- Display application status with color-coded badges
- Show application timeline with status changes
- Allow status filtering
- Support refresh functionality
- Handle loading and error states

## Technical Specifications
- Use standalone component architecture
- Implement OnPush change detection strategy
- Use Angular Material components (mat-card, mat-chip, mat-progress-spinner)
- Include proper TypeScript interfaces for application data
- Implement accessibility features (ARIA labels, screen reader support)

## Design Requirements
- Apple-inspired clean card layout
- Status badges with appropriate colors (green for approved, orange for pending, red for rejected)
- Timeline component showing status progression
- Responsive design for mobile and desktop
- Smooth loading animations

## Testing Requirements
- Test component initialization and data loading
- Test status filtering functionality
- Test error handling scenarios
- Test accessibility compliance
- Mock external dependencies
```

## Quality Checklist

Before submitting AI-generated component code:

- [ ] Component follows Single Responsibility Principle
- [ ] Uses standalone architecture
- [ ] Implements proper TypeScript typing
- [ ] Includes comprehensive unit tests
- [ ] Follows Angular style guide
- [ ] Implements accessibility features
- [ ] Uses Angular Material components correctly
- [ ] Follows Apple-inspired design principles
- [ ] Includes proper error handling
- [ ] Has responsive design implementation
- [ ] Code is properly formatted and documented
