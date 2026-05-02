# Run script for CosmoChat (PowerShell)
$JAVAFX_HOME = "javafx/javafx-sdk-21.0.11/lib"
$BUILD_DIR = "build/classes"

# Collect all JARs from JavaFX and lib
$javafxJars = Get-ChildItem -Path $JAVAFX_HOME -Filter *.jar | ForEach-Object { $_.FullName }
$libJars = Get-ChildItem -Path "lib" -Filter *.jar | ForEach-Object { $_.FullName }
$allPaths = @($BUILD_DIR) + $javafxJars + $libJars
$MODULE_PATH = [System.String]::Join(";", $allPaths)

Write-Host "`n=== Running CosmoChat ===" -ForegroundColor Cyan
Write-Host "Module path includes:"
Write-Host "  - Build dir: $BUILD_DIR"
Write-Host "  - JavaFX JARs: $($javafxJars.Count)"
Write-Host "  - Library JARs: $($libJars.Count)`n" -ForegroundColor Gray

# Launch the application
java --module-path $MODULE_PATH -m cosmochat/cosmochat.CosmoChatApp
