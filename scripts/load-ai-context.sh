#!/bin/bash

# AI Context Injector Script
# This script automatically includes essential context files for AI agents

echo "🤖 AI Agent Context Loader - Student Accommodation System"
echo "==========================================================="
echo ""

# Function to display file content with header
show_context_file() {
    local file_path="$1"
    local description="$2"
    
    if [ -f "$file_path" ]; then
        echo "📋 $description"
        echo "📁 File: $file_path"
        echo "---"
        head -50 "$file_path"
        echo ""
        echo "... [File continues - see full content in $file_path]"
        echo ""
        echo "=========================================="
        echo ""
    else
        echo "⚠️  Missing: $file_path"
        echo ""
    fi
}

echo "Loading essential context files for AI agents..."
echo ""

# Primary context files (always include)
show_context_file "AI_AGENT_GUIDELINES.md" "PRIMARY GUIDELINES - Quick Reference for All Development"

show_context_file ".github/instructions/student-accomodation.instructions.md" "DETAILED INSTRUCTIONS - Complete Project Specification"

show_context_file "docs/DEVELOPMENT_WORKFLOW.md" "DEVELOPMENT PROCESS - TDD and AI-Assisted Workflow"

show_context_file "tools/ai-prompts/README.md" "PROMPT STANDARDS - AI Development Guidelines"

# Secondary context files (include based on task)
echo "📚 Additional Context Files Available:"
echo "• docs/BUSINESS_REQUIREMENTS.md - Business context and requirements"
echo "• docs/architecture/ - Architecture Decision Records (ADRs)"
echo "• tools/ai-prompts/templates/ - Specific prompt templates"
echo ""

# Display current project structure
echo "🗂️  PROJECT STRUCTURE OVERVIEW:"
echo "================================"

if command -v tree >/dev/null 2>&1; then
    tree -L 3 -I 'node_modules|build|dist|*.log'
else
    find . -type d -not -path '*/node_modules*' -not -path '*/build*' -not -path '*/dist*' | head -20
fi

echo ""
echo "🎯 KEY PRINCIPLES TO REMEMBER:"
echo "==============================="
echo "✅ Single Responsibility Principle for all components"
echo "✅ Angular Material with Apple-inspired design"
echo "✅ Test-Driven Development (80%+ coverage)"
echo "✅ Cloud-native, container-first architecture"
echo "✅ Accessibility compliance (WCAG 2.1 AA)"
echo "✅ TypeScript strict mode and proper typing"
echo ""

echo "🚀 QUICK START CHECKLIST:"
echo "========================="
echo "□ Understand the specific requirement"
echo "□ Identify Single Responsibility boundary"
echo "□ Write tests first (TDD approach)"
echo "□ Use Angular Material components"
echo "□ Apply Apple-inspired styling"
echo "□ Include proper error handling"
echo "□ Ensure accessibility compliance"
echo "□ Document the implementation"
echo ""

echo "💡 CONTEXT LOADED SUCCESSFULLY!"
echo "You now have all the essential guidelines and context needed."
echo "Refer to AI_AGENT_GUIDELINES.md for the complete quick reference."
echo ""
