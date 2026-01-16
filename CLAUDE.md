# Tender Tracker Pro - Claude Code Knowledge Base

---

## MANDATORY EXECUTION PROTOCOL - READ THIS FIRST

**For EVERY feature/task, follow this protocol. NO EXCEPTIONS.**

### Phase 0: PROVE You Read This
Before doing anything else, quote this rule in your response:
> "I must create an OpenSpec proposal and STOP for approval before writing any implementation code."

---

## OpenSpec-Driven Workflow

```
+---------------------------------------------------------------------+
|  PHASE 1: PROPOSAL                                                  |
|  /openspec-proposal <feature>                                       |
|     - Creates: proposal.md, tasks.md, spec-delta.md                 |
|     - Defines: scope, acceptance criteria, implementation plan      |
|                                                                     |
|  STOP - Wait for user approval of specs                             |
|                                                                     |
|  PHASE 2: APPLY                                                     |
|  /openspec-apply <change-name>                                      |
|     - TDD RED: Write failing tests                                  |
|     - TDD GREEN: Implement to pass                                  |
|     - TEST LOOP: Until all tests pass                               |
|     - CI LOOP: Until CI green                                       |
|     - VISUAL LOOP: Until UI verified                                |
|     - Check off tasks in tasks.md                                   |
|                                                                     |
|  PHASE 3: ARCHIVE                                                   |
|  /openspec-archive <change-name>                                    |
|     - Verify all tasks complete                                     |
|     - Merge specs to openspec/specs/                                |
|     - Move to archive/                                              |
|     - Update LESSONS_LEARNED.md                                     |
+---------------------------------------------------------------------+
```

---

## Build Strategy - CRITICAL

### Android Builds: GitHub Actions ONLY

**NEVER build Android locally on ARM64/aarch64 machines.**

```bash
# LOCAL - What you CAN do:
git add -A && git commit -m "message"
git push

# LOCAL - What you CANNOT do:
# ./gradlew assembleDebug    (FAILS - AAPT2 x86_64 on ARM)
# ./gradlew test             (FAILS)
# ./gradlew anything         (FAILS)

# After push, use CI:
gh run list --limit 3
gh run watch
gh run download <run-id> -n app-debug
adb install -r app-debug.apk
```

### Python Backend: Local Development OK

```bash
# These work locally:
pytest tests/
python -m src.api.main
uvicorn src.api.main:app --reload
```

---

## Railway Deployment - Key Rules

### Must Use

- **Dockerfile builder** (not Nixpacks)
- **python:3.12-bookworm** base image
- **`--loop asyncio`** with uvicorn (DNS fix)
- **`playwright install chromium --with-deps`**
- **Separate requirements-prod.txt**

### DATABASE_URL Conversion

Railway uses `postgres://` but SQLAlchemy async needs `postgresql+asyncpg://`:

```python
@field_validator("database_url", mode="before")
@classmethod
def fix_database_url(cls, v: str) -> str:
    if v and v.startswith("postgres://"):
        v = v.replace("postgres://", "postgresql+asyncpg://", 1)
    return v
```

---

## Three Loops - KEEP GOING Until Success

| Loop | When | Action |
|------|------|--------|
| TEST LOOP | Test fails | Fix code -> Run test -> Repeat until PASS |
| CI LOOP | CI fails | `gh run view --log-failed` -> Fix -> Push -> Repeat until GREEN |
| VISUAL LOOP | Screenshot wrong | Fix UI -> Rebuild -> Reinstall -> Verify -> Repeat until CORRECT |

### Critical Rules

1. **SPEC FIRST**: Always create OpenSpec proposal before any code
2. **ONE STOP**: Only stop after Phase 1 (proposal). Then run to completion.
3. **SPEC ADHERENCE**: Every change must match the approved spec
4. **NEVER GIVE UP**: Loop until each phase succeeds.
5. **EVIDENCE REQUIRED**: Use `/done` command - it validates all evidence.
6. **TDD REQUIRED**: Test first, then implement.

---

## Know Your Limitations

