# How to Use AI Agent Context System

This guide explains how to ensure AI agents always have access to project guidelines and maintain consistency across the team.

## 🎯 Overview

The AI Context System ensures that every AI agent working on the Student Accommodation System has immediate access to:

- Project guidelines and standards
- Architecture principles and patterns
- Development workflows and best practices
- Quality requirements and checklists

## 📁 Key Files for AI Context

### Primary Context Files (Always Include)

1. **`AI_AGENT_GUIDELINES.md`** - Complete quick reference for AI agents
2. **`.github/instructions/student-accomodation.instructions.md`** - Detailed project specifications
3. **`docs/DEVELOPMENT_WORKFLOW.md`** - TDD and AI-assisted development process
4. **`.ai-context.json`** - Machine-readable context configuration

### Secondary Context Files (As Needed)

- `docs/BUSINESS_REQUIREMENTS.md` - Business context
- `docs/architecture/*.md` - Architecture Decision Records
- `tools/ai-prompts/templates/*.md` - Specific prompt templates

## 🚀 Quick Start Methods

### Method 1: Run Context Loader Script

```bash
# Linux/Mac
./scripts/load-ai-context.sh

# Windows PowerShell
.\scripts\load-ai-context.ps1
```

### Method 2: Manual Context Loading

Copy and paste this at the start of any AI conversation:

```markdown
## SYSTEM CONTEXT - Student Accommodation System

You are working on an enterprise Angular + Spring Boot application with these requirements:

**Architecture**: Single Responsibility Principle + Cloud-Native design
**UI Framework**: Angular Material with Apple-inspired design
**Testing**: Test-Driven Development with 80%+ coverage
**Technology Stack**: Angular 18+, Spring Boot 3+, PostgreSQL, Docker

**Key Guidelines** (see AI_AGENT_GUIDELINES.md for complete reference):

- Every component serves a single responsibility
- Use Angular Material components with Apple-style customization
- Write tests first, then implement (TDD approach)
- Include comprehensive error handling and accessibility
- Follow TypeScript strict mode and proper typing
- Ensure container-ready, cloud-native implementation

Please confirm you understand these principles before proceeding.
```

### Method 3: Reference Specific Files

At the start of your AI session, reference the key files:

```markdown
Please load and reference these key project files:

- AI_AGENT_GUIDELINES.md (primary standards)
- .github/instructions/student-accomodation.instructions.md
- docs/DEVELOPMENT_WORKFLOW.md

Ensure all code generation follows our Single Responsibility Principle,
uses Angular Material with Apple design, and includes comprehensive tests.
```

## 🎨 Context for Specific Tasks

### For UI/Component Development

```markdown
Context: Angular Component Development

- Use standalone components with OnPush change detection
- Apply Angular Material with Apple-inspired styling
- Include accessibility attributes (ARIA labels, roles)
- Write comprehensive unit tests (80%+ coverage)
- Follow responsive design patterns

Reference: tools/ai-prompts/templates/component-generation.md
```

### For Service/API Development

```markdown
Context: Service Development

- Follow Single Responsibility Principle strictly
- Use RxJS for reactive programming patterns
- Include proper error handling and logging
- Write unit and integration tests
- Document with JSDoc/OpenAPI

Reference: tools/ai-prompts/templates/service-generation.md
```

### For Testing

```markdown
Context: Test-Driven Development

- Write failing tests first (RED phase)
- Implement minimal code to pass (GREEN phase)
- Refactor while maintaining tests (REFACTOR phase)
- Cover edge cases and error scenarios
- Maintain 80%+ code coverage

Reference: tools/ai-prompts/templates/test-generation.md
```

## 🔧 VS Code Integration

### Workspace Settings

The `.vscode/settings.json` file includes:

- File associations for AI context files
- Syntax highlighting for guidelines
- Search exclusions for cleaner results
- Editor configurations for consistency

### Recommended Extensions

- **Todo Tree**: Highlights AI-specific comments
- **Bookmark**: Quick access to context files
- **Angular Language Service**: Enhanced Angular support
- **ESLint**: Code quality enforcement

## 📝 Writing AI-Friendly Code Comments

