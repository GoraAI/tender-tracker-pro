# Spec Delta: Automatic Tender Filling with Qualification Threshold and To-Do List

This document describes what specifications will be ADDED, MODIFIED, or REMOVED by this change.

---

## ADDED

### application-workflow.md

```markdown
# Specification: Application Workflow

## Overview
The application workflow enables users to automatically fill tender applications for qualifying tenders using their company profile, answer library, and on-device Gemma LLM.

## Qualification System

### Threshold Criteria
A tender qualifies for auto-fill if ALL conditions are met:

1. **Score Threshold**: Score ≥ 70 (user-configurable, default 70)
2. **Capability Match**: ≥ 2 capabilities from user profile match tender requirements (keyword matching)
3. **Value Range**: Tender estimated value within user's configured range (min/max)
4. **Deadline Feasibility**: Tender deadline ≥ 7 days from now (user-configurable, default 7)

### Qualification Result Schema
```json
{
  "qualifies": true,
  "score": 85,
  "reasons": [
    "✓ Score: 85/100 (High relevance)",
    "✓ Capabilities: 3/4 match (Water Treatment, Pipe Supply, Maintenance)",
    "✓ Value: R500K (Within your range R100K-R1M)",
    "✓ Deadline: 14 days (Feasible)"
  ],
  "confidence": 0.9,
  "checked_at": "2026-02-07T10:30:00Z"
}
```

### Non-Qualifying Explanation
If tender does NOT qualify, explain WHY for each failed criterion:
```json
{
  "qualifies": false,
  "score": 55,
  "reasons": [
    "✗ Score: 55/100 (Below threshold of 70)",
    "✓ Capabilities: 1/4 match (only Maintenance)",
    "✗ Missing capabilities: Road Construction, Asphalt, Heavy Equipment",
    "✓ Value: R800K (In range)",
    "✗ Deadline: 4 days (Too tight - need 7+ days)"
  ],
  "confidence": 0.85,
  "suggestion": "Improve your capabilities or adjust threshold to include this tender"
}
```

## Auto-Fill Mapping

### Field Categories
| Category | Fields | Source | Auto-Fill? |
|----------|--------|--------|------------|
| **Company Details** | Legal name, Trading name, Registration number, VAT number, Physical address, Contact details | CompanyProfile | ✅ Yes |
| **Capabilities** | Services offered, Technical capabilities, Certifications | CompanyProfile.capabilities | ✅ Yes (filtered by tender requirements) |
| **Standard Responses** | Company overview, Years in business, Number of employees, B-BBEE level | AnswerLibrary | ✅ Yes |
| **Past Projects** | Project descriptions (general) | AnswerLibrary (category="past_projects") | ✅ Yes (requires user review) |
| **Compliance Docs** | Company registration, Tax clearance, Insurance, B-BBEE cert | DocumentLibrary | ✅ Yes (attachments) |
| **Pricing** | Bill of Quantities, Unit costs, Totals | User input | ❌ No (manual entry) |
| **Technical Responses** | Methodology, Project plan, Technical approach | Gemma-generated | 🤖 AI-assisted (requires user review) |

### Auto-Fill Algorithm
```python
def auto_fill_application(tender_id, user_id):
    # 1. Load data sources
    company = get_company_profile(user_id)
    answers = get_answer_library(user_id)
    documents = get_document_library(user_id)
    template = get_application_template(tender.source_id)  # e.g., "joburg"

    # 2. Map template fields to available data
    auto_filled_fields = {}
    for field in template.fields:
        if field.name == "company_name":
            auto_filled_fields[field.name] = {
                "value": company.legal_name,
                "auto_filled": True,
                "source": "CompanyProfile"
            }
        elif field.name == "capabilities":
            # Filter capabilities by tender requirements
            matching_caps = match_capabilities(company.capabilities, tender.requirements_summary)
            auto_filled_fields[field.name] = {
                "value": matching_caps,
                "auto_filled": True,
                "source": "CompanyProfile (filtered)"
            }
        elif field.name == "company_overview":
            answer = answers.find(category="company_overview")
            if answer:
                auto_filled_fields[field.name] = {
                    "value": answer.answer_text,
                    "auto_filled": True,
                    "source": "AnswerLibrary"
                }
        # ... continue for all fields

    # 3. Identify missing required fields
    missing_fields = [f for f in template.fields if f.required and f.name not in auto_filled_fields]

    # 4. Create draft application
    application = TenderApplication(
        tender_id=tender_id,
        user_id=user_id,
        status="draft",
        auto_filled_fields=auto_filled_fields,
        created_at=now()
    )

    return application, missing_fields
