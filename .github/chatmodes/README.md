# Chat Modes Guide

This directory contains custom chat modes for the Student Accommodation System project.

## Available Chat Modes

### 🚀 Enterprise Architect Mode

**File:** `enterprise-architect.chatmode.md`

**Purpose:** Comprehensive full-stack development agent that autonomously handles all aspects of Angular 18+ and Spring Boot 3+ development with full MCP server integration.

**When to use:**
- Feature development (frontend or backend)
- Bug fixes requiring investigation
- Refactoring and code improvements
- Architecture decisions
- Testing implementation
- Documentation updates

**Key capabilities:**
- **Discovery-First:** Always explores codebase before acting
- **Serena Integration:** Efficient symbolic code navigation
- **Planning:** Creates detailed plans with sequential thinking
- **Latest Docs:** Fetches current documentation automatically
- **Best Practices:** Follows Angular 18+ and Spring Boot 3+ standards
- **Playwright Testing:** Frontend E2E and component testing
- **Full MCP Support:** Uses all installed MCP servers
- **Autonomous:** Completes tasks end-to-end without stopping

**MCP Servers Used:**
- ✅ Serena (oraios) - Symbolic code intelligence
- ✅ Angular CLI - Angular best practices and docs
- ✅ Sequential Thinking - Complex problem solving
- ✅ Memory - Project knowledge graph
- ✅ Playwright - Browser automation and testing
- ✅ Atlassian - Jira/Confluence integration (if configured)

**Workflow:**
1. **Discover** - Explores codebase with Serena, checks memory, fetches docs
2. **Think** - Uses sequential thinking for complex problems
3. **Plan** - Creates detailed implementation plan
4. **Execute** - Implements with TDD approach
5. **Verify** - Runs tests, checks errors
6. **Document** - Updates memory and documentation

**Example prompts:**
```
"Implement room search with filtering by price and location"
"Add SSO authentication with Azure AD"
"Refactor BookingService to use event-driven architecture"
"Create Playwright E2E tests for the booking flow"
"Debug the payment processing timeout issue"
"Update frontend to use Angular 18 signals"
```

## How to Use Chat Modes

### In VS Code:

1. **Open Chat View** - Click the Copilot icon or use `Ctrl+Alt+I`

2. **Select Chat Mode** - Click the mode dropdown in the chat input field

3. **Choose "Enterprise Architect"** - Your custom mode will appear in the list

4. **Start Chatting** - The mode's instructions are automatically applied

### Switching Modes:

You can switch between modes anytime during a conversation:
- Use the mode dropdown menu
- Different modes have different tools and behaviors
- Context carries over when switching

### Handoffs:

The Enterprise Architect mode includes handoffs for workflow transitions:
- **Review & Test** - After implementation, review and run comprehensive tests
- **Document Changes** - Document changes and create ADRs

Click handoff buttons that appear after responses to transition smoothly.

## Creating New Chat Modes

To create a new chat mode:

1. Create a new file: `.github/chatmodes/your-mode-name.chatmode.md`

2. Add front matter:
```yaml
---
description: Brief description of the mode
tools: ['tool1', 'tool2', ...]
model: Claude Sonnet 4
handoffs:
  - label: Next Step
    agent: target-mode
    prompt: Prompt text
    send: false
---
```

3. Add mode instructions in the body using Markdown

4. Save and it will appear in the chat mode dropdown

## Available Tools

Tools you can specify in chat modes:

**Core VS Code:**
- `extensions` - VS Code extensions API
- `codebase` - Codebase search and navigation
- `usages` - Find code usages
- `vscodeAPI` - VS Code API access
- `problems` - Error and warning diagnostics
- `changes` - Git changes and diffs

**Development:**
- `testFailure` - Test failure information
- `terminalSelection` - Terminal text selection
- `terminalLastCommand` - Last terminal command
- `openSimpleBrowser` - Open URLs in VS Code

**Search & Research:**
- `fetch` - Fetch web pages
- `findTestFiles` - Locate test files
- `searchResults` - Search results
- `githubRepo` - GitHub repository search

**Execution:**
- `runCommands` - Run terminal commands
- `runTasks` - Run VS Code tasks
- `editFiles` - Edit files directly
- `runNotebooks` - Run notebook cells

**Creation:**
- `search` - General search
- `new` - Create new files/projects

**MCP Tools:**
All MCP server tools are available automatically (Serena, Angular CLI, Sequential Thinking, Memory, Playwright, etc.)

## Best Practices

### For Using Enterprise Architect Mode:

1. **Be Specific** - Provide clear requirements
2. **Trust the Process** - Let it complete discovery before implementation
3. **Review Plans** - Check the plan before major changes
4. **Let It Finish** - Don't interrupt, it will complete all steps
5. **Check Memory** - It records learnings for future tasks

### For Creating Custom Modes:

1. **Keep Instructions Focused** - One clear purpose per mode
2. **Specify Relevant Tools** - Only include needed tools
3. **Use Handoffs** - Create workflows between modes
4. **Reference Files** - Link to instruction files to reuse content
5. **Test Thoroughly** - Try various prompts to verify behavior

## Configuration

### Enable Chat Modes:

Chat modes are enabled by default in VS Code. No configuration needed.

### Custom Locations:

By default, workspace chat modes are in `.github/chatmodes/`. To add more locations:

1. Open Settings (`Ctrl+,`)
2. Search for `chat.modeFilesLocations`
3. Add custom paths

### Sync Across Devices:

User-level chat modes (in your profile) sync automatically with VS Code Settings Sync.

## Troubleshooting

### Chat Mode Not Appearing:

- Ensure file ends with `.chatmode.md`
- Check file is in `.github/chatmodes/`
- Reload VS Code window
- Verify front matter syntax is valid YAML

### Tools Not Working:

- Check tool names match available tools
- Verify MCP servers are configured in `.vscode/mcp.json`
- Check MCP server status in VS Code

### Mode Behaving Unexpectedly:

- Review the instructions in the mode file
- Check if multiple instruction files are conflicting
- Try disabling repository custom instructions temporarily

## Resources

- [VS Code Custom Chat Modes Docs](https://code.visualstudio.com/docs/copilot/customization/custom-chat-modes)
- [VS Code Custom Instructions Docs](https://code.visualstudio.com/docs/copilot/customization/custom-instructions)
- [GitHub Copilot Customization](https://docs.github.com/en/copilot/customizing-copilot)
- [Awesome Copilot Examples](https://github.com/github/awesome-copilot)

## Contributing

To add new chat modes or improve existing ones:

1. Create/edit the `.chatmode.md` file
2. Test thoroughly with various prompts
3. Document usage in this README
4. Commit changes following conventional commits

---

**Note:** Chat modes are applied in addition to repository custom instructions. The Enterprise Architect mode works best with the project's existing instruction files in `.github/instructions/`.