### Context Comments in Code

```typescript
/**
 * AI_CONTEXT: This service follows Single Responsibility Principle
 * Handles only student application management operations
 * Uses RxJS for reactive programming, includes error handling
 * Reference: AI_AGENT_GUIDELINES.md - Service patterns
 */
@Injectable({ providedIn: "root" })
export class StudentApplicationService {
  // Implementation...
}
```

### Pattern Documentation

```typescript
// AI_PATTERN: Standard Angular component structure
// - Standalone component with OnPush change detection
// - Angular Material with Apple styling
// - Comprehensive unit tests included
// - Accessibility compliance (WCAG 2.1 AA)
```

## 🔄 Maintaining Context Consistency

### Regular Updates

1. **Weekly**: Review AI_AGENT_GUIDELINES.md for accuracy
2. **Monthly**: Update prompt templates based on learnings
3. **Quarterly**: Refresh architecture documentation
4. **As Needed**: Update context files when patterns change

### Quality Assurance

- All AI-generated code must be reviewed against guidelines
- Track consistency metrics across team members
- Document successful patterns and anti-patterns
- Share learnings in team retrospectives

### Version Control

- Tag major updates to context files
- Maintain changelog for guideline changes
- Archive outdated patterns with reasons
- Link context updates to specific requirements

## 📊 Monitoring Effectiveness

### Success Metrics

- **Consistency**: AI-generated code follows project patterns
- **Quality**: Reduced code review feedback
- **Speed**: Faster development while maintaining standards
- **Adoption**: Team usage of context system

### Feedback Collection

- Track which context files are most referenced
- Monitor AI agent performance with different context levels
- Collect developer feedback on context usefulness
- Measure development velocity improvements

## 🛠️ Troubleshooting

### Common Issues

**Problem**: AI generates inconsistent code styles
**Solution**: Ensure AI_AGENT_GUIDELINES.md is loaded first

**Problem**: AI doesn't follow architecture patterns
**Solution**: Reference specific ADR files from docs/architecture/

**Problem**: AI skips testing requirements
**Solution**: Use TDD template and emphasize test-first approach

**Problem**: AI ignores accessibility requirements
**Solution**: Explicitly mention WCAG 2.1 AA compliance in prompts

### Best Practices

1. **Always start with context loading**
2. **Be explicit about requirements**
3. **Reference specific guideline sections**
4. **Validate output against checklists**
5. **Provide feedback to improve context**

## 🎓 Team Training

### New Team Members

1. Review AI_AGENT_GUIDELINES.md thoroughly
2. Practice with context loading scripts
3. Complete sample tasks using AI assistance
4. Pair with experienced developer for first AI sessions
5. Contribute improvements to context documentation

### Ongoing Education

- Regular workshops on effective AI prompting
- Share successful prompt patterns in team meetings
- Document lessons learned from AI development
- Stay updated with AI tool capabilities and updates

## 🔮 Advanced Techniques

### Context Injection Automation

Create IDE snippets for common context patterns:

```json
{
  "ai-context": {
    "prefix": "ai-ctx",
    "body": [
      "## AI Context: $1",
      "Architecture: Single Responsibility + Cloud Native",
      "UI: Angular Material + Apple Design",
      "Testing: TDD with 80%+ coverage",
      "Reference: AI_AGENT_GUIDELINES.md",
      "$0"
    ]
  }
}
```

### Custom AI Prompts

Develop project-specific AI prompts for common tasks:

```markdown
Generate an Angular service for [DOMAIN] that:

- Follows Single Responsibility Principle
- Uses RxJS reactive patterns
- Includes comprehensive error handling
- Has 80%+ test coverage with unit and integration tests
- Integrates with existing API patterns

Reference patterns: AI_AGENT_GUIDELINES.md - Service Generation
```

---

## 📞 Support

For questions about the AI Context System:

1. Check existing documentation first
2. Review successful examples in the codebase
3. Ask team members who have used the system effectively
4. Contribute improvements back to the documentation

**Remember**: The goal is consistent, high-quality code generation that follows our established patterns and principles. The context system is a tool to achieve this consistency across all team members and AI interactions.
