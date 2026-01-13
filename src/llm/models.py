"""
Data models for LLM management.
"""

from dataclasses import dataclass
from datetime import datetime
from enum import Enum
from pathlib import Path
from typing import Optional


class ModelStatus(Enum):
    """Model status enum."""

    NOT_DOWNLOADED = "not_downloaded"
    DOWNLOADING = "downloading"
    DOWNLOADED = "downloaded"
    LOADING = "loading"
    LOADED = "loaded"
    ERROR = "error"


@dataclass
class ModelInfo:
    """Model metadata and status."""

    name: str
    display_name: str
    hf_repo: str
    hf_filename: str
    local_path: Path
    quantization: str
    parameters: str

    # Runtime info
    status: ModelStatus
    file_size: Optional[int] = None  # bytes
    download_date: Optional[datetime] = None
    checksum: Optional[str] = None
    version: Optional[str] = None

    # Error tracking
    error: Optional[str] = None

    def to_dict(self) -> dict:
        """Convert to dictionary for JSON serialization."""
        return {
            "name": self.name,
            "display_name": self.display_name,
            "hf_repo": self.hf_repo,
            "hf_filename": self.hf_filename,
            "local_path": str(self.local_path),
            "quantization": self.quantization,
            "parameters": self.parameters,
            "status": self.status.value,
            "file_size": self.file_size,
            "download_date": (
                self.download_date.isoformat() if self.download_date else None
            ),
            "checksum": self.checksum,
            "version": self.version,
            "error": self.error,
        }

    @classmethod
    def from_dict(cls, data: dict) -> "ModelInfo":
        """Create ModelInfo from dictionary."""
        return cls(
            name=data["name"],
            display_name=data["display_name"],
            hf_repo=data["hf_repo"],
            hf_filename=data["hf_filename"],
            local_path=Path(data["local_path"]),
            quantization=data["quantization"],
            parameters=data["parameters"],
            status=ModelStatus(data["status"]),
            file_size=data.get("file_size"),
            download_date=(
                datetime.fromisoformat(data["download_date"])
                if data.get("download_date")
                else None
            ),
            checksum=data.get("checksum"),
            version=data.get("version"),
            error=data.get("error"),
        )


@dataclass
class DownloadProgress:
    """Download progress tracking."""

    downloaded_bytes: int
    total_bytes: int
    speed_mbps: float
    eta_seconds: Optional[int]

    @property
    def percent(self) -> float:
        """Get download percentage."""
        if self.total_bytes == 0:
            return 0.0
        return (self.downloaded_bytes / self.total_bytes) * 100

    def __str__(self) -> str:
        """Human-readable progress."""
        mb_down = self.downloaded_bytes / 1024 / 1024
        mb_total = self.total_bytes / 1024 / 1024
        eta = f"{self.eta_seconds}s" if self.eta_seconds else "unknown"
        return (
            f"{self.percent:.1f}% ({mb_down:.1f}/{mb_total:.1f} MB) "
            f"@ {self.speed_mbps:.1f} MB/s, ETA: {eta}"
        )
