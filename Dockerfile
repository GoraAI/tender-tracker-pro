# Tender Tracker Pro - Production Dockerfile for Railway
# Based on: https://playwright.dev/python/docs/docker
# and https://docs.railway.com/guides/fastapi
# Cache bust: 2026-01-16-v2

# Use Python 3.12 bookworm (recommended by Playwright docs)
FROM python:3.12-bookworm

# Set Python environment variables
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    # Playwright browser path - critical for user context
    PLAYWRIGHT_BROWSERS_PATH=/ms-playwright \
    # Memory optimization for containers
    MALLOC_ARENA_MAX=2

# Create virtual environment (Railway best practice)
RUN python -m venv /opt/venv
ENV PATH=/opt/venv/bin:$PATH

# Upgrade pip
RUN pip install --upgrade pip

# Set work directory
WORKDIR /app

# Copy requirements first for better caching
COPY requirements-prod.txt .

# Install Python dependencies
RUN pip install --no-cache-dir -r requirements-prod.txt

# Install Playwright with all system dependencies
# --with-deps installs browser AND system dependencies automatically
RUN playwright install chromium --with-deps

# Copy application code
COPY . .

# Create startup script for Railway (supports IPv6)
RUN printf '#!/bin/bash\n\
RUN_PORT="${PORT:-8000}"\n\
exec uvicorn src.api.main:app --host "[::]" --port "$RUN_PORT"\n' > /app/start.sh && \
    chmod +x /app/start.sh

# Clean up apt cache to reduce image size
RUN apt-get clean && rm -rf /var/lib/apt/lists/*

# Run as non-root user for security (Playwright recommendation)
RUN useradd -m -u 1000 appuser && \
    chown -R appuser:appuser /app /opt/venv /ms-playwright
USER appuser

# Start command
CMD ["/app/start.sh"]
