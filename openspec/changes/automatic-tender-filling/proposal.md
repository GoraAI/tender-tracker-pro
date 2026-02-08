# Proposal: Automatic Tender Filling with Qualification Threshold and To-Do List

## Summary
Implement automatic tender application filling for qualifying tenders (score ≥70) with AI-assisted content generation, intelligent to-do list for quote finalization, and local document management.

## Motivation

**Current Pain Point**: Users manually apply to tenders outside the app after viewing scores. This involves:
- Downloading PDFs manually
- Re-typing company details for every tender
- Manually answering repetitive questions
- No tracking of application progress
- 2+ hours per tender application

**Solution**: Auto-fill tender applications using user's company profile and on-device Gemma LLM to:
- Reduce application time from 2+ hours to <30 minutes
- Eliminate repetitive data entry
- Generate intelligent to-do lists for missing items
- Use AI to draft technical responses
- Track application completion progress

**Business Impact**:
- Users can apply to 3-5x more tenders in same time
- Higher quality applications (AI-assisted responses)
- Better tender tracking and organization
- Competitive advantage in tender submissions

## Scope

### ✅ INCLUDED in MVP

**Backend (Python/FastAPI):**
- Wire TenderExtractor to extraction pipeline (use Android Gemma via API)
- New database models: `tender_applications`, `answer_library`, `document_library`, `application_templates`
- QualificationService: Evaluate if tender meets threshold (score ≥70, 2+ capabilities, 7+ days deadline, value in range)
- ApplicationAssemblyService: Map tender fields to user data, generate draft application
- ToDoGeneratorService: Analyze tender requirements, generate prioritized to-do items
- AnswerLibraryService: CRUD for reusable answer templates
- DocumentLibraryService: CRUD for locally stored documents (B-BBEE cert, tax clearance, etc.)
- API endpoints: `/applications/*`, `/qualifications/*`, `/answer-library/*`, `/document-library/*`

**Android (Kotlin/Compose):**
- AutoFillSettingsScreen: Configure qualification threshold
- ApplicationPreviewScreen: Show auto-filled application with editable fields
- ToDoListWidget: Display prioritized to-do items with progress bar
- ToDoDetailScreen: Show to-do details, mark complete, attach documents
- QuoteBuilderScreen: Enter pricing with automatic total calculations
- AnswerLibraryScreen: Manage reusable answer templates
- DocumentLibraryScreen: Upload/manage common documents (local storage)
- ApplicationSubmissionScreen: Final review, generate PDF locally
- GemmaService integration: Use on-device Gemma to generate technical responses

**Features:**
- Qualification threshold engine (configurable by user)
- Auto-fill 5 field types: company details, capabilities, standard answers, past projects, documents
- AI-generated technical responses (using on-device Gemma)
- Intelligent to-do list generation (4 types: missing fields, review, pricing, documents)
- Simple quote builder (manual entry, basic arithmetic)
- Progress tracking (X/Y tasks complete)
- Local document storage (sync to backend on submission)
- PDF generation locally (no auto-submit to portal)
- Support for 1 tender source: Johannesburg (City of Joburg)

### ❌ NOT INCLUDED in MVP (Future Phase 2)

- Auto-submit to government portals (Playwright automation)
- Complex quote calculations (markup, overheads, competitive pricing)
- Cost database integration for unit prices
- Multi-source support (Cape Town, Durban, national tenders)
- Answer library "smart suggestions" (AI-recommended answers)
- Document auto-attachment based on tender requirements
- Cloud document storage (S3/R2)
- Desktop/web version of application builder

## Acceptance Criteria

### Qualification Engine
- [ ] System evaluates tenders against threshold: score ≥70, 2+ capabilities match, 7+ days deadline, value in range
- [ ] User can configure threshold in Settings → Auto-Fill Settings
- [ ] TenderListScreen shows "⚡ Auto-Fill Ready" or "✋ Manual Only" badge
- [ ] TenderDetailScreen shows "WHY Qualified" explanation (4 factors: score, capabilities, value, deadline)
- [ ] TenderDetailScreen shows "WHY NOT Qualified" explanation for non-qualifying tenders
- [ ] API endpoint `GET /tenders/{id}/qualification` returns qualification result

