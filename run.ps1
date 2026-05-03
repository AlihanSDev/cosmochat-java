 # Load .env file if exists (for HF_TOKEN when using direct HuggingFace API)
 $envFile = "backend/spring-huggingface/.env"
 if (Test-Path $envFile) {
     Write-Host "Loading environment from $envFile..." -ForegroundColor Gray
     Get-Content $envFile | ForEach-Object {
         if ($_ -match "^\s*([^=]+)\s*=\s*(.*)\s*$") {
             $name = $matches[1]
             $value = $matches[2]
             $value = $value.Trim('"').Trim("'")
             Set-Item -Path "Env:\$name" -Value $value -ErrorAction SilentlyContinue
         }
     }
 }
 
 # Prepare Java options
 $javaOpts = @()
 if ($env:HF_TOKEN) {
     $javaOpts += "-DHF_TOKEN=$env:HF_TOKEN"
 }
 
 # Launch the application
 Write-Host "`n=== Running CosmoChat (No Backend) ===" -ForegroundColor Yellow
 Write-Host "Module path includes:"
 Write-Host "  - Build dir: $BUILD_DIR"
 Write-Host "  - JavaFX JARs: $($javafxJars.Count)"
 Write-Host "  - Library JARs: $($libJars.Count)`n" -ForegroundColor Gray
 
 Write-Host "Note: No AI backend is configured." -ForegroundColor Yellow
 Write-Host "Select a model from the dropdown — but sendMessage will fail unless:" -ForegroundColor Gray
 Write-Host "  - 'Qwen 1.5B' → run: python backend/qwen_api.py" -ForegroundColor Gray
 Write-Host "  - 'Qwen 7B' / 'Mistral' / 'Deepseek' → HuggingFace direct (HF_TOKEN set in .env)`n" -ForegroundColor Gray
 
 # Launch
 if ($javaOpts.Count -gt 0) {
     java @javaOpts --module-path $MODULE_PATH -m cosmochat/cosmochat.CosmoChatApp
 } else {
     java --module-path $MODULE_PATH -m cosmochat/cosmochat.CosmoChatApp
 }
