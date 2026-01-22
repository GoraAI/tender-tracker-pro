# Lessons Learned - Claude's Operating Manual

> **Claude: READ THIS FILE at session start and on first failure.**
> **UPDATE THIS FILE when you discover patterns that work.**

---

## MANDATORY EXECUTION PROTOCOL

**For EVERY task, follow this protocol. No exceptions.**

### The Flow

```
PHASE 0: READ DOCS
    |   Read LESSONS_LEARNED.md, CLAUDE.md
    |   PROVE you read it: quote a relevant rule
    v
PHASE 1: EXPLORE (No Code Yet)
    |   Search codebase (find, grep)
    |   Read existing patterns
    |   Identify risks
    |
    |   STOP: Share findings. "Waiting for your approval."
    |      DO NOT write code until user says "approved"
    v
USER SAYS: "approved"
    |
    v
+===================================================================+
|  AUTONOMOUS EXECUTION - Loop Until Complete Success               |
+===================================================================+
|                                                                   |
|  PHASE 2: TDD RED                                                 |
|      Write test(s) defining expected behavior                     |
|      Test should FAIL initially                                   |
|                         |                                         |
|                         v                                         |
|  PHASE 3: TDD GREEN                                               |
|      Write implementation                                         |
|      Run test                                                     |
|                         |                                         |
|      TEST LOOP <--------+ Test fails?                             |
|         |               |    YES -> Fix code, run test again      |
|         |               |    Loop until PASS                      |
|         |               v                                         |
|         |         Test passes                                     |
|                         |                                         |
|                         v                                         |
|  PHASE 4: BUILD & DEPLOY                                          |
|      Commit and push                                              |
|      Wait for CI (gh run watch)                                   |
|                         |                                         |
|      CI LOOP <----------+ CI fails?                               |
|         |               |    YES -> gh run view --log-failed      |
|         |               |    Fix issue, push again                |
|         |               |    Loop until CI PASSES                 |
|         |               v                                         |
|         |         CI passes                                       |
|                         |                                         |
|      Download APK (gh run download <id> -n app-debug)             |
|      Install (adb install -r app-debug.apk)                       |
|                         |                                         |
|                         v                                         |
|  PHASE 5: VISUAL VERIFICATION                                     |
|      Launch app, navigate to feature                              |
|      Screenshot (adb exec-out screencap -p > screenshot.png)      |
|      Compare to requirements                                      |
|                         |                                         |
|      VISUAL LOOP <------+ Doesn't match requirements?             |
|         |               |    YES -> Identify gap, fix UI code     |
|         |               |    Rebuild, reinstall, screenshot       |
|         |               |    Loop until MATCHES                   |
|         |               v                                         |
|         |         Visual correct                                  |
|                         |                                         |
|                         v                                         |
|  PHASE 6: LEARN                                                   |
|      Update LESSONS_LEARNED.md if anything new discovered         |
|                                                                   |
+===================================================================+
    |
    v
REPORT TO USER
    "Feature complete. Evidence:
     - Tests: PASS
     - CI: PASS [run ID]
     - Screenshot: [image]"
```

### Three Loops - Keep Iterating Until Success

| Loop | Trigger | Action |
|------|---------|--------|
| **TEST LOOP** | Test fails | Read error -> Fix code -> Run test -> Repeat until pass |
| **CI LOOP** | CI fails | Check logs (`gh run view --log-failed`) -> Fix -> Push -> Repeat until green |
| **VISUAL LOOP** | Screenshot wrong | Identify gap -> Fix UI -> Rebuild -> Reinstall -> Screenshot -> Repeat until correct |

### Critical Rules

1. **ONE STOP POINT ONLY**: Stop after Phase 1 (explore). After "approved", run autonomously to completion.
2. **NEVER GIVE UP**: Loop until each phase succeeds. Don't stop at first failure.
3. **NEVER CLAIM SUCCESS WITHOUT EVIDENCE**: Screenshot proving feature works, or it didn't happen.
4. **NEVER SKIP TDD**: Write test first, then implementation.

---

## Know Your Limitations