### Auto-Fill Engine
- [ ] System auto-fills 5 field categories: company details, capabilities, standard answers, past projects, compliance documents
- [ ] Auto-filled fields sourced from: CompanyProfile, answer_library, document_library
- [ ] ApplicationPreviewScreen displays auto-filled content (all fields editable)
- [ ] User can edit any auto-filled field before submission
- [ ] System tracks which fields were auto-filled vs manually edited (analytics)
- [ ] Auto-fill completes in <3 seconds for qualifying tenders

### Gemma AI Technical Responses
- [ ] User can tap "Generate Response" for technical questions in to-do items
- [ ] On-device Gemma generates draft responses based on tender requirements
- [ ] AI responses shown with "✨ AI-Generated" badge
- [ ] User reviews and edits AI responses before accepting
- [ ] System includes disclaimer: "AI-generated content - please review carefully"

### To-Do List Generator
- [ ] System generates 4 to-do item types: missing fields, review auto-filled content, pricing, documents, technical responses
- [ ] To-do items prioritized: 🔴 High (blockers) → 🟡 Medium (important) → 🟢 Low (optional)
- [ ] ToDoListWidget shows progress: "X/Y tasks complete (Z%)"
- [ ] User can manually check off to-do items
- [ ] System auto-checks to-do items when dependency fulfilled (e.g., document uploaded → item checked)
- [ ] To-do items linked to tender sections (e.g., "Answer Section 3.2 - Technical Approach")

### Quote Builder
- [ ] QuoteBuilderScreen displays tender Bill of Quantities (if extracted by TenderExtractor)
- [ ] User manually enters unit costs for line items
- [ ] System calculates row totals and grand total (basic arithmetic: quantity × unit_cost = total)
- [ ] Pricing saved to `tender_applications.pricing_data` (JSON format)
- [ ] Completing pricing auto-checks "Prepare Pricing" to-do item
- [ ] Draft pricing can be saved and resumed later

### Answer Library Management
- [ ] User can create reusable answer templates (category, question, answer_text)
- [ ] Answer categories: "company_overview", "past_projects", "technical_capabilities", "compliance"
- [ ] AnswerLibraryScreen shows list of saved answers (searchable)
- [ ] User can edit/delete answer templates
- [ ] Auto-fill uses answer library to populate standard questions
- [ ] System suggests answer templates based on tender keywords

### Document Library Management
- [ ] User can upload documents locally: B-BBEE cert, tax clearance, company registration, insurance, references
- [ ] Document types: "company_registration", "tax_clearance", "bbee_certificate", "insurance", "reference_letter"
- [ ] DocumentLibraryScreen shows list of uploaded documents with expiry dates
- [ ] System warns user when documents expire (⚠️ Expiring Soon badge)
- [ ] Auto-fill attaches relevant documents to application
- [ ] Documents stored locally on device (not cloud)

### Application Submission
- [ ] ApplicationSubmissionScreen shows final review checklist:
  - ✓ All required fields filled
  - ✓ All required documents attached
  - ✓ Pricing complete
  - ✓ All to-do items checked
- [ ] User can download application as PDF (locally generated)
- [ ] Application status tracked: Draft → Submitted
- [ ] System saves submission timestamp
- [ ] User manually submits PDF to government portal (no auto-submit in MVP)

### Non-Functional Requirements
- [ ] Auto-fill generation completes in <3 seconds
- [ ] To-do list updates in real-time (no page refresh)
- [ ] All data stored locally first (offline-first architecture)
- [ ] Background sync to backend on connectivity
- [ ] User data privacy: answer library and documents NOT shared across users
- [ ] PDF generation works offline (no backend required)
- [ ] UI responsive on devices with 4GB+ RAM (minimum spec)

## Technical Approach

### Phase 1: Backend Infrastructure (Week 1-2)

**1. Wire TenderExtractor to Pipeline**
- Modify `src/scrapers/pipeline_service.py:run_extract()`
- Replace pdfplumber raw text with TenderExtractor structured extraction
- Call Android Gemma via API endpoint for LLM parsing
- Extract fields: title, reference, deadline, estimated_value, requirements_summary, contact_name, contact_email, line_items (Bill of Quantities)
- Store extraction confidence score

