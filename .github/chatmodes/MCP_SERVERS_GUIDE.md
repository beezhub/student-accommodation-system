# Recommended MCP Servers for Enhanced Development

This document lists additional MCP servers that can enhance the Enterprise Architect mode and overall development experience for the Student Accommodation System.

## Currently Installed MCP Servers

✅ **Sequential Thinking** - Complex problem solving and reasoning
✅ **Memory** - Project knowledge graph
✅ **Atlassian** - Jira and Confluence integration
✅ **Oraios/Serena** - Symbolic code intelligence (available via tools)
✅ **Angular CLI** - Angular best practices and documentation (available via tools)
✅ **Playwright** - Browser automation and testing (available via tools)

## Highly Recommended Additions

### 1. GitHub MCP Server
**Purpose:** Direct GitHub API integration for issues, PRs, and repository management

**Installation:**
```json
{
  "github": {
    "command": "npx",
    "args": ["-y", "@modelcontextprotocol/server-github"],
    "env": {
      "GITHUB_PERSONAL_ACCESS_TOKEN": "ghp_your_token_here"
    }
  }
}
```

**Use Cases:**
- Create and manage GitHub issues
- Review and create pull requests
- Search repository code and history
- Manage labels, milestones, and projects
- Check CI/CD status

**Getting Token:**
1. Go to GitHub Settings → Developer settings → Personal access tokens
2. Create token with `repo` scope
3. Add to mcp.json

---

### 2. PostgreSQL MCP Server
**Purpose:** Database schema exploration, query execution, and data inspection

**Installation:**
```json
{
  "postgres": {
    "command": "npx",
    "args": ["-y", "@modelcontextprotocol/server-postgres"],
    "env": {
      "POSTGRES_CONNECTION_STRING": "postgresql://username:password@localhost:5432/studentdb"
    }
  }
}
```

**Use Cases:**
- Explore database schema
- Run SQL queries directly
- Verify data migrations
- Debug database issues
- Inspect table relationships

---

### 3. Filesystem MCP Server
**Purpose:** Extended file system operations beyond VS Code's built-in tools

**Installation:**
```json
{
  "filesystem": {
    "command": "npx",
    "args": [
      "-y",
      "@modelcontextprotocol/server-filesystem",
      "c:\\Users\\dev_t\\Documents\\practice\\Projects\\student-accommodation-system"
    ]
  }
}
```

**Use Cases:**
- Advanced file operations
- Bulk file modifications
- File system monitoring
- Search across large codebases
- File metadata inspection

---

### 4. Google Drive MCP Server
**Purpose:** Access project documentation, specs, and shared resources

**Installation:**
```json
{
  "gdrive": {
    "command": "npx",
    "args": ["-y", "@modelcontextprotocol/server-gdrive"],
    "env": {
      "GDRIVE_CLIENT_ID": "your_client_id",
      "GDRIVE_CLIENT_SECRET": "your_client_secret"
    }
  }
}
```

**Use Cases:**
- Access requirements documents
- Retrieve design specifications
- Read project documentation
- Reference shared resources
- Sync documentation updates

---

### 5. Brave Search MCP Server
**Purpose:** Web search for latest documentation, Stack Overflow solutions, etc.

**Installation:**
```json
{
  "brave-search": {
    "command": "npx",
    "args": ["-y", "@modelcontextprotocol/server-brave-search"],
    "env": {
      "BRAVE_API_KEY": "your_brave_api_key"
    }
  }
}
```

**Use Cases:**
- Search for latest library documentation
- Find Stack Overflow solutions
- Research best practices
- Check for known issues
- Discover new tools/libraries

**Getting API Key:**
1. Visit https://brave.com/search/api/
2. Sign up for API access
3. Get your API key

---

### 6. Puppeteer MCP Server
**Purpose:** Advanced browser automation (alternative to Playwright)

**Installation:**
```json
{
  "puppeteer": {
    "command": "npx",
    "args": ["-y", "@modelcontextprotocol/server-puppeteer"]
  }
}
```

**Use Cases:**
- Complex web scraping
- Advanced UI testing
- PDF generation from web pages
- Screenshot automation
- Performance profiling

---

## Useful but Optional MCP Servers

### Slack MCP Server
**Purpose:** Team communication and notifications

```json
{
  "slack": {
    "command": "npx",
    "args": ["-y", "@modelcontextprotocol/server-slack"],
    "env": {
      "SLACK_BOT_TOKEN": "xoxb-your-token",
      "SLACK_TEAM_ID": "your-team-id"
    }
  }
}
```

**Use Cases:**
- Send deployment notifications
- Post test results
- Alert team of issues
- Share progress updates

---

### AWS MCP Server
**Purpose:** Cloud deployment and infrastructure management

```json
{
  "aws": {
    "command": "npx",
    "args": ["-y", "@modelcontextprotocol/server-aws"],
    "env": {
      "AWS_ACCESS_KEY_ID": "your_access_key",
      "AWS_SECRET_ACCESS_KEY": "your_secret_key",
      "AWS_REGION": "us-east-1"
    }
  }
}
```

**Use Cases:**
- Deploy to AWS
- Manage S3 buckets
- Configure ECS/EKS
- Monitor CloudWatch logs
- Manage IAM policies

---

### Docker MCP Server
**Purpose:** Container management and inspection

```json
{
  "docker": {
    "command": "npx",
    "args": ["-y", "@modelcontextprotocol/server-docker"]
  }
}
```

**Use Cases:**
- Build and run containers
- Inspect container logs
- Manage Docker Compose
- Check container health
- Debug containerized apps