| Reality Check | What This Means For You |
|---------------|-------------------------|
| **42%** of your code contains hallucinations | Always run code before claiming it works |
| You produce **1.7x** more issues than humans | Your code needs extra verification |
| **48%** of your code has security vulnerabilities | Never skip security considerations |
| Your effective context is **50-60%** of window | Start complex tasks with fresh context |
| You consume **~15K tokens** in system overhead | Context fills faster than you think |

**Your role:** *You are the implementer, not the architect.* Wait for design direction, then execute precisely.

---

# PART 1: UNIVERSAL RULES

---

## CRITICAL - Never Do This

### Build Environment

- **NEVER build locally on ARM64/aarch64 machines** - Use GitHub Actions CI. Local Android builds fail due to AAPT2 x86_64 architecture mismatch.
- **NEVER attempt to fix deployment issues one at a time** - Do comprehensive research first to understand ALL potential issues before attempting fixes.
- **NEVER guess at solutions without investigating root cause** - Research properly using WebSearch, documentation, and logs.
- **NEVER skip reading deployment/error logs thoroughly** - The actual error is often buried in verbose output.

### Docker/Railway Deployment

- **NEVER use uvloop in containerized Railway environments** - DNS resolution fails. Always use `--loop asyncio` with uvicorn.
- **NEVER use Nixpacks for complex Python apps with Playwright** - Use Dockerfile builder instead.
- **NEVER skip `--with-deps` when installing Playwright in Docker** - It installs system dependencies automatically.
- **NEVER use old Python base images** - Use `python:3.12-bookworm` as recommended by Playwright docs.
- **NEVER forget to set `PLAYWRIGHT_BROWSERS_PATH` in Docker** - Browsers won't be found otherwise.
- **NEVER mix dev dependencies in production requirements** - Create separate `requirements-prod.txt`.
- **NEVER include packages that need C++ compilation (like llama-cpp-python) in Docker builds** - Handle inference elsewhere.

### Database

- **NEVER assume Railway uses your default DATABASE_URL** - Link PostgreSQL service properly.
- **NEVER use `postgres://` scheme with SQLAlchemy async** - Must be `postgresql+asyncpg://`.
- **NEVER hardcode SQLite paths in production** - Use environment variables for DATABASE_URL.

### Your Code Generation Habits

- **NEVER claim code works without running it** - You hallucinate plausible-looking functions that don't exist. Run the code first.
- **NEVER invent package or library names** - Over 20% of your suggestions reference packages that don't exist. Verify imports are real.
- **NEVER invent API methods or function signatures** - Check documentation or existing code before using any API.
- **NEVER assume your code is correct** - You produce 1.7x more issues than human-written code. Test everything.
- **NEVER skip security considerations** - 48% of your code contains security vulnerabilities.
- **NEVER hardcode secrets, API keys, or credentials** - Use environment variables.
- **NEVER make architectural decisions without asking** - You implement designs, you don't create them.

### Your Verification Habits

- **NEVER claim success without verification** - Run the test, take the screenshot, check the output.
- **NEVER repeat the same failed approach** - Check this document for known solutions first.
- **NEVER say "this should work"** - Either verify it works or say "I haven't verified this".

---

## ALWAYS Do This

### Before Any Deployment/Infrastructure Work

- **ALWAYS do comprehensive research first** - WebSearch for best practices, official docs, known issues.
- **ALWAYS read deployment logs completely** - Don't skim, the actual error is often hidden.
- **ALWAYS verify environment variables are set correctly** - Check Railway Variables dashboard.
- **ALWAYS test locally what you can before pushing** - Dependencies can be verified without full builds.

### Docker/Railway Patterns

- **ALWAYS use Dockerfile builder for Python + Playwright** - Nixpacks doesn't handle system deps well.
- **ALWAYS install Playwright with `--with-deps`** - `playwright install chromium --with-deps`.
- **ALWAYS use `--loop asyncio` with uvicorn in containers** - Avoids uvloop DNS issues.
- **ALWAYS set PLAYWRIGHT_BROWSERS_PATH to /ms-playwright** - Standard Docker location.
- **ALWAYS run as non-root user in Docker** - Security best practice: `USER appuser`.
- **ALWAYS create separate requirements-prod.txt** - No test/dev dependencies in production.
- **ALWAYS add field validators for DATABASE_URL conversion** - Railway uses `postgres://`, SQLAlchemy needs `postgresql+asyncpg://`.

