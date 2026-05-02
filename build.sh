$JAVAFX_HOME = "javafx/javafx-sdk-21.0.11/lib"
$BUILD = "build/classes"
$MODULE_PATH = "$JAVAFX_HOME;lib/*"

# Clean previous build
Remove-Item "$BUILD/*" -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path "$BUILD" -Force | Out-Null

# Copy resources
Copy-Item "src/cosmochat/styles.css" "$BUILD/cosmochat/" -Force

# Compile
Write-Host "Compiling with module path: $MODULE_PATH" -ForegroundColor Cyan
javac --module-path $MODULE_PATH -encoding UTF-8 -d $BUILD src/cosmochat/module-info.java src/cosmochat/*.java

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build successful!" -ForegroundColor Green
} else {
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}