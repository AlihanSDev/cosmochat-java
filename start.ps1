# CosmoChat — Complete Launcher (one-click startup)

Write-Host "`n🚀 CosmoChat — Complete Startup" -ForegroundColor Cyan
Write-Host ("="*60) -ForegroundColor Gray

$ErrorActionPreference = "Stop"

# ── 1. Build ────────────────────────────────────────────────────────────────
Write-Host "`n[1/3] Building Java application..." -ForegroundColor Yellow

$MAVEN = $null
$possible = @(
    "..\apache-maven-3.9.9\bin\mvn.cmd",
    "..\apache-maven\bin\mvn.cmd",
    "mvn.cmd"
)
foreach ($p in $possible) {
    $resolved = Resolve-Path $p -ErrorAction SilentlyContinue
    if ($resolved) { $MAVEN = $resolved; break }
}

if (-not $MAVEN) {
    Write-Host "❌ Maven not found. Place it in ../apache-maven-3.9.9/ or add to PATH" -ForegroundColor Red
    exit 1
}

Write-Host "Using Maven: $MAVEN`n" -ForegroundColor Gray
& $MAVEN clean compile | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "`n❌ Build failed!`n" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Build complete`n" -ForegroundColor Green

# ── 2. Python Backend ────────────────────────────────────────────────────────
Write-Host "[2/3] Checking Python backend..." -ForegroundColor Yellow

function Start-PythonBackend {
    Write-Host "   Starting python backend\coder_api.py ..." -ForegroundColor Gray
    Start-Process -FilePath "python" -ArgumentList "backend\coder_api.py" -WindowStyle Minimized
    Start-Sleep -Seconds 8
}

$backendRunning = $false
try {
    $health = Invoke-RestMethod -Uri "http://127.0.0.1:5001/health" -Method Get -TimeoutSec 2 -ErrorAction Stop
    if ($health.status -eq "ok" -and $health.loaded) {
        Write-Host "✅ Python backend already running" -ForegroundColor Green
        Write-Host "   Model: $($health.model), Loaded: $($health.loaded)`n" -ForegroundColor Gray
        $backendRunning = $true
    }
} catch {}

if (-not $backendRunning) {
    Start-PythonBackend
    try {
        $health = Invoke-RestMethod -Uri "http://127.0.0.1:5001/health" -Method Get -TimeoutSec 10 -ErrorAction Stop
        if ($health.status -eq "ok" -and $health.loaded) {
            Write-Host "✅ Python backend started`n" -ForegroundColor Green
        } else {
            Write-Host "❌ Backend started but model not loaded`n" -ForegroundColor Red
            exit 1
        }
    } catch {
        Write-Host "❌ Failed to start Python backend`n" -ForegroundColor Red
        Write-Host "   Run manually: python backend\coder_api.py`n" -ForegroundColor Gray
        exit 1
    }
}

# ── 3. Launch JavaFX ─────────────────────────────────────────────────────────
Write-Host "[3/3] Starting JavaFX UI..." -ForegroundColor Yellow

$USER = $env:USERPROFILE
$paths = @(
    "target/classes",
    "$USER\.m2\repository\org\openjfx\javafx-base\21.0.11\javafx-base-21.0.11-win.jar",
    "$USER\.m2\repository\org\openjfx\javafx-controls\21.0.11\javafx-controls-21.0.11-win.jar",
    "$USER\.m2\repository\org\openjfx\javafx-graphics\21.0.11\javafx-graphics-21.0.11-win.jar",
    "$USER\.m2\repository\org\openjfx\javafx-web\21.0.11\javafx-web-21.0.11-win.jar",
    "$USER\.m2\repository\org\openjfx\javafx-media\21.0.11\javafx-media-21.0.11-win.jar",
    "$USER\.m2\repository\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar",
    "$USER\.m2\repository\org\xerial\sqlite-jdbc\3.45.2.0\sqlite-jdbc-3.45.2.0.jar",
    "$USER\.m2\repository\org\slf4j\slf4j-api\1.7.36\slf4j-api-1.7.36.jar"
)

foreach ($p in $paths) {
    if (-not (Test-Path $p)) {
        Write-Host "❌ Missing: $p" -ForegroundColor Red
        exit 1
    }
}

$MODULE_PATH = [System.String]::Join(';', $paths)
java --module-path "$MODULE_PATH" -m cosmochat/cosmochat.CosmoChatApp
