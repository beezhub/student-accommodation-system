# AI Context Injection System

This system ensures AI agents always have access to project guidelines and context.

## Auto-Include Files for AI Agents

### Primary Context Files

These files should always be included in AI agent context:

1. **`AI_AGENT_GUIDELINES.md`** - Primary quick reference
2. **`.github/instructions/student-accomodation.instructions.md`** - Detailed project instructions
3. **`docs/DEVELOPMENT_WORKFLOW.md`** - Development process guidelines
4. **`tools/ai-prompts/README.md`** - Prompt engineering standards

### Secondary Context Files (Include as needed)

- **`docs/BUSINESS_REQUIREMENTS.md`** - Business context and requirements
- **`docs/architecture/*.md`** - Architecture decision records
- **`tools/ai-prompts/templates/*.md`** - Specific prompt templates

## Implementation Strategies

### 1. File Naming Convention

Use consistent prefixes to make files discoverable:

- `AI_AGENT_*` - Files specifically for AI context
- `GUIDELINES_*` - Specific guideline documents
- `CONTEXT_*` - Additional context files

### 2. AI Instructions in Code Comments

Add special comments in key files:

```typescript
/**
 * AI_CONTEXT: This component follows Single Responsibility Principle
 * Always use Angular Material with Apple-inspired design
 * Implement OnPush change detection and comprehensive tests
 * Refer to AI_AGENT_GUIDELINES.md for complete standards
 */
```

### 3. Workspace Configuration

Create VS Code settings to highlight important files:

```json
// .vscode/settings.json
{
  "files.associations": {
    "**/AI_AGENT_*.md": "markdown",
    "**/GUIDELINES_*.md": "markdown"
  },
  "workbench.colorCustomizations": {
    "tab.activeBorder": "#007ACC",
    "tab.activeForeground": "#007ACC"
  }
}
```

## Context Injection Methods

### Method 1: Explicit File References

When starting any AI conversation, reference these files:

```markdown
Please reference these guidelines while working:

- AI_AGENT_GUIDELINES.md (primary standards)
- .github/instructions/student-accomodation.instructions.md
- docs/DEVELOPMENT_WORKFLOW.md

Ensure all generated code follows our Single Responsibility Principle,
uses Angular Material with Apple design, and includes comprehensive tests.
```

### Method 2: Auto-Include Prompts

Create reusable prompt snippets:

```markdown
## SYSTEM_CONTEXT

You are working on the Student Accommodation System.
Key principles:

- Single Responsibility Principle for all components
- Angular Material with Apple-inspired design
- Test-Driven Development (80%+ coverage)
- Cloud-native, container-first architecture
- Open-source technology stack

Always check AI_AGENT_GUIDELINES.md for complete standards.
```

### Method 3: Template Headers

Add headers to all prompt templates:

```markdown
---
CONTEXT: Student Accommodation System
ARCHITECTURE: Single Responsibility + Cloud Native
UI_FRAMEWORK: Angular Material + Apple Design
TESTING: TDD with 80%+ coverage
GUIDELINES: See AI_AGENT_GUIDELINES.md
---
```

## AI Agent Onboarding Checklist

For any new AI agent or conversation:

### Phase 1: Context Loading

- [ ] Load AI_AGENT_GUIDELINES.md
- [ ] Review project structure and key files
- [ ] Understand Single Responsibility Principle application
- [ ] Familiarize with Apple-inspired design requirements
- [ ] Review TDD workflow and testing standards

### Phase 2: Pattern Recognition

- [ ] Study existing component patterns
- [ ] Understand service architecture
- [ ] Review API design patterns
- [ ] Learn project-specific conventions
- [ ] Identify reusable code templates

### Phase 3: Quality Standards

- [ ] Understand test coverage requirements (80%+)
- [ ] Learn error handling patterns
- [ ] Review accessibility requirements (WCAG 2.1 AA)
- [ ] Understand performance optimization needs
- [ ] Learn security implementation patterns

## Maintaining Context Consistency

### Regular Updates

- Update AI_AGENT_GUIDELINES.md when patterns change
- Keep prompt templates synchronized with guidelines
- Document new patterns and anti-patterns
- Review and refresh context files quarterly

### Version Control

- Tag major guideline updates
- Maintain changelog for context files
- Archive outdated patterns
- Track effectiveness of different approaches

### Quality Assurance

- Regular audits of AI-generated code against guidelines
- Feedback loop to improve context documents
- Team reviews of guideline effectiveness
- Continuous improvement based on results

## Advanced Context Strategies

### Contextual Code Comments

Embed context directly in source code:

```typescript
// AI_PATTERN: Standard Angular service following SRP
// Always inject dependencies, use RxJS for async operations
// Include error handling and logging
// Reference: AI_AGENT_GUIDELINES.md - Service Generation

@Injectable({ providedIn: "root" })
export class StudentService {
  // Implementation follows established patterns
}
```

### Smart File Organization

Organize files to make patterns discoverable:

```
src/
├── _patterns/           # Reusable code patterns
│   ├── component.template.ts
│   ├── service.template.ts
│   └── test.template.spec.ts
├── _guidelines/         # Embedded guidelines
│   ├── naming-conventions.md
│   └── code-patterns.md
└── app/                 # Application code
```

### Automated Context Injection

Create scripts to automatically include context:

```bash
#!/bin/bash
# inject-context.sh
echo "Including AI context for: $1"
echo "Guidelines: AI_AGENT_GUIDELINES.md"
echo "Instructions: .github/instructions/student-accomodation.instructions.md"
echo "Workflow: docs/DEVELOPMENT_WORKFLOW.md"
```

## Context File Templates

### For New Features

```markdown
## AI Context for [Feature Name]

### Business Context

[Brief description of what this feature does]

### Technical Context

[Architecture patterns to follow]

### Testing Context

[Specific testing requirements]

### Guidelines Reference

- Primary: AI_AGENT_GUIDELINES.md
- Architecture: docs/architecture/[relevant-adr].md
- Patterns: tools/ai-prompts/templates/[template].md
```

### For Bug Fixes

```markdown
## AI Context for Bug Fix

### Problem Description

[What's broken and why]

### Solution Approach

[How to fix while maintaining patterns]

### Testing Strategy

[How to prevent regression]

### Guidelines Reference

- Quality standards: AI_AGENT_GUIDELINES.md
- Testing patterns: tools/ai-prompts/templates/test-generation.md
```

## Monitoring Context Effectiveness

### Success Metrics

- Consistency of AI-generated code with guidelines
- Reduction in code review feedback
- Faster development velocity
- Higher test coverage maintenance
- Better adherence to architectural principles

### Feedback Collection

- Track which context files are most referenced
- Monitor AI agent performance with different context levels
- Collect team feedback on context usefulness
- Measure time saved vs context overhead

### Continuous Improvement

- Regular context document reviews
- Update based on common AI mistakes
- Refine based on successful patterns
- Evolve with project maturity

---

**Key Principle**: Make guidelines so accessible and clear that any AI agent can immediately understand and apply project standards consistently.
