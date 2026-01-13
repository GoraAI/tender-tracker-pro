# FR-LLM-001: Gemma 3n Model Management

**Status**: PROPOSED
**Priority**: P0
**Created**: 2026-01-12
**Dependencies**: None

---

## Summary

Implement model management system for downloading, loading, and managing Google's Gemma 3n (3-billion parameter) model for local LLM inference.

---

## Motivation

The tender tracker needs local LLM capabilities for:
- Extracting structured information from tender PDFs
- Scoring tender relevance to user criteria
- Operating offline without API costs

Gemma 3n is ideal because:
- Small enough to run on consumer hardware (3B params)
- Good performance on extraction/classification tasks
- Free and open-source from Google
- Supports GGUF format via llama.cpp

---

## Scope

### In Scope
- Model downloading from Hugging Face
- Model storage and versioning
- Model loading with llama.cpp-python
- Configuration management
- Progress tracking for downloads
- Model validation (checksum, format)
- Memory-efficient loading options
- Model metadata tracking

### Out of Scope
- Fine-tuning or training
- Multiple model support (future feature)
- GPU acceleration (CPU-only for now)
- Model quantization (use pre-quantized GGUF)
- Web UI for model management

---

## Acceptance Criteria

- [ ] Can download Gemma 3n GGUF model from Hugging Face
- [ ] Shows download progress (MB/s, ETA)
- [ ] Validates model integrity (checksum)
- [ ] Stores model in organized directory structure
- [ ] Can load model into memory for inference
- [ ] Supports configurable context window size
- [ ] Handles download resume on interruption
- [ ] Tracks model metadata (version, size, download date)
- [ ] Provides model status check (downloaded, loaded, etc.)
- [ ] Handles missing/corrupted model gracefully
- [ ] Logs all operations with structlog
- [ ] Unit tests for all components (mocked downloads)

---

## Technical Approach

### Architecture

```
ModelManager
├── ModelDownloader (Hugging Face API)
├── ModelLoader (llama.cpp-python)
├── ModelStorage (filesystem)
└── ModelMetadata (JSON tracking)
```

### Model Selection

**Model**: `google/gemma-2-3b-it-GGUF` (Instruction-tuned)
**Quantization**: Q4_K_M (4-bit, balanced quality/size)
**Size**: ~2.3 GB
**Format**: GGUF (llama.cpp compatible)

**Why this variant?**
- Small enough for consumer hardware
- Instruction-tuned for extraction tasks
- Q4_K_M: Best balance of quality and size
- Native GGUF support in llama.cpp-python

### Storage Structure

```
models/
├── gemma-3n/
│   ├── gemma-2-3b-it-q4_k_m.gguf    # Model file
│   ├── metadata.json                 # Model info
│   └── config.json                   # Load config
└── .cache/                           # Download cache
```

### Download Strategy

1. Use `huggingface_hub` library for official API
2. Show progress with `tqdm`
3. Resume partial downloads automatically
4. Verify file integrity with checksum
5. Atomic writes (download to temp, then rename)

### Loading Strategy

1. Use `llama-cpp-python` for inference
2. Load with CPU-only (n_gpu_layers=0)
3. Configurable context window (2048-8192 tokens)
4. Lazy loading (only when needed)
5. Keep loaded model in memory for reuse

---

## Implementation Plan

### Phase 1: Model Downloader
- Download from Hugging Face with progress
- Resume support
- Checksum validation
- Storage management

### Phase 2: Model Loader
- llama.cpp-python integration
- Configuration management
- Memory management
- Error handling

### Phase 3: Model Metadata
- Track model info (version, size, date)
- Model status checks
- Version management

### Phase 4: Testing
- Unit tests with mocked downloads
- Integration tests with small test model
- Performance benchmarks

---

## Configuration

```yaml
# config/models/gemma_3n.yaml
model:
  name: "gemma-3n"
  hf_repo: "google/gemma-2-3b-it-GGUF"
  hf_filename: "3b_it_q4_k_m.gguf"
  local_path: "models/gemma-3n"

download:
  chunk_size: 8388608  # 8 MB chunks
  resume: true
  verify_checksum: true
  max_retries: 3

inference:
  n_ctx: 4096          # Context window
  n_batch: 512         # Batch size
  n_threads: 4         # CPU threads
  temperature: 0.1     # Low temp for extraction
  max_tokens: 2048     # Max output
```

---

## API Design

### ModelManager

```python
class ModelManager:
    """Manage Gemma 3n model lifecycle."""

    def __init__(self, config: dict):
        """Initialize with model config."""

    async def download_model(self) -> ModelInfo:
        """Download model from Hugging Face."""
        # Returns: ModelInfo with path, size, checksum

    def is_downloaded(self) -> bool:
        """Check if model is downloaded."""

    def load_model(self) -> LlamaModel:
        """Load model into memory."""
        # Returns: llama-cpp-python Llama instance

    def unload_model(self) -> None:
        """Unload model from memory."""

    def get_model_info(self) -> ModelInfo:
        """Get model metadata."""

    def delete_model(self) -> None:
        """Delete downloaded model."""
```

### ModelDownloader

```python
class ModelDownloader:
    """Download models from Hugging Face."""

    async def download(
        self,
        repo_id: str,
        filename: str,
        local_dir: Path,
    ) -> Path:
        """Download model file with progress."""
        # Uses huggingface_hub.hf_hub_download
        # Shows progress with callback
        # Returns: Path to downloaded file
```

### ModelLoader

```python
class ModelLoader:
    """Load GGUF models with llama.cpp."""

    def load(self, model_path: Path, config: dict) -> Llama:
        """Load GGUF model."""
        # Returns: Llama instance from llama-cpp-python

    def unload(self, model: Llama) -> None:
        """Unload model from memory."""
```

---

## Dependencies

New packages required:

```
llama-cpp-python==0.2.90    # LLM inference
huggingface-hub==0.20.3     # Model downloads
tqdm==4.66.1                # Progress bars
```

---

## Performance Targets

- **Download**: Show progress every 1 second
- **Load time**: < 10 seconds on consumer hardware
- **Memory usage**: < 4 GB RAM
- **Inference**: Will be tested in FR-LLM-002

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Large model file (2.3 GB) | Slow downloads | Resume support, progress tracking |
| Out of memory on load | Crash | Check available RAM before loading |
| Model API changes | Download fails | Pin versions, add fallback URLs |
| Corrupted download | Invalid model | Checksum validation |
| llama.cpp build issues | Can't load model | Use pre-built wheels |

---

## Testing Strategy

1. **Unit Tests**:
   - Mock Hugging Face downloads
   - Test storage logic
   - Test metadata management
   - Test configuration loading

2. **Integration Tests**:
   - Download small test model (tiny-llama)
   - Load and verify inference works
   - Test resume on interrupted download

3. **Manual Tests**:
   - Full Gemma 3n download
   - Load on target hardware
   - Memory usage validation

---

## Success Metrics

- [ ] Model downloads successfully
- [ ] Download resumes after interruption
- [ ] Model loads without errors
- [ ] Memory usage < 4 GB
- [ ] All unit tests pass
- [ ] Documentation complete

---

## Future Enhancements

- GPU acceleration (CUDA/Metal)
- Multiple model support
- Model switching at runtime
- Quantization options
- Model fine-tuning support
