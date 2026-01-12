# Tender Tracker Pro - Architecture Documentation

**Version:** 1.0.0
**Last Updated:** 2026-01-12
**Status:** Initial Design

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Core Philosophy](#core-philosophy)
3. [System Components](#system-components)
4. [Data Flow](#data-flow)
5. [Technology Stack](#technology-stack)
6. [Design Decisions](#design-decisions)
7. [Performance & Scalability](#performance--scalability)

---

## Architecture Overview

Tender Tracker Pro follows a **Navigation-First Architecture** with PDF documents as the source of truth. The system automates tender discovery, analysis, and notification using local AI models.

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    TENDER TRACKER PRO                           │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ NAVIGATION   │    │ EXTRACTION   │    │   OUTPUT     │
│   LAYER      │───▶│    LAYER     │───▶│   LAYER      │
└──────────────┘    └──────────────┘    └──────────────┘
```

---

## Core Philosophy

### 1. Navigation > Scraping

**Primary Method:** Browser automation with Playwright
- Mimics human behavior
- Resilient to layout changes
- Handles JavaScript-rendered content
- Avoids bot detection

**Fallback Method:** HTML scraping with BeautifulSoup
- Activated after 3 consecutive navigation failures
- Returns to navigation after 24 hours
- Lightweight and fast

### 2. PDF as Source of Truth

**Why PDFs?**
- Official authoritative documents
- Complete information (vs partial HTML)
- Standardized format
- Human-verifiable

**Extraction Pipeline:**
```
PDF → pdfplumber (text extraction) → Tesseract (OCR fallback) →
Gemma 3n (structured extraction) → JSON storage
```

### 3. Privacy-First AI

**Local Inference Only:**
- Gemma 3n models (E2B 2B, E4B 4B)
- llama.cpp for efficient inference
- Zero external API calls
- No data leaves local system

**Dynamic Model Routing:**
- Simple tasks (<2000 tokens) → E2B model
- Complex tasks (>2000 tokens) → E4B model
- Automatic selection based on context length

---

## System Components

### 1. Navigation Layer

**Purpose:** Discover and download tender PDFs from websites

**Components:**
- `browser_engine.py`: Playwright wrapper with human-like behavior
- `pdf_discovery.py`: Pattern-based PDF link finder
- `pdf_downloader.py`: Download manager with retry logic
- `navigation_flows/`: Site-specific navigation configurations

**Key Features:**
- Human-like delays (1-3s randomized)
- Cookie consent handling
- Pagination support
- Screenshot capture on errors
- Deduplication tracking

### 2. Extraction Layer

**Purpose:** Extract text and structure from PDFs

**Components:**
- `pdf_extractor.py`: pdfplumber-based text extraction
- `ocr_fallback.py`: Tesseract OCR for scanned documents

**Process:**
1. Extract text with pdfplumber
2. If empty → trigger OCR with Tesseract
3. Extract tables as structured JSON
4. Limit output to 50,000 characters
5. Save to `data/extracted/{tender_id}.json`

### 3. LLM Analysis Layer

**Purpose:** Extract structured information and score relevance

**Components:**
- `model_manager.py`: Load and manage Gemma 3n models
- `router.py`: Dynamic E2B/E4B routing
- `extraction_agent.py`: PDF text → structured JSON
- `scoring_agent.py`: Tender relevance scoring

**Extraction Fields:**
- tender_id, title, organization, description
- closing_date (ISO 8601 with timezone)
- contact information
- budget_range, industry_category, location
- requirements_list, mandatory_documents
- evaluation_criteria

**Scoring Dimensions:**
- Industry match (25 points)
- Location match (20 points)
- Size/value fit (20 points)
- Capability match (20 points)
- Deadline feasibility (15 points)

**Total:** 0-100 score with priority classification

### 4. Output Layer

**Purpose:** Generate reports and notifications

**Components:**
- `csv_generator.py`: Daily CSV reports
- `dashboard.py`: HTML visualization
- `email_sender.py`: SMTP-based notifications
- `discord_webhook.py`: Discord integration (P2)

**Output Formats:**
- CSV: `tenders_YYYY-MM-DD.csv` (UTF-8 with BOM)
- JSON: `data/processed/{source}/{tender_id}.json`
- Dashboard: `dashboard/index.html` (auto-refresh)

---

## Data Flow

### End-to-End Pipeline

```
1. NAVIGATE
   └─ Playwright opens listing page
   └─ Wait for page load (networkidle)
   └─ Human-like delays

2. DISCOVER
   └─ Scan DOM for PDF links
   └─ Match patterns: /wp-content/uploads/*.pdf
   └─ Extract link URLs and context
   └─ Deduplicate against tracking file

3. DOWNLOAD
   └─ Click PDF link in browser
   └─ Download to documents/downloads/{source}/{YYYY-MM}/
   └─ Verify PDF integrity (%PDF- header)
   └─ Retry up to 3 times on failure

4. EXTRACT TEXT
   └─ pdfplumber extracts text
   └─ If empty → Tesseract OCR
   └─ Extract tables as JSON
   └─ Save to data/extracted/

5. LLM ANALYSIS
   └─ Route to E2B or E4B based on length
   └─ Extract structured fields
   └─ Validate required fields
   └─ Flag low-confidence extractions

6. SCORE RELEVANCE
   └─ Load company profile
   └─ Calculate dimension scores
   └─ Sum to 0-100 total
   └─ Generate rationale

7. OUTPUT
   └─ Save JSON to data/processed/
   └─ Add row to CSV report
   └─ Update dashboard data

8. NOTIFY
   └─ If score ≥70 → queue email
   └─ Batch emails (max 1/hour)
   └─ Send via SMTP
   └─ Log delivery status
```

---

## Technology Stack

| Layer | Technology | Justification |
|-------|------------|---------------|
| **Browser Automation** | Playwright (Python) | Human-like navigation, JS support, multi-browser |
| **PDF Extraction** | pdfplumber + Tesseract | Native text + OCR fallback |
| **LLM Inference** | llama.cpp / llama-cpp-python | Efficient local Gemma 3n inference |
| **Fallback Scraping** | requests + BeautifulSoup4 | Lightweight HTML parsing |
| **Data Storage** | JSON + SQLite (optional) | Transparency + queryability |
| **Configuration** | YAML + Pydantic | Human-readable + validation |
| **Scheduling** | APScheduler | Python-native cron-like scheduling |
| **Dashboard** | HTML + Chart.js | No server required, simple deployment |
| **Email** | smtplib | Python standard library |
| **Logging** | structlog | Structured JSON logging |
| **Testing** | pytest + pytest-cov | Industry standard |

---

## Design Decisions

### Decision 1: Navigation-First vs Scraping-First

**Choice:** Navigation-First with Playwright

**Rationale:**
- Survives website layout changes
- Handles JavaScript-rendered content
- Mimics human behavior (anti-bot)
- Simpler pipeline (PDF is source of truth)
- Fallback to scraping available

**Trade-offs:**
- Higher resource usage (browser)
- Slower than pure HTTP requests
- Requires browser installation

### Decision 2: PDF as Source of Truth

**Choice:** Extract ALL data from PDFs, not HTML

**Rationale:**
- PDF is authoritative document
- Complete information vs partial HTML
- Standardized format
- Human-verifiable
- Resilient to website changes

**Trade-offs:**
- Requires PDF download (bandwidth)
- OCR needed for scanned docs
- Extraction complexity

### Decision 3: Local AI vs Cloud API

**Choice:** Local Gemma 3n models via llama.cpp

**Rationale:**
- Privacy-first (no data leaves system)
- Zero API costs
- Offline operation
- Full control over inference
- Predictable performance

**Trade-offs:**
- Requires capable hardware (8GB RAM min)
- Model management complexity
- Limited context length vs GPT-4

### Decision 4: Dynamic Model Routing

**Choice:** E2B for simple tasks, E4B for complex tasks

**Rationale:**
- Optimize speed/quality trade-off
- E2B: 500ms TTFT, 40 tok/s
- E4B: 800ms TTFT, 25 tok/s
- Automatic selection based on context length

**Routing Logic:**
```python
if context_tokens < 2000:
    use_model("gemma-3n-e2b")  # Fast
else:
    use_model("gemma-3n-e4b")  # Accurate
```

### Decision 5: JSON + SQLite Hybrid Storage

**Choice:** Primary JSON files, optional SQLite

**Rationale:**
- JSON: Transparent, human-readable, versionable
- SQLite: Queryable, indexed, relational
- Hybrid: Best of both worlds

**File Structure:**
```
data/processed/{source}/{tender_id}.json  # Primary storage
data/tenders.db                            # Optional index
```

---

## Performance & Scalability

### Performance Targets

| Metric | MVP1 Target | Production Target |
|--------|-------------|-------------------|
| Scrape cycle | <10 min | <5 min |
| PDF download | >500 KB/s | >1 MB/s |
| Text extraction | <5s/doc | <3s/doc |
| LLM TTFT (E2B) | <500ms | <300ms |
| LLM TTFT (E4B) | <800ms | <500ms |
| End-to-end latency | <5 min | <3 min |

### Scalability Considerations

**MVP1 (Single Source):**
- ~10-20 tenders per scrape
- 1 scrape every 4 hours = 6 scrapes/day
- ~60-120 tenders/day
- Storage: ~50MB/month

**MVP2 (5 Sources):**
- ~50-100 tenders per scrape
- 5 sources × 6 scrapes/day = 30 scrapes/day
- ~300-600 tenders/day
- Storage: ~250MB/month

**MVP3 (17 Sources):**
- ~200-400 tenders per scrape
- 17 sources × 6 scrapes/day = 102 scrapes/day
- ~1000-2000 tenders/day
- Storage: ~1GB/month

**Bottlenecks:**
1. **LLM Inference:** Sequential processing limits throughput
   - *Mitigation:* Request queue (max 3 concurrent)
   - *Future:* Batch inference, GPU optimization

2. **PDF Download:** Network bandwidth
   - *Mitigation:* Concurrent downloads (max 3)
   - *Future:* Adaptive concurrency, CDN caching

3. **Storage:** JSON file proliferation
   - *Mitigation:* 90-day retention, compression
   - *Future:* Database migration, archival

### Hardware Requirements

| Component | Minimum | Recommended | Production |
|-----------|---------|-------------|------------|
| **CPU** | 4 cores | 8 cores | 16 cores |
| **RAM** | 8 GB | 16 GB | 32 GB |
| **Storage** | 20 GB | 50 GB | 200 GB SSD |
| **GPU** | None | 6GB VRAM | 12GB VRAM |
| **Network** | 5 Mbps | 20 Mbps | 100 Mbps |

---

## Security & Privacy

### Data Protection

1. **No Cloud Processing**
   - All AI inference local
   - No external API calls
   - Data never leaves system

2. **Credential Storage**
   - Environment variables
   - Encrypted config files (future)
   - Never in code or logs

3. **Log Sanitization**
   - Strip credentials from logs
   - No sensitive data in screenshots
   - Structured logging with filters

4. **Secure Downloads**
   - HTTPS verification
   - PDF integrity checks
   - Malware scanning (future)

---

## Monitoring & Observability

### Logging

**Structured JSON Logging:**
```json
{
  "timestamp": "2026-01-12T12:00:00+02:00",
  "level": "INFO",
  "component": "navigator",
  "action": "pdf_download",
  "source": "jhb-water",
  "tender_id": "RFQJW087TN25",
  "status": "SUCCESS",
  "duration_ms": 1234,
  "file_size_kb": 245
}
```

**Log Files:**
- `logs/navigator.log` - Navigation and downloads
- `logs/extraction.log` - PDF text extraction
- `logs/llm.log` - LLM inference metrics
- `logs/notification.log` - Email/Discord delivery

### Metrics (Future: Prometheus)

- Scrape success rate
- PDF download success rate
- Extraction accuracy
- LLM TTFT p50/p95/p99
- Tender discovery rate
- Notification delivery rate

### Alerts

- 2 consecutive scrape failures → Alert
- Zero tenders found (unexpected) → Alert
- LLM TTFT >2s → Alert
- Disk usage >90% → Alert
- Fallback scraping activated → Alert

---

## Future Enhancements

### Short Term (MVP2)
- Multi-source support (5 sources)
- Cross-source deduplication
- Discord notifications
- Dashboard improvements

### Medium Term (MVP3)
- 17+ sources
- Google Search integration
- AI-assisted response generation
- Database migration

### Long Term
- Real-time tender monitoring
- Mobile app
- Multi-user support
- Tender response templates
- CRM integration

---

## References

- PRD: `docs/PRD.md` (full requirements)
- API Documentation: `docs/API.md`
- Model Setup: `docs/MODEL_SETUP.md`
- Evaluation Framework: `docs/EVALUATION.md`

---

**Document Version:** 1.0.0
**Last Updated:** 2026-01-12
**Author:** Architecture Team
