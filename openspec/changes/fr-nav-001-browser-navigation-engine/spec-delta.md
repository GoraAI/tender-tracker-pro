# FR-NAV-001 Specification Delta

**Feature:** Browser Navigation Engine
**Type:** NEW FEATURE
**Impact:** ARCHITECTURE FOUNDATION

---

## Summary of Changes

This feature introduces the core browser navigation engine that will be used by all future navigation-dependent features. It establishes the navigation-first architecture pattern for the project.

---

## ADDED Specifications

### 1. Browser Engine API

**New Module:** `src/navigator/browser_engine.py`

```python
class BrowserEngine:
    """
    Playwright-based browser navigation engine.

    Provides human-like website navigation with automatic cookie handling,
    pagination support, and error recovery.
    """

    def __init__(self, config: dict):
        """
        Initialize browser engine with configuration.

        Args:
            config: Browser configuration from app_config.yaml
                Required keys:
                - browser.type: str ("chromium", "firefox", "webkit")
                - browser.headless: bool
                - browser.viewport: dict {width, height}
                - browser.user_agent: str
                - timeouts.page_load: int (seconds)
                - timeouts.navigation: int (seconds)
                - delays.min_ms: int
                - delays.max_ms: int
        """

    async def initialize(self) -> None:
        """
        Launch Playwright browser instance.

        Raises:
            BrowserInitError: If browser fails to launch
        """

    async def navigate(
        self,
        url: str,
        wait_for: str = "networkidle",
        timeout_seconds: int = 30
    ) -> Page:
        """
        Navigate to URL and return page object.

        Args:
            url: Target URL (must be valid http/https)
            wait_for: Load state to wait for
                - "load": Page load event
                - "domcontentloaded": DOM ready
                - "networkidle": No network activity for 500ms
            timeout_seconds: Maximum wait time

        Returns:
            Playwright Page object ready for interaction

        Raises:
            NavigationError: If navigation fails after retries
            NavigationTimeout: If page load exceeds timeout
        """

    async def handle_cookie_consent(self, page: Page) -> bool:
        """
        Detect and accept cookie consent dialogs.

        Tries multiple selector patterns:
        1. button:has-text('Accept')
        2. button:has-text('Agree')
        3. .cookie-accept
        4. #cookie-accept

        Args:
            page: Playwright Page object

        Returns:
            True if consent dialog was handled, False if not found
        """

    async def paginate(
        self,
        page: Page,
        next_button_selectors: list[str],
        max_pages: int = 5
    ) -> list[Page]:
        """
        Handle pagination by clicking Next buttons.

        Args:
            page: Starting page with pagination
            next_button_selectors: List of selectors to try for Next button
            max_pages: Maximum pages to load (prevents infinite loops)

        Returns:
            List of Page objects (one per page loaded, including initial)
        """

    async def screenshot(
        self,
        page: Page,
        name: str,
        path: str = "logs/screenshots"
    ) -> str:
        """
        Capture screenshot of current page.

        Args:
            page: Page to screenshot
            name: Base filename (without extension)
            path: Directory to save screenshot

        Returns:
            Full path to saved screenshot

        Raises:
            ScreenshotError: If capture fails
        """

    async def close(self) -> None:
        """Close browser and cleanup resources."""
```

### 2. Navigation Configuration Schema

**Location:** `config/sources/{source_id}.yaml`

**New Section:** `navigation`

```yaml
navigation:
  # Method: browser_navigation (this feature) or html_scraping (fallback)
  method: "browser_navigation"

  # Browser settings
  browser:
    type: "chromium"  # chromium, firefox, webkit
    headless: true
    viewport:
      width: 1920
      height: 1080
    user_agent: "Mozilla/5.0 (...)"

  # Navigation steps (sequence of actions)
  steps:
    - action: "goto"
      url: "${urls.rfq_listing}"
      wait_for: "networkidle"
      timeout_ms: 30000

    - action: "wait_for_selector"
      selector: "a[href*='.pdf']"
      timeout_ms: 10000
      optional: true

  # Human-like behavior
  delays:
    min_ms: 1000
    max_ms: 3000
    between_pages_ms: 2000

  # Cookie consent handling
  cookie_consent:
    enabled: true
    selectors:
      - "button:has-text('Accept')"
      - "button:has-text('Agree')"
      - ".cookie-accept"
      - "#cookie-accept"

  # Pagination
  pagination:
    enabled: true
    next_button_selectors:
      - "a:has-text('Next')"
      - "a.next"
      - ".pagination .next"
    max_pages: 5
```

### 3. Error Types

**New Exceptions:** `src/navigator/exceptions.py`

```python
class NavigationError(TenderTrackerError):
    """Base error for navigation failures."""

class BrowserInitError(NavigationError):
    """Browser failed to initialize."""

class NavigationTimeout(NavigationError):
    """Page load exceeded timeout."""

class ScreenshotError(NavigationError):
    """Screenshot capture failed."""
```

### 4. Logging Events

**New Structured Log Events:**

