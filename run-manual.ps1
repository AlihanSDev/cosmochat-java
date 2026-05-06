# Run script for CosmoChat (Manual compilation — no Maven required)
# Use after building with .\build.ps1

# Load .env if exists
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

$JAVAFX_HOME = "javafx/javafx-sdk-21.0.11/lib"
$BUILD_DIR = "build/classes"

# Check build
if (-not (Test-Path "$BUILD_DIR/cosmochat/CosmoChatApp.class")) {
    Write-Host "`nBuild not found! Run .\build.ps1 first." -ForegroundColor Red
    exit 1
}

# Prepare module path
$javafxJars = Get-ChildItem -Path $JAVAFX_HOME -Filter *.jar | ForEach-Object { $_.FullName }
$libJars = if (Test-Path "lib") { Get-ChildItem -Path "lib" -Filter *.jar | ForEach-Object { $_.FullName } } else { @() }
$MODULE_PATH = [System.String]::Join(";", $javafxJars + $libJars + $BUILD_DIR)

Write-Host "`n=== Running CosmoChat (Manual) ===" -ForegroundColor Yellow
Write-Host "Module path: $MODULE_PATH`n" -ForegroundColor Gray

Write-Host "Note: AI backends require external services:" -ForegroundColor Yellow
Write-Host "  • Qwen 1.5B (local) → start: python backend/qwen_api.py" -ForegroundColor Gray
Write-Host "  • Qwen 7B / Mistral / Deepseek → HuggingFace (set HF_TOKEN in .env)" -ForegroundColor Gray
Write-Host "  • Or use Spring HuggingFace backend: cd backend/spring-huggingface && mvn spring-boot:run`n" -ForegroundColor Gray

# Java options
$javaOpts = @()
if ($env:HF_TOKEN) {
    $javaOpts += "-DHF_TOKEN=$env:HF_TOKEN"
}
if ($env:HF_MODEL_ID) {
    $javaOpts += "-DHF_MODEL_ID=$env:HF_MODEL_ID"
}

# Launch
if ($javaOpts.Count -gt 0) {
    java @javaOpts --module-path $MODULE_PATH -m cosmochat/cosmochat.CosmoChatApp
} else {
    java --module-path $MODULE_PATH -m cosmochat/cosmochat.CosmoChatApp
}
