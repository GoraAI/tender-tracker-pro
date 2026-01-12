# OpenSpec Proposal: FR-NAV-001 Browser Navigation Engine

**Feature ID:** FR-NAV-001
**Status:** 🟡 PROPOSED (Awaiting Approval)
**Priority:** P0 (Critical for MVP1)
**Created:** 2026-01-12
**Dependencies:** None

---

## Summary

Implement a Playwright-based browser navigation engine that mimics human behavior to reliably navigate South African government procurement websites, handle dynamic content, manage cookies, and support pagination—providing the foundation for the navigation-first tender discovery pipeline.

---

## Motivation

### Problem Statement

Government procurement websites (like Johannesburg Water SCM) present several challenges for automated tender discovery:

1. **JavaScript-rendered content** - Tender listings load dynamically
2. **Cookie consent dialogs** - Block automated access
3. **Bot detection** - Static scraping triggers anti-bot measures
4. **Layout changes** - HTML structure changes frequently
5. **Pagination** - Tenders span multiple pages

Traditional HTML scraping is fragile and frequently breaks. We need a robust navigation-first approach.

### Why Playwright?

| Requirement | Playwright | Traditional Scraping |
|-------------|------------|---------------------|
| JavaScript support | ✅ Full | ❌ None |
| Human-like behavior | ✅ Yes | ❌ No |
| Cookie handling | ✅ Automatic | ⚠️ Manual |
| Anti-bot resilience | ✅ High | ❌ Low |
| Cross-browser testing | ✅ 3 engines | ❌ N/A |

---

## Scope

### In Scope

✅ **Core Navigation**
- Initialize Playwright browser (Chromium headless)
- Navigate to configured URLs
- Wait for page load completion (networkidle, domcontentloaded)
- Handle navigation timeouts gracefully

✅ **Human-like Behavior**
- Random delays between actions (1-3 seconds)
- Realistic viewport size (1920x1080)
- Standard user agent strings
- Mouse movement simulation (future)

✅ **Cookie & Popup Handling**
- Automatic cookie consent detection
- Click "Accept" / "Agree" buttons
- Handle multiple selector patterns
- Skip if no consent dialog found

✅ **Pagination Support**
- Detect "Next" buttons
- Click with human-like delay
- Process multiple pages (configurable limit)
- Stop when no more pages

✅ **Error Handling**
- Screenshot capture on failures
- Retry logic (up to 3 attempts)
- Detailed error logging
- Fallback trigger (after 3 consecutive failures)

✅ **Configuration**
- Per-source YAML configuration
- Browser settings (type, headless, viewport)
- Timeout values
- Delay ranges
- Cookie consent selectors

### Out of Scope

❌ **Not in this feature:**
- PDF link discovery (FR-NAV-002)
- PDF downloading (FR-NAV-003)
- HTML scraping fallback (FR-SCRAPE-001)
- Multi-source orchestration (MVP2)
- Proxy support (MVP3)

---

## Technical Approach

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                  BrowserEngine                               │
│  (src/navigator/browser_engine.py)                          │
└─────────────────────────────────────────────────────────────┘
                        │
                        ├─ initialize()      → Launch browser
                        ├─ navigate(url)     → Navigate to page
                        ├─ handle_cookies()  → Accept dialogs
                        ├─ paginate()        → Handle "Next"
                        ├─ screenshot()      → Capture on error
                        └─ close()           → Cleanup
```

### Class Design

```python
class BrowserEngine:
    """Playwright-based browser navigation engine."""

    def __init__(self, config: dict):
        """Initialize with configuration from app_config.yaml."""

    async def initialize(self):
        """Launch Playwright browser instance."""

    async def navigate(self, url: str, wait_for: str = "networkidle") -> Page:
        """Navigate to URL and return page object."""

    async def handle_cookie_consent(self, page: Page) -> bool:
        """Detect and accept cookie consent dialogs."""

    async def paginate(self, page: Page, max_pages: int = 5) -> list[Page]:
        """Handle pagination by clicking Next buttons."""

    async def screenshot(self, page: Page, name: str):
        """Capture screenshot to logs/screenshots/."""

    async def close(self):
        """Close browser and cleanup resources."""
```

### Configuration Example

```yaml
# config/sources/jhb_water.yaml
navigation:
  browser:
    type: "chromium"
    headless: true
    viewport: {width: 1920, height: 1080}

  steps:
    - action: "goto"
      url: "${urls.rfq_listing}"
      wait_for: "networkidle"
      timeout_ms: 30000

  delays:
    min_ms: 1000
    max_ms: 3000

  cookie_consent:
    enabled: true
    selectors:
      - "button:has-text('Accept')"
      - ".cookie-accept"

  pagination:
    enabled: true
    next_button_selectors:
      - "a:has-text('Next')"
    max_pages: 5
