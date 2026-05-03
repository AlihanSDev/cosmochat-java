# Run script for CosmoChat with Qwen Coder (Python backend)
$JAVAFX_HOME = "javafx/javafx-sdk-21.0.11/lib"
$BUILD_DIR = "build/classes"

# Collect all JARs
$javafxJars = Get-ChildItem -Path $JAVAFX_HOME -Filter *.jar | ForEach-Object { $_.FullName }
$libJars = Get-ChildItem -Path "lib" -Filter *.jar | ForEach-Object { $_.FullName }
$allPaths = @($BUILD_DIR) + $javafxJars + $libJars
$MODULE_PATH = [System.String]::Join(";", $allPaths)

Write-Host "`n=== Running CosmoChat with Qwen Coder (Python) ===" -ForegroundColor Green
Write-Host "Module path includes:"
Write-Host "  - Build dir: $BUILD_DIR"
Write-Host "  - JavaFX JARs: $($javafxJars.Count)"
Write-Host "  - Library JARs: $($libJars.Count)`n" -ForegroundColor Gray

# Check Python backend
Write-Host "Checking Python API server..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://127.0.0.1:5001/health" -Method Get -TimeoutSec 3
    if ($response.status -eq "ok") {
        Write-Host "✅ Python Qwen API is running on port 5001" -ForegroundColor Green
        Write-Host "   Model: $($response.model)" -ForegroundColor Gray
        Write-Host "   Loaded: $($response.loaded)`n" -ForegroundColor Gray
    } else {
        Write-Host "⚠️  Python API returned non-ok status`n" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Python Qwen API is NOT running!" -ForegroundColor Red
    Write-Host "   Start it with: python backend/qwen_api.py" -ForegroundColor Cyan
    Write-Host "   Or run without backend: .\run.ps1`n" -ForegroundColor Cyan
    Read-Host "Press Enter to continue launching CosmoChat anyway (will show model selector but API calls will fail)"
}

# Launch JavaFX application
Write-Host "Starting JavaFX application..." -ForegroundColor Cyan
java --module-path $MODULE_PATH -m cosmochat/cosmochat.CosmoChatApp
