# Architecture Decision Records (ADRs)

This directory contains Architecture Decision Records for the Student Accommodation System.

## About ADRs

Architecture Decision Records (ADRs) document important architectural decisions made during the development process. They provide context, reasoning, and consequences of technical choices.

## ADR Template

Each ADR should follow this structure:

```markdown
# ADR-[NUMBER]: [Title]

**Status**: [Proposed | Accepted | Rejected | Superseded]
**Date**: [YYYY-MM-DD]
**Deciders**: [List of people involved in the decision]

## Context
[Describe the issue that motivates the decision or change]

## Decision
[Describe the architectural decision and why it was chosen]

## Consequences
### Positive
- [List positive outcomes]

### Negative  
- [List negative outcomes or risks]

### Neutral
- [List neutral consequences]

## References
- [Links to relevant documentation or discussions]
```

## Current ADRs

### Infrastructure & Deployment
- [ADR-001: Container-First Development Strategy](001-container-first-strategy.md)
- [ADR-002: Microservices Architecture](002-microservices-architecture.md)
- [ADR-003: Cloud-Native Design Principles](003-cloud-native-design.md)

### Frontend Architecture
- [ADR-004: Angular Material with Apple Design System](004-angular-material-apple-design.md)
- [ADR-005: Single Responsibility Component Architecture](005-single-responsibility-components.md)
- [ADR-006: Standalone Components Strategy](006-standalone-components.md)

### Backend Architecture
- [ADR-007: Spring Boot Microservices](007-spring-boot-microservices.md)
- [ADR-008: Database Per Service Pattern](008-database-per-service.md)
- [ADR-009: Event-Driven Communication](009-event-driven-communication.md)

### Development Process
- [ADR-010: AI-Driven Test-First Development](010-ai-driven-tdd.md)
- [ADR-011: Prompt Engineering Standards](011-prompt-engineering-standards.md)
- [ADR-012: Code Review Process for AI-Generated Code](012-ai-code-review-process.md)

### Technology Choices
- [ADR-013: Open Source Technology Stack](013-open-source-stack.md)
- [ADR-014: PostgreSQL as Primary Database](014-postgresql-database.md)
- [ADR-015: MinIO for Object Storage](015-minio-object-storage.md)

## Creating New ADRs

1. Copy the template above
2. Number sequentially (next available number)
3. Use descriptive title
4. Fill in all sections completely
5. Get team review before marking as "Accepted"
6. Update this index when adding new ADRs

## ADR Lifecycle

- **Proposed**: Under discussion
- **Accepted**: Decision approved and being implemented
- **Rejected**: Decision was considered but rejected
- **Superseded**: Replaced by a newer ADR (link to replacement)
