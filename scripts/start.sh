#!/bin/bash
set -e

echo "========================================"
echo "Tender Tracker Pro - Startup Script"
echo "========================================"
echo "Running database migrations..."
alembic upgrade head || { echo "Migration failed!"; exit 1; }
echo "Migrations complete. Starting server..."

RUN_PORT="${PORT:-8000}"
echo "Server will listen on port: ${RUN_PORT}"

exec uvicorn src.api.main:app --host 0.0.0.0 --port "${RUN_PORT}" --loop asyncio
