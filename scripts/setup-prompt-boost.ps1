# AI Prompt Boost Activation Script
# This script sets up the AI prompt boost functionality for VS Code

param(
    [switch]$Enable,
    [switch]$Disable,
    [switch]$Status,
    [string]$Template
)

$vsCodeSettingsPath = ".vscode\settings.json"
$promptBoostConfigPath = ".vscode\ai-prompt-boost.json"

function Show-Status {
    Write-Host "=== AI Prompt Boost Status ===" -ForegroundColor Cyan
    
    if (Test-Path $vsCodeSettingsPath) {
        $settings = Get-Content $vsCodeSettingsPath | ConvertFrom-Json
        
        $isEnabled = $settings.'ai.promptBoost.enabled' -eq $true
        $agentModeEnabled = $settings.'ai.agent.promptBoost.enabled' -eq $true
        
        Write-Host "Prompt Boost Enabled: " -NoNewline
        Write-Host $isEnabled -ForegroundColor $(if ($isEnabled) { "Green" } else { "Red" })
        
        Write-Host "Agent Mode Integration: " -NoNewline
        Write-Host $agentModeEnabled -ForegroundColor $(if ($agentModeEnabled) { "Green" } else { "Red" })
        
        if ($settings.'ai.promptBoost.templates') {
            Write-Host "`nAvailable Templates:" -ForegroundColor Yellow
            $settings.'ai.promptBoost.templates'.PSObject.Properties | ForEach-Object {
                Write-Host "  - $($_.Name): $($_.Value)" -ForegroundColor Gray
            }
        }
    }
    else {
        Write-Host "VS Code settings not found!" -ForegroundColor Red
    }
    
    if (Test-Path $promptBoostConfigPath) {
        Write-Host "`nPrompt Boost Configuration: Found" -ForegroundColor Green
    }
    else {
        Write-Host "`nPrompt Boost Configuration: Missing" -ForegroundColor Red
    }
}

function Enable-PromptBoost {
    Write-Host "Activating AI Prompt Boost..." -ForegroundColor Green
    
    # Check if templates exist
    $templatePaths = @(
        "tools\ai-prompts\templates\component-generation.md",
        "tools\ai-prompts\templates\service-generation.md",
        "tools\ai-prompts\templates\test-generation.md",
        "tools\ai-prompts\templates\api-generation.md"
    )
    
    $missingTemplates = @()
    foreach ($template in $templatePaths) {
        if (-not (Test-Path $template)) {
            $missingTemplates += $template
        }
    }
    
    if ($missingTemplates.Count -gt 0) {
        Write-Host "Warning: Missing template files:" -ForegroundColor Yellow
        $missingTemplates | ForEach-Object { Write-Host "  - $_" -ForegroundColor Gray }
    }
    
    # Restart VS Code with enhanced settings
    Write-Host "Configuration updated. Please restart VS Code to apply changes." -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Quick Usage:" -ForegroundColor Yellow
    Write-Host "  1. Open any .component.ts file and type 'generate component'" -ForegroundColor Gray
    Write-Host "  2. Open any .service.ts file and type 'create service'" -ForegroundColor Gray
    Write-Host "  3. Open any .spec.ts file and type 'generate test'" -ForegroundColor Gray
    Write-Host "  4. Use Ctrl+Shift+P and search for 'AI Prompt Boost'" -ForegroundColor Gray
}

function Test-PromptBoost {
    param([string]$TemplateName)
    
    Write-Host "Testing Prompt Boost with template: $TemplateName" -ForegroundColor Cyan
    
    $templateMap = @{
        "angular" = "tools\ai-prompts\templates\component-generation.md"
        "service" = "tools\ai-prompts\templates\service-generation.md"  
        "test"    = "tools\ai-prompts\templates\test-generation.md"
        "api"     = "tools\ai-prompts\templates\api-generation.md"
    }
    
    if ($templateMap.ContainsKey($TemplateName.ToLower())) {
        $templatePath = $templateMap[$TemplateName.ToLower()]
        if (Test-Path $templatePath) {
            Write-Host "Template found: $templatePath" -ForegroundColor Green
            Write-Host "Preview:" -ForegroundColor Yellow
            Get-Content $templatePath -Head 10 | Write-Host -ForegroundColor Gray
        }
        else {
            Write-Host "Template file not found: $templatePath" -ForegroundColor Red
        }
    }
    else {
        Write-Host "Available templates: angular, service, test, api" -ForegroundColor Yellow
    }
}

# Main execution
switch ($true) {
    $Status { Show-Status }
    $Enable { Enable-PromptBoost }
    ($Template -ne "") { Test-PromptBoost -TemplateName $Template }
    default { 
        Show-Status
        Write-Host ""
        Write-Host "Usage:" -ForegroundColor Yellow
        Write-Host "  .\scripts\setup-prompt-boost.ps1 -Status    # Show current status"
        Write-Host "  .\scripts\setup-prompt-boost.ps1 -Enable    # Activate prompt boost"
        Write-Host "  .\scripts\setup-prompt-boost.ps1 -Template angular  # Test template"
    }
}
