# Simple build script using Maven (recommended)
# Requires: Java 17+, Maven

Write-Host "`n=== Building CosmoChat ===" -ForegroundColor Cyan

# Resolve Maven path: check local (../apache-maven-*) else try PATH
$MAVEN = $null
$possiblePaths = @(
    "..\apache-maven-3.9.9\bin\mvn.cmd",
    "..\apache-maven\bin\mvn.cmd",
    "mvn.cmd"
)

foreach ($p in $possiblePaths) {
    $full = Resolve-Path $p -ErrorAction SilentlyContinue
    if ($full) {
        $MAVEN = $full
        break
    }
}

if (-not $MAVEN) {
    Write-Host "❌ Maven not found." -ForegroundColor Red
    Write-Host "   Expected: ..\apache-maven-3.9.9\bin\mvn.cmd`n" -ForegroundColor Yellow
    exit 1
}

Write-Host "Using Maven: $MAVEN`n" -ForegroundColor Gray

& $MAVEN clean compile | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "`n❌ Build failed!`n" -ForegroundColor Red
    exit 1
}

Write-Host "`n✅ Build successful!" -ForegroundColor Green
Write-Host "Output: target/classes`n" -ForegroundColor Gray
