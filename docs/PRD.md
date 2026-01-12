# MASTER PRD: Tender Tracker Pro - Johannesburg Water Edition

**Version:** 1.1.0  
**Last Updated:** January 12, 2026  
**Methodology:** GORA (Goal-Oriented Requirements Architecture)  
**Author:** GORA Product Manager  
**Target Platform:** Desktop/Server (Python-based)  

---

This is the complete Product Requirements Document for Tender Tracker Pro MVP1.
For implementation, this PRD has been analyzed and the system architecture has been
designed in ARCHITECTURE.md.

All P0 features from this PRD must be implemented before MVP1 can ship.

Refer to PROJECT_STATUS.md for current implementation status and next steps.

---

## Navigation-First Architecture

This PRD emphasizes a Navigation-First approach where:
1. Playwright navigates websites like a human user
2. PDF links are discovered on listing pages
3. PDFs are downloaded directly via browser
4. ALL tender information is extracted from PDF content (not HTML)
5. Gemma 3n models analyze and score tenders
6. HTML scraping is a fallback only when navigation fails

---

For the complete PRD content, see the original document provided.
This file serves as a reference point in the project structure.

Key sections:
- Part I: Product Foundation (Vision, Strategy, Success Metrics)
- Part II: Functional Requirements (FR-NAV, FR-EXTRACT, FR-LLM, FR-REPORT, FR-NOTIFY)
- Part III: Non-Functional Requirements (Performance, Security, Privacy)
- Part IV: AI Evaluation Framework
- Part V: Operations (Monitoring, Rollback, Quality Review)
- Part VI: Roadmap (MVP1, MVP2, MVP3 phasing)
- Appendices: Architecture diagrams, directory structure, configurations

---

P0 Features (Critical for MVP1):
- FR-NAV-001: Browser Navigation Engine
- FR-NAV-002: PDF Link Discovery
- FR-NAV-003: Direct PDF Download
- FR-EXTRACT-001: PDF Text Extraction
- FR-LLM-001: Gemma 3n Model Management
- FR-LLM-002: PDF-Based Tender Information Extraction
- FR-LLM-003: Relevance Scoring

Target Website: https://scm.johannesburgwater.co.za/supply-chain/request-for-quotations/open-rfq-2/