```

## To-Do List Generation

### To-Do Item Types
| Type | Priority | Description | Auto-Generated? |
|------|----------|-------------|-----------------|
| **missing_field** | 🔴 High | Required field not auto-filled | Yes |
| **review** | 🟡 Medium | Review auto-filled content | Yes |
| **pricing** | 🔴 High | Enter pricing/quote | Yes (if tender has line_items) |
| **document** | 🔴 High | Upload missing document | Yes (if required by tender) |
| **technical_response** | 🔴 High | Answer technical question | Yes (from tender requirements) |
| **final_check** | 🟢 Low | Final review checklist | Yes (standard) |

### To-Do Item Schema
```json
{
  "id": "uuid",
  "category": "technical_response",
  "priority": "high",
  "title": "Answer Technical Question (Section 3.2)",
  "description": "Provide detailed methodology for water treatment plant construction and maintenance.",
  "section_reference": "Section 3.2 - Technical Approach",
  "estimated_time": "30-45 minutes",
  "can_use_ai": true,
  "completed": false,
  "completed_at": null
}
```

### To-Do Generation Logic
```python
def generate_todos(tender, application):
    todos = []

    # 1. Missing required fields → High priority
    for field in template.required_fields:
        if field not in application.auto_filled_fields:
            todos.append({
                "category": "missing_field",
                "priority": "high",
                "title": f"Provide {field.display_name}",
                "description": f"Required field: {field.description}",
                "section_reference": field.section
            })

    # 2. Review auto-filled content → Medium priority
    for field, data in application.auto_filled_fields.items():
        if data["auto_filled"]:
            todos.append({
                "category": "review",
                "priority": "medium",
                "title": f"Review Auto-Filled: {field.display_name}",
                "description": f"Verify accuracy of {field.display_name} (source: {data['source']})"
            })

    # 3. Pricing → High priority (if tender has line_items)
    if tender.line_items:
        todos.append({
            "category": "pricing",
            "priority": "high",
            "title": "Prepare Pricing Schedule",
            "description": f"Enter unit costs for {len(tender.line_items)} line items",
            "section_reference": "Section 5 - Pricing",
            "estimated_time": "1-2 hours"
        })

    # 4. Technical responses → High priority
    technical_questions = extract_technical_questions(tender.requirements_summary)
    for question in technical_questions:
        todos.append({
            "category": "technical_response",
            "priority": "high",
            "title": f"Answer: {question.short_title}",
            "description": question.full_text,
            "section_reference": question.section,
            "can_use_ai": True,
            "estimated_time": "30-60 minutes"
        })

    # 5. Documents → High priority (if missing)
    required_docs = extract_required_documents(tender.requirements_summary)
    for doc_type in required_docs:
        if not has_document(user_id, doc_type):
            todos.append({
                "category": "document",
                "priority": "high",
                "title": f"Upload {doc_type.display_name}",
                "description": f"Required document: {doc_type.description}",
                "section_reference": doc_type.section
            })

    # 6. Final checklist → Low priority
    todos.append({
        "category": "final_check",
        "priority": "low",
        "title": "Final Review",
        "description": "Review entire application before submission"
    })

    # Sort by priority: High → Medium → Low
    return sorted(todos, key=lambda t: {"high": 0, "medium": 1, "low": 2}[t["priority"]])
