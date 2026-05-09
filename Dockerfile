# Multi-stage build for Python backend with llama-cpp-python
FROM python:3.11-slim-bookworm AS builder

# Install build dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    cmake \
    git \
    curl \
    wget \
    pkg-config \
    libssl-dev \
    libffi-dev \
    && rm -rf /var/lib/apt/lists/*

# Set working directory to backend (so relative paths work correctly)
WORKDIR /app/backend

# Copy requirements first for better layer caching
COPY backend/requirements-docker.txt .

# Install Python dependencies
# Use --prefer-binary to avoid compiling llama-cpp-python from source
RUN pip install --no-cache-dir --upgrade pip setuptools wheel
RUN pip install --no-cache-dir --prefer-binary -r requirements-docker.txt

# --- Runtime stage ---
FROM python:3.11-slim-bookworm

# Install runtime dependencies
RUN apt-get update && apt-get install -y \
    libgomp1 \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app/backend

# Copy installed packages from builder
COPY --from=builder /usr/local/lib/python3.11/site-packages /usr/local/lib/python3.11/site-packages
COPY --from=builder /usr/local/bin /usr/local/bin

# Copy backend application code
COPY backend/ /app/backend/

# Create models directory
RUN mkdir -p /app/backend/models

# Expose ports for both API services
EXPOSE 5000 5001

# Set environment variables
ENV PYTHONUNBUFFERED=1 \
    FLASK_ENV=production

# Default command - run both services via gunicorn (multi-process)
CMD ["sh", "-c", "\
    gunicorn --bind 0.0.0.0:5000 --workers 1 --threads 4 --timeout 120 qwen_api:app & \
    gunicorn --bind 0.0.0.0:5001 --workers 1 --threads 4 --timeout 120 coder_api:app & \
    wait -n; exit \$? \
"]
