# Run script for CosmoChat (NO backend)
# This runs the app without any AI backend.
# Use run-with-coder.ps1 for Python Qwen backend, or run-with-hf.ps1 for HuggingFace Spring backend
$JAVAFX_HOME = "javafx/javafx-sdk-21.0.11/lib"
$BUILD_DIR = "build/classes"

# Collect all JARs from JavaFX and lib
$javafxJars = Get-ChildItem -Path $JAVAFX_HOME -Filter *.jar | ForEach-Object { $_.FullName }
$libJars = Get-ChildItem -Path "lib" -Filter *.jar | ForEach-Object { $_.FullName }
$allPaths = @($BUILD_DIR) + $javafxJars + $libJars
$MODULE_PATH = [System.String]::Join(";", $allPaths)

Write-Host "`n=== Running CosmoChat (No Backend) ===" -ForegroundColor Yellow
Write-Host "Module path includes:"
Write-Host "  - Build dir: $BUILD_DIR"
Write-Host "  - JavaFX JARs: $($javafxJars.Count)"
Write-Host "  - Library JARs: $($libJars.Count)`n" -ForegroundColor Gray

Write-Host "Note: No AI backend is configured." -ForegroundColor Yellow
Write-Host "Select a model from the dropdown — but sendMessage will fail unless:" -ForegroundColor Gray
Write-Host "  - 'Qwen 1.5B' → run: python backend/qwen_api.py" -ForegroundColor Gray
Write-Host "  - 'Qwen 7B' / 'Mistral' / 'Deepseek' → run: backend/spring-huggingface`n" -ForegroundColor Gray

# Launch the application
java --module-path $MODULE_PATH -m cosmochat/cosmochat.CosmoChatApp