```

## Gemma AI Integration

### Technical Response Generation
When user taps "Generate Response" for a technical question:

```python
def generate_technical_response(question, tender, company_profile):
    prompt = f"""You are assisting with a tender application for {company_profile.legal_name}.

Company Profile:
- Legal Name: {company_profile.legal_name}
- Capabilities: {', '.join(company_profile.capabilities)}
- B-BBEE Level: {company_profile.bbee_level}
- Years in Business: {company_profile.years_in_business}

Tender Context:
- Title: {tender.title}
- Reference: {tender.reference}
- Requirements: {tender.requirements_summary[:500]}...

Question to Answer:
{question}

Instructions:
- Provide a professional, detailed response (200-500 words)
- Demonstrate our company's capabilities and experience
- Use specific examples where possible
- Maintain formal business tone
- Focus on how we meet the tender requirements

Response:"""

    response = gemma_model.generate(prompt)

    return {
        "response_text": response,
        "generated_at": now(),
        "disclaimer": "AI-generated content - please review and edit before submission",
        "confidence": 0.8,
        "word_count": len(response.split())
    }
```

### AI Response UI Pattern
```
┌────────────────────────────────────────────┐
│ ✨ AI-Generated Response                   │
│ ⚠️  Please review carefully before using   │
├────────────────────────────────────────────┤
│ [AI response text here...]                 │
│ [editable text area]                       │
├────────────────────────────────────────────┤
│ Word count: 387 / 500 max                  │
│                                            │
│ [🔄 Regenerate]  [✓ Accept & Use]         │
└────────────────────────────────────────────┘
```

## Quote Builder

### Line Item Structure
```json
{
  "line_items": [
    {
      "item_no": "1",
      "description": "Pipe Supply - 100mm UPVC",
      "unit": "m",
      "quantity": 500,
      "unit_cost": null,  // User enters
      "total": null       // Auto-calculated
    },
    {
      "item_no": "2",
      "description": "Installation - Pipe Laying",
      "unit": "m",
      "quantity": 500,
      "unit_cost": null,
      "total": null
    }
  ],
  "grand_total": null  // Sum of all totals
}
```

### Calculation Logic
```python
def calculate_totals(line_items):
    for item in line_items:
        if item.unit_cost is not None:
            item.total = item.quantity * item.unit_cost
        else:
            item.total = None

    # Calculate grand total (only for items with total)
    grand_total = sum(item.total for item in line_items if item.total is not None)

    return {
        "line_items": line_items,
        "grand_total": grand_total,
        "items_complete": len([i for i in line_items if i.total is not None]),
        "items_total": len(line_items)
    }
```

## PDF Generation

### PDF Structure
```
┌─────────────────────────────────────────┐
│ TENDER APPLICATION                      │
│ {Tender Title}                          │
│ Reference: {Tender Reference}           │
│ Date: {Generated Date}                  │
├─────────────────────────────────────────┤
│ SECTION 1: COMPANY DETAILS              │
│ Legal Name: {value}                     │
│ Registration Number: {value}            │
│ Contact Details: {value}                │
│ ...                                     │
├─────────────────────────────────────────┤
│ SECTION 2: CAPABILITIES                 │
│ {capability 1}                          │
│ {capability 2}                          │
│ ...                                     │
├─────────────────────────────────────────┤
│ SECTION 3: TECHNICAL RESPONSES          │
│ Question 3.1: {question}                │
│ Answer: {response}                      │
│ ...                                     │
├─────────────────────────────────────────┤
│ SECTION 4: PRICING SCHEDULE             │
│ Item | Description | Qty | Cost | Total │
│ 1    | Pipe Supply | 500 | R150 | R75K  │
│ ...                                     │
│ GRAND TOTAL: R487,500                   │
├─────────────────────────────────────────┤
│ SECTION 5: SUPPORTING DOCUMENTS         │
│ ✓ Company Registration (attached)       │
│ ✓ B-BBEE Certificate (attached)         │
│ ✓ Tax Clearance (attached)              │
│ ...                                     │
├─────────────────────────────────────────┤
│ Generated by Tender Tracker Pro         │
│ {Timestamp}                             │
└─────────────────────────────────────────┘
```

### PDF Generation Code (Android)
```kotlin
fun generateApplicationPdf(application: TenderApplication): File {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()  // A4 size
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas
    val paint = Paint()

    var yPosition = 50f

    // Header
    paint.textSize = 18f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("TENDER APPLICATION", 50f, yPosition, paint)
    yPosition += 30f

    // Tender details
    paint.textSize = 12f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText("Title: ${application.tender.title}", 50f, yPosition, paint)
    yPosition += 20f
    canvas.drawText("Reference: ${application.tender.reference}", 50f, yPosition, paint)
    yPosition += 40f

    // Section 1: Company Details
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("SECTION 1: COMPANY DETAILS", 50f, yPosition, paint)
    yPosition += 25f

    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    for ((field, data) in application.auto_filled_fields.filter { it.key.startsWith("company_") }) {
        canvas.drawText("${field.displayName()}: ${data.value}", 50f, yPosition, paint)
        yPosition += 20f
    }

    // ... continue for all sections

    pdfDocument.finishPage(page)

    // Save to Downloads folder
    val file = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "TenderApplication_${application.tender.reference}_${System.currentTimeMillis()}.pdf"
    )
    pdfDocument.writeTo(FileOutputStream(file))
    pdfDocument.close()

    return file
}
```

## Progress Tracking

### Progress Calculation
```python
def calculate_progress(application):
    total_todos = len(application.to_do_items)
    completed_todos = len([t for t in application.to_do_items if t["completed"]])

    return {
        "completed": completed_todos,
        "total": total_todos,
        "percentage": (completed_todos / total_todos * 100) if total_todos > 0 else 0,
        "ready_to_submit": completed_todos == total_todos
    }
