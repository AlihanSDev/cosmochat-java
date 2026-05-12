# Запуск CosmoChat с локальным Qwen Coder (Python backend)

Write-Host "`n=== Running CosmoChat with Qwen Coder (Python) ===" -ForegroundColor Green

$ErrorActionPreference = "Stop"

function Import-DotEnvIfPresent {
    param([string]$Path = ".env")
    if (-not (Test-Path -LiteralPath $Path)) { return }
    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line) { return }
        if ($line.StartsWith("#")) { return }
        $idx = $line.IndexOf("=")
        if ($idx -lt 1) { return }
        $key = $line.Substring(0, $idx).Trim()
        $value = $line.Substring($idx + 1).Trim()
        if ($value.Length -ge 2) {
            $first = $value[0]
            $last = $value[$value.Length - 1]
            if (($first -eq '"' -and $last -eq '"') -or ($first -eq "'" -and $last -eq "'")) {
                $value = $value.Substring(1, $value.Length - 2)
            }
        }
        if ($key) { Set-Item -Path ("env:{0}" -f $key) -Value $value }
    }
}

Import-DotEnvIfPresent

$USER = $env:USERPROFILE
$JARS = @(
    "$USER\.m2\repository\org\openjfx\javafx-base\21.0.11\javafx-base-21.0.11-win.jar",
    "$USER\.m2\repository\org\openjfx\javafx-controls\21.0.11\javafx-controls-21.0.11-win.jar",
    "$USER\.m2\repository\org\openjfx\javafx-graphics\21.0.11\javafx-graphics-21.0.11-win.jar",
    "$USER\.m2\repository\org\openjfx\javafx-web\21.0.11\javafx-web-21.0.11-win.jar",
    "$USER\.m2\repository\org\openjfx\javafx-media\21.0.11\javafx-media-21.0.11-win.jar",
    "$USER\.m2\repository\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar",
    "$USER\.m2\repository\org\postgresql\postgresql\42.7.4\postgresql-42.7.4.jar",
    "$USER\.m2\repository\org\slf4j\slf4j-api\1.7.36\slf4j-api-1.7.36.jar"
)

foreach ($jar in $JARS) {
    if (-not (Test-Path $jar)) {
        Write-Host "❌ Missing dependency: $jar" -ForegroundColor Red
        Write-Host "   Run: .\build-maven.ps1 first to fetch dependencies`n" -ForegroundColor Yellow
        exit 1
    }
}

$BUILD_DIR = "target/classes"
$MODULE_PATH = "target/classes;" + ($JARS -join ';')

Write-Host "`nModule path configured:" -ForegroundColor Gray
Write-Host "  Build dir: $BUILD_DIR"
Write-Host "  JavaFX: base, controls, graphics, web, media"
Write-Host "  Libs: gson, postgresql, slf4j-api`n" -ForegroundColor Gray

# Check Python API
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
    Write-Host "   Start it with: python backend\coder_api.py`n" -ForegroundColor Cyan
    Read-Host "Press Enter to continue launching CosmoChat anyway (API calls will fail)"
}

Write-Host "Starting JavaFX application..." -ForegroundColor Cyan
java --module-path "$MODULE_PATH" -m cosmochat/cosmochat.CosmoChatApp
