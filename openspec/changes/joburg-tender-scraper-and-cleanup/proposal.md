# Proposal: joburg-tender-scraper-and-cleanup

## Summary
Add City of Johannesburg tender scraper and remove non-working sources (jhb_water) to maintain a clean, production-ready tender pipeline with only functional sources.

## Motivation

### Why This Change Is Needed

**Current State Issues:**
1. **Non-working sources**: `jhb_water.yaml` configuration exists but source is not functional
2. **Limited tender coverage**: Only SARS is currently working, limiting tender discovery
3. **Technical debt**: Keeping broken scrapers creates maintenance burden and confusion

**Opportunity:**
- City of Johannesburg (joburg.org.za) publishes active tenders and quotations
- User has provided complete navigation path to tender documents
- Adds valuable municipal tender source

### Business Impact
- ✅ Increases tender coverage with major South African municipality
- ✅ Reduces confusion by removing non-functional sources
- ✅ Maintains clean, production-ready codebase
- ✅ Demonstrates scalable architecture for future sources

## Scope

### What's INCLUDED

**1. Add Johannesburg Tender Scraper**
- Create `config/sources/joburg.yaml` configuration
- Implement navigation path:
  ```
  https://joburg.org.za/
  → /work_/Pages/Work%20in%20Joburg/Tenders%20and%20Quotations/Tenders-and-Quotations.aspx
  → /work_/Pages/Work%20in%20Joburg/Tenders%20and%20Quotations/Links/Quotations/Quotations.aspx
  → /work_/Pages/2026-Tenders/Request-for-Quotations.aspx
  → PDF document links
  ```
- Configure browser automation for multi-page navigation
- Extract tender metadata (title, reference number, closing dates)
- Download PDF documents
- Text extraction from PDFs

**2. Remove Non-Working Sources**
- Delete `config/sources/jhb_water.yaml`
- Remove any jhb_water-specific code/references
- Update documentation to reflect removed source

**3. Architecture Validation**
- Ensure existing scraper architecture scales to new source
- Validate configuration-driven approach works for complex navigation
- Test fallback mechanisms

### What's NOT Included
- ❌ Not adding other sources beyond Johannesburg (future task)
- ❌ Not refactoring core scraper engine (use existing)
- ❌ Not changing database schema (existing schema supports this)
- ❌ Not modifying frontend (existing tender list will show new source)

## Acceptance Criteria

### Functionality
- [ ] Johannesburg scraper successfully navigates all 4+ pages in tender path
- [ ] Extracts tender metadata: title, reference number, closing date, description
- [ ] Downloads tender PDF documents to correct storage location
- [ ] Extracts text from downloaded PDFs
- [ ] Saves tender data to database with `source_id="joburg"`
- [ ] Tenders appear in API `/api/v1/tenders/` endpoint
- [ ] Tenders display correctly in Android app

### Code Quality
- [ ] Configuration follows existing YAML pattern (like sars.yaml)
- [ ] No hard-coded URLs in Python code (all in YAML)
- [ ] Error handling for failed navigation steps
- [ ] Logging at appropriate levels (INFO, WARNING, ERROR)
- [ ] Retry logic for transient failures

### Cleanup
- [ ] `config/sources/jhb_water.yaml` deleted
- [ ] No references to `jhb_water` in codebase
- [ ] README updated to list only working sources (SARS, Johannesburg)

### Testing
- [ ] Manual test: Run scraper and verify tenders downloaded
- [ ] Manual test: Check database has joburg tenders
- [ ] Manual test: View tenders in Android app
- [ ] CI passes (lint, tests)

### Documentation
- [ ] README lists Johannesburg as active source
- [ ] Configuration documented in joburg.yaml comments
- [ ] Any lessons learned added to LESSONS_LEARNED.md

## Technical Approach

### 1. Configuration Strategy

**Follow Existing Pattern:**
- Copy `sars.yaml` as template
- Customize for Johannesburg's multi-page navigation
- Use `browser_navigation` method (Playwright)

**Key Configuration Sections:**
```yaml
source:
  id: "joburg"
  name: "City of Johannesburg"
  enabled: true

urls:
  main_site: "https://joburg.org.za/"
  tenders_landing: "https://joburg.org.za/work_/Pages/Work%20in%20Joburg/Tenders%20and%20Quotations/Tenders-and-Quotations.aspx"
  quotations: "https://joburg.org.za/work_/Pages/Work%20in%20Joburg/Tenders%20and%20Quotations/Links/Quotations/Quotations.aspx"
  rfq_listing: "https://joburg.org.za/work_/Pages/2026-Tenders/Request-for-Quotations.aspx"

navigation:
  method: "browser_navigation"
  steps:
    - action: "goto"
      url: "${urls.rfq_listing}"  # Go directly to RFQ listing page
      wait_for: "networkidle"
    - action: "wait_for_selector"
      selector: "a[href$='.pdf']"
      timeout_ms: 10000
```

