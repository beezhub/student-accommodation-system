# AI Prompt Boost Usage Guide

## Overview

AI Prompt Boost is now configured for your Student Accommodation System project to automatically enhance prompts when working in agent mode. This integration provides intelligent context-aware prompt enhancement using your project's templates and patterns.

## How It Works

When you interact with the AI assistant in VS Code, the prompt boost system will:

1. **Analyze Context**: Automatically detect the type of file you're working on
2. **Select Template**: Choose the appropriate prompt template based on file patterns
3. **Enhance Prompt**: Boost your natural language request with structured context
4. **Apply Standards**: Ensure generated code follows your project's architectural patterns

## Available Templates

| Template            | Trigger Words                     | File Patterns      | Purpose                                          |
| ------------------- | --------------------------------- | ------------------ | ------------------------------------------------ |
| `angular-component` | "component", "generate component" | `*.component.ts`   | Angular components with Material Design          |
| `spring-service`    | "service", "create service"       | `*Service.java`    | Spring Boot services with microservices patterns |
| `test-tdd`          | "test", "generate test"           | `*.spec.ts`        | Angular unit tests with TDD approach             |
| `java-test`         | "java test", "junit"              | `*Test.java`       | Java tests with JUnit 5 and Mockito              |
| `api-endpoint`      | "api", "endpoint", "rest"         | `*Controller.java` | RESTful API endpoints with OpenAPI               |

## Usage Examples

### 1. Angular Component Generation

**Your Input**: "Create a student profile component"

**Prompt Boost Enhancement**:

- Detects you're working on a `.component.ts` file
- Applies `angular-component` template
- Adds Apple-inspired Material Design requirements
- Includes accessibility and responsive design specifications
- Adds comprehensive testing requirements

### 2. Spring Service Creation

**Your Input**: "Generate a booking management service"

**Prompt Boost Enhancement**:

- Detects `*Service.java` pattern
- Applies `spring-service` template
- Includes microservices architecture patterns
- Adds security and transaction management
- Includes comprehensive test generation

### 3. Test Generation

**Your Input**: "Write tests for this component"

**Prompt Boost Enhancement**:

- Detects `.spec.ts` or `Test.java` file
- Applies appropriate test template
- Includes TDD methodology
- Adds comprehensive test scenarios
- Ensures 80%+ coverage requirements

## Agent Mode Integration

The prompt boost system is specifically configured for agent mode:

### Automatic Context Enhancement

```json
{
  "ai.agent.promptBoost.enabled": true,
  "ai.agent.context.smartSelection": true,
  "ai.context.includeProjectStructure": true
}
```

### Smart File Detection

The system automatically detects:

- File type and extension
- Current working directory
- Related project files
- Existing architectural patterns

### MCP Integration

Enhanced with Model Context Protocol for:

- Filesystem analysis
- Git history awareness
- Memory persistence across sessions

## Quick Start

1. **Check Status**:

   ```powershell
   .\scripts\setup-prompt-boost.ps1 -Status
   ```

2. **Test Templates**:

   ```powershell
   .\scripts\setup-prompt-boost.ps1 -Template angular
   ```

3. **Use in VS Code**:
   - Open any file in your project
   - Start typing your request naturally
   - The system will automatically enhance your prompt

## Manual Template Usage

If you want to explicitly use a template:

1. **Command Palette**: `Ctrl+Shift+P` → "AI Prompt Boost"
2. **Shortcuts**:
   - `Ctrl+Shift+A`: Angular component template
   - `Ctrl+Shift+T`: Test generation template
   - `Ctrl+Shift+S`: Service generation template

## Customization

### Adding New Templates

1. Create template file in `tools/ai-prompts/templates/`
2. Update `.vscode/ai-prompt-boost.json` configuration
3. Add trigger patterns and file associations

### Modifying Enhancement Rules

Edit `.vscode/settings.json`:

```json
{
  "ai.promptBoost.enhancementRules": [
    {
      "trigger": "your-keyword",
      "action": "boost",
      "template": "your-template",
      "when": "*.your-extension"
    }
  ]
}
```

## Troubleshooting

### Common Issues

1. **Templates Not Loading**

   - Check if template files exist in `tools/ai-prompts/templates/`
   - Verify file paths in configuration

2. **Boost Not Triggering**

   - Ensure VS Code settings are properly configured
   - Restart VS Code after configuration changes
   - Check file patterns match your current file

3. **Context Not Including Project Files**
   - Verify MCP integration is enabled
   - Check excluded patterns in settings

### Debug Mode

Enable debug logging:

```json
{
  "ai.promptBoost.debug": true,
  "ai.agent.debug": true
}
```

## Best Practices

1. **Use Natural Language**: Type your requests naturally, the system will enhance them
2. **Be Specific**: Include context about what you're trying to achieve
3. **File Context**: Work from the appropriate file type for better template matching
4. **Review Generated Code**: Always review AI-generated code before integration
5. **Iterative Refinement**: Use follow-up prompts to refine the generated code

## Integration with Development Workflow

The prompt boost system integrates seamlessly with your existing workflow:

- **TDD Approach**: Automatically includes test generation requirements
- **Architecture Compliance**: Ensures generated code follows project patterns
- **Security Standards**: Includes security best practices in generated code
- **Documentation**: Automatically includes documentation requirements
- **Quality Gates**: Applies code quality standards and coverage requirements

## Support

For issues or questions:

1. Check the configuration with `.\scripts\setup-prompt-boost.ps1 -Status`
2. Review template files in `tools/ai-prompts/templates/`
3. Verify VS Code settings in `.vscode/settings.json`
4. Test individual templates with the setup script