```python
# Navigation started
logger.info(
    "navigation_started",
    source="jhb-water",
    url="https://...",
    wait_for="networkidle"
)

# Navigation succeeded
logger.info(
    "navigation_succeeded",
    source="jhb-water",
    url="https://...",
    load_time_ms=2340,
    final_url="https://..."  # After redirects
)

# Cookie consent handled
logger.info(
    "cookie_consent_handled",
    source="jhb-water",
    selector="button:has-text('Accept')",
    found=True
)

# Pagination
logger.info(
    "pagination_page_loaded",
    source="jhb-water",
    page_number=2,
    total_pages=3,
    next_button_found=True
)

# Navigation failed
logger.error(
    "navigation_failed",
    source="jhb-water",
    url="https://...",
    error="TimeoutError: Page load timeout",
    attempt=1,
    max_attempts=3,
    screenshot_path="logs/screenshots/jhb-water_2026-01-12_12-34-56.png"
)
```

### 5. Performance Metrics

**New Metrics to Track:**

| Metric | Description | Target | Alert |
|--------|-------------|--------|-------|
| `navigation.page_load_time_ms` | Time to load page | p99 <30000 | >45000 |
| `navigation.success_rate` | % successful navigations | >95% | <90% |
| `navigation.memory_usage_mb` | Browser memory usage | <500 | >750 |
| `navigation.screenshot_time_ms` | Screenshot capture time | <2000 | >5000 |
| `navigation.cookie_consent_found` | % pages with cookie dialogs | 0-100% | N/A |
| `navigation.pagination_pages` | Avg pages per scrape | 1-5 | N/A |

---

## MODIFIED Specifications

### 1. Application Configuration

**File:** `config/app_config.yaml`

**Modified Section:** `navigation` (expanded)

**Before:**
```yaml
# (Did not exist)
```

**After:**
```yaml
navigation:
  default_method: "playwright"
  browser:
    type: "chromium"
    headless: true
    viewport: {width: 1920, height: 1080}
  timeouts:
    page_load: 30
    navigation: 60
  delays:
    min_seconds: 1.0
    max_seconds: 3.0
  screenshot_on_error: true
  max_retries: 3
```

### 2. Requirements

**File:** `requirements.txt`

**Added Dependencies:**
```
playwright==1.41.0
```

---

## REMOVED Specifications

None. This is a new feature with no removed functionality.

---

## Dependency Changes

### New Dependencies

1. **Playwright** (already installed)
   - Version: 1.41.0
   - Purpose: Browser automation
   - License: Apache 2.0

### Modified Dependencies

None.

---

## Migration Guide

### For Developers

No migration needed. This is a new feature.

**To use BrowserEngine in new code:**

```python
from src.navigator.browser_engine import BrowserEngine

# Initialize
engine = BrowserEngine(config["navigation"])
await engine.initialize()

# Navigate
page = await engine.navigate("https://example.com")

# Handle cookies
await engine.handle_cookie_consent(page)

# Paginate
pages = await engine.paginate(page, next_selectors, max_pages=5)

# Screenshot on error
try:
    # ... navigation ...
except NavigationError:
    await engine.screenshot(page, "error")
    raise

# Cleanup
await engine.close()
```

### For Configuration

Add `navigation` section to any new source YAML files (see above schema).

---

## Testing Impact

### New Test Files

- `tests/test_browser_engine.py` - Unit tests (10 tests)
- `tests/integration/test_jhb_water_navigation.py` - Integration tests (5 tests)

### Test Coverage Target

- BrowserEngine class: >80%
- Overall navigation module: >75%

---

## Documentation Impact

### New Documentation

- `docs/NAVIGATION.md` - Navigation tutorial and patterns
- `docs/API.md` - BrowserEngine API section (added)

### Modified Documentation

- `README.md` - Add navigation feature to features list
- `docs/ARCHITECTURE.md` - Add navigation layer diagram
- `PROJECT_STATUS.md` - Mark FR-NAV-001 as complete

---

## Rollout Plan

### Phase 1: Development & Testing

1. Implement BrowserEngine class
2. Write unit tests (all passing)
3. Write integration tests (JHB Water)
4. Manual verification

### Phase 2: Integration

1. Verify JHB Water navigation works
2. Run 7-day reliability test
3. Monitor metrics

### Phase 3: Documentation

1. Write NAVIGATION.md
2. Update API.md
3. Add examples

### Phase 4: Handoff

1. Feature complete
2. Ready for FR-NAV-002 (PDF Discovery) to use

---

## Risk Assessment

### Low Risk

- Playwright is mature and well-documented
- Human-like behavior reduces bot detection
- Screenshot debugging aids troubleshooting

### Medium Risk

- Website changes could break navigation (mitigated: fallback scraper in FR-SCRAPE-001)
- Memory usage with multiple browsers (mitigated: close after use)

### High Risk

None identified.

---

## Backwards Compatibility

**Impact:** None. This is a new feature.

**Breaking Changes:** None.

---

## Future Extensions

These are explicitly out of scope but planned for future:

1. **MVP2:** Multi-browser support (Firefox, WebKit)
2. **MVP2:** Proxy rotation
3. **MVP3:** CAPTCHA handling
4. **MVP3:** Session management (cookies, local storage)
5. **MVP3:** Headful mode debugging UI

---

## Approval

**Specification Changes Approved By:**

- [ ] Technical Lead: _____________________ Date: _____
- [ ] Product Owner: _____________________ Date: _____

---

**Spec Delta Status:** 🟡 DRAFT (Pending Approval)
**Last Updated:** 2026-01-12