### 2. Implementation Strategy

**Leverage Existing Infrastructure:**
- Use existing Playwright scraper (`src/scrapers/playwright_scraper.py`)
- Use existing PDF downloader
- Use existing text extraction pipeline
- Use existing database models

**No New Components Needed:**
- Configuration file is sufficient
- Existing scraper engine handles multi-page navigation
- Existing YAML loader supports new source

### 3. Removal Strategy

**Clean Removal:**
```bash
# Delete configuration
rm config/sources/jhb_water.yaml

# Search for references
grep -r "jhb_water" src/
grep -r "jhb_water" config/
grep -r "jhb_water" *.md

# Remove any found references
```

### 4. Testing Strategy

**Manual Testing Steps:**
```bash
# 1. Test scraper
python -m src.scrapers.main --source joburg --test

# 2. Check downloads
ls documents/downloads/joburg/

# 3. Check database
python -m src.scrapers.check_db --source joburg

# 4. Test API
curl http://localhost:8000/api/v1/tenders/?source=joburg

# 5. Test Android app
# Launch app, verify Johannesburg tenders appear
```

## Implementation Plan

### Phase 1: Cleanup (15 minutes)
1. Delete `config/sources/jhb_water.yaml`
2. Search and remove any `jhb_water` references
3. Commit: "chore: remove non-working jhb_water source"

### Phase 2: Create Johannesburg Configuration (30 minutes)
1. Create `config/sources/joburg.yaml`
2. Configure navigation steps for multi-page path
3. Configure PDF discovery patterns
4. Test YAML syntax validity

### Phase 3: Test Scraper (45 minutes)
1. Run scraper in test mode
2. Verify navigation works through all pages
3. Verify PDF downloads
4. Debug any issues
5. Adjust selectors/timing if needed

### Phase 4: Validate End-to-End (30 minutes)
1. Run full scrape
2. Check database for tenders
3. Test API endpoint
4. View in Android app
5. Document any issues

### Phase 5: Documentation (15 minutes)
1. Update README with Johannesburg source
2. Add configuration comments
3. Update LESSONS_LEARNED.md if needed

**Total Estimated Time:** 2-3 hours

## Architecture Notes

### Scalability Validation

This change validates that our architecture supports:
- ✅ **Configuration-driven scrapers** - no code changes for new sources
- ✅ **Multi-page navigation** - handles complex tender site structures
- ✅ **Different URL patterns** - SharePoint-style URLs (joburg.org.za uses SharePoint)
- ✅ **Flexible selectors** - can adapt to different site structures

### Future Sources

This pattern will work for:
- Other South African municipalities (Cape Town, Durban, etc.)
- Provincial governments
- National departments
- Parastatals (Eskom, Transnet, etc.)

**Only requirement:** Provide navigation path and sample tender URL

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Johannesburg site structure changes | Scraper breaks | Use flexible selectors, implement fallback scraping |
| SharePoint authentication required | Can't access tenders | Check if public access, implement auth if needed |
| Rate limiting / blocking | Scraper blocked | Add delays, rotate user agents, respect robots.txt |
| Large PDF files | Slow downloads | Configure size limits, implement streaming |
| Pagination on RFQ page | Miss tenders | Check for pagination, implement if present |

## References

### Existing Specifications
- See `config/sources/sars.yaml` for configuration pattern
- See `src/scrapers/playwright_scraper.py` for browser automation
- See `src/scrapers/pdf_downloader.py` for PDF handling

### Related Documentation
- README.md - Source list
- LESSONS_LEARNED.md - Scraper patterns

### External References
- City of Johannesburg tender portal: https://joburg.org.za/
- Navigation path provided by user
- Sample tender PDF: https://joburg.org.za/work_/Documents/2026-Tenders/COJ0082-25-26_TRAINING-FIRE-INSPECTOR-AND-FIRE-INVESTIGATOR.pdf

## Success Metrics

**Immediate:**
- Joburg scraper runs without errors
- At least 5+ tenders discovered and downloaded
- Tenders visible in Android app within 1 hour of scrape

**Long-term:**
- Joburg scraper runs reliably on schedule (daily/weekly)
- Zero manual intervention required
- Ready to add next source using same pattern

---

**Status:** PROPOSED
**Created:** 2026-01-22
**Author:** Claude Sonnet 4.5
**Requires Approval:** YES
