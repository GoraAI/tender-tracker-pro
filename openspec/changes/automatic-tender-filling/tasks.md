# Tasks: Automatic Tender Filling with Qualification Threshold and To-Do List

## Backend Tasks

### Phase 1: Infrastructure Setup (Week 1-2)

- [ ] **TASK 1.1: Wire TenderExtractor to Pipeline**
  - File: `src/scrapers/pipeline_service.py`
  - Modify: `run_extract()` method (line 351)
  - Replace: pdfplumber raw text extraction with TenderExtractor structured extraction
  - Add: Error handling for extraction failures (fall back to pdfplumber)
  - Acceptance: `pytest tests/scrapers/test_pipeline_service.py::test_run_extract_with_tender_extractor`

- [ ] **TASK 1.2: Create Gemma API Endpoint for PDF Extraction**
  - New file: `src/api/routers/gemma.py`
  - Endpoint: `POST /api/v1/gemma/extract`
  - Body: `{pdf_base64: str, extraction_prompt: str}`
  - Response: `{extracted_data: dict, confidence: float}`
  - Call Android Gemma via WebSocket or HTTP endpoint (device must be running)
  - Acceptance: Manual test with curl, PDF uploaded → structured JSON returned

- [ ] **TASK 1.3: Create Database Models**
  - New file: `src/models/tender_application.py`
  - Models: `TenderApplication`, `AnswerLibrary`, `DocumentLibrary`, `ApplicationTemplate`
  - Fields: See proposal.md Technical Approach section
  - Acceptance: `alembic revision --autogenerate -m "Add application models"` + `alembic upgrade head` succeeds

- [ ] **TASK 1.4: Create CRUD Repositories**
  - New files:
    - `src/repositories/application_repository.py`
    - `src/repositories/answer_library_repository.py`
    - `src/repositories/document_library_repository.py`
  - Methods: `create()`, `get_by_id()`, `update()`, `delete()`, `list_by_user()`
  - Acceptance: Unit tests for each repository (CRUD operations)

- [ ] **TASK 1.5: Build QualificationService**
  - New file: `src/services/qualification_service.py`
  - Method: `evaluate_tender(tender_id: str, user_id: str) -> QualificationResult`
  - Logic:
    - Get tender score from Score table
    - Get user criteria from UserCriteria table
    - Check: score ≥ 70
    - Check: 2+ capabilities match (keyword matching in requirements_summary)
    - Check: value in user's value range
    - Check: deadline ≥ 7 days from now
  - Return: `{qualifies: bool, score: int, reasons: [str], confidence: float}`
  - Acceptance: `pytest tests/services/test_qualification_service.py` (5 test cases: qualifies, low score, no capabilities, tight deadline, value mismatch)

- [ ] **TASK 1.6: Build ApplicationAssemblyService**
  - New file: `src/services/application_assembly_service.py`
  - Method: `generate_draft(tender_id: str, user_id: str) -> TenderApplication`
  - Logic:
    - Load: CompanyProfile, AnswerLibrary, DocumentLibrary
    - Load: ApplicationTemplate for tender source (e.g., "joburg")
    - Map template fields to available data:
      - `company_name` → `CompanyProfile.legal_name`
      - `capabilities` → `CompanyProfile.capabilities` (filter by tender requirements)
      - `company_overview` → `AnswerLibrary.find(category="company_overview")`
      - `tax_clearance` → `DocumentLibrary.find(type="tax_clearance")`
    - Mark fields: `{field_name: value, auto_filled: true/false}`
    - Identify missing required fields
  - Return: `TenderApplication` with status=draft
  - Acceptance: `pytest tests/services/test_application_assembly_service.py` (test auto-fill, test missing fields)