| Reality | Number | Your Action |
|---------|--------|-------------|
| Your code hallucination rate | **42%** | Always run code before claiming it works |
| Your issues vs human code | **1.7x more** | Test everything, expect review |
| Your security vulnerabilities | **48%** | Never skip security considerations |
| Your effective context | **50-60%** | Don't start complex tasks when context is filling up |

**Your Role:** *You are the implementer, not the architect.* Wait for design direction, then execute precisely.

---

## Commands

### OpenSpec Commands (Spec-Driven Development)

| Command | Purpose |
|---------|---------|
| `/openspec-proposal <feature>` | Create change proposal with specs (ALWAYS START HERE) |
| `/openspec-apply <change-name>` | Implement approved specs |
| `/openspec-archive <change-name>` | Archive completed change, update specs/ |

### Workflow Commands

| Command | Purpose |
|---------|---------|
| `/route-task <task>` | Route task to appropriate workflow |
| `/status` | Project status |
| `/done` | Validate evidence, generate completion report |
| `/verify` | Lint + tests |
| `/ship "msg"` | Commit, push |
| `/ci-status` | Check GitHub Actions status |

### Utility Commands

| Command | Purpose |
|---------|---------|
| `/add-lesson` | Update LESSONS_LEARNED.md |
| `/opus-think` | Switch to Opus for planning |
| `/sonnet` | Switch to Sonnet for implementation |

---

## App Info

### Android
- **Package**: `com.tendertracker.app`
- **Production API**: `https://tender-tracker-pro-production.up.railway.app/api/v1/`
- **Dev API (emulator)**: `http://10.0.2.2:8000/api/v1/`
- **Dev API (physical via adb reverse)**: `http://127.0.0.1:8000/api/v1/`

### Backend
- **Framework**: FastAPI
- **Database**: PostgreSQL (Railway) / SQLite (dev)
- **Browser Automation**: Playwright
- **Deployment**: Railway (Dockerfile)

---

## Project Structure

```
tender_tracker_pro/
├── src/                      # Python backend
│   ├── api/                  # FastAPI endpoints
│   │   ├── main.py          # App entry
│   │   ├── config.py        # Settings with DATABASE_URL fix
│   │   └── routers/         # API routes
│   ├── scrapers/            # Tender scraping
│   ├── extraction/          # PDF extraction
│   ├── llm/                 # LLM integration
│   └── models/              # Database models
├── android/                 # Android app
│   └── app/src/main/java/
│       └── com/tendertracker/app/
│           ├── di/          # Hilt DI (NetworkModule.kt has BASE_URL)
│           ├── ui/          # Compose screens
│           └── data/        # Repository layer
├── config/                  # YAML scraper configs
├── openspec/                # OpenSpec change proposals
│   ├── changes/            # Active changes
│   ├── specs/              # Merged specs
│   └── archive/            # Completed changes
├── tests/                   # Python tests
├── Dockerfile               # Railway deployment
├── railway.json             # Railway config
├── requirements-prod.txt    # Production deps
├── requirements.txt         # Dev deps
├── CLAUDE.md               # This file
└── LESSONS_LEARNED.md      # Patterns and learnings
```

---

## Read Order

1. **LESSONS_LEARNED.md** - FIRST, always (contains Railway deployment fixes)
2. **openspec/changes/** - For active work
3. **src/api/config.py** - DATABASE_URL handling
4. **android/.../di/NetworkModule.kt** - API base URL

---

## AUTOMATIC LESSON CAPTURE

### Trigger 1: Claude fails -> then succeeds
**Action:** IMMEDIATELY append to LESSONS_LEARNED.md
- Add failed approach to "CRITICAL - Never Do This"
- Add working approach to "ALWAYS Do This"
- Add entry to Change Log

### Trigger 2: User redirects Claude -> new approach works
**Action:** IMMEDIATELY append to LESSONS_LEARNED.md
- What Claude tried that didn't work -> "Never Do This"
- What user suggested that worked -> "Always Do This"
- Add entry to Change Log

### Trigger 3: Multiple deployment/build failures in a row
**Action:** STOP. Do comprehensive research FIRST.
- WebSearch for best practices
- Read official documentation
- Understand ALL potential issues before fixing one at a time
