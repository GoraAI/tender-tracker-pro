# Navigation Tutorial - Browser Automation Guide

This tutorial demonstrates how to use the `BrowserEngine` for automated tender website scraping.

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Configuration](#configuration)
3. [Basic Navigation](#basic-navigation)
4. [Handling Cookie Consent](#handling-cookie-consent)
5. [Pagination](#pagination)
6. [Error Handling](#error-handling)
7. [Complete Example](#complete-example)
8. [Troubleshooting](#troubleshooting)

---

## Quick Start

```python
import asyncio
from src.navigator import BrowserEngine

async def main():
    # Load configuration
    config = {
        "browser": {
            "type": "chromium",
            "headless": True,
            "viewport": {"width": 1920, "height": 1080}
        },
        "timeouts": {
            "page_load": 30,
            "navigation": 60
        },
        "delays": {
            "min_ms": 1000,
            "max_ms": 3000
        },
        "max_retries": 3,
        "screenshot_on_error": True
    }

    # Initialize browser
    engine = BrowserEngine(config)
    await engine.initialize()

    # Navigate to tender site
    page = await engine.navigate("https://example.com/tenders")

    # Clean up
    await engine.close()

if __name__ == "__main__":
    asyncio.run(main())
```

---

## Configuration

### Browser Configuration

```yaml
# config/sources/example_site.yaml
name: "Example Tender Site"
enabled: true
navigation:
  browser:
    type: "chromium"        # Options: chromium, firefox, webkit
    headless: true          # Run without GUI
    user_agent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64)..."
    viewport:
      width: 1920
      height: 1080

  timeouts:
    page_load: 30           # Seconds
    navigation: 60          # Seconds

  delays:
    min_ms: 1000            # Minimum delay (1 second)
    max_ms: 3000            # Maximum delay (3 seconds)

  max_retries: 3            # Retry failed navigations
  screenshot_on_error: true # Capture debug screenshots
```

### Loading Configuration

```python
import yaml
from pathlib import Path

def load_source_config(source_name: str) -> dict:
    """Load configuration for a tender source."""
    config_path = Path(f"config/sources/{source_name}.yaml")

    with open(config_path) as f:
        source_config = yaml.safe_load(f)

    return {
        "browser": source_config["navigation"]["browser"],
        "timeouts": source_config["navigation"]["timeouts"],
        "delays": source_config["navigation"]["delays"],
        "max_retries": 3,
        "screenshot_on_error": True
    }

# Use it
config = load_source_config("jhb_water")
engine = BrowserEngine(config)
```

---

## Basic Navigation

### Simple Navigation

```python
async def navigate_to_tenders():
    engine = BrowserEngine(config)
    await engine.initialize()

    # Navigate and wait for page to fully load
    page = await engine.navigate(
        "https://example.com/tenders",
        wait_for="networkidle"  # Wait until network is idle
    )

    # Access page content
    title = await page.title()
    print(f"Page title: {title}")

    url = page.url
    print(f"Current URL: {url}")

    await engine.close()
```

### Wait Conditions

```python
# Wait for network to be idle (default, best for dynamic content)
page = await engine.navigate(url, wait_for="networkidle")

# Wait for DOM to load (faster, good for static content)
page = await engine.navigate(url, wait_for="domcontentloaded")

# Wait for initial load event only (fastest)
page = await engine.navigate(url, wait_for="load")
```

### Custom Timeout

```python
# Override default timeout for slow pages
page = await engine.navigate(
    "https://slow-site.com/tenders",
    wait_for="networkidle",
    timeout_seconds=60  # 1 minute timeout
)
```

---

## Handling Cookie Consent

Many tender sites show cookie consent dialogs. The `BrowserEngine` automatically detects and accepts them:

```python
async def navigate_with_cookie_handling():
    engine = BrowserEngine(config)
    await engine.initialize()

    # Navigate to page
    page = await engine.navigate("https://example.com/tenders")

    # Try to handle cookie consent (if present)
    consent_handled = await engine.handle_cookie_consent(page)

    if consent_handled:
        print("✓ Cookie consent accepted")
    else:
        print("ℹ No cookie consent dialog found")

    # Now page is ready for interaction
    await engine.close()
```

### Supported Cookie Selectors

The engine automatically tries these patterns:
- `button:has-text('Accept')`
- `button:has-text('Agree')`
- `.cookie-accept`
- `#cookie-accept`
- `[id*='cookie'][id*='accept']`
- `[class*='cookie'][class*='accept']`

### Custom Cookie Selectors

If the default patterns don't work, you can add custom selectors:

```python
# In your source configuration
navigation:
  cookie_consent:
    selectors:
      - "button[data-consent='accept']"
      - ".gdpr-accept-button"
      - "#accept-cookies-btn"
```

---

## Pagination

Navigate through multiple pages of tender listings:

```python
async def scrape_all_pages():
    engine = BrowserEngine(config)
    await engine.initialize()

    # Navigate to first page
    page = await engine.navigate("https://example.com/tenders")

    # Handle cookies
    await engine.handle_cookie_consent(page)

    # Paginate through results (max 10 pages)
    pages = await engine.paginate(
        page,
        next_button_selectors=[
            "a:has-text('Next')",
            "button:has-text('→')",
            ".pagination .next",
            "[aria-label='Next page']"
        ],
        max_pages=10
    )

    print(f"Loaded {len(pages)} pages")

    # Process each page
    for i, p in enumerate(pages, 1):
        print(f"Processing page {i}...")
        content = await p.content()
        # Extract tender data from content...

    await engine.close()
```

### How Pagination Works

1. Engine checks for Next button using selectors (in order)
2. If found and visible, clicks it
3. Waits for navigation to complete
4. Adds human-like delay (1-3 seconds)
5. Repeats until:
   - Next button not found
   - OR max_pages reached

---

## Error Handling

The `BrowserEngine` includes robust error handling:

### Automatic Retry

```python
# Engine automatically retries failed navigations 3 times
# with exponential backoff (2s, 4s, 8s)

try:
    page = await engine.navigate("https://unreliable-site.com")
except NavigationTimeout as e:
    print(f"Navigation failed after 3 retries: {e}")
    # Screenshot automatically captured at logs/screenshots/
except NavigationError as e:
    print(f"Navigation error: {e}")
```

### Exception Types

```python
from src.navigator import (
    NavigationError,      # Base exception
    BrowserInitError,     # Browser failed to start
    NavigationTimeout,    # Page load timeout
    ScreenshotError      # Screenshot capture failed
)

try:
    await engine.initialize()
except BrowserInitError:
    print("Failed to start browser. Is Playwright installed?")
    # Run: playwright install chromium

try:
    page = await engine.navigate(url)
except NavigationTimeout:
    print("Page took too long to load")
except NavigationError:
    print("Navigation failed")
```

### Manual Screenshots

Capture screenshots for debugging:

```python
# Take screenshot
screenshot_path = await engine.screenshot(
    page,
    name="debug_view",
    path="logs/screenshots"
)
print(f"Screenshot saved: {screenshot_path}")
# Output: logs/screenshots/debug_view_20260112_143022.png
```

---

## Complete Example

Here's a complete example that scrapes Johannesburg Water tenders:

```python
import asyncio
import yaml
from pathlib import Path
from src.navigator import BrowserEngine, NavigationError

async def scrape_jhb_water_tenders():
    """Scrape all RFQ listings from Johannesburg Water."""

    # Load configuration
    config_path = Path("config/sources/jhb_water.yaml")
    with open(config_path) as f:
        source_config = yaml.safe_load(f)

    browser_config = {
        "browser": source_config["navigation"]["browser"],
        "timeouts": source_config["navigation"]["timeouts"],
        "delays": source_config["navigation"]["delays"],
        "max_retries": 3,
        "screenshot_on_error": True
    }

    # Initialize browser
    engine = BrowserEngine(browser_config)

    try:
        await engine.initialize()
        print("✓ Browser initialized")

        # Navigate to RFQ listing page
        url = "https://scm.johannesburgwater.co.za/supply-chain/request-for-quotations/open-rfq-2/"
        page = await engine.navigate(url)
        print(f"✓ Navigated to: {page.url}")

        # Handle cookie consent
        if await engine.handle_cookie_consent(page):
            print("✓ Cookie consent handled")

        # Paginate through all pages
        pages = await engine.paginate(
            page,
            next_button_selectors=[
                "a:has-text('Next')",
                ".pagination .next"
            ],
            max_pages=5
        )
        print(f"✓ Loaded {len(pages)} pages")

        # Process each page
        all_tenders = []
        for i, p in enumerate(pages, 1):
            print(f"Processing page {i}/{len(pages)}...")

            # Extract tender listings from page
            content = await p.content()
            # ... extract tender data ...

            # Take screenshot for verification
            await engine.screenshot(p, f"jhb_water_page_{i}")

        print(f"✓ Found {len(all_tenders)} tenders")
        return all_tenders

    except NavigationError as e:
        print(f"✗ Navigation failed: {e}")
        return []

    finally:
        # Always close browser
        await engine.close()
        print("✓ Browser closed")

if __name__ == "__main__":
    tenders = asyncio.run(scrape_jhb_water_tenders())
```

---

## Troubleshooting

### Browser Won't Start

**Problem:** `BrowserInitError: Failed to initialize browser`

**Solutions:**
```bash
# Install Playwright browsers
playwright install chromium

# Or install all browsers
playwright install

# Check Playwright is installed
pip show playwright
```

### Page Loads Timeout

**Problem:** `NavigationTimeout` after retries

**Solutions:**
1. Increase timeout:
   ```python
   page = await engine.navigate(url, timeout_seconds=60)
   ```

2. Use different wait condition:
   ```python
   page = await engine.navigate(url, wait_for="load")  # Faster
   ```

3. Check if site is actually down:
   ```bash
   curl -I https://example.com
   ```

### Cookie Consent Not Detected

**Problem:** Cookie dialog blocks content but isn't handled

**Solutions:**
1. Check the selector manually:
   ```python
   # Navigate first
   page = await engine.navigate(url)

   # Take screenshot to see the dialog
   await engine.screenshot(page, "cookie_dialog")

   # Try custom selector
   button = await page.query_selector("button#your-accept-button")
   if button:
       await button.click()
   ```

2. Add custom selector to config:
   ```yaml
   navigation:
     cookie_consent:
       selectors:
         - "button#custom-accept"
   ```

### Pagination Doesn't Work

**Problem:** `paginate()` only returns 1 page

**Solutions:**
1. Verify Next button selector:
   ```python
   # Take screenshot to see pagination controls
   await engine.screenshot(page, "pagination")

   # Check if Next button exists
   next_btn = await page.query_selector("a:has-text('Next')")
   if next_btn:
       print("Next button found")
   else:
       print("Next button not found - check selector")
   ```

2. Try different selectors:
   ```python
   pages = await engine.paginate(
       page,
       next_button_selectors=[
           ".pagination-next",
           "a[rel='next']",
           "button.next-page"
       ],
       max_pages=10
   )
   ```

### Bot Detection

**Problem:** Site detects and blocks the scraper

**Solutions:**
1. Already implemented:
   - Random delays (1-3s) between actions
   - Realistic user agent string
   - Proper viewport size

2. Additional measures:
   ```python
   # Use non-headless mode (slower but looks more human)
   config["browser"]["headless"] = False

   # Increase delays
   config["delays"]["min_ms"] = 2000
   config["delays"]["max_ms"] = 5000
   ```

### Screenshots Not Saving

**Problem:** `screenshot()` fails silently

**Solutions:**
1. Check directory exists:
   ```bash
   mkdir -p logs/screenshots
   ```

2. Use absolute path:
   ```python
   screenshot_path = await engine.screenshot(
       page,
       "test",
       path="/home/user/tender_tracker_pro/logs/screenshots"
   )
   ```

---

## Best Practices

1. **Always close the browser:**
   ```python
   try:
       # ... use engine ...
   finally:
       await engine.close()  # Always clean up
   ```

2. **Handle cookie consent early:**
   ```python
   page = await engine.navigate(url)
   await engine.handle_cookie_consent(page)  # Do this before other interactions
   ```

3. **Use appropriate wait conditions:**
   - `networkidle` for dynamic content (default)
   - `domcontentloaded` for static content (faster)
   - `load` for simple pages (fastest)

4. **Limit pagination:**
   ```python
   # Don't scrape thousands of pages
   pages = await engine.paginate(page, selectors, max_pages=10)
   ```

5. **Check logs for debugging:**
   ```bash
   # Engine logs all navigation events
   tail -f logs/tender_tracker.log
   ```

6. **Test incrementally:**
   ```python
   # Test one step at a time
   await engine.initialize()  # Test this first
   page = await engine.navigate(url)  # Then this
   # ... etc
   ```

---

## Next Steps

- **Extracting Data:** See [EXTRACTION.md](EXTRACTION.md) for PDF parsing
- **LLM Analysis:** See [MODEL_SETUP.md](MODEL_SETUP.md) for AI integration
- **Complete Workflow:** See [README.md](README.md) for end-to-end pipeline

---

**Questions?** Check [API.md](API.md) for detailed API reference.
