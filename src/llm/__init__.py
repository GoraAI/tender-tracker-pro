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
