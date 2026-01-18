# FR-MOBILE-003: Conversational Chat Interface

**Status**: PROPOSED
**Priority**: P0
**Created**: 2026-01-13
**Dependencies**: FR-MOBILE-001 (Android Foundation), FR-MOBILE-002 (On-Device Gemma)

---

## Summary

Implement a conversational chat interface that allows users to interact with the Gemma AI using natural language to query tenders, ask questions, get summaries, and receive recommendations - all processed on-device.

---

## Motivation

Natural language interaction provides:
- **Intuitive UX**: Ask questions instead of navigating menus
- **Quick answers**: "Show me water tenders closing this week"
- **Contextual help**: "Why did this tender score low?"
- **Hands-free**: Voice input support (future)

Key use cases:
1. "Find tenders related to construction in Gauteng"
2. "Summarize tender JW-2026-001"
3. "What are the requirements for this tender?"
4. "Why is my score only 45 for this tender?"
5. "Show high-priority tenders closing soon"

---

## Scope

### In Scope
- Chat UI with message bubbles (user/assistant)
- Message input with send button
- Chat history persistence (Room)
- Context-aware responses (current tender/screen)
- Tender search via natural language
- Tender summary generation
- Score explanation generation
- Typing indicator during inference
- Message timestamps
- Copy message to clipboard
- Scroll to bottom on new message

### Out of Scope
- Voice input/output (future)
- Multi-turn memory beyond current session
- Image/document attachments
- Sharing conversations
- Export chat history
- Rich message formatting (markdown)

---

## Acceptance Criteria

- [ ] Chat screen shows message history
- [ ] User can type and send messages
- [ ] AI responds using on-device Gemma
- [ ] Shows typing indicator during inference
- [ ] Messages persist across app restarts
- [ ] Can search tenders via natural language
- [ ] Can request tender summaries
- [ ] Can ask why a score is low/high
- [ ] Context-aware when viewing specific tender
- [ ] Can copy messages to clipboard
- [ ] Smooth scrolling with many messages
- [ ] Unit tests for message parsing
- [ ] UI tests for chat interaction

---

## Technical Approach

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    CHAT SCREEN                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  MessageList (LazyColumn)                             │  │
│  │  ┌────────────────────────────────────────────────┐  │  │
│  │  │ UserMessage: "Find water tenders"              │  │  │
│  │  └────────────────────────────────────────────────┘  │  │
│  │  ┌────────────────────────────────────────────────┐  │  │
│  │  │ AssistantMessage: "Found 5 water tenders..."   │  │  │
│  │  └────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  [TextField: Type a message...]         [Send 📤]    │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Intent Classification

The chat system classifies user intents:

```kotlin
enum class ChatIntent {
    SEARCH_TENDERS,      // "find tenders about X"
    SUMMARIZE_TENDER,    // "summarize tender X"
    EXPLAIN_SCORE,       // "why is this score low"
    LIST_HIGH_PRIORITY,  // "show high priority tenders"
    LIST_CLOSING_SOON,   // "tenders closing this week"
    GENERAL_QUESTION,    // anything else
}
```

### Chat Flow

```
1. User sends message
2. Intent classification (regex + keywords)
3. Based on intent:
   - SEARCH: Query Room DB → Format results
   - SUMMARIZE: Load tender → Gemma prompt → Summary
   - EXPLAIN: Load tender + criteria → Gemma → Explanation
   - GENERAL: Gemma prompt → Response
4. Save messages to Room
5. Update UI
```

### System Prompt for Chat

```
You are a helpful tender tracking assistant for {company_name}.

CONTEXT:
- Current screen: {current_screen}
- Viewing tender: {current_tender_title} (if applicable)
- User's criteria: {user_criteria_summary}

Respond helpfully and concisely. When discussing tenders:
- Always mention reference numbers
- Include deadlines when relevant
- Explain scores in terms of user's criteria

Keep responses under 200 words unless specifically asked for detail.
```

---

## Implementation Plan

### Phase 1: Chat Data Layer (5 tasks)
- Create ChatMessage entity (Room)
- Create ChatMessageDao
- Create ChatRepository interface
- Create ChatRepositoryImpl
- Migration for chat_messages table

### Phase 2: Chat Domain Layer (4 tasks)
- Create ChatMessage domain model
- Create SendMessageUseCase
- Create GetChatHistoryUseCase
- Create IntentClassifier

### Phase 3: Chat Presentation (8 tasks)
- Create ChatViewModel with MVI
- Create ChatState data class
- Create ChatIntent sealed class
- Create ChatScreen composable
- Create MessageBubble component
- Create MessageInput component
- Create TypingIndicator component
- Wire up navigation

### Phase 4: AI Integration (5 tasks)
- Create ChatResponseGenerator
- Implement search response
- Implement summarize response
- Implement score explanation response
- Implement general response

### Phase 5: Polish & Testing (4 tasks)
- Add copy to clipboard
- Add message timestamps
- Unit tests for IntentClassifier
- UI tests for ChatScreen

---

## Configuration

