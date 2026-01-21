"""
Unit tests for LLM data models.
"""

from datetime import datetime
from pathlib import Path


from src.llm.models import DownloadProgress, ModelInfo, ModelStatus


def test_model_status_enum():
    """Test ModelStatus enum values."""
    assert ModelStatus.NOT_DOWNLOADED.value == "not_downloaded"
    assert ModelStatus.DOWNLOADING.value == "downloading"
    assert ModelStatus.DOWNLOADED.value == "downloaded"
    assert ModelStatus.LOADING.value == "loading"
    assert ModelStatus.LOADED.value == "loaded"
    assert ModelStatus.ERROR.value == "error"


def test_model_info_creation():
    """Test ModelInfo creation."""
    model_info = ModelInfo(
        name="test-model",
        display_name="Test Model",
        hf_repo="test/repo",
        hf_filename="model.gguf",
        local_path=Path("models/test"),
        quantization="Q4_K_M",
        parameters="3B",
        status=ModelStatus.NOT_DOWNLOADED,
    )

    assert model_info.name == "test-model"
    assert model_info.status == ModelStatus.NOT_DOWNLOADED
    assert model_info.file_size is None
    assert model_info.error is None


def test_model_info_to_dict():
    """Test ModelInfo serialization to dict."""
    download_date = datetime(2026, 1, 12, 10, 30, 0)

    model_info = ModelInfo(
        name="test-model",
        display_name="Test Model",
        hf_repo="test/repo",
        hf_filename="model.gguf",
        local_path=Path("models/test"),
        quantization="Q4_K_M",
        parameters="3B",
        status=ModelStatus.DOWNLOADED,
        file_size=1000000,
        download_date=download_date,
        version="1.0",
    )

    data = model_info.to_dict()

    assert data["name"] == "test-model"
    assert data["status"] == "downloaded"
    assert data["file_size"] == 1000000
    assert data["download_date"] == "2026-01-12T10:30:00"
    assert data["version"] == "1.0"
    assert data["local_path"] == "models/test"


def test_model_info_from_dict():
    """Test ModelInfo deserialization from dict."""
    data = {
        "name": "test-model",
        "display_name": "Test Model",
        "hf_repo": "test/repo",
        "hf_filename": "model.gguf",
        "local_path": "models/test",
        "quantization": "Q4_K_M",
        "parameters": "3B",
        "status": "downloaded",
        "file_size": 1000000,
        "download_date": "2026-01-12T10:30:00",
        "version": "1.0",
        "checksum": "abc123",
        "error": None,
    }

    model_info = ModelInfo.from_dict(data)

    assert model_info.name == "test-model"
    assert model_info.status == ModelStatus.DOWNLOADED
    assert model_info.file_size == 1000000
    assert model_info.download_date == datetime(2026, 1, 12, 10, 30, 0)
    assert isinstance(model_info.local_path, Path)


def test_download_progress_percent():
    """Test DownloadProgress percent calculation."""
    progress = DownloadProgress(
        downloaded_bytes=500 * 1024 * 1024,  # 500 MB
        total_bytes=1000 * 1024 * 1024,  # 1000 MB
        speed_mbps=10.5,
        eta_seconds=60,
    )

    assert progress.percent == 50.0


def test_download_progress_str():
    """Test DownloadProgress string representation."""
    progress = DownloadProgress(
        downloaded_bytes=500 * 1024 * 1024,  # 500 MB
        total_bytes=1000 * 1024 * 1024,  # 1000 MB
        speed_mbps=10.5,
        eta_seconds=60,
    )

    progress_str = str(progress)

    assert "50.0%" in progress_str
    assert "500.0/1000.0 MB" in progress_str
    assert "10.5 MB/s" in progress_str
    assert "60s" in progress_str


def test_download_progress_zero_total():
    """Test DownloadProgress with zero total bytes."""
    progress = DownloadProgress(
        downloaded_bytes=0,
        total_bytes=0,
        speed_mbps=0.0,
        eta_seconds=None,
    )

    assert progress.percent == 0.0
    assert "unknown" in str(progress)
