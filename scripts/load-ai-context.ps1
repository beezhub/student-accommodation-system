# PowerShell version for Windows users
# AI Context Injector Script

Write-Host "🤖 AI Agent Context Loader - Student Accommodation System" -ForegroundColor Green
Write-Host "===========================================================" -ForegroundColor Green
Write-Host ""

function Show-ContextFile {
    param(
        [string]$FilePath,
        [string]$Description
    )
    
    if (Test-Path $FilePath) {
        Write-Host "📋 $Description" -ForegroundColor Yellow
        Write-Host "📁 File: $FilePath" -ForegroundColor Gray
        Write-Host "---"
        Get-Content $FilePath -Head 50
        Write-Host ""
        Write-Host "... [File continues - see full content in $FilePath]"
        Write-Host ""
        Write-Host "==========================================" -ForegroundColor Gray
        Write-Host ""
    }
    else {
        Write-Host "⚠️  Missing: $FilePath" -ForegroundColor Red
        Write-Host ""
    }
}

Write-Host "Loading essential context files for AI agents..." -ForegroundColor Cyan
Write-Host ""

# Primary context files (always include)
Show-ContextFile "AI_AGENT_GUIDELINES.md" "PRIMARY GUIDELINES - Quick Reference for All Development"

Show-ContextFile ".github\instructions\student-accomodation.instructions.md" "DETAILED INSTRUCTIONS - Complete Project Specification"

Show-ContextFile "docs\DEVELOPMENT_WORKFLOW.md" "DEVELOPMENT PROCESS - TDD and AI-Assisted Workflow"

Show-ContextFile "tools\ai-prompts\README.md" "PROMPT STANDARDS - AI Development Guidelines"

# Secondary context files (include based on task)
Write-Host "📚 Additional Context Files Available:" -ForegroundColor Yellow
Write-Host "• docs\BUSINESS_REQUIREMENTS.md - Business context and requirements"
Write-Host "• docs\architecture\ - Architecture Decision Records (ADRs)"
Write-Host "• tools\ai-prompts\templates\ - Specific prompt templates"
Write-Host ""

# Display current project structure
Write-Host "🗂️  PROJECT STRUCTURE OVERVIEW:" -ForegroundColor Cyan
Write-Host "================================"

try {
    if (Get-Command tree -ErrorAction SilentlyContinue) {
        tree /F /A
    }
    else {
        Get-ChildItem -Directory -Recurse -Depth 2 | Select-Object FullName
    }
}
catch {
    Get-ChildItem -Directory | Select-Object Name
}

Write-Host ""
Write-Host "🎯 KEY PRINCIPLES TO REMEMBER:" -ForegroundColor Green
Write-Host "==============================="
Write-Host "✅ Single Responsibility Principle for all components"
Write-Host "✅ Angular Material with Apple-inspired design"
Write-Host "✅ Test-Driven Development (80%+ coverage)"
Write-Host "✅ Cloud-native, container-first architecture"
Write-Host "✅ Accessibility compliance (WCAG 2.1 AA)"
Write-Host "✅ TypeScript strict mode and proper typing"
Write-Host ""

Write-Host "🚀 QUICK START CHECKLIST:" -ForegroundColor Blue
Write-Host "========================="
Write-Host "□ Understand the specific requirement"
Write-Host "□ Identify Single Responsibility boundary"
Write-Host "□ Write tests first (TDD approach)"
Write-Host "□ Use Angular Material components"
Write-Host "□ Apply Apple-inspired styling"
Write-Host "□ Include proper error handling"
Write-Host "□ Ensure accessibility compliance"
Write-Host "□ Document the implementation"
Write-Host ""

Write-Host "💡 CONTEXT LOADED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "You now have all the essential guidelines and context needed."
Write-Host "Refer to AI_AGENT_GUIDELINES.md for the complete quick reference."
Write-Host ""
