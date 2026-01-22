# Spec Delta: joburg-tender-scraper-and-cleanup

## ADDED

### config/sources/joburg.yaml

**New scraper configuration for City of Johannesburg tender source.**

```yaml
# City of Johannesburg Tender Scraper Configuration
# Source: https://joburg.org.za/
# Navigation: Main site → Tenders and Quotations → Quotations → RFQ Listing → PDFs

source:
  id: "joburg"
  name: "City of Johannesburg"
  display_name: "City of Johannesburg"
  description: "Tenders and quotations from the City of Johannesburg"
  enabled: true
  category: "municipal"
  region: "Gauteng"

urls:
  main_site: "https://joburg.org.za/"
  tenders_landing: "https://joburg.org.za/work_/Pages/Work%20in%20Joburg/Tenders%20and%20Quotations/Tenders-and-Quotations.aspx"
  quotations: "https://joburg.org.za/work_/Pages/Work%20in%20Joburg/Tenders%20and%20Quotations/Links/Quotations/Quotations.aspx"
  rfq_listing: "https://joburg.org.za/work_/Pages/2026-Tenders/Request-for-Quotations.aspx"

# Navigation configuration for browser automation (Playwright)
navigation:
  method: "browser_navigation"
  steps:
    # Go directly to RFQ listing page (most efficient path)
    - action: "goto"
      url: "${urls.rfq_listing}"
      wait_for: "networkidle"
      timeout_ms: 30000

    # Wait for PDF links to appear (SharePoint dynamic content)
    - action: "wait_for_selector"
      selector: "a[href$='.pdf']"
      timeout_ms: 10000

# PDF discovery patterns
discovery:
  # Selector for PDF links on RFQ listing page
  pdf_selector: "a[href$='.pdf']"

  # Tender metadata extraction patterns
  metadata_patterns:
    # Title: Extract from link text or adjacent text
    title:
      selector: "a[href$='.pdf']"
      attribute: "text"
      fallback_selector: "td.ms-vb2"

    # Reference number: Usually in format COJ####-##-##
    reference_number:
      pattern: "COJ\\d{4}-\\d{2}-\\d{2}"
      selector: "a[href$='.pdf']"
      attribute: "text"

    # Closing date: Look for date patterns near PDF link
    closing_date:
      pattern: "\\d{2}/\\d{2}/\\d{4}"
      selector: "td"
      date_format: "%d/%m/%Y"

    # Description: Extract from adjacent table cells
    description:
      selector: "td.ms-vb2"
      attribute: "text"
      max_length: 500

# PDF download configuration
download:
  # Save PDFs to documents/downloads/joburg/
  storage_path: "documents/downloads/joburg"

  # Filename pattern: {reference_number}_{title}.pdf
  filename_pattern: "{reference_number}_{title}.pdf"

  # Download settings
  max_file_size_mb: 50
  timeout_seconds: 120
  retry_attempts: 3
  retry_delay_seconds: 5

# Text extraction configuration
extraction:
  # Extract text from downloaded PDFs
  enabled: true

  # Storage path for extracted text
  storage_path: "documents/extracted/joburg"

  # Extraction settings
  method: "pdfplumber"
  fallback_method: "pypdf2"
  clean_text: true
  extract_tables: false

# Fallback scraping if browser automation fails
fallback_scraping:
  enabled: true
  method: "requests"

  # Try direct HTTP request to RFQ listing page
  steps:
    - action: "get"
      url: "${urls.rfq_listing}"
      headers:
        User-Agent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

    - action: "parse_html"
      find_all: "a[href$='.pdf']"
      extract_href: true

# Monitoring and error handling
monitoring:
  # Alert if no tenders found (might indicate site structure change)
  min_expected_tenders: 1

  # Alert if scraper fails
  alert_on_failure: true

  # Retry configuration
  retry_on_failure: true
  max_retries: 3
  backoff_multiplier: 2

# Scheduling
schedule:
  # Run daily at 8 AM SAST
  cron: "0 8 * * *"
  timezone: "Africa/Johannesburg"

  # Enable scheduled runs
  enabled: true

# Notes
# - Johannesburg uses SharePoint, which requires browser automation
# - PDF links are dynamically loaded, so wait_for_selector is critical
# - Reference numbers follow pattern: COJ####-##-## (e.g., COJ0082-25-26)
# - Site structure may change; monitor min_expected_tenders
# - Fallback scraping may not work with SharePoint dynamic content
```

