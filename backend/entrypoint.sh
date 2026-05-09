#!/bin/sh
# Entrypoint script for Docker containers
# Checks for required model files before starting the API server

set -e

echo "============================================"
echo "CosmoChat Python Backend - Container Start"
echo "============================================"

# Model paths to check
QWEN_MODEL="models/qwen2.5-1.5b-instruct-gguf/Qwen2.5-1.5B-Instruct-Q8_0.gguf"
CODER_MODEL="models/qwen2.5-coder/qwen2.5-coder-1.5b-instruct-q2_k.gguf"

# Determine which service is starting based on command argument
SERVICE="$1"

check_model() {
    MODEL_PATH="$1"
    SERVICE_NAME="$2"

    if [ ! -f "$MODEL_PATH" ]; then
        echo "❌ Model not found for $SERVICE_NAME: $MODEL_PATH"
        echo ""
        echo "Please download the model first:"
        echo "  python backend/download_${SERVICE_NAME}.py"
        echo ""
        echo "Or mount a host directory with models:"
        echo "  docker-compose -f docker-compose.yml up -d"
        echo ""
        exit 1
    fi

    SIZE=$(du -h "$MODEL_PATH" | cut -f1)
    echo "✅ $SERVICE_NAME model found: $MODEL_PATH ($SIZE)"
}

echo "🔍 Checking model files..."

case "$SERVICE" in
    qwen)
        check_model "$QWEN_MODEL" "instruct"
        shift
        exec gunicorn --bind 0.0.0.0:5000 --workers 1 --threads 4 --timeout 120 backend.qwen_api:app "$@"
        ;;
    coder)
        check_model "$CODER_MODEL" "coder"
        shift
        exec gunicorn --bind 0.0.0.0:5001 --workers 1 --threads 4 --timeout 120 backend.coder_api:app "$@"
        ;;
    *)
        echo "Starting all services (no model checks)..."
        exec "$@"
        ;;
esac
