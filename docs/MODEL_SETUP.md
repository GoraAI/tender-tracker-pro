# Gemma 3n Model Setup Guide

**Purpose:** Instructions for downloading and configuring Gemma 3n models for local inference

---

## Overview

Tender Tracker Pro uses **Gemma 3n** models (E2B and E4B) for on-device AI analysis. These models run locally via `llama.cpp`, requiring no internet connection or API costs.

### Models Required

| Model | Parameters | Size (Q4) | Use Case | Performance Target |
|-------|------------|-----------|----------|-------------------|
| **Gemma 3n E2B** | 2B | ~3 GB | Quick extraction, simple tasks | TTFT <500ms, >40 tok/s |
| **Gemma 3n E4B** | 4B | ~6 GB | Deep analysis, scoring | TTFT <800ms, >25 tok/s |

---

## Step 1: Download Models

### Option A: Official Hugging Face (Recommended)

```bash
# Install huggingface-hub if not already installed
pip install huggingface-hub

# Download Gemma 3n E2B (Q4_K_M quantization)
huggingface-cli download \
  google/gemma-3n-2b-it-GGUF \
  gemma-3n-2b-it-Q4_K_M.gguf \
  --local-dir ~/tender_tracker_pro/models/

# Download Gemma 3n E4B (Q4_K_M quantization)
huggingface-cli download \
  google/gemma-3n-4b-it-GGUF \
  gemma-3n-4b-it-Q4_K_M.gguf \
  --local-dir ~/tender_tracker_pro/models/
```

### Option B: Direct Download with wget

```bash
cd ~/tender_tracker_pro/models/

# Gemma 3n E2B
wget https://huggingface.co/google/gemma-3n-2b-it-GGUF/resolve/main/gemma-3n-2b-it-Q4_K_M.gguf

# Gemma 3n E4B
wget https://huggingface.co/google/gemma-3n-4b-it-GGUF/resolve/main/gemma-3n-4b-it-Q4_K_M.gguf
```

### Option C: Manual Download

1. Visit: https://huggingface.co/google/gemma-3n-2b-it-GGUF
2. Download: `gemma-3n-2b-it-Q4_K_M.gguf`
3. Visit: https://huggingface.co/google/gemma-3n-4b-it-GGUF
4. Download: `gemma-3n-4b-it-Q4_K_M.gguf`
5. Move files to: `~/tender_tracker_pro/models/`

---

## Step 2: Rename Models (For Config Compatibility)

```bash
cd ~/tender_tracker_pro/models/

# Rename to match config expectations
mv gemma-3n-2b-it-Q4_K_M.gguf gemma-3n-e2b-q4.gguf
mv gemma-3n-4b-it-Q4_K_M.gguf gemma-3n-e4b-q4.gguf

# Verify files
ls -lh *.gguf
```

Expected output:
```
-rw-r--r-- 1 user user 1.7G Jan 12 12:00 gemma-3n-e2b-q4.gguf
-rw-r--r-- 1 user user 3.2G Jan 12 12:00 gemma-3n-e4b-q4.gguf
```

---

## Step 3: Install llama.cpp Python Bindings

### CPU-Only Installation

```bash
cd ~/tender_tracker_pro
source venv/bin/activate
pip install llama-cpp-python
```

### GPU Acceleration (CUDA)

For NVIDIA GPUs with CUDA support:

```bash
# Set CUDA flags
export CMAKE_ARGS="-DLLAMA_CUBLAS=on"
export FORCE_CMAKE=1

# Install with GPU support
pip install llama-cpp-python --force-reinstall --no-cache-dir
```

### GPU Acceleration (Apple Silicon)

For M1/M2/M3 Macs:

```bash
# Metal backend is enabled by default on macOS
pip install llama-cpp-python
```

---

## Step 4: Verify Installation

Create a test script:

```bash
cd ~/tender_tracker_pro
cat > test_models.py << 'EOF'
#!/usr/bin/env python3
"""Test Gemma 3n model loading and inference."""

import time
from llama_cpp import Llama

def test_model(model_path, model_name):
    print(f"\n{'='*60}")
    print(f"Testing {model_name}")
    print(f"{'='*60}")

    try:
        # Load model
        print(f"Loading {model_path}...")
        start = time.time()
        llm = Llama(
            model_path=model_path,
            n_ctx=4096,
            n_gpu_layers=-1,  # Use GPU if available
            verbose=False
        )
        load_time = time.time() - start
        print(f"✓ Model loaded in {load_time:.2f}s")

        # Test inference
        prompt = "Extract the tender ID from: 'RFQ Reference: JW123-2025'"
        print(f"\nPrompt: {prompt}")

        start = time.time()
        response = llm(
            prompt,
            max_tokens=50,
            temperature=0.1,
            stop=["\\n"]
        )
        ttft = time.time() - start

        text = response['choices'][0]['text']
        tokens = response['usage']['completion_tokens']
        tokens_per_sec = tokens / ttft if ttft > 0 else 0

        print(f"\nResponse: {text}")
        print(f"\n✓ TTFT: {ttft*1000:.0f}ms")
        print(f"✓ Tokens: {tokens}")
        print(f"✓ Speed: {tokens_per_sec:.1f} tok/s")

        return True

    except Exception as e:
        print(f"✗ Error: {e}")
        return False

if __name__ == "__main__":
    models = [
        ("models/gemma-3n-e2b-q4.gguf", "Gemma 3n E2B (2B)"),
        ("models/gemma-3n-e4b-q4.gguf", "Gemma 3n E4B (4B)")
    ]

    results = []
    for model_path, model_name in models:
        results.append(test_model(model_path, model_name))

    print(f"\n{'='*60}")
    print("Summary")
    print(f"{'='*60}")
    for (path, name), success in zip(models, results):
        status = "✓ PASS" if success else "✗ FAIL"
        print(f"{status}: {name}")
EOF

chmod +x test_models.py
```

Run the test:

```bash
source venv/bin/activate
python test_models.py
```

Expected output:
```
============================================================
Testing Gemma 3n E2B (2B)
============================================================
Loading models/gemma-3n-e2b-q4.gguf...
✓ Model loaded in 2.34s

Prompt: Extract the tender ID from: 'RFQ Reference: JW123-2025'

Response: JW123-2025

✓ TTFT: 450ms
✓ Tokens: 5
✓ Speed: 42.5 tok/s

============================================================
Testing Gemma 3n E4B (4B)
============================================================
Loading models/gemma-3n-e4b-q4.gguf...
✓ Model loaded in 4.12s

Prompt: Extract the tender ID from: 'RFQ Reference: JW123-2025'

Response: JW123-2025

✓ TTFT: 720ms
✓ Tokens: 5
✓ Speed: 28.3 tok/s

============================================================
Summary
============================================================
✓ PASS: Gemma 3n E2B (2B)
✓ PASS: Gemma 3n E4B (4B)
```

---

## Step 5: Update Configuration

The models should now work with the default configuration. Verify settings in `config/app_config.yaml`:

```yaml
llm:
  models:
    e2b:
      path: "models/gemma-3n-e2b-q4.gguf"
      parameters: 2000000000
      context_length: 4096
      quantization: "Q4_K_M"
    e4b:
      path: "models/gemma-3n-e4b-q4.gguf"
      parameters: 4000000000
      context_length: 8192
      quantization: "Q4_K_M"

  engine: "llama.cpp"
  device: "auto"  # auto, cpu, cuda, metal
  gpu_layers: -1  # -1 = all layers on GPU
  threads: 8      # CPU threads
```

---

## Quantization Options

Different quantizations trade off between size, speed, and quality:

| Quantization | Size | Speed | Quality | Recommended For |
|--------------|------|-------|---------|-----------------|
| **Q4_K_M** | ~1.7GB (E2B) | Fast | Good | **Production (default)** |
| Q4_K_S | ~1.5GB | Faster | Fair | Speed-critical |
| Q5_K_M | ~2.1GB | Medium | Better | Quality-critical |
| Q8_0 | ~3.2GB | Slower | Best | Maximum accuracy |

To use a different quantization:
1. Download the desired `.gguf` file
2. Update paths in `config/app_config.yaml`
3. Restart the application

---

## Troubleshooting

### Model Not Found

**Error:** `FileNotFoundError: models/gemma-3n-e2b-q4.gguf`

**Solution:**
```bash
cd ~/tender_tracker_pro
ls -l models/  # Verify files exist
# If missing, re-download (see Step 1)
```

### Out of Memory

**Error:** `RuntimeError: Failed to allocate memory`

**Solutions:**
1. Use smaller quantization (Q4_K_S instead of Q8_0)
2. Reduce context length in config: `n_ctx: 2048`
3. Use CPU-only: `gpu_layers: 0`
4. Close other applications

### Slow Inference

**Issue:** TTFT >2s, <10 tok/s

**Solutions:**

1. **Enable GPU:**
   ```bash
   # Check GPU
   nvidia-smi  # NVIDIA
   # or
   system_profiler SPDisplaysDataType  # macOS

   # Reinstall with GPU support
   export CMAKE_ARGS="-DLLAMA_CUBLAS=on"
   pip install llama-cpp-python --force-reinstall
   ```

2. **Increase GPU layers:**
   ```yaml
   gpu_layers: -1  # All layers (faster)
   ```

3. **Use smaller model for simple tasks:**
   - E2B for extraction, categorization
   - E4B only for scoring, analysis

4. **Reduce context:**
   ```yaml
   context_length: 2048  # From 4096
   ```

### GPU Not Detected

**Issue:** GPU available but not used

**Check:**
```python
from llama_cpp import llama_cpp
print(llama_cpp.llama_supports_gpu())  # Should return True
```

**Solutions:**
- Verify CUDA installation: `nvcc --version`
- Reinstall with CMAKE flags (see Step 3)
- Check `gpu_layers: -1` in config

---

## Hardware Requirements

### Minimum

- **CPU:** 4 cores, 2.0 GHz
- **RAM:** 8 GB
- **Storage:** 10 GB (models + data)
- **GPU:** None (CPU inference works)

### Recommended

- **CPU:** 8 cores, 3.0 GHz
- **RAM:** 16 GB
- **Storage:** 20 GB SSD
- **GPU:** 6 GB VRAM (NVIDIA RTX 3060 or Apple M1)

### Performance Expectations

| Hardware | E2B TTFT | E2B Speed | E4B TTFT | E4B Speed |
|----------|----------|-----------|----------|-----------|
| CPU Only (8 cores) | 800ms | 20 tok/s | 1500ms | 12 tok/s |
| GPU (6GB VRAM) | 400ms | 45 tok/s | 700ms | 28 tok/s |
| GPU (12GB VRAM) | 300ms | 60 tok/s | 500ms | 35 tok/s |

---

## Alternative: MediaPipe

For Google Coral TPU or embedded devices, use MediaPipe instead of llama.cpp:

```bash
pip install mediapipe
```

Update config:
```yaml
llm:
  engine: "mediapipe"  # Instead of "llama.cpp"
```

Models must be in TFLite format. Conversion instructions: https://developers.google.com/mediapipe

---

## Next Steps

1. ✓ Models downloaded and verified
2. → Update `config/company_profile.yaml` with your company details
3. → Run first test: `python -m src.main --source jhb-water --once`

---

## References

- Gemma 3n Documentation: https://ai.google.dev/gemma
- llama.cpp: https://github.com/ggerganov/llama.cpp
- Hugging Face Models: https://huggingface.co/google
- Quantization Guide: https://github.com/ggerganov/llama.cpp/blob/master/docs/quantization.md

---

**Last Updated:** 2026-01-12