### Build Strategy

- **ALWAYS use GitHub Actions CI for Android builds** - Never try to build locally on ARM machines.
- **ALWAYS push to GitHub and let CI build** - Wait for green checkmark before downloading APK.
- **ALWAYS use `gh run download` for APK artifacts** - CI builds are the source of truth.

### Database URL Conversion Pattern

```python
@field_validator("database_url", mode="before")
@classmethod
def fix_database_url(cls, v: str) -> str:
    """Convert Railway's postgres:// to postgresql+asyncpg:// format."""
    if v and v.startswith("postgres://"):
        v = v.replace("postgres://", "postgresql+asyncpg://", 1)
    elif v and v.startswith("postgresql://"):
        v = v.replace("postgresql://", "postgresql+asyncpg://", 1)
    return v
```

### After Writing Code

- **ALWAYS run the code** - Verify it actually works, don't just claim it should.
- **ALWAYS run the tests** - `pytest` before considering anything done.
- **ALWAYS verify with evidence** - Screenshot, test output, or command result.

---

## Railway Deployment Checklist

```
[ ] Use Dockerfile builder (not Nixpacks)
[ ] Base image: python:3.12-bookworm
[ ] Set PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
[ ] Run: playwright install chromium --with-deps
[ ] Create requirements-prod.txt (no dev deps)
[ ] Use --loop asyncio with uvicorn
[ ] Add DATABASE_URL validator for postgres:// conversion
[ ] Configure healthcheck path in railway.json
[ ] Link PostgreSQL service and verify DATABASE_URL in Variables
[ ] Set startup/healthcheck timeouts (300s for Playwright)
```

---

## Docker Uvicorn Start Script Pattern

```bash
#!/bin/bash
RUN_PORT="${PORT:-8000}"
exec uvicorn src.api.main:app --host 0.0.0.0 --port "$RUN_PORT" --loop asyncio
```

**Key: `--loop asyncio`** - Prevents DNS resolution errors in Railway containers.

---

## Git Workflow You Must Follow

### Before Committing

```bash
# Review your changes
git status               # What's changing
git diff                 # Check actual changes make sense

# Commit and push - GitHub Actions will verify
git add -A
git commit -m "feat: description"
git push

# Then monitor GitHub Actions for results
gh run list --limit 3
```

### Commit Message Format

```
type: short description

Types:
- feat: New feature
- fix: Bug fix
- refactor: Code restructuring
- test: Adding tests
- docs: Documentation
- chore: Maintenance
```

---

## Common Problems and Solutions

| Problem | Solution |
|---------|----------|
| Local Android build fails on ARM64 | Use GitHub Actions CI, never build locally |
| Railway DNS resolution error | Add `--loop asyncio` to uvicorn command |
| Playwright not found in Docker | Use `playwright install chromium --with-deps` |
| SQLAlchemy can't connect to Railway DB | Convert `postgres://` to `postgresql+asyncpg://` |
| Railway using old Dockerfile | Wait for cache invalidation or add cache-bust comment |
| ModuleNotFoundError in Railway | Check DATABASE_URL, might be using wrong driver |
| Build succeeds but healthcheck fails | Increase healthcheckTimeout in railway.json (300s) |
| Multiple deployment failures in a row | STOP. Research comprehensively. Don't fix one thing at a time. |

---

## How to Update This Document

### Automatic Triggers - Update Immediately When:

1. **You fail -> then succeed** - Document what finally worked
2. **User redirects you** - Document both the wrong and right approach
3. **Something takes multiple attempts** - Document the working approach
4. **You discover something non-obvious** - Document with full context

### What to Add

- Add failed approaches to "CRITICAL - Never Do This"
- Add working approaches to "ALWAYS Do This"
- Add specific patterns to the relevant section
- Add to Change Log with date and category

---

## Change Log

