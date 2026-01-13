"""
Unit tests for ModelManager (integration-style with mocking).
"""

import tempfile
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from llm.manager import ModelManager
from llm.models import ModelStatus


@pytest.fixture
def temp_config():
    """Create temporary config file for testing."""
    config_content = """
model:
  name: "test-model"
  display_name: "Test Model"
  hf_repo: "test/repo"
  hf_filename: "model.gguf"
  local_path: "models/test-model"
  quantization: "Q4_K_M"
  parameters: "3B"

download:
  chunk_size: 8388608
  resume: true
  verify_checksum: true
  max_retries: 3
  timeout_seconds: 300

inference:
  n_ctx: 4096
  n_batch: 512
  n_threads: 4
  n_gpu_layers: 0
  temperature: 0.1
  top_p: 0.9
  top_k: 40
  repeat_penalty: 1.1
  max_tokens: 2048
  use_mmap: true
  use_mlock: false
  low_vram: false
"""
    with tempfile.NamedTemporaryFile(mode="w", suffix=".yaml", delete=False) as f:
        f.write(config_content)
        config_path = f.name

    # Change to temp models directory
    with tempfile.TemporaryDirectory() as tmpdir:
        yield config_path, tmpdir


def test_manager_initialization(temp_config):
    """Test ModelManager initialization."""
    config_path, tmpdir = temp_config

    manager = ModelManager(config_path=config_path)

    assert manager.model_info.name == "test-model"
    assert manager.model_info.status == ModelStatus.NOT_DOWNLOADED
    assert manager.storage is not None
    assert manager.downloader is not None
    assert manager.loader is not None


def test_is_downloaded_false(temp_config):
    """Test is_downloaded when model not downloaded."""
    config_path, tmpdir = temp_config

    manager = ModelManager(config_path=config_path)

    assert manager.is_downloaded() is False


def test_is_downloaded_true(temp_config):
    """Test is_downloaded when model exists."""
    config_path, tmpdir = temp_config

    manager = ModelManager(config_path=config_path)

    # Create fake model file
    model_dir = manager.storage.get_model_path("test-model")
    model_file = model_dir / "model.gguf"
    model_file.write_text("fake model content")

    assert manager.is_downloaded() is True


@pytest.mark.asyncio
async def test_download_model_success(temp_config):
    """Test successful model download."""
    config_path, tmpdir = temp_config

    manager = ModelManager(config_path=config_path)

    # Mock the downloader
    with patch.object(
        manager.downloader, "download", new_callable=AsyncMock
    ) as mock_download:
        # Create fake downloaded file
        model_dir = manager.storage.get_model_path("test-model")
        fake_file = model_dir / "model.gguf"
        fake_file.write_text("fake model content")

        mock_download.return_value = fake_file

        # Download model
        result = await manager.download_model()

        assert result.status == ModelStatus.DOWNLOADED
        assert result.file_size > 0
        assert result.download_date is not None

        # Verify downloader was called correctly
        mock_download.assert_called_once()


@pytest.mark.asyncio
async def test_download_model_failure(temp_config):
    """Test model download failure."""
    config_path, tmpdir = temp_config

    manager = ModelManager(config_path=config_path)

    # Mock the downloader to raise exception
    with patch.object(
        manager.downloader, "download", new_callable=AsyncMock
    ) as mock_download:
        mock_download.side_effect = Exception("Download failed")

        # Download should raise exception
        with pytest.raises(Exception, match="Download failed"):
            await manager.download_model()

        # Verify error status
        assert manager.model_info.status == ModelStatus.ERROR
        assert manager.model_info.error == "Download failed"


def test_load_model_not_downloaded(temp_config):
    """Test loading model when not downloaded."""
    config_path, tmpdir = temp_config

    manager = ModelManager(config_path=config_path)

    with pytest.raises(FileNotFoundError, match="Model not downloaded"):
        manager.load_model()


def test_load_model_success(temp_config):
    """Test successful model loading."""
    config_path, tmpdir = temp_config

    manager = ModelManager(config_path=config_path)

    # Create fake model file
    model_dir = manager.storage.get_model_path("test-model")
    model_file = model_dir / "model.gguf"
    model_file.write_text("fake model content")

    # Mock the loader
    with patch.object(manager.loader, "load") as mock_load:
        fake_model = MagicMock()
        mock_load.return_value = fake_model

        # Load model
        loaded_model = manager.load_model()

        assert loaded_model == fake_model
        assert manager.model_info.status == ModelStatus.LOADED

        # Verify loader was called with correct params
        mock_load.assert_called_once()
        call_args = mock_load.call_args
        assert call_args.kwargs["n_ctx"] == 4096
        assert call_args.kwargs["n_batch"] == 512


def test_load_model_already_loaded(temp_config):
    """Test loading model when already loaded."""
    config_path, tmpdir = temp_config

    manager = ModelManager(config_path=config_path)

    # Create fake model file
    model_dir = manager.storage.get_model_path("test-model")
    model_file = model_dir / "model.gguf"
    model_file.write_text("fake model content")

    # Set loader as already loaded
    fake_model = MagicMock()
    manager.loader.loaded_model = fake_model

    # Load model (should return existing)
    loaded_model = manager.load_model()

    assert loaded_model == fake_model


def test_unload_model(temp_config):
    """Test unloading model."""
    config_path, tmpdir = temp_config

    manager = ModelManager(config_path=config_path)

    # Simulate loaded model
    manager.model_info.status = ModelStatus.LOADED
    manager.loader.loaded_model = MagicMock()

    # Unload
    manager.unload_model()

    assert manager.loader.loaded_model is None
    assert manager.model_info.status == ModelStatus.DOWNLOADED


def test_get_model_info(temp_config):
    """Test getting model info."""
    config_path, tmpdir = temp_config

    manager = ModelManager(config_path=config_path)

    info = manager.get_model_info()

    assert info.name == "test-model"
    assert info.display_name == "Test Model"


def test_delete_model(temp_config):
    """Test deleting model."""
    config_path, tmpdir = temp_config

    manager = ModelManager(config_path=config_path)

    # Create fake model files
    model_dir = manager.storage.get_model_path("test-model")
    model_file = model_dir / "model.gguf"
    model_file.write_text("fake model content")

    # Verify exists
    assert model_dir.exists()

    # Delete
    manager.delete_model()

    # Verify deleted
    assert not model_dir.exists()
    assert manager.model_info.status == ModelStatus.NOT_DOWNLOADED
