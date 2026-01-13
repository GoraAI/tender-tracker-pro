# FR-LLM-001: Model Management - Spec Delta

**Status**: PROPOSED
**Date**: 2026-01-12

---

## ADDED

### Configuration: config/models/gemma_3n.yaml

```yaml
# Gemma 3n Model Configuration

model:
  name: "gemma-3n"
  display_name: "Gemma 2 3B Instruct"
  hf_repo: "google/gemma-2-3b-it-GGUF"
  hf_filename: "3b_it_q4_k_m.gguf"
  local_path: "models/gemma-3n"
  quantization: "Q4_K_M"
  parameters: "3B"

download:
  chunk_size: 8388608  # 8 MB
  resume: true
  verify_checksum: true
  max_retries: 3
  timeout_seconds: 300

inference:
  # Model loading config
  n_ctx: 4096          # Context window (tokens)
  n_batch: 512         # Batch size
  n_threads: 4         # CPU threads (null = auto-detect)
  n_gpu_layers: 0      # CPU-only (0 = no GPU)

  # Generation config
  temperature: 0.1     # Low for extraction (0.0-1.0)
  top_p: 0.9          # Nucleus sampling
  top_k: 40           # Top-k sampling
  repeat_penalty: 1.1  # Penalize repetition
  max_tokens: 2048    # Max output length

  # Memory management
  use_mmap: true      # Memory-map model file
  use_mlock: false    # Lock in RAM (risky on low-mem systems)
  low_vram: false     # Reduce VRAM usage

metadata:
  version: "1.0"
  architecture: "gemma"
  license: "Apache 2.0"
  context_length: 8192  # Max supported
  vocab_size: 256000
```

---

### Module: src/llm/models.py

```python
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
```

---

### Module: src/llm/storage.py

```python
"""
Model filesystem storage management.
"""

import json
from datetime import datetime
from pathlib import Path
from typing import Optional

import structlog

from .models import ModelInfo, ModelStatus

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
```

---

### Module: src/llm/downloader.py

```python
"""
Model download from Hugging Face.
"""

import hashlib
import time
from pathlib import Path
from typing import Callable, Optional

import structlog
from huggingface_hub import hf_hub_download
from tqdm import tqdm

from .models import DownloadProgress

logger = structlog.get_logger()


class ModelDownloader:
    """Download models from Hugging Face."""

    def __init__(self, cache_dir: Optional[str] = None):
        """
        Initialize downloader.

        Args:
            cache_dir: Optional cache directory for downloads
        """
        self.cache_dir = Path(cache_dir) if cache_dir else None

    async def download(
        self,
        repo_id: str,
        filename: str,
        local_dir: Path,
        progress_callback: Optional[Callable[[DownloadProgress], None]] = None,
    ) -> Path:
        """
        Download model from Hugging Face.

        Args:
            repo_id: Hugging Face repo (e.g., "google/gemma-2-3b-it-GGUF")
            filename: File to download
            local_dir: Local directory to save to
            progress_callback: Optional callback for progress updates

        Returns:
            Path to downloaded file

        Raises:
            Exception: If download fails
        """
        logger.info(
            "download_started",
            repo_id=repo_id,
            filename=filename,
            local_dir=str(local_dir),
        )

        start_time = time.time()

        try:
            # Setup progress tracking
            progress_bar = None
            if progress_callback:
                progress_bar = tqdm(
                    unit="B",
                    unit_scale=True,
                    unit_divisor=1024,
                    desc=f"Downloading {filename}",
                )

            # Download from Hugging Face
            # Note: This is a blocking operation, but we wrap it in async
            # for consistency with the rest of the codebase
            downloaded_path = hf_hub_download(
                repo_id=repo_id,
                filename=filename,
                local_dir=str(local_dir),
                cache_dir=str(self.cache_dir) if self.cache_dir else None,
                resume_download=True,  # Resume if interrupted
                local_dir_use_symlinks=False,  # Copy, don't symlink
            )

            if progress_bar:
                progress_bar.close()

            duration_sec = time.time() - start_time
            file_path = Path(downloaded_path)
            file_size = file_path.stat().st_size

            logger.info(
                "download_complete",
                repo_id=repo_id,
                filename=filename,
                file_size=file_size,
                duration_sec=duration_sec,
                path=str(file_path),
            )

            return file_path

        except Exception as e:
            logger.error(
                "download_failed",
                repo_id=repo_id,
                filename=filename,
                error=str(e),
            )
            raise

    def verify_checksum(self, file_path: Path, expected_checksum: str) -> bool:
        """
        Verify file SHA256 checksum.

        Args:
            file_path: Path to file
            expected_checksum: Expected SHA256 hex digest

        Returns:
            True if checksum matches
        """
        logger.info("verifying_checksum", path=str(file_path))

        sha256 = hashlib.sha256()
        with open(file_path, "rb") as f:
            for chunk in iter(lambda: f.read(8192), b""):
                sha256.update(chunk)

        actual_checksum = sha256.hexdigest()
        matches = actual_checksum == expected_checksum

        if matches:
            logger.info("checksum_verified", path=str(file_path))
        else:
            logger.error(
                "checksum_mismatch",
                path=str(file_path),
                expected=expected_checksum,
                actual=actual_checksum,
            )

        return matches
```

