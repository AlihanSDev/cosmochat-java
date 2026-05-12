# CosmoChat -- Complete Launcher (one-click startup)

# Force UTF-8 for file operations and console output
$PSDefaultParameterValues['*:Encoding'] = 'utf8'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "`n[START] CosmoChat -- Complete Startup" -ForegroundColor Cyan
Write-Host ("="*60) -ForegroundColor Gray

try { Set-PSDebug -Off } catch {}
$ErrorActionPreference = "Stop"

$scriptRoot = $PSScriptRoot
if (-not $scriptRoot) {
    $scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
}

if (-not $env:COSMOCHAT_START_REEXEC) {
    $env:COSMOCHAT_START_REEXEC = "1"
    $exe = (Get-Command powershell.exe -ErrorAction SilentlyContinue).Source
    if (-not $exe) { $exe = "powershell.exe" }
    $argsList = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", $MyInvocation.MyCommand.Path
    )
    if ($args.Count -gt 0) { $argsList += $args }
    Start-Process -FilePath $exe -ArgumentList $argsList -Wait
    return
}

function Import-DotEnvIfPresent {
    param([string]$Path)
    $values = @{}
    if (-not $Path) { $Path = Join-Path $scriptRoot ".env" }
    if (-not (Test-Path -LiteralPath $Path)) { return $values }

    $isFirstLine = $true
    foreach ($rawLine in (Get-Content -LiteralPath $Path -Encoding UTF8)) {
        # Strip UTF-8 BOM from first line if present
        if ($isFirstLine -and $rawLine.StartsWith([char]0xFEFF)) {
            $rawLine = $rawLine.Substring(1)
        }
        $isFirstLine = $false

        $line = $rawLine.Trim()
        if (-not $line) { continue }
        if ($line.StartsWith("#")) { continue }
        $idx = $line.IndexOf("=")
        if ($idx -lt 1) { continue }
        $key = $line.Substring(0, $idx).Trim()
        $value = $line.Substring($idx + 1).Trim()
        if ($value.Length -ge 2) {
            $first = $value[0]
            $last = $value[$value.Length - 1]
            if (($first -eq '"' -and $last -eq '"') -or ($first -eq "'" -and $last -eq "'")) {
                $value = $value.Substring(1, $value.Length - 2)
            }
        }
        if (-not $key) { continue }
        $values[$key] = $value
        Set-Item -Path ("env:{0}" -f $key) -Value $value
    }

    return $values
}

$dotEnv = Import-DotEnvIfPresent

$defaultDbName = if ($dotEnv.ContainsKey("POSTGRES_DB")) { $dotEnv["POSTGRES_DB"] } elseif ($env:POSTGRES_DB) { $env:POSTGRES_DB } else { "cosmochat" }
$defaultDbUser = if ($dotEnv.ContainsKey("POSTGRES_USER")) { $dotEnv["POSTGRES_USER"] } elseif ($env:POSTGRES_USER) { $env:POSTGRES_USER } else { "cosmochat" }
$defaultDbPassword = if ($dotEnv.ContainsKey("POSTGRES_PASSWORD")) { $dotEnv["POSTGRES_PASSWORD"] } elseif ($env:POSTGRES_PASSWORD) { $env:POSTGRES_PASSWORD } else { "cosmochat" }

$dbUrl = if ($dotEnv.ContainsKey("COSMOCHAT_DB_URL")) { $dotEnv["COSMOCHAT_DB_URL"] } elseif ($env:COSMOCHAT_DB_URL) { $env:COSMOCHAT_DB_URL } else { ("jdbc:postgresql://127.0.0.1:5433/{0}" -f $defaultDbName) }
$dbUser = if ($dotEnv.ContainsKey("COSMOCHAT_DB_USER")) { $dotEnv["COSMOCHAT_DB_USER"] } elseif ($env:COSMOCHAT_DB_USER) { $env:COSMOCHAT_DB_USER } else { $defaultDbUser }
$dbPassword = if ($dotEnv.ContainsKey("COSMOCHAT_DB_PASSWORD")) { $dotEnv["COSMOCHAT_DB_PASSWORD"] } elseif ($env:COSMOCHAT_DB_PASSWORD) { $env:COSMOCHAT_DB_PASSWORD } else { $defaultDbPassword }

if ([string]::IsNullOrWhiteSpace($dbUrl)) { $dbUrl = ("jdbc:postgresql://127.0.0.1:5433/{0}" -f $defaultDbName) }
if ([string]::IsNullOrWhiteSpace($dbUser)) { $dbUser = $defaultDbUser }
if ([string]::IsNullOrWhiteSpace($dbPassword)) { $dbPassword = $defaultDbPassword }

# -- 1. Build ---------------------------------------------------------------
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
    Write-Host "[ERR] Maven not found. Place it in ../apache-maven-3.9.9/ or add to PATH" -ForegroundColor Red
    exit 1
}

Write-Host "Using Maven: $MAVEN`n" -ForegroundColor Gray
& $MAVEN clean compile | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "`n[ERR] Build failed!`n" -ForegroundColor Red
    exit 1
}
Write-Host "[OK] Build complete`n" -ForegroundColor Green

# -- 2. Python Backend ------------------------------------------------------
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
        Write-Host "[OK] Python backend already running" -ForegroundColor Green
        Write-Host "   Model: $($health.model), Loaded: $($health.loaded)`n" -ForegroundColor Gray
        $backendRunning = $true
    }
} catch {}

if (-not $backendRunning) {
    Start-PythonBackend
    try {
        $health = Invoke-RestMethod -Uri "http://127.0.0.1:5001/health" -Method Get -TimeoutSec 10 -ErrorAction Stop
        if ($health.status -eq "ok" -and $health.loaded) {
            Write-Host "[OK] Python backend started`n" -ForegroundColor Green
        } else {
            Write-Host "[WARN] Python backend started but model not loaded; continuing anyway`n" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "[WARN] Failed to start Python backend; continuing anyway`n" -ForegroundColor Yellow
        Write-Host "   You can run it manually: python backend\coder_api.py`n" -ForegroundColor Gray
    }
}

# -- 3. Launch JavaFX --------------------------------------------------------
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
    "$USER\.m2\repository\org\postgresql\postgresql\42.7.4\postgresql-42.7.4.jar",
    "$USER\.m2\repository\org\slf4j\slf4j-api\1.7.36\slf4j-api-1.7.36.jar"
)

foreach ($p in $paths) {
    if (-not (Test-Path $p)) {
        Write-Host "[ERR] Missing: $p" -ForegroundColor Red
        exit 1
    }
}

$MODULE_PATH = [System.String]::Join(';', $paths)

# DB connectivity check (prints detected config + ensures schema exists)
$javaArgsDbInit = @(
  "-Dcosmochat.db.url=$dbUrl",
  "-Dcosmochat.db.user=$dbUser",
  "-Dcosmochat.db.password=$dbPassword",
  "--module-path", $MODULE_PATH,
  "-m", "cosmochat/cosmochat.database.DbInit"
)
Start-Process -FilePath "java" -ArgumentList $javaArgsDbInit -NoNewWindow -Wait

$javaArgsApp = @(
  "-Dcosmochat.db.url=$dbUrl",
  "-Dcosmochat.db.user=$dbUser",
  "-Dcosmochat.db.password=$dbPassword",
  "--module-path", $MODULE_PATH,
  "-m", "cosmochat/cosmochat.CosmoChatApp"
)
$process = Start-Process -FilePath "java" -ArgumentList $javaArgsApp -PassThru
$process.WaitForExit()
