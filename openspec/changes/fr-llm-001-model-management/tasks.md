# FR-LLM-001: Model Management - Implementation Tasks

**Status**: PROPOSED
**Total Tasks**: 35

---

## Phase 1: Project Setup (4 tasks)

- [ ] Install llama-cpp-python with pip
- [ ] Install huggingface-hub library
- [ ] Create models/ directory structure
- [ ] Create config/models/gemma_3n.yaml

## Phase 2: Data Models (3 tasks)

- [ ] Create src/llm/models.py with ModelInfo dataclass
- [ ] Create src/llm/models.py with ModelStatus enum
- [ ] Create src/llm/__init__.py with exports

## Phase 3: Model Storage (4 tasks)

- [ ] Create src/llm/storage.py with ModelStorage class
- [ ] Implement get_model_path() method
- [ ] Implement save_metadata() method
- [ ] Implement load_metadata() method

## Phase 4: Model Downloader (6 tasks)

- [ ] Create src/llm/downloader.py with ModelDownloader class
- [ ] Implement download() method using huggingface_hub
- [ ] Add progress callback with tqdm
- [ ] Add resume support for interrupted downloads
- [ ] Add checksum validation
- [ ] Add atomic file operations (temp → final)

## Phase 5: Model Loader (5 tasks)

- [ ] Create src/llm/loader.py with ModelLoader class
- [ ] Implement load() method using llama-cpp-python
- [ ] Add configuration support (n_ctx, n_threads, etc.)
- [ ] Implement unload() method
- [ ] Add memory usage tracking

## Phase 6: Model Manager (5 tasks)

- [ ] Create src/llm/manager.py with ModelManager class
- [ ] Implement download_model() orchestration
- [ ] Implement is_downloaded() check
- [ ] Implement load_model() with caching
- [ ] Implement get_model_info() method

## Phase 7: Unit Tests (8 tasks)

- [ ] Create tests/test_llm_models.py (ModelInfo tests)
- [ ] Create tests/test_llm_storage.py (4 tests)
- [ ] Create tests/test_llm_downloader.py (mock HF, 5 tests)
- [ ] Create tests/test_llm_loader.py (mock llama.cpp, 3 tests)
- [ ] Create tests/test_llm_manager.py (integration, 6 tests)
- [ ] Run all tests: pytest tests/test_llm*.py
- [ ] Fix any test failures
- [ ] Achieve 90%+ coverage

---

**Total**: 35 tasks
