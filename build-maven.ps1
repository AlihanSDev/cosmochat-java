# Simple build script using Maven Wrapper (recommended)
# Requires: Java 17+

Write-Host "`n=== Building CosmoChat ===" -ForegroundColor Cyan

# Check if Maven Wrapper exists
$MAVEN_WRAPPER = "./mvnw"
if (-not (Test-Path $MAVEN_WRAPPER)) {
    # Fallback to system Maven if wrapper missing
    $MAVEN = "mvn"
    Write-Host "Maven Wrapper not found, using system Maven..." -ForegroundColor Yellow
}
else {
    $MAVEN = $MAVEN_WRAPPER
    Write-Host "Using Maven Wrapper" -ForegroundColor Gray
}

# Ensure wrapper is executable on *nix (ignored on Windows)
if ($IsLinux -or $IsMacOS) {
    chmod +x $MAVEN_WRAPPER
}

Write-Host "Running: $MAVEN clean compile`n" -ForegroundColor Gray

& $MAVEN clean compile | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "`n❌ Build failed!`n" -ForegroundColor Red
    exit 1
}

Write-Host "`n✅ Build successful!" -ForegroundColor Green
Write-Host "Output: target/classes`n" -ForegroundColor Gray