```

### Progress UI
```
┌────────────────────────────────────────┐
│ Application Progress                   │
│ ████████████░░░░░░░░  4/7 (57%)        │
│                                        │
│ Remaining:                             │
│ 🔴 Prepare Pricing                     │
│ 🔴 Upload B-BBEE Certificate           │
│ 🟡 Review Capabilities                 │
└────────────────────────────────────────┘
```

## Offline Support

### Local-First Architecture
- All application data stored locally first (SQLite/Room database)
- Background sync to backend when network available
- PDF generation works completely offline
- Gemma responses work offline (on-device LLM)

### Sync Strategy
```python
def sync_application(application_id):
    local_app = get_local_application(application_id)

    if is_online():
        try:
            # Upload to backend
            remote_app = api.update_application(local_app)

            # Update local with server timestamps
            local_app.synced_at = now()
            local_app.sync_status = "synced"
            save_local_application(local_app)
        except NetworkError:
            local_app.sync_status = "pending"
    else:
        local_app.sync_status = "offline"
```

## Error Handling

### Gemma Generation Failure
If Gemma fails to generate response:
1. Show error: "AI generation failed. Please try again or write manually."
2. Offer retry button: "🔄 Regenerate"
3. Fallback: Show empty text area for manual entry
4. Log error for debugging

### Extraction Failure
If TenderExtractor fails:
1. Fall back to pdfplumber raw text
2. Show warning: "Could not extract structured data. Some auto-fill features may be limited."
3. Allow user to continue with partial auto-fill

### Network Failure
If API calls fail:
1. Save data locally
2. Show banner: "Working offline. Changes will sync when connected."
3. Retry sync automatically when network restored

## Security & Privacy

### Data Isolation
- Each user's AnswerLibrary and DocumentLibrary are private (user_id scoped)
- No cross-user data sharing
- Documents stored locally on device (not cloud)

### Document Security
- Local documents stored in app-private directory (`/data/user/0/.../files/documents/`)
- Require user authentication to access
- No automatic upload to backend (only metadata synced)

### AI Generation Disclaimer
Always show disclaimer for AI-generated content:
> ⚠️ **AI-Generated Content**: This response was generated by an AI model. Please review carefully and edit as needed before submission. You are responsible for the accuracy of all information.
```

---

## MODIFIED

### tender-scoring.md

