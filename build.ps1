# Build script for CosmoChat (PowerShell)
$JAVAFX_HOME = "javafx/javafx-sdk-21.0.11/lib"
$BUILD_DIR = "build/classes"

# Collect all JAR files from JavaFX lib and project lib
$javafxJars = Get-ChildItem -Path $JAVAFX_HOME -Filter *.jar | ForEach-Object { $_.FullName }
$libJars = Get-ChildItem -Path "lib" -Filter *.jar | ForEach-Object { $_.FullName }
$allJars = $javafxJars + $libJars
$MODULE_PATH = [System.String]::Join(";", $allJars)

Write-Host "`n=== CosmoChat Build ===" -ForegroundColor Cyan
Write-Host "JavaFX modules: $($javafxJars.Count) JARs" -ForegroundColor Gray
Write-Host "Library modules: $($libJars.Count) JARs" -ForegroundColor Gray
Write-Host "Module path length: $($MODULE_PATH.Length) chars`n" -ForegroundColor Gray

# Clean previous build
if (Test-Path $BUILD_DIR) {
    Remove-Item "$BUILD_DIR/*" -Recurse -Force -ErrorAction SilentlyContinue
} else {
    New-Item -ItemType Directory -Path $BUILD_DIR -Force | Out-Null
}

# Ensure cosmochat package directory exists for resources
New-Item -ItemType Directory -Path "$BUILD_DIR/cosmochat" -Force | Out-Null

# Copy resources
Copy-Item "src/cosmochat/styles.css" "$BUILD_DIR/cosmochat/" -Force

# Collect all Java source files recursively
$javaFiles = Get-ChildItem -Path "src" -Filter *.java -Recurse | ForEach-Object { $_.FullName }

# Compile
Write-Host "Compiling $($javaFiles.Count) Java files..." -ForegroundColor Yellow
javac --module-path $MODULE_PATH -encoding UTF-8 -d $BUILD_DIR $javaFiles

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nBuild successful! Output: $BUILD_DIR" -ForegroundColor Green
} else {
    Write-Host "`nBuild failed!" -ForegroundColor Red
    exit 1
}
