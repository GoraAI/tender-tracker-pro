"""
High-level model management orchestration.
"""

from datetime import datetime
from pathlib import Path

import structlog
import yaml
from llama_cpp import Llama

from .downloader import ModelDownloader
from .loader import ModelLoader
from .models import ModelInfo, ModelStatus
from .storage import ModelStorage

logger = structlog.get_logger()


class ModelManager:
    """Orchestrate model lifecycle."""

    def __init__(self, config_path: str = "config/models/gemma_3n.yaml"):
        """
        Initialize model manager.

        Args:
            config_path: Path to model configuration YAML
        """
        with open(config_path) as f:
            self.config = yaml.safe_load(f)

        self.storage = ModelStorage(
            base_path=self.config["model"]["local_path"].split("/")[0]
        )
        self.downloader = ModelDownloader()
        self.loader = ModelLoader()

        self.model_info = self._build_model_info()

    def _build_model_info(self) -> ModelInfo:
        """Build ModelInfo from config."""
        cfg = self.config["model"]

        # Try to load existing metadata
        existing = self.storage.load_metadata(cfg["name"])
        if existing:
            return existing

        # Create new ModelInfo
        return ModelInfo(
            name=cfg["name"],
            display_name=cfg["display_name"],
            hf_repo=cfg["hf_repo"],
            hf_filename=cfg["hf_filename"],
            local_path=Path(cfg["local_path"]),
            quantization=cfg["quantization"],
            parameters=cfg["parameters"],
            status=ModelStatus.NOT_DOWNLOADED,
        )

    async def download_model(self) -> ModelInfo:
        """
        Download model from Hugging Face.

        Returns:
            Updated ModelInfo

        Raises:
            Exception: If download fails
        """
        logger.info("starting_model_download", model=self.model_info.name)

        self.model_info.status = ModelStatus.DOWNLOADING
        self.storage.save_metadata(self.model_info)

        try:
            # Download model file
            model_dir = self.storage.get_model_path(self.model_info.name)

            downloaded_path = await self.downloader.download(
                repo_id=self.model_info.hf_repo,
                filename=self.model_info.hf_filename,
                local_dir=model_dir,
            )

            # Update metadata
            self.model_info.status = ModelStatus.DOWNLOADED
            self.model_info.file_size = downloaded_path.stat().st_size
            self.model_info.download_date = datetime.now()
            self.storage.save_metadata(self.model_info)

            logger.info(
                "model_downloaded",
                model=self.model_info.name,
                size_mb=self.model_info.file_size / 1024 / 1024,
            )

            return self.model_info

        except Exception as e:
            self.model_info.status = ModelStatus.ERROR
            self.model_info.error = str(e)
            self.storage.save_metadata(self.model_info)
            raise

    def is_downloaded(self) -> bool:
        """
        Check if model is downloaded.

        Returns:
            True if model file exists
        """
        return self.storage.model_file_exists(
            self.model_info.name, self.model_info.hf_filename
        )

    def load_model(self) -> Llama:
        """
        Load model into memory.

        Returns:
            Loaded Llama model instance

        Raises:
            FileNotFoundError: If model not downloaded
            Exception: If loading fails
        """
        if not self.is_downloaded():
            raise FileNotFoundError(
                f"Model not downloaded: {self.model_info.name}. "
                f"Run download_model() first."
            )

        # Check if already loaded
        if self.loader.is_loaded():
            logger.info("model_already_loaded", model=self.model_info.name)
            return self.loader.loaded_model

        logger.info("loading_model_into_memory", model=self.model_info.name)

        self.model_info.status = ModelStatus.LOADING
        self.storage.save_metadata(self.model_info)

        try:
            model_path = self.storage.get_model_file_path(
                self.model_info.name, self.model_info.hf_filename
            )

            # Load with config
            inference_cfg = self.config["inference"]
            model = self.loader.load(
                model_path=model_path,
                n_ctx=inference_cfg["n_ctx"],
                n_batch=inference_cfg["n_batch"],
                n_threads=inference_cfg.get("n_threads"),
                n_gpu_layers=inference_cfg["n_gpu_layers"],
                use_mmap=inference_cfg["use_mmap"],
                use_mlock=inference_cfg["use_mlock"],
            )

            self.model_info.status = ModelStatus.LOADED
            self.storage.save_metadata(self.model_info)

            return model

        except Exception as e:
            self.model_info.status = ModelStatus.ERROR
            self.model_info.error = str(e)
            self.storage.save_metadata(self.model_info)
            raise

    def unload_model(self) -> None:
        """Unload model from memory."""
        self.loader.unload()
        self.model_info.status = ModelStatus.DOWNLOADED
        self.storage.save_metadata(self.model_info)

    def get_model_info(self) -> ModelInfo:
        """
        Get current model information.

        Returns:
            ModelInfo with latest status
        """
        # Reload from disk to get latest
        loaded = self.storage.load_metadata(self.model_info.name)
        if loaded:
            self.model_info = loaded
        return self.model_info

    def delete_model(self) -> None:
        """Delete downloaded model."""
        self.unload_model()
        self.storage.delete_model(self.model_info.name)
        self.model_info.status = ModelStatus.NOT_DOWNLOADED
        logger.info("model_deleted", model=self.model_info.name)