```

---

## Acceptance Criteria

### Functional Requirements

- [ ] **AC1:** System launches Chromium browser in headless mode
- [ ] **AC2:** System navigates to configured URL within 30 seconds
- [ ] **AC3:** System waits for page load (networkidle) before proceeding
- [ ] **AC4:** System detects and clicks cookie consent buttons
- [ ] **AC5:** System implements random delays (1-3s) between actions
- [ ] **AC6:** System handles pagination by clicking "Next" button
- [ ] **AC7:** System stops pagination at configured max_pages limit
- [ ] **AC8:** System captures screenshot on navigation failure
- [ ] **AC9:** System retries navigation up to 3 times on failure
- [ ] **AC10:** System logs all navigation steps with timestamps
- [ ] **AC11:** System closes browser cleanly after navigation

### Non-Functional Requirements

- [ ] **AC12:** Page load completes within 30 seconds (99th percentile)
- [ ] **AC13:** Navigation success rate >95% over 7 days
- [ ] **AC14:** System uses <500MB memory during navigation
- [ ] **AC15:** Cookie consent detection works for 3 selector patterns
- [ ] **AC16:** Error logs include URL, timestamp, and error message

### Test Coverage

- [ ] **AC17:** Unit tests for BrowserEngine class (>80% coverage)
- [ ] **AC18:** Integration test navigates to JHB Water RFQ page
- [ ] **AC19:** Test handles cookie consent dialog
- [ ] **AC20:** Test handles pagination (3 pages)
- [ ] **AC21:** Test captures screenshot on navigation failure
- [ ] **AC22:** Test retries on network error

---

## Test Plan

### Unit Tests

```python
# tests/test_browser_engine.py

def test_browser_initialization():
    """Test browser launches successfully."""

async def test_navigate_to_url():
    """Test navigation to valid URL."""

async def test_navigation_timeout():
    """Test timeout handling for slow pages."""

async def test_cookie_consent_detection():
    """Test cookie dialog detection and clicking."""

async def test_pagination():
    """Test clicking Next button and loading next page."""

async def test_screenshot_on_error():
    """Test screenshot capture when navigation fails."""

def test_human_like_delays():
    """Test delays are within configured range."""
```

### Integration Tests

```python
# tests/integration/test_jhb_water_navigation.py

async def test_navigate_jhb_water_rfq_page():
    """Test full navigation to JHB Water RFQ listing."""
    engine = BrowserEngine(config)
    page = await engine.navigate(JHB_WATER_RFQ_URL)
    assert page.url == JHB_WATER_RFQ_URL
    assert "Request for Quotations" in await page.content()

async def test_handle_jhb_water_cookies():
    """Test cookie consent handling on JHB Water site."""

async def test_jhb_water_pagination():
    """Test paginating through JHB Water tender listings."""
```

### Manual Verification

1. **Visual Check:** Run in headed mode, observe human-like behavior
2. **Performance Check:** Measure page load times over 10 runs
3. **Cookie Check:** Verify cookie dialog is accepted automatically
4. **Pagination Check:** Verify all pages are loaded
5. **Error Check:** Verify screenshots are saved on failure

---

## Implementation Tasks

See `tasks.md` for detailed task breakdown.

---

## Performance Targets

| Metric | Target | Alert | Don't Ship |
|--------|--------|-------|------------|
| Page load time (p99) | <30s | >45s | >60s |
| Navigation success rate | >95% | <90% | <85% |
| Memory usage | <500MB | >750MB | >1GB |
| Screenshot capture | <2s | >5s | >10s |

---

## Security & Privacy

- ✅ No credentials stored in code (environment variables only)
- ✅ User agent uses standard Chrome UA (not identifying)
- ✅ All navigation logged (no sensitive data)
- ✅ Screenshots saved locally (not uploaded)

---

## Rollback Plan

If navigation engine has critical bugs:

1. **Trigger:** Navigation success rate <80% over 24 hours
2. **Action:** Disable BrowserEngine, activate HTML scraping fallback
3. **Notification:** Alert admin via configured channel
4. **Recovery:** Fix bugs, test on staging, re-enable

---

## Dependencies

**Before Implementation:**
- [x] Playwright installed (`pip install playwright`)
- [x] Chromium browser installed (`playwright install chromium`)
- [x] Configuration files created (`config/sources/jhb_water.yaml`)

**After Implementation:**
- FR-NAV-002 (PDF Link Discovery) will use BrowserEngine
- FR-NAV-003 (PDF Download) will use BrowserEngine

---

## Documentation

- [ ] Add BrowserEngine to API.md
- [ ] Update ARCHITECTURE.md with navigation flow diagram
- [ ] Create navigation tutorial in docs/NAVIGATION.md
- [ ] Add troubleshooting guide for common navigation issues

---

## Success Metrics

**Definition of Done:**

- ✅ All 22 acceptance criteria met
- ✅ Unit test coverage >80%
- ✅ Integration tests pass
- ✅ Manual verification complete
- ✅ Code reviewed by 1 person
- ✅ Documentation updated
- ✅ Performance targets met

**Post-Launch Metrics:**

- Navigation success rate >95% (week 1)
- Zero bot detection incidents (week 1)
- Page load p99 <30s (week 1)

---

## Open Questions

None at this time. Approach is clear from PRD.

---

## Sign-Off

**Approval Status:**
```
Status: ✅ APPROVED
Date: 2026-01-12
Approver: Product Owner
```

**Notes:** All acceptance criteria reviewed and approved. Proceeding with implementation.

---

**Proposal Status:** ✅ APPROVED (Ready for Implementation)