```diff
# Specification: Tender Scoring

## Overview
Tender scoring ranks tenders based on relevance to user's company profile and criteria.

## Scoring Algorithm
[... existing content ...]

## Score Display
[... existing content ...]

+ ## Qualification Threshold (NEW)
+
+ Scores are used to determine auto-fill eligibility:
+ - Score ≥ 70 (default threshold) qualifies tender for automatic application filling
+ - Threshold is user-configurable in Settings → Auto-Fill Settings
+ - Qualification also considers: capabilities match, value range, deadline feasibility
+
+ See: `openspec/specs/application-workflow.md` for full qualification logic.

## Score Storage
[... existing content ...]
```

### tender-detail.md

```diff
# Specification: Tender Detail Screen

## Overview
[... existing content ...]

## UI Components
[... existing content ...]

### Score Card
[... existing content ...]

+ ### Qualification Section (NEW)
+
+ If tender qualifies for auto-fill (score ≥ 70 + other criteria):
+ - Show badge: "⚡ Auto-Fill Ready" (green)
+ - Show "WHY Qualified" explanation (4 factors: score, capabilities, value, deadline)
+ - Show button: "Start Application (Auto-Fill)" → navigates to ApplicationPreviewScreen
+
+ If tender does NOT qualify:
+ - Show badge: "✋ Manual Only" (gray)
+ - Show "WHY NOT Qualified" explanation (failed criteria with suggestions)
+ - No auto-fill button (user can still view PDF, use chat)
+
+ See: `openspec/specs/application-workflow.md` for qualification logic.

## Actions
[... existing content ...]
```

### settings.md

```diff
# Specification: Settings Screen

## Overview
[... existing content ...]

## Settings Categories
[... existing content ...]

+ ### Auto-Fill Settings (NEW)
+
+ Navigate to: AutoFillSettingsScreen
+
+ Configurable options:
+ - Minimum Score (0-100, default 70): Score threshold for auto-fill qualification
+ - Minimum Capabilities Match (0-10, default 2): Number of capabilities that must match
+ - Minimum Deadline Days (0-30, default 7): Minimum days before deadline to qualify
+ - Value Range Check (toggle, default ON): Require tender value within user's range
+
+ Changes take effect immediately (re-evaluates qualification for all tenders).

## Navigation
[... existing content ...]
```

---

## REMOVED

No specifications are being removed by this change.

---

## Implementation Notes

### Backend Dependencies
- TenderExtractor must be wired to pipeline FIRST (TASK 1.1)
- Database migrations must run before API endpoints work (TASK 1.3)
- Gemma API endpoint required for extraction (TASK 1.2)

### Android Dependencies
- Qualification logic requires backend API (can't be calculated locally - needs Score table)
- Gemma integration requires MediaPipe already configured (✅ already complete per user)
- PDF generation requires no external dependencies (uses Android built-in PdfDocument)

### Testing Dependencies
- End-to-end testing requires backend running (qualification, application creation)
- Gemma response testing can use mock (don't require actual Gemma for unit tests)
- PDF generation testing requires real device or emulator (Robolectric doesn't support PdfDocument)

---

## Migration Path

### Phase 1: MVP (This Change)
- Qualification threshold
- Auto-fill 5 field types
- Gemma-generated technical responses
- Simple quote builder
- Local document storage
- Manual PDF download
- Support 1 source (Joburg)

### Phase 2: Full Automation (Future)
- Auto-submit to portals (Playwright)
- Advanced quote builder (cost databases, markup)
- Multi-source support (Cape Town, Durban, national)
- Answer library suggestions (AI-recommended)
- Document auto-attachment (smart matching)
- Cloud document backup (optional)

---

## Related Specifications

**Existing Specs (to be updated):**
- `openspec/specs/tender-scoring.md` - Add qualification threshold section
- `openspec/specs/tender-detail.md` - Add qualification section UI
- `openspec/specs/settings.md` - Add auto-fill settings category

**New Specs (to be created):**
- `openspec/specs/application-workflow.md` - Complete specification (above)

**Future Specs (Phase 2):**
- `openspec/specs/application-submission.md` - Auto-submit via Playwright
- `openspec/specs/answer-library-suggestions.md` - AI-recommended answers
- `openspec/specs/multi-source-templates.md` - Support multiple tender sources
