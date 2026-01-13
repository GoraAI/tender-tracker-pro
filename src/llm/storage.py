"""
Model filesystem storage management.
"""

import json
from pathlib import Path
from typing import Optional

import structlog

from .models import ModelInfo

logger = structlog.get_logger()


class ModelStorage:
    """Manage model files on disk."""

    def __init__(self, base_path: str = "models"):
        """
        Initialize storage manager.

        Args:
            base_path: Base directory for models
        """
        self.base_path = Path(base_path)
        self.base_path.mkdir(parents=True, exist_ok=True)

    def get_model_path(self, model_name: str) -> Path:
        """
        Get path to model directory.

        Args:
            model_name: Model identifier

        Returns:
            Path to model directory
        """
        model_dir = self.base_path / model_name
        model_dir.mkdir(parents=True, exist_ok=True)
        return model_dir

    def get_model_file_path(self, model_name: str, filename: str) -> Path:
        """
        Get path to model file.

        Args:
            model_name: Model identifier
            filename: Model filename

        Returns:
            Path to model file
        """
        return self.get_model_path(model_name) / filename

    def save_metadata(self, model_info: ModelInfo) -> None:
        """
        Save model metadata to JSON.

        Args:
            model_info: Model information to save
        """
        model_dir = self.get_model_path(model_info.name)
        metadata_path = model_dir / "metadata.json"

        with open(metadata_path, "w") as f:
            json.dump(model_info.to_dict(), f, indent=2)

        logger.info(
            "metadata_saved",
            model_name=model_info.name,
            path=str(metadata_path),
        )

    def load_metadata(self, model_name: str) -> Optional[ModelInfo]:
        """
        Load model metadata from JSON.

        Args:
            model_name: Model identifier

        Returns:
            ModelInfo if exists, None otherwise
        """
        model_dir = self.get_model_path(model_name)
        metadata_path = model_dir / "metadata.json"

        if not metadata_path.exists():
            logger.debug("metadata_not_found", model_name=model_name)
            return None

        try:
            with open(metadata_path, "r") as f:
                data = json.load(f)

            model_info = ModelInfo.from_dict(data)
            logger.info("metadata_loaded", model_name=model_name)
            return model_info

        except Exception as e:
            logger.error(
                "metadata_load_failed",
                model_name=model_name,
                error=str(e),
            )
            return None

    def model_file_exists(self, model_name: str, filename: str) -> bool:
        """
        Check if model file exists.

        Args:
            model_name: Model identifier
            filename: Model filename

        Returns:
            True if file exists
        """
        path = self.get_model_file_path(model_name, filename)
        return path.exists()

    def get_model_file_size(self, model_name: str, filename: str) -> Optional[int]:
        """
        Get model file size in bytes.

        Args:
            model_name: Model identifier
            filename: Model filename

        Returns:
            File size in bytes, or None if not found
        """
        path = self.get_model_file_path(model_name, filename)
        if path.exists():
            return path.stat().st_size
        return None

    def delete_model(self, model_name: str) -> None:
        """
        Delete all model files.

        Args:
            model_name: Model identifier
        """
        import shutil

        model_dir = self.get_model_path(model_name)
        if model_dir.exists():
            shutil.rmtree(model_dir)
            logger.info("model_deleted", model_name=model_name)