- [ ] **TASK 1.7: Build ToDoGeneratorService**
  - New file: `src/services/todo_generator_service.py`
  - Method: `generate_todos(tender_id: str, application: TenderApplication) -> List[ToDoItem]`
  - Logic:
    - Parse tender requirements (from TenderExtractor.requirements_summary)
    - Identify missing required fields → 🔴 High priority to-do
    - Identify auto-filled fields → 🟡 Medium priority review to-do
    - Check if pricing required (has line_items) → 🔴 High priority to-do
    - Check if documents missing → 🔴 High priority to-do
    - Generate standard checklist → 🟢 Low priority to-do
  - Prioritize: High → Medium → Low
  - Return: JSON array of to-do items (see proposal.md for schema)
  - Acceptance: `pytest tests/services/test_todo_generator_service.py` (test all item types, test prioritization)

- [ ] **TASK 1.8: Create API Endpoints for Qualification**
  - New file: `src/api/routers/qualification.py`
  - Endpoint: `GET /api/v1/tenders/{tender_id}/qualification`
  - Query param: `user_id` (from auth token)
  - Response: `QualificationResult` (from QualificationService)
  - Acceptance: Manual test with Postman/curl → qualifying tender returns 200 + reasons

- [ ] **TASK 1.9: Create API Endpoints for Applications**
  - New file: `src/api/routers/applications.py`
  - Endpoints:
    - `POST /api/v1/applications` (create draft)
    - `GET /api/v1/applications/{id}`
    - `PUT /api/v1/applications/{id}` (update fields)
    - `DELETE /api/v1/applications/{id}`
    - `POST /api/v1/applications/{id}/submit` (mark submitted)
  - Auth: Require JWT token, verify user owns application
  - Acceptance: Integration tests for all endpoints (create → get → update → submit → delete)

