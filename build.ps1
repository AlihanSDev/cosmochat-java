# Build script for CosmoChat (PowerShell) — No Maven required
# Uses javac directly with JavaFX modules

$JAVAFX_HOME = "javafx/javafx-sdk-21.0.11/lib"
$BUILD_DIR = "build/classes"
$SRC_DIR = "src/main/java"
$RES_DIR = "src/main/resources"

Write-Host "`n=== CosmoChat Build (Manual) ===" -ForegroundColor Cyan

# Check Java
try {
    $javaVer = javac -version 2>&1
    Write-Host "Java compiler: $javaVer" -ForegroundColor Green
} catch {
    Write-Host "Java not found! Install JDK 21+" -ForegroundColor Red
    exit 1
}

# Check JavaFX
if (-not (Test-Path $JAVAFX_HOME)) {
    Write-Host "JavaFX SDK not found at $JAVAFX_HOME" -ForegroundColor Red
    Write-Host "Download from: https://gluonhq.com/products/javafx/" -ForegroundColor Yellow
    exit 1
}

# Collect all JARs
$javafxJars = Get-ChildItem -Path $JAVAFX_HOME -Filter *.jar | ForEach-Object { $_.FullName }
$libJars = if (Test-Path "lib") { Get-ChildItem -Path "lib" -Filter *.jar | ForEach-Object { $_.FullName } } else { @() }
$allJars = $javafxJars + $libJars
$MODULE_PATH = [System.String]::Join(";", $allJars)

Write-Host "JavaFX modules: $($javafxJars.Count) JARs" -ForegroundColor Gray
Write-Host "Library modules: $($libJars.Count) JARs" -ForegroundColor Gray
Write-Host "Module path length: $($MODULE_PATH.Length) chars`n" -ForegroundColor Gray

# Clean
if (Test-Path $BUILD_DIR) {
    Remove-Item "$BUILD_DIR/*" -Recurse -Force -ErrorAction SilentlyContinue
} else {
    New-Item -ItemType Directory -Path $BUILD_DIR -Force | Out-Null
}

# Create package dirs for resources
New-Item -ItemType Directory -Path "$BUILD_DIR/cosmochat" -Force | Out-Null
New-Item -ItemType Directory -Path "$BUILD_DIR/cosmochat/application/dto" -Force | Out-Null
New-Item -ItemType Directory -Path "$BUILD_DIR/cosmochat/application/mapper" -Force | Out-Null
New-Item -ItemType Directory -Path "$BUILD_DIR/cosmochat/application/port" -Force | Out-Null
New-Item -ItemType Directory -Path "$BUILD_DIR/cosmochat/application/service" -Force | Out-Null
New-Item -ItemType Directory -Path "$BUILD_DIR/cosmochat/domain" -Force | Out-Null
New-Item -ItemType Directory -Path "$BUILD_DIR/cosmochat/domain/port" -Force | Out-Null
New-Item -ItemType Directory -Path "$BUILD_DIR/cosmochat/infrastructure" -Force | Out-Null
New-Item -ItemType Directory -Path "$BUILD_DIR/cosmochat/infrastructure/adapter" -Force | Out-Null
New-Item -ItemType Directory -Path "$BUILD_DIR/cosmochat/auth" -Force | Out-Null
New-Item -ItemType Directory -Path "$BUILD_DIR/cosmochat/database" -Force | Out-Null
New-Item -ItemType Directory -Path "$BUILD_DIR/cosmochat/security" -Force | Out-Null

# Copy resources
Copy-Item "$RES_DIR/cosmochat/styles.css" "$BUILD_DIR/cosmochat/" -Force

# Collect Java files
$javaFiles = Get-ChildItem -Path $SRC_DIR -Filter *.java -Recurse | ForEach-Object { $_.FullName }
Write-Host "Compiling $($javaFiles.Count) Java files..." -ForegroundColor Yellow
Write-Host "Source dir: $SRC_DIR" -ForegroundColor Gray
Write-Host "Output dir: $BUILD_DIR`n" -ForegroundColor Gray

# Compile
javac --module-path $MODULE_PATH -encoding UTF-8 -d $BUILD_DIR $javaFiles

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nBuild successful! Output: $BUILD_DIR" -ForegroundColor Green
    Write-Host "Run with: .\run.ps1" -ForegroundColor Cyan
} else {
    Write-Host "`nBuild failed!" -ForegroundColor Red
    exit 1
}