```kotlin
object ChatConfig {
    const val MAX_MESSAGE_LENGTH = 1000
    const val MAX_HISTORY_MESSAGES = 100
    const val TYPING_INDICATOR_DELAY_MS = 500L
    const val MAX_RESPONSE_TOKENS = 500

    // Intent keywords
    val SEARCH_KEYWORDS = listOf("find", "search", "show", "list", "get")
    val SUMMARIZE_KEYWORDS = listOf("summarize", "summary", "overview", "brief")
    val EXPLAIN_KEYWORDS = listOf("why", "explain", "reason", "how come")
    val PRIORITY_KEYWORDS = listOf("high priority", "important", "top", "best")
    val DEADLINE_KEYWORDS = listOf("closing", "deadline", "due", "expiring")
}
```

---

## API Design

### ChatViewModel

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val getChatHistoryUseCase: GetChatHistoryUseCase,
    private val chatResponseGenerator: ChatResponseGenerator
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    fun onIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.SendMessage -> handleSendMessage(intent.text)
            is ChatIntent.LoadHistory -> loadHistory()
            is ChatIntent.CopyMessage -> copyToClipboard(intent.messageId)
            is ChatIntent.SetContext -> setContext(intent.tenderId)
        }
    }
}
```

### ChatState

```kotlin
data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isTyping: Boolean = false,
    val error: String? = null,
    val contextTenderId: String? = null
)

sealed class ChatIntent {
    data class SendMessage(val text: String) : ChatIntent()
    object LoadHistory : ChatIntent()
    data class CopyMessage(val messageId: String) : ChatIntent()
    data class SetContext(val tenderId: String?) : ChatIntent()
}
```

### ChatResponseGenerator

```kotlin
class ChatResponseGenerator @Inject constructor(
    private val gemmaInferenceEngine: GemmaInferenceEngine,
    private val tenderRepository: TenderRepository,
    private val intentClassifier: IntentClassifier
) {
    suspend fun generateResponse(
        message: String,
        context: ChatContext
    ): Result<String> {
        val intent = intentClassifier.classify(message)

        return when (intent) {
            ChatIntent.SEARCH_TENDERS -> handleSearch(message)
            ChatIntent.SUMMARIZE_TENDER -> handleSummarize(context.tenderId)
            ChatIntent.EXPLAIN_SCORE -> handleExplainScore(context.tenderId)
            ChatIntent.LIST_HIGH_PRIORITY -> handleHighPriority()
            ChatIntent.LIST_CLOSING_SOON -> handleClosingSoon()
            ChatIntent.GENERAL_QUESTION -> handleGeneral(message, context)
        }
    }
}
```

### IntentClassifier

```kotlin
class IntentClassifier {
    fun classify(message: String): UserIntent {
        val lower = message.lowercase()

        return when {
            containsAny(lower, ChatConfig.SUMMARIZE_KEYWORDS) -> UserIntent.SUMMARIZE_TENDER
            containsAny(lower, ChatConfig.EXPLAIN_KEYWORDS) -> UserIntent.EXPLAIN_SCORE
            containsAny(lower, ChatConfig.PRIORITY_KEYWORDS) -> UserIntent.LIST_HIGH_PRIORITY
            containsAny(lower, ChatConfig.DEADLINE_KEYWORDS) -> UserIntent.LIST_CLOSING_SOON
            containsAny(lower, ChatConfig.SEARCH_KEYWORDS) -> UserIntent.SEARCH_TENDERS
            else -> UserIntent.GENERAL_QUESTION
        }
    }
}
```

---

## UI Components

### MessageBubble

```kotlin
@Composable
fun MessageBubble(
    message: ChatMessage,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == MessageRole.USER
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bubbleColor,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .combinedClickable(onLongClick = onLongClick)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.content,
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = message.timestamp.format(),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
```

### TypingIndicator

```kotlin
@Composable
fun TypingIndicator() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        repeat(3) { index ->
            val delay = index * 150
            val infiniteTransition = rememberInfiniteTransition()
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -8f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes { durationMillis = 600 },
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(delay)
                )
            )

            Box(
                modifier = Modifier
                    .offset(y = offsetY.dp)
                    .size(8.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        CircleShape
                    )
            )
        }
    }
}
```

---

## Performance Targets

- **Message send**: < 100ms to show in UI
- **AI response**: < 5 seconds (E2B) / < 8 seconds (E4B)
- **History load**: < 200ms for 100 messages
- **Scroll performance**: 60 FPS with 100+ messages

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Slow AI response | Poor UX | Typing indicator, timeout with retry |
| Intent misclassification | Wrong response | Fallback to general, improve keywords |
| Long chat history | Memory issues | Pagination, archive old messages |
| Context loss | Irrelevant responses | Pass context in system prompt |

---

## Testing Strategy

1. **Unit Tests**:
   - IntentClassifier with various inputs
   - Message parsing and formatting
   - ChatState transitions

2. **UI Tests**:
   - Send message flow
   - Scroll behavior
   - Copy to clipboard
   - Typing indicator

3. **Integration Tests**:
   - Full chat flow with mock Gemma
   - Message persistence

---

## Success Metrics

- [ ] Chat history loads correctly
- [ ] Messages send and display
- [ ] AI responses are relevant
- [ ] Intent classification accurate (>80%)
- [ ] Typing indicator shows during inference
- [ ] Copy to clipboard works
- [ ] Performance targets met
- [ ] All tests pass

---

## Future Enhancements

- Voice input/output
- Multi-turn conversation memory
- Suggested responses
- Rich message formatting
- Share conversation
- Export to PDF
- Quick action buttons