**2. Database Models**
```python
# New models in src/models/
class TenderApplication(Base):
    id, tender_id, user_id, status (draft/submitted)
    auto_filled_fields (JSON), pricing_data (JSON), to_do_items (JSON array)
    created_at, submitted_at

class AnswerLibrary(Base):
    id, user_id, category, question, answer_text
    created_at, updated_at

class DocumentLibrary(Base):
    id, user_id, document_type, file_path (local), expires_at
    uploaded_at

class ApplicationTemplate(Base):
    id, source_id (e.g., "joburg"), template_name
    fields (JSON schema), section_order (JSON array)
```

**3. New Services**
```python
# src/services/qualification_service.py
def evaluate_tender(tender_id, user_id) -> QualificationResult:
    # Check: score ≥70, 2+ capabilities match, 7+ days deadline, value in range
    # Return: qualifies (bool), reasons (list), confidence (float)

# src/services/application_assembly_service.py
def generate_draft(tender_id, user_id) -> TenderApplication:
    # Map tender fields → CompanyProfile/AnswerLibrary/DocumentLibrary
    # Mark fields as auto-filled
    # Identify missing required fields

# src/services/todo_generator_service.py
def generate_todos(tender_id, application_id) -> List[ToDoItem]:
    # Analyze tender requirements
    # Compare with available data
    # Generate prioritized to-do items (missing fields, review, pricing, documents)
```

**4. API Endpoints**
```
POST   /api/v1/gemma/extract         # Call Android Gemma for PDF extraction
GET    /api/v1/tenders/{id}/qualification
POST   /api/v1/applications
GET    /api/v1/applications/{id}
PUT    /api/v1/applications/{id}
DELETE /api/v1/applications/{id}
POST   /api/v1/applications/{id}/submit
GET    /api/v1/answer-library
POST   /api/v1/answer-library
PUT    /api/v1/answer-library/{id}
DELETE /api/v1/answer-library/{id}
GET    /api/v1/document-library
POST   /api/v1/document-library
DELETE /api/v1/document-library/{id}
```

### Phase 2: Android UI (Week 3-4)

**1. Settings Integration**
```kotlin
// AutoFillSettingsScreen.kt
- Threshold sliders: min score (70), min capabilities (2), min deadline days (7)
- Value range selection
- Preview qualification explanation
```

**2. Application Workflow Screens**
```kotlin
// ApplicationPreviewScreen.kt
- Section cards: Company Details, Capabilities, Technical Responses, Pricing, Documents
- Edit buttons for each field
- "View To-Do List" button
- Progress bar at top

// ToDoListWidget.kt
- Grouped by priority (High/Medium/Low)
- Progress: "X/Y complete (Z%)"
- Tap item → ToDoDetailScreen

// ToDoDetailScreen.kt
- Item description, section reference
- "Generate Response" button (for technical questions - calls Gemma)
- File attachment picker (for document items)
- "Mark Complete" checkbox

// QuoteBuilderScreen.kt
- Table: Item, Description, Unit, Qty, Unit Cost, Total
- Manual entry fields
- Auto-calculate totals
- Save draft button

// AnswerLibraryScreen.kt
- List of saved answers by category
- Add/Edit/Delete
- Search/filter

// DocumentLibraryScreen.kt
- Grid of document cards
- Upload button
- Expiry date badge
- Delete/replace actions

// ApplicationSubmissionScreen.kt
- Final checklist
- "Download PDF" button
- "Mark as Submitted" button
```

**3. Gemma Integration for Response Generation**
```kotlin
// GemmaResponseGenerator.kt
suspend fun generateTechnicalResponse(
    question: String,
    tenderContext: String,
    companyProfile: CompanyProfile
): String {
    val prompt = """
        You are assisting with a tender application.

        Company Profile:
        ${companyProfile.toPrompt()}

        Tender Context:
        $tenderContext

        Question to Answer:
        $question

        Provide a professional, detailed response (200-500 words).
    """
    return gemmaInferenceModel.generateResponse(prompt)
}
```

### Phase 3: Integration & Testing (Week 5-6)

