# Tender Tracker Pro - Johannesburg Water Edition

**Version:** 1.0.0 (MVP1)
**Architecture:** Navigation-First with PDF as Source of Truth
**AI Engine:** Gemma 3n (E2B + E4B) - Local Inference

---

## Overview

Tender Tracker Pro is an intelligent, privacy-first tender discovery and analysis platform that automatically navigates South African government procurement websites, downloads tender PDFs, and uses on-device AI to extract, score, and prioritize opportunities.

### Key Features (MVP1)

- **Navigation-First Architecture**: Playwright-based browser automation mimics human behavior
- **PDF as Source of Truth**: All data extracted from PDF documents, not HTML
- **Local AI Processing**: Gemma 3n models (E2B 2B, E4B 4B) for zero-cloud dependency
- **Smart Scoring**: Relevance scoring 0-100 based on company profile
- **Automated Notifications**: Email alerts for high-priority tenders (score ≥70)

---

## Architecture

```
Navigate Website → Find PDF Links → Download PDFs → Extract Text →
LLM Analysis (Gemma E4B) → Relevance Scoring → CSV Reports + Email Notifications
```

**Fallback**: HTML scraping with BeautifulSoup when navigation fails

---

## Directory Structure

```
tender_tracker_pro/
├── config/               # Configuration files (YAML)
│   ├── sources/          # Per-source navigation configs
├── data/                 # Processed data, reports, tracking
├── documents/downloads/  # Downloaded PDFs by source/date
├── models/               # Gemma 3n GGUF models
├── logs/                 # Structured logs + error screenshots
├── src/                  # Python source code
│   ├── navigator/        # Playwright-based navigation
│   ├── fallback/         # HTML scraping fallback
│   ├── extractors/       # PDF text extraction (pdfplumber + Tesseract)
│   ├── llm/              # Gemma 3n model management & agents
│   ├── reporting/        # CSV + dashboard generation
│   └── notifications/    # Email + Discord
├── dashboard/            # HTML/JS visualization
├── tests/                # Unit + integration tests
└── evals/                # AI evaluation datasets
```

---

## Quick Start

### Prerequisites

- Python 3.10+
- 8GB RAM minimum (16GB recommended)
- 20GB disk space
- Ubuntu 22.04+ / Windows 10+ / macOS 12+

### Installation

```bash
# Clone repository
cd ~/tender_tracker_pro

# Create virtual environment
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Install Playwright browsers
playwright install chromium

# Download Gemma models (instructions in docs/MODEL_SETUP.md)
```

### Configuration

1. Copy example configs:
```bash
cp config/app_config.example.yaml config/app_config.yaml
cp config/company_profile.example.yaml config/company_profile.yaml
cp config/email_config.example.yaml config/email_config.yaml
```

2. Edit `config/company_profile.yaml` with your company details
3. Edit `config/email_config.yaml` with SMTP settings
4. Edit `config/sources/jhb_water.yaml` if needed (defaults should work)

### Running

```bash
# Run single scrape cycle
python -m src.main --source jhb-water --once

# Run scheduled (every 4 hours)
python -m src.main --source jhb-water --schedule

# View dashboard
python -m http.server 8080 --directory dashboard
# Open http://localhost:8080
```

---

## MVP1 Target: Johannesburg Water

**Source:** scm.johannesburgwater.co.za
**Target URL:** https://scm.johannesburgwater.co.za/supply-chain/request-for-quotations/open-rfq-2/
**PDF Pattern:** `/wp-content/uploads/{YYYY}/{MM}/*.pdf`

---

## Performance Targets

| Metric | Target | Alert | Don't Ship |
|--------|--------|-------|------------|
| Scrape cycle | <10 min | >20 min | >30 min |
| PDF download | >500 KB/s | <200 KB/s | <100 KB/s |
| Text extraction | <5s/doc | >10s | >30s |
| LLM TTFT (E2B) | <500ms | >800ms | >1500ms |
| LLM TTFT (E4B) | <800ms | >1200ms | >2000ms |
| Extraction accuracy | >90% | <85% | <75% |
| Scoring accuracy | >85% | <80% | <70% |

---

## Development Status

**Current Phase:** Project Setup
**Next:** Architecture Documentation → OpenSpec P0 Proposals

### P0 Features (Critical for MVP1)

- [ ] FR-NAV-001: Browser Navigation Engine
- [ ] FR-NAV-002: PDF Link Discovery
- [ ] FR-NAV-003: Direct PDF Download
- [ ] FR-EXTRACT-001: PDF Text Extraction
- [ ] FR-LLM-001: Gemma 3n Model Management
- [ ] FR-LLM-002: PDF-Based Tender Information Extraction
- [ ] FR-LLM-003: Relevance Scoring

### P1 Features (High Priority)

- [ ] FR-SCRAPE-001: Fallback HTML Scraping
- [ ] FR-REPORT-001: CSV Report Generation
- [ ] FR-NOTIFY-001: Email Notifications

---

## Documentation

- `docs/PRD.md` - Full Product Requirements Document
- `docs/ARCHITECTURE.md` - System architecture and design decisions
- `docs/MODEL_SETUP.md` - Gemma 3n model download and configuration
- `docs/API.md` - Internal API documentation
- `docs/EVALUATION.md` - AI evaluation framework

---

## Testing

```bash
# Run all tests
pytest tests/

# Run with coverage
pytest --cov=src tests/

# Run specific test module
pytest tests/test_navigator.py

# Run evaluations
python evals/run_evals.py
```

---

## Support

For issues, questions, or contributions:
- GitHub Issues: [Create Issue]
- Documentation: `docs/`
- PRD: `docs/PRD.md`

---

## License

Proprietary - Internal Use Only

---

**Built with:** Python 3.10+ | Playwright | pdfplumber | Gemma 3n | llama.cpp
