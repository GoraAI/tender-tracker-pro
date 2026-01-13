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
