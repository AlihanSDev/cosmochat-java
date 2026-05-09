# Run script for CosmoChat (Maven-based)
# Uses Maven Wrapper if available

# Load .env if exists (for HF_TOKEN)
$envFile = "backend/spring-huggingface/.env"
if (Test-Path $envFile) {
    Write-Host "Loading environment from $envFile..." -ForegroundColor Gray
    Get-Content $envFile | ForEach-Object {
        if ($_ -match "^\s*([^=]+)\s*=\s*(.*)\s*$") {
            $name = $matches[1]
            $value = $matches[2]
            $value = $value.Trim('"').Trim("'")
            Set-Item -Path "Env:\$name" -Value $value -ErrorAction SilentlyContinue
        }
    }
}

Write-Host "`n=== Running CosmoChat (Maven) ===" -ForegroundColor Yellow

# Determine Maven command
$MAVEN = "./mvnw"
if (-not (Test-Path $MAVEN)) {
    $MAVEN = "mvn"
    Write-Host "Using system Maven" -ForegroundColor Gray
} else {
    Write-Host "Using Maven Wrapper" -ForegroundColor Gray
    # Ensure wrapper is executable on Unix
    if ($IsLinux -or $IsMacOS) { chmod +x $MAVEN }
}

Write-Host "`nNote: AI backends require external services:" -ForegroundColor Yellow
Write-Host "  • Qwen 1.5B (local) → start: python backend/qwen_api.py" -ForegroundColor Gray
Write-Host "  • Qwen 7B / Mistral / Deepseek → HuggingFace (set HF_TOKEN in .env)" -ForegroundColor Gray
Write-Host "  • Or use Spring HuggingFace backend: cd backend/spring-huggingface && mvn spring-boot:run`n" -ForegroundColor Gray

# Build Maven arguments
$mvnArgs = @("javafx:run")
if ($env:HF_TOKEN) {
    $mvnArgs += "-DHF_TOKEN=$($env:HF_TOKEN)"
}
if ($env:HF_MODEL_ID) {
    $mvnArgs += "-DHF_MODEL_ID=$($env:HF_MODEL_ID)"
}

# Run
& $MAVEN @mvnArgs
