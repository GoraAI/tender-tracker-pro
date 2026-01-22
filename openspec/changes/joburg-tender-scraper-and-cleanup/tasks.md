# Tasks: joburg-tender-scraper-and-cleanup

## Phase 1: Cleanup Non-Working Sources

- [ ] **Delete jhb_water configuration**
  - Delete `config/sources/jhb_water.yaml`
  - Acceptance: File no longer exists

- [ ] **Search for jhb_water references**
  - Search codebase for "jhb_water" string
  - Acceptance: No references found in src/, config/, or docs

- [ ] **Update README**
  - Remove jhb_water from source list
  - List only SARS and Johannesburg as active sources
  - Acceptance: README accurately reflects working sources

## Phase 2: Create Johannesburg Configuration

- [ ] **Create joburg.yaml configuration**
  - Copy sars.yaml as template
  - Configure source metadata (id, name, enabled)
  - Acceptance: Valid YAML syntax

- [ ] **Configure URLs section**
  - Add main_site, tenders_landing, quotations, rfq_listing URLs
  - Use user-provided navigation path
  - Acceptance: All URLs accessible and correct

- [ ] **Configure navigation steps**
  - Set method: "browser_navigation"
  - Add goto action to rfq_listing page
  - Add wait_for_selector for PDF links
  - Acceptance: Navigation config matches SARS pattern

- [ ] **Configure PDF discovery**
  - Set selector: `a[href$='.pdf']`
  - Configure metadata extraction patterns
  - Acceptance: Selector matches Johannesburg site structure

- [ ] **Configure fallback scraping**
  - Add fallback_scraping section
  - Configure timeout and retry settings
  - Acceptance: Same pattern as SARS config

## Phase 3: Test Scraper

- [ ] **Validate YAML syntax**
  - Run YAML validator
  - Check for syntax errors
  - Acceptance: No YAML parse errors

- [ ] **Test navigation manually**
  - Open browser, navigate through path manually
  - Verify all links work
  - Verify PDF links are discoverable
  - Acceptance: Manual navigation succeeds

- [ ] **Run scraper in test mode**
  - Execute: `python -m src.scrapers.main --source joburg --test`
  - Monitor logs for navigation success
  - Acceptance: Scraper navigates without errors

- [ ] **Verify PDF discovery**
  - Check logs for discovered PDF links
  - Verify tender metadata extracted
  - Acceptance: At least 5+ tenders discovered

- [ ] **Test PDF downloads**
  - Check `documents/downloads/joburg/` directory
  - Verify PDFs downloaded successfully
  - Acceptance: PDFs exist and are valid

- [ ] **Test text extraction**
  - Verify text extracted from PDFs
  - Check for readable content
  - Acceptance: Text files exist with content

## Phase 4: Database & API Validation

- [ ] **Run full scrape**
  - Execute: `python -m src.scrapers.main --source joburg`
  - Wait for completion
  - Acceptance: Scraper completes without fatal errors

- [ ] **Check database**
  - Query tenders table: `SELECT * FROM tenders WHERE source_id='joburg'`
  - Verify tender records exist
  - Acceptance: At least 5+ joburg tenders in database

- [ ] **Test API endpoint**
  - Execute: `curl http://localhost:8000/api/v1/tenders/?source=joburg`
  - Verify JSON response with joburg tenders
  - Acceptance: API returns joburg tenders

- [ ] **Verify tender metadata**
  - Check title, reference_number, closing_date fields
  - Verify PDF document paths
  - Acceptance: All required fields populated

## Phase 5: Android App Validation

- [ ] **Build and install app**
  - Push code to GitHub
  - Wait for CI build
  - Download and install APK
  - Acceptance: APK installs successfully

- [ ] **View tenders in app**
  - Open app, navigate to tender list
  - Verify Johannesburg tenders appear
  - Acceptance: Joburg tenders visible

- [ ] **Test tender detail view**
  - Tap on a Johannesburg tender
  - Verify all details display correctly
  - Acceptance: Tender details render properly

- [ ] **Test PDF viewing**
  - Attempt to open PDF from tender detail
  - Verify PDF loads
  - Acceptance: PDF opens without errors

## Phase 6: Documentation & Cleanup

- [ ] **Update README**
  - Add Johannesburg to active sources list
  - Document configuration location
  - Acceptance: README lists SARS and Johannesburg

- [ ] **Document configuration**
  - Add comments to joburg.yaml
  - Explain navigation path
  - Acceptance: Config is self-documenting

- [ ] **Update LESSONS_LEARNED.md**
  - Add any discoveries about SharePoint sites
  - Document selector patterns that worked
  - Add navigation timing requirements
  - Acceptance: Lessons captured

- [ ] **Run CI validation**
  - Push all changes
  - Wait for CI to pass (lint, tests)
  - Acceptance: CI is green

## Phase 7: Monitoring & Verification

- [ ] **Schedule scraper run**
  - Configure scraper to run on schedule (if not already)
  - Verify Johannesburg included in runs
  - Acceptance: Scraper schedule includes joburg

- [ ] **Monitor first scheduled run**
  - Check logs for successful execution
  - Verify new tenders discovered
  - Acceptance: Scheduled run completes successfully

- [ ] **Verify no regressions**
  - Test SARS scraper still works
  - Verify existing tenders unchanged
  - Acceptance: SARS continues to work

## Testing Checklist

- [ ] Manual browser navigation through full path
- [ ] Scraper test mode execution
- [ ] Full scrape execution
- [ ] Database query verification
- [ ] API endpoint test
- [ ] Android app visual verification
- [ ] PDF download and extraction
- [ ] CI/CD pipeline validation

## Success Criteria

All tasks must be completed and checked before using `/openspec-archive`.

**Definition of Done**:
- All tasks marked `[x]`
- Johannesburg scraper runs without errors
- At least 5+ tenders discovered and stored
- Tenders visible in Android app
- jhb_water completely removed
- CI is green
- Documentation updated