---

### Module: src/llm/loader.py

```python
"""
Model loading with llama.cpp.
"""

from pathlib import Path
from typing import Optional

import structlog
from llama_cpp import Llama

logger = structlog.get_logger()


class ModelLoader:
    """Load GGUF models with llama.cpp."""

    def __init__(self):
        """Initialize model loader."""
        self.loaded_model: Optional[Llama] = None

    def load(
        self,
        model_path: Path,
        n_ctx: int = 4096,
        n_batch: int = 512,
        n_threads: Optional[int] = None,
        n_gpu_layers: int = 0,
        use_mmap: bool = True,
        use_mlock: bool = False,
        verbose: bool = False,
    ) -> Llama:
        """
        Load GGUF model file.

        Args:
            model_path: Path to .gguf file
            n_ctx: Context window size (tokens)
            n_batch: Batch size for prompt processing
            n_threads: CPU threads (None = auto-detect)
            n_gpu_layers: GPU layers to offload (0 = CPU-only)
            use_mmap: Memory-map model file
            use_mlock: Lock model in RAM
            verbose: Enable llama.cpp logging

        Returns:
            Loaded Llama model instance

        Raises:
            FileNotFoundError: If model file doesn't exist
            Exception: If loading fails
        """
        if not model_path.exists():
            raise FileNotFoundError(f"Model file not found: {model_path}")

        logger.info(
            "loading_model",
            path=str(model_path),
            n_ctx=n_ctx,
            n_threads=n_threads,
            n_gpu_layers=n_gpu_layers,
        )

        try:
            model = Llama(
                model_path=str(model_path),
                n_ctx=n_ctx,
                n_batch=n_batch,
                n_threads=n_threads,
                n_gpu_layers=n_gpu_layers,
                use_mmap=use_mmap,
                use_mlock=use_mlock,
                verbose=verbose,
            )

            self.loaded_model = model

            logger.info(
                "model_loaded",
                path=str(model_path),
                n_ctx=n_ctx,
            )

            return model

        except Exception as e:
            logger.error("model_load_failed", path=str(model_path), error=str(e))
            raise

    def unload(self) -> None:
        """Unload model from memory."""
        if self.loaded_model:
            # llama-cpp-python doesn't have explicit unload
            # Let Python GC handle it
            self.loaded_model = None
            logger.info("model_unloaded")

    def is_loaded(self) -> bool:
        """Check if model is loaded."""
        return self.loaded_model is not None
```

---

### Module: src/llm/manager.py

```python
"""
High-level model management orchestration.
"""

from datetime import datetime
from pathlib import Path
from typing import Optional

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
```

---

### Module: src/llm/__init__.py

```python
"""
LLM module for local inference with Gemma 3n.

Components:
- ModelManager: High-level model lifecycle management
- ModelDownloader: Download models from Hugging Face
- ModelLoader: Load GGUF models with llama.cpp
- ModelStorage: Filesystem operations
- ModelInfo: Model metadata
"""

from .downloader import ModelDownloader
from .loader import ModelLoader
from .manager import ModelManager
from .models import DownloadProgress, ModelInfo, ModelStatus
from .storage import ModelStorage

__all__ = [
    "ModelManager",
    "ModelDownloader",
    "ModelLoader",
    "ModelStorage",
    "ModelInfo",
    "ModelStatus",
    "DownloadProgress",
]
```

---

## MODIFIED

None - This is a new feature with no modifications to existing code.

---

## REMOVED

None - This is a new feature with no removals.