- [ ] **TASK 1.10: Create API Endpoints for Answer Library**
  - New file: `src/api/routers/answer_library.py`
  - Endpoints:
    - `GET /api/v1/answer-library` (list user's answers)
    - `POST /api/v1/answer-library` (create answer)
    - `PUT /api/v1/answer-library/{id}` (update)
    - `DELETE /api/v1/answer-library/{id}`
  - Acceptance: Integration tests (CRUD)

- [ ] **TASK 1.11: Create API Endpoints for Document Library**
  - New file: `src/api/routers/document_library.py`
  - Endpoints:
    - `GET /api/v1/document-library` (list user's documents)
    - `POST /api/v1/document-library` (upload - stores metadata, file on device)
    - `DELETE /api/v1/document-library/{id}`
  - Note: Files stored locally on Android, only metadata synced to backend
  - Acceptance: Integration tests (list, create metadata, delete)

- [ ] **TASK 1.12: Seed Application Template for Joburg**
  - New file: `scripts/seed_joburg_template.py`
  - Create: `ApplicationTemplate` for source_id="joburg"
  - Fields: Define JSON schema for typical Joburg tender (company details, capabilities, pricing, documents)
  - Run: `python scripts/seed_joburg_template.py`
  - Acceptance: Template exists in database, can be queried via API

---

## Android Tasks

### Phase 2: UI Implementation (Week 3-4)

- [ ] **TASK 2.1: Add Qualification Badge to TenderListScreen**
  - File: `android/app/.../ui/screens/tenders/TenderListScreen.kt`
  - Add: Badge overlay on tender card
  - Logic: If `tender.score >= 70` → "⚡ Auto-Fill Ready" (green), else "✋ Manual Only" (gray)
  - API call: `GET /tenders/{id}/qualification` (cache result)
  - Acceptance: Visual screenshot test (qualifying vs non-qualifying tender)

- [ ] **TASK 2.2: Add "Start Application" Button to TenderDetailScreen**
  - File: `android/app/.../ui/screens/tenders/TenderDetailScreen.kt`
  - Add: Button below score card: "Start Application (Auto-Fill)"
  - Show: "WHY Qualified" section (4 factors from API)
  - OnClick: Navigate to ApplicationPreviewScreen
  - Acceptance: Tapping button navigates to ApplicationPreviewScreen with loading state

- [ ] **TASK 2.3: Create AutoFillSettingsScreen**
  - New file: `android/app/.../ui/screens/settings/AutoFillSettingsScreen.kt`
  - UI:
    - Slider: Minimum Score (0-100, default 70)
    - Slider: Minimum Capabilities Match (0-10, default 2)
    - Slider: Minimum Deadline Days (0-30, default 7)
    - Toggle: Value Range Check (default ON)
  - Save: To `UserCriteria` or new `AutoFillSettings` table
  - Acceptance: Changing threshold updates qualification results (visual test)

- [ ] **TASK 2.4: Create ApplicationPreviewScreen**
  - New file: `android/app/.../ui/screens/applications/ApplicationPreviewScreen.kt`
  - UI:
    - Top: Progress bar "X/Y tasks complete (Z%)"
    - Sections (cards):
      - Company Details (auto-filled, editable)
      - Capabilities (auto-filled, editable)
      - Technical Responses (to-do items)
      - Pricing (to-do item if line_items exist)
      - Documents (auto-filled, attachable)
    - Bottom: Button "View To-Do List (X remaining)"
  - API: `POST /applications` on load (generate draft)
  - State: ApplicationPreviewViewModel with StateFlow<ApplicationUiState>
  - Acceptance: Screen loads with auto-filled data, progress bar updates, edits save to backend

- [ ] **TASK 2.5: Create ToDoListWidget**
  - New file: `android/app/.../ui/components/ToDoListWidget.kt`
  - UI:
    - Grouped by priority (🔴 High, 🟡 Medium, 🟢 Low)
    - Each item: Checkbox, title, section reference
    - Tap item → Navigate to ToDoDetailScreen
  - State: List of to-do items from `tender_applications.to_do_items` (JSON)
  - Acceptance: Widget displays 3 priority groups, tapping navigates to detail

- [ ] **TASK 2.6: Create ToDoDetailScreen**
  - New file: `android/app/.../ui/screens/applications/ToDoDetailScreen.kt`
  - UI:
    - Header: To-do title, priority badge, section reference
    - Body: Description (e.g., "Answer Section 3.2 - Technical Approach")
    - Actions:
      - Button: "Generate Response" (if technical question) → calls Gemma
      - Button: "Attach Document" (if document item) → file picker
      - Checkbox: "Mark Complete"
    - AI Response Preview: If generated, show "✨ AI-Generated" badge + editable text
  - Gemma Integration: Call `GemmaResponseGenerator.generateTechnicalResponse()`
  - Acceptance: "Generate Response" produces AI text, user can edit, marking complete updates progress

- [ ] **TASK 2.7: Create GemmaResponseGenerator**
  - New file: `android/app/.../services/GemmaResponseGenerator.kt`
  - Method: `suspend fun generateTechnicalResponse(question: String, tenderContext: String, companyProfile: CompanyProfile): String`
  - Prompt template:
    ```
    You are assisting with a tender application.

    Company: ${companyProfile.legal_name}
    Capabilities: ${companyProfile.capabilities.joinToString()}

    Tender Context:
    $tenderContext

    Question:
    $question

    Provide a professional, detailed response (200-500 words) demonstrating our capabilities.
    ```
  - Use: `gemmaInferenceModel.generateResponse(prompt)`
  - Acceptance: Unit test with mock Gemma (returns non-empty string)

- [ ] **TASK 2.8: Create QuoteBuilderScreen**
  - New file: `android/app/.../ui/screens/applications/QuoteBuilderScreen.kt`
  - UI:
    - Table: Item No, Description, Unit, Qty, Unit Cost, Total
    - Input fields: Unit Cost (numeric keyboard)
    - Auto-calculate: Total = Qty × Unit Cost
    - Bottom: Grand Total, "Save Draft", "Mark Complete"
  - State: `List<LineItem>` from tender's `line_items` (if extracted)
  - Save: To `tender_applications.pricing_data` (JSON)
  - Acceptance: Entering unit costs updates totals, saving marks to-do complete

- [ ] **TASK 2.9: Create AnswerLibraryScreen**
  - New file: `android/app/.../ui/screens/library/AnswerLibraryScreen.kt`
  - UI:
    - List: Grouped by category (Company Overview, Past Projects, Technical, Compliance)
    - Card: Question + Answer preview (first 100 chars)
    - Actions: Edit, Delete
    - FAB: "Add Answer"
  - Dialog: Add/Edit Answer (category dropdown, question text, answer multiline)
  - API: `GET /answer-library`, `POST /answer-library`, `PUT /answer-library/{id}`, `DELETE /answer-library/{id}`
  - Acceptance: CRUD operations work, answers sync to backend

- [ ] **TASK 2.10: Create DocumentLibraryScreen**
  - New file: `android/app/.../ui/screens/library/DocumentLibraryScreen.kt`
  - UI:
    - Grid: Document cards (icon, type, expiry date)
    - Badge: "⚠️ Expiring Soon" if < 30 days
    - Actions: View, Replace, Delete
    - FAB: "Upload Document"
  - File picker: Select PDF/image from device
  - Storage: Local file path (e.g., `/data/user/0/.../files/documents/{filename}`)
  - API: `POST /document-library` (upload metadata only, file stays local)
  - Acceptance: Upload document → appears in grid, expiry warning shows correctly

- [ ] **TASK 2.11: Create ApplicationSubmissionScreen**
  - New file: `android/app/.../ui/screens/applications/ApplicationSubmissionScreen.kt`
  - UI:
    - Checklist:
      - ✓ All required fields filled
      - ✓ All documents attached
      - ✓ Pricing complete
      - ✓ All to-do items checked
    - Button: "Download PDF" (generate locally)
    - Button: "Mark as Submitted"
  - PDF Generation: Use Android `PdfDocument` API
    - Sections: Company Details, Capabilities, Technical Responses, Pricing, Documents List
    - Save to: `/storage/emulated/0/Download/TenderApplication_{reference}.pdf`
  - API: `POST /applications/{id}/submit` (update status)
  - Acceptance: PDF generated with all content, file saved to Downloads, status updated

- [ ] **TASK 2.12: Update Navigation**
  - Files: `Screen.kt`, `TenderTrackerNavHost.kt`
  - Add routes:
    - `ApplicationPreview(tenderId: String)`
    - `ToDoDetail(applicationId: String, todoId: String)`
    - `QuoteBuilder(applicationId: String)`
    - `AnswerLibrary`
    - `DocumentLibrary`
    - `ApplicationSubmission(applicationId: String)`
    - `AutoFillSettings`
  - Link: Settings → Auto-Fill Settings
  - Acceptance: All screens navigable, back button works

---

## Testing Tasks

### Phase 3: Testing & Validation (Week 5-6)

- [ ] **TASK 3.1: Backend Unit Tests**
  - Test files:
    - `tests/services/test_qualification_service.py` (5 test cases)
    - `tests/services/test_application_assembly_service.py` (3 test cases)
    - `tests/services/test_todo_generator_service.py` (6 test cases)
  - Coverage: >80% for new services
  - Acceptance: `pytest tests/ --cov=src/services` passes with >80% coverage

- [ ] **TASK 3.2: Backend Integration Tests**
  - Test files:
    - `tests/api/test_qualification.py` (GET /tenders/{id}/qualification)
    - `tests/api/test_applications.py` (CRUD endpoints)
    - `tests/api/test_answer_library.py` (CRUD endpoints)
    - `tests/api/test_document_library.py` (CRUD endpoints)
  - Acceptance: `pytest tests/api/` passes (all endpoints return 200/201)

- [ ] **TASK 3.3: Android Unit Tests**
  - Test files:
    - `ApplicationPreviewViewModelTest.kt` (auto-fill loading, edit field)
    - `ToDoListViewModelTest.kt` (mark complete, progress calculation)
    - `GemmaResponseGeneratorTest.kt` (prompt formatting, response parsing)
    - `QuoteBuilderViewModelTest.kt` (total calculation, save draft)
  - Acceptance: `./gradlew test` passes (all ViewModel tests green)

- [ ] **TASK 3.4: End-to-End Flow Test (Manual)**
  - Scenario 1: Qualifying Tender → Auto-Fill → Complete To-Dos → Submit
    1. Browse tenders → see "⚡ Auto-Fill Ready" badge
    2. Tap tender → see "WHY Qualified" explanation
    3. Tap "Start Application" → draft generated in <3s
    4. Review auto-filled content → edit company name
    5. View to-do list → 5 items (3 high, 2 medium)
    6. Generate AI response for technical question → review, accept
    7. Enter pricing → totals calculated correctly
    8. Upload B-BBEE cert → to-do auto-checked
    9. Download PDF → file saved to Downloads, contains all data
    10. Mark as submitted → status updated
  - Acceptance: All steps complete without errors, PDF readable

- [ ] **TASK 3.5: End-to-End Flow Test (Non-Qualifying Tender)**
  - Scenario 2: Non-Qualifying Tender → Manual Only
    1. Browse tenders → see "✋ Manual Only" badge
    2. Tap tender → see "WHY NOT Qualified" (score 55, below threshold)
    3. No "Start Application" button shown
    4. User can still view PDF, use chat
  - Acceptance: Non-qualifying tender blocked from auto-fill

- [ ] **TASK 3.6: Visual Regression Tests**
  - Screenshot tests for:
    - TenderListScreen (with qualification badges)
    - TenderDetailScreen ("WHY Qualified" section)
    - ApplicationPreviewScreen (progress bar, sections)
    - ToDoListWidget (priority groups)
    - QuoteBuilderScreen (table layout)
    - ApplicationSubmissionScreen (checklist)
  - Tool: Paparazzi or Compose Preview screenshots
  - Acceptance: Screenshots match approved baseline

- [ ] **TASK 3.7: Maestro Flow Tests**
  - Flow 1: `auto_fill_flow.yaml`
    - Launch app → navigate to tender → start application → complete to-do → submit
  - Flow 2: `answer_library_crud.yaml`
    - Navigate to Answer Library → add answer → edit answer → delete answer
  - Acceptance: `maestro test flows/` passes (both flows green)

- [ ] **TASK 3.8: Performance Testing**
  - Metrics:
    - Auto-fill generation time: <3 seconds (measure with Systrace)
    - Gemma response time: <10 seconds (acceptable for on-device LLM)
    - PDF generation time: <5 seconds for 10-page application
    - App memory usage: <300MB during application workflow
  - Tool: Android Profiler
  - Acceptance: All metrics within targets on mid-range device (4GB RAM)

- [ ] **TASK 3.9: Device Compatibility Testing**
  - Test on:
    - Android 10 (API 29) - minimum supported
    - Android 13 (API 33) - common
    - Android 14 (API 34) - latest
  - Devices: Emulator + 1 physical device
  - Acceptance: App works on all 3 API levels without crashes

- [ ] **TASK 3.10: Edge Case Testing**
  - Test cases:
    - Tender with no line_items (no pricing to-do)
    - Tender with missing deadline (qualification fails gracefully)
    - Application with 0 to-do items (all auto-filled)
    - Document expired (shows warning, blocks submission)
    - Gemma fails to generate response (shows error, allows retry)
    - Network offline (local data still accessible)
  - Acceptance: All edge cases handled gracefully (no crashes)

---

## Documentation Tasks

- [ ] **TASK 4.1: Update README**
  - Section: "Automatic Tender Filling"
  - Document: Qualification threshold, auto-fill process, to-do list, PDF generation
  - Acceptance: README.md updated with new feature description

- [ ] **TASK 4.2: Update LESSONS_LEARNED.md**
  - Add discoveries:
    - TenderExtractor wiring approach
    - Gemma prompt engineering for technical responses
    - PDF generation library choice
    - To-do priority algorithm
  - Acceptance: LESSONS_LEARNED.md has new "Automatic Tender Filling" section

- [ ] **TASK 4.3: Create User Guide**
  - New file: `docs/USER_GUIDE_AUTO_FILL.md`
  - Sections:
    - How to configure threshold
    - How to start auto-fill
    - How to use Gemma for responses
    - How to complete to-do items
    - How to download PDF
  - Acceptance: User guide complete with screenshots

- [ ] **TASK 4.4: Update API Documentation**
  - Add endpoint docs:
    - `/api/v1/tenders/{id}/qualification`
    - `/api/v1/applications/*`
    - `/api/v1/answer-library/*`
    - `/api/v1/document-library/*`
  - Tool: FastAPI auto-docs (Swagger UI)
  - Acceptance: All new endpoints documented at `/docs`

- [ ] **TASK 4.5: Update Architecture Diagram**
  - File: `ARCHITECTURE.md`
  - Add: Application Assembly Service, Qualification Service, To-Do Generator Service
  - Update: Data flow diagram with auto-fill pipeline
  - Acceptance: ARCHITECTURE.md reflects new components

---

## Deployment Tasks

- [ ] **TASK 5.1: Database Migration (Production)**
  - Run: `alembic upgrade head` on Railway
  - Verify: New tables exist (tender_applications, answer_library, document_library, application_templates)
  - Acceptance: Migration succeeds without errors

- [ ] **TASK 5.2: Seed Joburg Template (Production)**
  - Run: `python scripts/seed_joburg_template.py` on Railway
  - Verify: Template queryable via API
  - Acceptance: GET `/api/v1/application-templates/joburg` returns 200

- [ ] **TASK 5.3: Backend Deployment**
  - Push to main branch
  - Verify: Railway auto-deploys new services
  - Test: Qualification endpoint works in production
  - Acceptance: Production API returns 200 for all new endpoints

- [ ] **TASK 5.4: Android APK Build**
  - Push to GitHub → CI builds APK
  - Download: `gh run download <run-id> -n app-debug`
  - Install: `adb install -r app-debug.apk`
  - Acceptance: APK installs successfully, no crashes on launch

- [ ] **TASK 5.5: Production Smoke Test**
  - Test in production:
    1. Browse tenders → qualification badges show
    2. Start application → draft generates
    3. Complete to-do → progress updates
    4. Download PDF → file valid
  - Acceptance: All steps work in production environment

---

## Task Summary

**Total Tasks**: 56
- Backend: 12 tasks
- Android: 12 tasks
- Testing: 10 tasks
- Documentation: 5 tasks
- Deployment: 5 tasks

**Estimated Effort**: 6-8 weeks (40-50 working days)
- Week 1-2: Backend (12 tasks)
- Week 3-4: Android (12 tasks)
- Week 5-6: Testing (10 tasks)
- Week 7-8: Documentation + Deployment (10 tasks)

**Critical Path**:
1. Wire TenderExtractor (TASK 1.1)
2. Create database models (TASK 1.3)
3. Build QualificationService (TASK 1.5)
4. Build ApplicationAssemblyService (TASK 1.6)
5. Build ToDoGeneratorService (TASK 1.7)
6. Create ApplicationPreviewScreen (TASK 2.4)
7. Create ToDoListWidget (TASK 2.5)
8. End-to-end testing (TASK 3.4)

---

**Next**: Review tasks → Approve proposal → `/openspec-apply automatic-tender-filling` to start implementation
