# Run script for CosmoChat with HuggingFace (Spring Boot backend)
$JAVAFX_HOME = "javafx/javafx-sdk-21.0.11/lib"
$BUILD_DIR = "build/classes"

# Collect all JARs
$javafxJars = Get-ChildItem -Path $JAVAFX_HOME -Filter *.jar | ForEach-Object { $_.FullName }
$libJars = Get-ChildItem -Path "lib" -Filter *.jar | ForEach-Object { $_.FullName }
$allPaths = @($BUILD_DIR) + $javafxJars + $libJars
$MODULE_PATH = [System.String]::Join(";", $allPaths)

Write-Host "`n=== Running CosmoChat with HuggingFace ===" -ForegroundColor Green
Write-Host "Module path includes:"
Write-Host "  - Build dir: $BUILD_DIR"
Write-Host "  - JavaFX JARs: $($javafxJars.Count)"
Write-Host "  - Library JARs: $($libJars.Count)`n" -ForegroundColor Gray

# Check Spring Boot backend
Write-Host "Checking HuggingFace service..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/health" -Method Get -TimeoutSec 3
    if ($response.status -eq "ok") {
        Write-Host "✅ HuggingFace service is running on port 8080" -ForegroundColor Green
        Write-Host "   Model: $($response.model)`n" -ForegroundColor Gray
    } else {
        Write-Host "⚠️  HuggingFace service returned non-ok status`n" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ HuggingFace service is NOT running!" -ForegroundColor Red
    Write-Host "   Start it with: cd backend/spring-huggingface; ./mvnw spring-boot:run" -ForegroundColor Cyan
    Write-Host "   Or run without backend: .\run.ps1`n" -ForegroundColor Cyan
    Read-Host "Press Enter to continue launching CosmoChat anyway (will show model selector but API calls will fail)"
}

 # Load .env file from backend/spring-huggingface
 $envFile = "backend/spring-huggingface/.env"
 if (Test-Path $envFile) {
     Write-Host "Loading environment from $envFile..." -ForegroundColor Gray
     Get-Content $envFile | ForEach-Object {
         if ($_ -match "^\s*([^=]+)\s*=\s*(.*)\s*$") {
             $name = $matches[1]
             $value = $matches[2]
             # Remove quotes if present
             $value = $value.Trim('"').Trim("'")
             Set-Item -Path "Env:\$name" -Value $value -ErrorAction SilentlyContinue
             Write-Host "  Set $name" -ForegroundColor Gray
         }
     }
 } else {
     Write-Host "Warning: .env file not found at $envFile" -ForegroundColor Yellow
 }
 
 # Prepare Java options with HF_TOKEN if available
 $javaOpts = @()
 if ($env:HF_TOKEN) {
     $javaOpts += "-DHF_TOKEN=$env:HF_TOKEN"
     Write-Host "  HF_TOKEN loaded from .env" -ForegroundColor Green
 }
 if ($env:HF_MODEL_ID) {
     $javaOpts += "-DHF_MODEL_ID=$env:HF_MODEL_ID"
 }
 
 # Launch JavaFX application
 Write-Host "Starting JavaFX application..." -ForegroundColor Cyan
 if ($javaOpts.Count -gt 0) {
     java @javaOpts --module-path $MODULE_PATH -m cosmochat/cosmochat.CosmoChatApp
 } else {
     java --module-path $MODULE_PATH -m cosmochat/cosmochat.CosmoChatApp
 }