## MODIFIED

### README.md

**Update active tender sources list.**

```diff
 ## Active Tender Sources

-- **SARS** - South African Revenue Service
-- **Johannesburg Water** - City of Johannesburg Water Utility
+- **SARS** - South African Revenue Service (National)
+- **City of Johannesburg** - Municipal tenders and quotations (Gauteng)

 ## Configuration

 Scraper configurations are located in `config/sources/`:

 - `sars.yaml` - SARS tender configuration
+- `joburg.yaml` - City of Johannesburg tender configuration
```

### LESSONS_LEARNED.md

**Add new section for SharePoint site scraping patterns.**

```diff
+## SharePoint Site Scraping
+
+### Pattern: City of Johannesburg
+
+**Challenge**: SharePoint sites use dynamic content loading, direct HTTP requests may not work.
+
+**Solution**:
+- Use `browser_navigation` method with Playwright
+- Add `wait_for: "networkidle"` to ensure page fully loads
+- Use `wait_for_selector` to wait for dynamic content (PDF links)
+- Set generous timeouts (30s for page load, 10s for selectors)
+
+**Selectors**:
+- PDF links: `a[href$='.pdf']` (attribute selector for PDF extension)
+- Table cells: `td.ms-vb2` (SharePoint default table class)
+
+**Timing**:
+- Page load: 30 seconds (SharePoint can be slow)
+- Selector wait: 10 seconds (dynamic content)
+
+**Fallback**:
+- Direct HTTP requests may not work with SharePoint
+- Browser automation is the most reliable method
+
 ## Configuration-Driven Scraper Architecture
```

## REMOVED

### config/sources/jhb_water.yaml

**Removed non-working Johannesburg Water configuration.**

**Reason for removal**: Source is not functional and creates technical debt.

**Impact**:
- No impact on functionality (source was already non-working)
- Reduces confusion about which sources are active
- Cleans up codebase

**Migration notes**:
- City of Johannesburg (joburg.yaml) replaces this as the municipal source
- No data migration needed (source produced no data)

## Summary of Changes

### Files Added: 1
- `config/sources/joburg.yaml` - New scraper configuration

### Files Modified: 2
- `README.md` - Updated active sources list
- `LESSONS_LEARNED.md` - Added SharePoint scraping patterns

### Files Removed: 1
- `config/sources/jhb_water.yaml` - Non-working source removed

### Net Impact
- ✅ **Increased coverage**: Municipal tenders from Johannesburg
- ✅ **Cleaner codebase**: Only working sources configured
- ✅ **Architecture validation**: Proves configuration-driven approach scales
- ✅ **Knowledge capture**: SharePoint patterns documented

## Testing Requirements

Before merging these spec changes:

1. **Validate YAML syntax**: Ensure joburg.yaml parses correctly
2. **Test navigation**: Manually verify all URLs accessible
3. **Test scraper**: Run in test mode, verify PDF discovery
4. **Test database**: Verify tenders saved with correct source_id
5. **Test API**: Verify joburg tenders returned by API
6. **Test app**: Verify tenders visible in Android app
7. **Test removal**: Verify jhb_water completely removed (no references)
8. **CI validation**: Ensure all tests pass

## Rollback Plan

If issues arise:

1. **Restore jhb_water.yaml**: `git checkout HEAD config/sources/jhb_water.yaml`
2. **Remove joburg.yaml**: `rm config/sources/joburg.yaml`
3. **Revert README**: `git checkout HEAD README.md`
4. **Revert LESSONS_LEARNED**: `git checkout HEAD LESSONS_LEARNED.md`

## Future Enhancements

This change enables future municipal sources:

- **Cape Town**: Similar SharePoint structure expected
- **Durban**: May use different portal (eThekwini)
- **Pretoria/Tshwane**: Similar to Johannesburg
- **Other metros**: Use joburg.yaml as template

**Key insight**: Configuration-driven approach proven to scale. Adding new sources requires only YAML files, no code changes.
