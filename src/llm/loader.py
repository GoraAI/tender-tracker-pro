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
