# AI Prompt Library for Student Accommodation System

This directory contains standardized AI prompts and templates to ensure consistent code generation and development practices across the team. Enhanced with MCP (Model Context Protocol) integration for improved context awareness and prompt boost functionality.

## Directory Structure

```
ai-prompts/
├── templates/
│   ├── component-generation.md       # Angular component creation
│   ├── mcp-component-generation.md   # MCP-enhanced component generation
│   ├── service-generation.md         # Service and API development
│   ├── test-generation.md           # Test case creation
│   ├── mcp-test-generation.md       # MCP-enhanced test generation
│   └── refactoring.md               # Code refactoring prompts
├── patterns/
│   ├── angular-patterns.md          # Angular-specific patterns
│   ├── spring-boot-patterns.md      # Spring Boot patterns
│   └── testing-patterns.md          # Testing patterns
└── examples/
    ├── successful-prompts.md         # Examples of effective prompts
    ├── mcp-examples.md              # MCP-enhanced prompt examples
    └── anti-patterns.md             # Common mistakes to avoid
```

## MCP Integration

### Available MCP Servers

- **Filesystem**: Analyze project structure and existing code
- **Git**: Understand repository history and changes
- **Memory**: Maintain context across sessions
- **GitHub**: Access repository metadata and issues
- **Brave Search**: Research best practices and solutions
- **PostgreSQL/SQLite**: Database operations and schema analysis

### Prompt Boost Templates

Pre-configured templates in VS Code settings:

- `angular-component`: Apple-inspired Angular components
- `spring-service`: Microservices-compliant Spring services
- `api-endpoint`: RESTful API design with OpenAPI
- `database-migration`: Flyway-compatible migrations
- `test-case`: TDD-compliant comprehensive tests

## Usage Guidelines

### 1. MCP-Enhanced Prompts

```markdown
Using the filesystem MCP, analyze existing [component-type] patterns, then generate a new [specific-component] following our established conventions.

Include:

- Pattern analysis from existing code
- Consistency with project architecture
- Integration points with existing services
- Comprehensive test coverage
```

### 2. Standard Prompt Structure

```markdown
## Context

[Brief description of what you're building]

## MCP Analysis

[Use relevant MCP servers to gather context]

- `filesystem`: Analyze existing patterns
- `git`: Review recent related changes
- `memory`: Recall previous similar implementations

## Requirements

[Specific functional requirements]

## Constraints

[Technical constraints, patterns to follow]

## Testing

[Test requirements and coverage expectations]

## Expected Output

[Specific files or code structure expected]
```

### 3. Prompt Boost Integration

Use template shortcuts for common patterns:

```
Generate a student profile management component using the angular-component template
```

```
Create a booking service using the spring-service template with proper error handling
```

### 4. Quality Checklist

- [ ] Follows Single Responsibility Principle
- [ ] Uses MCP analysis for consistency
- [ ] Includes comprehensive tests
- [ ] Adheres to project coding standards
- [ ] Contains proper error handling
- [ ] Includes accessibility considerations
- [ ] Follows Apple-inspired design principles
- [ ] Verified with filesystem MCP for integration

## Team Workflow

1. **MCP Analysis**: Use filesystem/git MCP to understand context
2. **Search Existing Prompts**: Check if a similar prompt exists
3. **Customize Template**: Adapt existing template or use prompt boost
4. **Context Enhancement**: Leverage MCP servers for better context
5. **Peer Review**: Have another team member review your prompt
6. **Generate Code**: Use AI with MCP enhancement to generate implementation
7. **Validation**: Use filesystem MCP to verify integration points
8. **Code Review**: Review generated code before integration
9. **Update Library**: Add successful patterns to the library

## MCP Workflow Examples

### Component Development

```
1. filesystem MCP: Analyze existing components
2. Use angular-component template
3. git MCP: Check recent component patterns
4. Generate with enhanced context
5. filesystem MCP: Verify file structure
```

### Service Development

```
1. filesystem MCP: Review existing services
2. github MCP: Check related issues/PRs
3. Use spring-service template
4. memory MCP: Maintain consistency
5. Generate with project context
```

## Prompt Versioning

- Use semantic versioning for prompt templates (v1.0.0)
- Document MCP integration improvements
- Track effectiveness metrics with MCP analytics
- Archive outdated patterns
- Version MCP server configurations

## Advanced MCP Usage

### Cross-Component Analysis

```
Using filesystem MCP, analyze all authentication-related components to understand our auth patterns, then generate a new OAuth integration component following the same architectural style.
```

### Repository Pattern Discovery

```
Use git MCP to trace the evolution of our repository pattern implementations, then use that context to generate a new entity repository that maintains consistency with our data access layer.
```

### Test Pattern Inheritance

```
Analyze existing test suites with filesystem MCP to identify our testing conventions, then generate tests for the new component that follow the same patterns and coverage levels.
```

## Contributing

When adding new prompts:

1. Use the standard template format
2. Include examples and expected outputs
3. Add to the appropriate category
4. Update this README with new patterns
5. Submit for team review

## Best Practices

- **Be Specific**: Vague prompts lead to inconsistent results
- **Include Context**: Provide relevant code snippets and requirements
- **Test Requirements**: Always specify testing expectations
- **Follow Patterns**: Stick to established architectural patterns
- **Document Results**: Track what works and what doesn't
