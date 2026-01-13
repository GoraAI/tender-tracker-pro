"""WebSocket endpoint for real-time job log streaming."""

import asyncio
import json
from datetime import datetime
from uuid import UUID

from fastapi import WebSocket, WebSocketDisconnect
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from ..database import async_session_maker as async_session_factory
from ..models.job import Job, JobLog


class ConnectionManager:
    """Manage WebSocket connections for job streaming."""

    def __init__(self):
        self.active_connections: dict[UUID, list[WebSocket]] = {}

    async def connect(self, websocket: WebSocket, job_id: UUID):
        """Accept a WebSocket connection for a job."""
        await websocket.accept()
        if job_id not in self.active_connections:
            self.active_connections[job_id] = []
        self.active_connections[job_id].append(websocket)

    def disconnect(self, websocket: WebSocket, job_id: UUID):
        """Remove a WebSocket connection."""
        if job_id in self.active_connections:
            if websocket in self.active_connections[job_id]:
                self.active_connections[job_id].remove(websocket)
            if not self.active_connections[job_id]:
                del self.active_connections[job_id]

    async def broadcast_to_job(self, job_id: UUID, message: dict):
        """Send a message to all connections watching a job."""
        if job_id in self.active_connections:
            dead_connections = []
            for websocket in self.active_connections[job_id]:
                try:
                    await websocket.send_json(message)
                except Exception:
                    dead_connections.append(websocket)

            # Clean up dead connections
            for ws in dead_connections:
                self.disconnect(ws, job_id)


manager = ConnectionManager()


async def websocket_endpoint(websocket: WebSocket, job_id: UUID):
    """WebSocket endpoint for streaming job logs."""
    await manager.connect(websocket, job_id)

    try:
        # Send initial job state and existing logs
        async with async_session_factory() as db:
            # Get job
            result = await db.execute(select(Job).where(Job.id == job_id))
            job = result.scalar_one_or_none()

            if not job:
                await websocket.send_json({
                    "type": "error",
                    "message": "Job not found",
                })
                await websocket.close()
                return

            # Send job status
            await websocket.send_json({
                "type": "status",
                "data": {
                    "id": str(job.id),
                    "status": job.status,
                    "progress": job.progress,
                    "total_items": job.total_items,
                    "processed_items": job.processed_items,
                    "error_message": job.error_message,
                },
            })

            # Send existing logs
            result = await db.execute(
                select(JobLog)
                .where(JobLog.job_id == job_id)
                .order_by(JobLog.created_at.asc())
            )
            logs = result.scalars().all()

            for log in logs:
                await websocket.send_json({
                    "type": "log",
                    "data": {
                        "id": str(log.id),
                        "level": log.level,
                        "message": log.message,
                        "timestamp": log.created_at.isoformat(),
                    },
                })

        # Keep connection alive and poll for updates
        last_log_id = logs[-1].id if logs else None
        last_status = job.status

        while True:
            await asyncio.sleep(1)  # Poll every second

            async with async_session_factory() as db:
                # Check for new logs
                query = select(JobLog).where(JobLog.job_id == job_id)
                if last_log_id:
                    query = query.where(JobLog.id > last_log_id)
                query = query.order_by(JobLog.created_at.asc())

                result = await db.execute(query)
                new_logs = result.scalars().all()

                for log in new_logs:
                    await websocket.send_json({
                        "type": "log",
                        "data": {
                            "id": str(log.id),
                            "level": log.level,
                            "message": log.message,
                            "timestamp": log.created_at.isoformat(),
                        },
                    })
                    last_log_id = log.id

                # Check for status changes
                result = await db.execute(select(Job).where(Job.id == job_id))
                job = result.scalar_one_or_none()

                if job and job.status != last_status:
                    await websocket.send_json({
                        "type": "status",
                        "data": {
                            "id": str(job.id),
                            "status": job.status,
                            "progress": job.progress,
                            "total_items": job.total_items,
                            "processed_items": job.processed_items,
                            "error_message": job.error_message,
                        },
                    })
                    last_status = job.status

                    # Close if job is done
                    if job.status in ("completed", "failed"):
                        await websocket.send_json({
                            "type": "complete",
                            "data": {"status": job.status},
                        })
                        break

    except WebSocketDisconnect:
        pass
    finally:
        manager.disconnect(websocket, job_id)
