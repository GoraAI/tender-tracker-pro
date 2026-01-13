"""
Unit tests for LLM model storage.
"""

import json
import tempfile
from datetime import datetime
from pathlib import Path

import pytest

from llm.models import ModelInfo, ModelStatus
from llm.storage import ModelStorage


@pytest.fixture
def temp_storage():
    """Create temporary storage for testing."""
    with tempfile.TemporaryDirectory() as tmpdir:
        storage = ModelStorage(base_path=tmpdir)
        yield storage


def test_storage_initialization(temp_storage):
    """Test storage initialization creates base path."""
    assert temp_storage.base_path.exists()
    assert temp_storage.base_path.is_dir()


def test_get_model_path(temp_storage):
    """Test getting model directory path."""
    model_path = temp_storage.get_model_path("test-model")

    assert model_path.exists()
    assert model_path.is_dir()
    assert model_path.name == "test-model"


def test_get_model_file_path(temp_storage):
    """Test getting model file path."""
    file_path = temp_storage.get_model_file_path("test-model", "model.gguf")

    assert file_path.parent.exists()  # Directory created
    assert file_path.name == "model.gguf"


def test_save_and_load_metadata(temp_storage):
    """Test saving and loading model metadata."""
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
        download_date=datetime(2026, 1, 12, 10, 30, 0),
        version="1.0",
    )

    # Save metadata
    temp_storage.save_metadata(model_info)

    # Load metadata
    loaded_info = temp_storage.load_metadata("test-model")

    assert loaded_info is not None
    assert loaded_info.name == "test-model"
    assert loaded_info.status == ModelStatus.DOWNLOADED
    assert loaded_info.file_size == 1000000
    assert loaded_info.version == "1.0"


def test_load_nonexistent_metadata(temp_storage):
    """Test loading metadata for nonexistent model."""
    loaded_info = temp_storage.load_metadata("nonexistent-model")

    assert loaded_info is None


def test_model_file_exists(temp_storage):
    """Test checking if model file exists."""
    # File doesn't exist yet
    assert not temp_storage.model_file_exists("test-model", "model.gguf")

    # Create file
    file_path = temp_storage.get_model_file_path("test-model", "model.gguf")
    file_path.write_text("fake model content")

    # File now exists
    assert temp_storage.model_file_exists("test-model", "model.gguf")


def test_get_model_file_size(temp_storage):
    """Test getting model file size."""
    # File doesn't exist
    assert temp_storage.get_model_file_size("test-model", "model.gguf") is None

    # Create file with known size
    file_path = temp_storage.get_model_file_path("test-model", "model.gguf")
    content = "x" * 1000
    file_path.write_text(content)

    # Get file size
    size = temp_storage.get_model_file_size("test-model", "model.gguf")
    assert size == 1000


def test_delete_model(temp_storage):
    """Test deleting model files."""
    # Create model files
    model_path = temp_storage.get_model_path("test-model")
    (model_path / "model.gguf").write_text("fake model")
    (model_path / "metadata.json").write_text("{}")

    # Verify files exist
    assert model_path.exists()
    assert (model_path / "model.gguf").exists()

    # Delete model
    temp_storage.delete_model("test-model")

    # Verify deleted
    assert not model_path.exists()


def test_metadata_json_format(temp_storage):
    """Test metadata is saved in correct JSON format."""
    model_info = ModelInfo(
        name="test-model",
        display_name="Test Model",
        hf_repo="test/repo",
        hf_filename="model.gguf",
        local_path=Path("models/test"),
        quantization="Q4_K_M",
        parameters="3B",
        status=ModelStatus.DOWNLOADED,
    )

    temp_storage.save_metadata(model_info)

    # Read JSON directly
    metadata_path = temp_storage.get_model_path("test-model") / "metadata.json"
    with open(metadata_path) as f:
        data = json.load(f)

    assert data["name"] == "test-model"
    assert data["status"] == "downloaded"
    assert "hf_repo" in data