**1. End-to-End Flow Testing**
- User browses tenders → sees qualification badge
- User taps "Start Application" → draft generated
- User reviews auto-filled content → edits fields
- User taps "Generate Response" for technical question → Gemma generates draft
- User completes to-do items → progress updates
- User enters pricing → totals calculated
- User downloads PDF → file saved locally

**2. Unit Testing**
```python
# Backend tests
tests/services/test_qualification_service.py
tests/services/test_application_assembly_service.py
tests/services/test_todo_generator_service.py

# Android tests
ApplicationPreviewViewModelTest.kt
ToDoListViewModelTest.kt
GemmaResponseGeneratorTest.kt
```

**3. Visual Verification**
- Screenshot tests for all new screens
- Maestro flows for application workflow
- Manual testing on physical device (Gemma requires on-device)

### Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│  TENDER DISCOVERY & SCORING (Existing)                      │
│  Playwright → Download PDF → Extract → Score                │
└───────────────────────┬─────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│  QUALIFICATION CHECK (NEW)                                   │
│  QualificationService.evaluate()                             │
│  → Check: score ≥70, capabilities, value, deadline          │
│  → Result: qualifies=true + reasons                          │
└───────────────────────┬─────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│  AUTO-FILL GENERATION (NEW)                                  │
│  ApplicationAssemblyService.generate_draft()                 │
│  → Load: CompanyProfile, AnswerLibrary, DocumentLibrary     │
│  → Fill: company_details, capabilities, standard_answers    │
│  → Identify: missing required fields                         │
│  → Save: TenderApplication (status=draft)                    │
└───────────────────────┬─────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│  TO-DO GENERATION (NEW)                                      │
│  ToDoGeneratorService.generate_todos()                       │
│  → Parse tender requirements (via TenderExtractor)           │
│  → Compare with auto-filled fields                           │
│  → Generate items: missing, review, pricing, documents       │
│  → Prioritize: High → Medium → Low                           │
└───────────────────────┬─────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│  USER INTERACTION (Android)                                  │
│  → Review auto-filled content                                │
│  → Generate AI responses (Gemma)                             │
│  → Enter pricing (QuoteBuilder)                              │
│  → Upload documents (DocumentLibrary)                        │
│  → Complete to-do items                                      │
└───────────────────────┬─────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│  SUBMISSION (NEW)                                            │
│  → Final validation (all required fields filled)             │
│  → Generate PDF locally (Android PDF library)                │
│  → Save to device storage                                    │
│  → Update status: draft → submitted                          │
│  → User manually uploads to government portal                │
└─────────────────────────────────────────────────────────────┘
```

## References

- **Existing Architecture**: See `/home/goraai/tender_tracker_pro/ARCHITECTURE.md`
- **Business Analysis**: `/tmp/claude-1000/.../scratchpad/business_analysis.md` (from analyst agent)
- **Related Models**:
  - `src/models/tender.py` (Tender, Score)
  - `src/models/user_criteria.py` (UserCriteria)
  - `src/models/company_profile.py` (CompanyProfile)
  - `android/domain/.../UserCriteria.kt`
- **Related Services**:
  - `src/services/relevance_scorer.py` (existing scoring)
  - `src/extraction/tender_extractor.py` (DEAD CODE - needs wiring)
  - `android/.../ScoringService.kt` (Gemma integration)
- **API Documentation**: FastAPI auto-docs at `/docs`
- **TenderExtractor Implementation**: `src/extraction/tender_extractor.py` (lines 1-150)

## Risk Mitigation

### Risk 1: TenderExtractor Accuracy
- **Risk**: Gemma may extract incorrect tender data (hallucinations)
- **Mitigation**:
  - Always show confidence score
  - Require user review of extracted data
  - Fall back to pdfplumber raw text if Gemma fails
  - Log extraction errors for monitoring

### Risk 2: Qualification Threshold Too Strict/Loose
- **Risk**: Threshold excludes good tenders OR includes bad tenders
- **Mitigation**:
  - Make threshold user-configurable
  - Provide "WHY" explanation for transparency
  - Allow user override ("Force Auto-Fill")
  - Track qualification accuracy over time (analytics)

### Risk 3: Gemma Response Quality
- **Risk**: AI-generated technical responses may be generic or incorrect
- **Mitigation**:
  - Always mark as "AI-Generated" with disclaimer
  - Require user review before accepting
  - Provide "Regenerate" button for multiple attempts
  - Save user edits to improve future prompts

### Risk 4: Complex Tender Forms
- **Risk**: Government tenders have 20-50 questions, hard to parse
- **Mitigation**:
  - Start with simple Joburg tenders (MVP)
  - Break into sections with progress tracking
  - Save drafts frequently (auto-save every 30s)
  - Prioritize to-do items by importance

### Risk 5: Document Expiry
- **Risk**: User submits application with expired documents (tax clearance, insurance)
- **Mitigation**:
  - Track expiry dates in DocumentLibrary
  - Show "⚠️ Expiring Soon" badge (30 days before)
  - Block submission if required document expired
  - Notification: "Tax clearance expires in 15 days"

## Success Metrics

### MVP Success Criteria (End of Week 8)
- [ ] **70%+ auto-fill rate**: 70% of qualifying tenders successfully auto-filled
- [ ] **Time savings**: Average application time <30 minutes (vs 2+ hours manual)
- [ ] **User satisfaction**: 80%+ find auto-fill helpful (survey)
- [ ] **To-do completion**: 90%+ of applications submitted with all items complete
- [ ] **Gemma response quality**: 70%+ of AI responses accepted with minor edits
- [ ] **System reliability**: 95%+ uptime for application generation
- [ ] **Device compatibility**: Works on 90%+ Android devices (4GB+ RAM)

### Phase 2 Goals (Future)
- Auto-submit to portals (Playwright): 50% of tenders support API submission
- Multi-source support: 3+ tender sources (Joburg, Cape Town, Durban)
- Advanced quote builder: Cost database integration, markup calculations
- Answer library suggestions: AI recommends best answers (80%+ acceptance)

## Timeline

### Week 1-2: Backend Infrastructure
- Day 1-2: Wire TenderExtractor, create Gemma API endpoint
- Day 3-4: Database models (migrations, CRUD)
- Day 5-7: QualificationService, ApplicationAssemblyService
- Day 8-10: ToDoGeneratorService, API endpoints

### Week 3-4: Android UI
- Day 1-2: AutoFillSettingsScreen, qualification badge in TenderList
- Day 3-5: ApplicationPreviewScreen, ToDoListWidget
- Day 6-7: QuoteBuilderScreen, ToDoDetailScreen
- Day 8-10: AnswerLibraryScreen, DocumentLibraryScreen, ApplicationSubmissionScreen

### Week 5-6: Integration & Testing
- Day 1-3: End-to-end flow testing, bug fixes
- Day 4-5: Unit tests (backend + Android)
- Day 6-7: Visual verification (screenshots, Maestro)
- Day 8-10: Polish, performance optimization, documentation

### Week 7-8: User Testing & Refinement
- Day 1-3: Internal testing with real tenders
- Day 4-5: User feedback collection, iteration
- Day 6-7: Final bug fixes, edge case handling
- Day 8-10: Release candidate build, deployment prep

**Total**: 6-8 weeks (flexible based on complexity discoveries)

## Open Questions

1. **Gemma API Rate Limiting**: Should we limit Gemma calls to prevent device resource exhaustion?
   - Suggestion: Max 5 AI generations per session, warn user if exceeded

2. **PDF Generation Library**: Which Android PDF library?
   - Options: iText (commercial license required), PDFBox (Apache 2.0), Android PdfDocument (built-in)
   - Recommendation: Android PdfDocument (no dependencies)

3. **Application Template Format**: How to represent tender form structure?
   - JSON schema with fields: `{name, type, required, section, validation_rules}`
   - Start with manual template for Joburg, automate extraction later

4. **Answer Library Sync**: Should answer library sync to backend for backup?
   - MVP: Local only (per Decision 5)
   - Phase 2: Optional cloud backup (user opt-in)

5. **To-Do Item Granularity**: How specific should to-do items be?
   - Example: "Answer Section 3.2" vs "Answer Question 3.2.1, 3.2.2, 3.2.3"
   - Recommendation: Section-level for MVP, question-level in Phase 2

---

**Next Steps**: Review this proposal → Approve → `/openspec-apply automatic-tender-filling` to implement

**Estimated Approval Time**: 15-30 minutes to review this document