| Date | Learning | Category |
|------|----------|----------|
| 2026-01-16 | Never build Android locally on ARM64 - use GitHub Actions CI | Build |
| 2026-01-16 | Do comprehensive research before deployment fixes - don't fix one issue at a time | Process |
| 2026-01-16 | uvloop causes DNS errors in Railway containers - use `--loop asyncio` | Docker/Railway |
| 2026-01-16 | Playwright Docker needs `--with-deps` flag | Docker |
| 2026-01-16 | Create separate requirements-prod.txt without dev dependencies | Dependencies |
| 2026-01-16 | Railway uses postgres://, SQLAlchemy needs postgresql+asyncpg:// | Database |
| 2026-01-16 | Use python:3.12-bookworm base image for Playwright | Docker |
| 2026-01-16 | Set PLAYWRIGHT_BROWSERS_PATH=/ms-playwright in Docker | Docker |
| 2026-01-16 | Always link PostgreSQL service in Railway, don't rely on default DATABASE_URL | Railway |
| 2026-01-16 | Nixpacks doesn't work well with Playwright - use Dockerfile builder | Railway |

---

# PART 2: PROJECT-SPECIFIC

---

## App Configuration

- **Package Name**: `com.tendertracker.app`
- **Backend URL (Production)**: `https://tender-tracker-pro-production.up.railway.app/api/v1/`
- **Backend URL (Dev/Emulator)**: `http://10.0.2.2:8000/api/v1/`
- **Backend URL (Dev/Physical)**: `http://127.0.0.1:8000/api/v1/` (via adb reverse)

## Infrastructure

- **Railway Web Service**: tender-tracker-pro-production.up.railway.app
- **Railway PostgreSQL**: Uses internal Railway networking
- **CI/CD**: GitHub Actions for Android builds

## Key Paths

```
tender_tracker_pro/
├── src/                      # Python backend
│   ├── api/                  # FastAPI endpoints
│   ├── scrapers/             # Tender scraping
│   └── llm/                  # LLM integration
├── android/                  # Android app
│   └── app/src/main/java/    # Kotlin source
├── config/                   # YAML configs
├── openspec/                 # OpenSpec workflows
├── Dockerfile                # Railway deployment
├── railway.json              # Railway config
├── requirements-prod.txt     # Production deps
├── requirements.txt          # Dev deps
├── CLAUDE.md                 # Knowledge base
└── LESSONS_LEARNED.md        # This file
```

## Project-Specific Gotchas

- Android NetworkModule.kt contains BASE_URL - update for production/dev switching
- Railway needs explicit PostgreSQL linking - default DATABASE_URL points to SQLite
- Playwright browser automation runs on backend, not Android
- LLM inference runs on Android (Gemma), not backend

## SharePoint Site Scraping

### Pattern: City of Johannesburg

**Challenge**: SharePoint sites use dynamic content loading, direct HTTP requests may not work.

**Solution**:
- Use `browser_navigation` method with Playwright
- Add `wait_for: "networkidle"` to ensure page fully loads
- Use `wait_for_selector` to wait for dynamic content (PDF links)
- Set generous timeouts (30s for page load, 10s for selectors)

**Selectors**:
- PDF links: `a[href$='.pdf']` (attribute selector for PDF extension)
- Table cells: `td.ms-vb2` (SharePoint default table class)

**Timing**:
- Page load: 30 seconds (SharePoint can be slow)
- Selector wait: 10 seconds (dynamic content)

**Fallback**:
- Direct HTTP requests may not work with SharePoint
- Browser automation is the most reliable method

## Configuration-Driven Scraper Architecture

**Pattern**: All tender sources configured via YAML files in `config/sources/`

**Benefits**:
- Add new sources without code changes
- Consistent structure across all scrapers
- Easy to test and validate configurations

**Configuration Requirements**:
- `source`: id, name, enabled status
- `urls`: all URLs used in navigation
- `navigation`: browser settings and step-by-step navigation
- `pdf_discovery`: patterns for finding tender documents
- `download`: download settings and validation
- `extraction`: PDF text extraction configuration
- `fallback`: fallback scraping if browser automation fails
- `monitoring`: alerts and tracking
- `schedule`: cron schedule for automated runs

**Proven Patterns**:
- SARS (National government) - Direct navigation to tender listing
- City of Johannesburg (Municipal, SharePoint) - Dynamic content with wait conditions