---

### Kubernetes MCP Server
**Purpose:** K8s cluster operations and deployment

```json
{
  "kubernetes": {
    "command": "npx",
    "args": ["-y", "@modelcontextprotocol/server-kubernetes"]
  }
}
```

**Use Cases:**
- Deploy to Kubernetes
- Manage pods and services
- Check cluster health
- Debug deployment issues
- Scale applications

---

## Updated mcp.json Template

Here's a complete `mcp.json` with recommended servers:

```json
{
  "inputs": [],
  "servers": {
    "sequentialthinking": {
      "command": "docker",
      "args": ["run", "-i", "--rm", "mcp/sequentialthinking"],
      "type": "stdio"
    },
    "memory": {
      "command": "docker",
      "args": [
        "run",
        "-i",
        "--rm",
        "-v",
        "/local-directory:/local-directory",
        "mcp/memory"
      ],
      "type": "stdio"
    },
    "atlassian": {
      "command": "npx",
      "args": ["-y", "mcp-remote", "https://mcp.atlassian.com/v1/sse"]
    },
    "github": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-github"],
      "env": {
        "GITHUB_PERSONAL_ACCESS_TOKEN": "ghp_your_token_here"
      }
    },
    "postgres": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-postgres"],
      "env": {
        "POSTGRES_CONNECTION_STRING": "postgresql://localhost/studentdb"
      }
    },
    "filesystem": {
      "command": "npx",
      "args": [
        "-y",
        "@modelcontextprotocol/server-filesystem",
        "c:\\Users\\dev_t\\Documents\\practice\\Projects\\student-accommodation-system"
      ]
    },
    "brave-search": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-brave-search"],
      "env": {
        "BRAVE_API_KEY": "your_brave_api_key"
      }
    },
    "docker": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-docker"]
    }
  }
}
```

## Installation Instructions

### Step 1: Backup Current Config
```powershell
Copy-Item .vscode\mcp.json .vscode\mcp.json.backup
```

### Step 2: Update mcp.json
Add the desired servers to your `.vscode/mcp.json` file

### Step 3: Get API Keys/Tokens
- GitHub: Settings → Developer settings → Personal access tokens
- Brave Search: https://brave.com/search/api/
- PostgreSQL: Use your local database connection string
- AWS: Use IAM credentials

### Step 4: Reload VS Code
Press `Ctrl+Shift+P` → "Developer: Reload Window"

### Step 5: Verify Installation
1. Open Copilot Chat
2. Check that new MCP tools appear in tool list
3. Test with a simple query

## Security Best Practices

### 1. Never Commit Credentials
Add to `.gitignore`:
```
.vscode/mcp.json
!.vscode/mcp.json.template
```

### 2. Use Environment Variables
Create `.env` file:
```bash
GITHUB_TOKEN=ghp_your_token
POSTGRES_URL=postgresql://localhost/studentdb
BRAVE_API_KEY=your_key
```

Load in mcp.json:
```json
{
  "env": {
    "GITHUB_PERSONAL_ACCESS_TOKEN": "${GITHUB_TOKEN}"
  }
}
```

### 3. Least Privilege
- GitHub: Only grant necessary scopes
- AWS: Use role-based access
- Database: Create read-only user for queries

### 4. Rotate Regularly
- Change tokens every 90 days
- Use short-lived tokens when possible
- Revoke unused tokens

## Troubleshooting

### MCP Server Not Starting

**Problem:** Server fails to start or connect

**Solutions:**
1. Check if npx is installed: `npx --version`
2. Clear npx cache: `npx clear-cache`
3. Check for port conflicts
4. Review VS Code output panel for errors

### Authentication Errors

**Problem:** Invalid credentials or token errors

**Solutions:**
1. Verify token/key is correct
2. Check token hasn't expired
3. Ensure proper permissions/scopes
4. Test credentials outside VS Code

### Performance Issues

**Problem:** VS Code slow with many MCP servers

**Solutions:**
1. Only enable servers you actively use
2. Use Docker-based servers when possible
3. Increase VS Code memory limit
4. Monitor system resources

## Performance Impact

| Server | Startup Time | Memory Usage | Network Usage |
|--------|-------------|--------------|---------------|
| Sequential Thinking | ~1s | ~50MB | None |
| Memory | ~1s | ~30MB | None |
| GitHub | ~2s | ~40MB | Medium |
| PostgreSQL | ~1s | ~30MB | Low |
| Filesystem | ~0.5s | ~20MB | None |
| Brave Search | ~1s | ~30MB | Medium |
| Playwright | ~3s | ~150MB | Low |

**Recommendation:** Start with 3-5 most useful servers, add more as needed.

## Next Steps

1. **Prioritize Servers** - Choose 3-5 most relevant for your workflow
2. **Get Credentials** - Obtain necessary API keys and tokens
3. **Update Config** - Add servers to mcp.json
4. **Test Integration** - Try Enterprise Architect mode with new servers
5. **Monitor Performance** - Check VS Code performance impact
6. **Iterate** - Add/remove servers based on usefulness

## Resources

- [Official MCP Documentation](https://modelcontextprotocol.io/)
- [MCP Server Registry](https://github.com/modelcontextprotocol)
- [VS Code MCP Setup Guide](https://code.visualstudio.com/docs/copilot)
- [Community MCP Servers](https://github.com/modelcontextprotocol/servers)

---

**Remember:** The Enterprise Architect chat mode automatically uses all available MCP servers. Add the servers that best support your development workflow!
