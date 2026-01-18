# Spec Delta: FR-MOBILE-003 Chat Interface

## ADDED

### android/data/src/main/java/com/tendertracker/data/local/entity/ChatMessageEntity.kt
```kotlin
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val content: String,
    val role: String,  // "user" or "assistant"
    val tenderId: String?,
    val timestamp: Long
)
```

### android/data/src/main/java/com/tendertracker/data/local/dao/ChatMessageDao.kt
```kotlin
@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMessages(limit: Int): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE timestamp < :before")
    suspend fun deleteOldMessages(before: Long)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()
}
```

### android/domain/src/main/java/com/tendertracker/domain/model/ChatMessage.kt
```kotlin
data class ChatMessage(
    val id: String,
    val content: String,
    val role: MessageRole,
    val tenderId: String?,
    val timestamp: Instant
)

enum class MessageRole {
    USER, ASSISTANT
}
```

### android/domain/src/main/java/com/tendertracker/domain/repository/ChatRepository.kt
```kotlin
interface ChatRepository {
    fun getMessages(): Flow<List<ChatMessage>>
    fun getRecentMessages(limit: Int): Flow<List<ChatMessage>>
    suspend fun sendMessage(message: ChatMessage)
    suspend fun clearHistory()
}
```

### android/domain/src/main/java/com/tendertracker/domain/usecase/SendMessageUseCase.kt
```kotlin
class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(content: String, role: MessageRole, tenderId: String? = null) {
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = content,
            role = role,
            tenderId = tenderId,
            timestamp = Instant.now()
        )
        chatRepository.sendMessage(message)
    }
}
```

### android/domain/src/main/java/com/tendertracker/domain/usecase/GetChatHistoryUseCase.kt
```kotlin
class GetChatHistoryUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(limit: Int = 100): Flow<List<ChatMessage>> {
        return chatRepository.getRecentMessages(limit)
    }
}
```

### android/app/src/main/java/com/tendertracker/ui/chat/IntentClassifier.kt
```kotlin
class IntentClassifier @Inject constructor() {

    fun classify(message: String): UserIntent {
        val lower = message.lowercase()

        return when {
            containsAny(lower, SUMMARIZE_KEYWORDS) -> UserIntent.SUMMARIZE_TENDER
            containsAny(lower, EXPLAIN_KEYWORDS) -> UserIntent.EXPLAIN_SCORE
            containsAny(lower, PRIORITY_KEYWORDS) -> UserIntent.LIST_HIGH_PRIORITY
            containsAny(lower, DEADLINE_KEYWORDS) -> UserIntent.LIST_CLOSING_SOON
            containsAny(lower, SEARCH_KEYWORDS) -> UserIntent.SEARCH_TENDERS
            else -> UserIntent.GENERAL_QUESTION
        }
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }

    companion object {
        private val SEARCH_KEYWORDS = listOf("find", "search", "show", "list", "get")
        private val SUMMARIZE_KEYWORDS = listOf("summarize", "summary", "overview", "brief")
        private val EXPLAIN_KEYWORDS = listOf("why", "explain", "reason", "how come")
        private val PRIORITY_KEYWORDS = listOf("high priority", "important", "top", "best")
        private val DEADLINE_KEYWORDS = listOf("closing", "deadline", "due", "expiring")
    }
}

enum class UserIntent {
    SEARCH_TENDERS,
    SUMMARIZE_TENDER,
    EXPLAIN_SCORE,
    LIST_HIGH_PRIORITY,
    LIST_CLOSING_SOON,
    GENERAL_QUESTION
}
```

### android/app/src/main/java/com/tendertracker/ui/chat/ChatViewModel.kt
```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val getChatHistoryUseCase: GetChatHistoryUseCase,
    private val chatResponseGenerator: ChatResponseGenerator
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    init {
        loadHistory()
    }

    fun onIntent(intent: ChatScreenIntent) {
        when (intent) {
            is ChatScreenIntent.SendMessage -> handleSendMessage(intent.text)
            is ChatScreenIntent.UpdateInput -> updateInput(intent.text)
            is ChatScreenIntent.CopyMessage -> copyToClipboard(intent.messageId)
            is ChatScreenIntent.SetContext -> setContext(intent.tenderId)
        }
    }

    private fun handleSendMessage(text: String) {
        viewModelScope.launch {
            // Add user message
            sendMessageUseCase(text, MessageRole.USER, _state.value.contextTenderId)

            // Show typing
            _state.update { it.copy(isTyping = true, inputText = "") }

            // Generate response
            val response = chatResponseGenerator.generateResponse(
                text,
                ChatContext(_state.value.contextTenderId)
            )

            // Add assistant message
            response.onSuccess { content ->
                sendMessageUseCase(content, MessageRole.ASSISTANT, _state.value.contextTenderId)
            }

            _state.update { it.copy(isTyping = false) }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            getChatHistoryUseCase().collect { messages ->
                _state.update { it.copy(messages = messages) }
            }
        }
    }
}
```

### android/app/src/main/java/com/tendertracker/ui/chat/ChatState.kt
```kotlin
data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isTyping: Boolean = false,
    val error: String? = null,
    val contextTenderId: String? = null
)

sealed class ChatScreenIntent {
    data class SendMessage(val text: String) : ChatScreenIntent()
    data class UpdateInput(val text: String) : ChatScreenIntent()
    data class CopyMessage(val messageId: String) : ChatScreenIntent()
    data class SetContext(val tenderId: String?) : ChatScreenIntent()
}
```

### android/app/src/main/java/com/tendertracker/ui/chat/ChatScreen.kt
```kotlin
@Composable
fun ChatScreen(
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Message list
        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = true,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.isTyping) {
                item { TypingIndicator() }
            }

            items(state.messages.reversed()) { message ->
                MessageBubble(
                    message = message,
                    onLongClick = { viewModel.onIntent(ChatScreenIntent.CopyMessage(message.id)) }
                )
            }
        }

        // Input bar
        MessageInput(
            text = state.inputText,
            onTextChange = { viewModel.onIntent(ChatScreenIntent.UpdateInput(it)) },
            onSend = { viewModel.onIntent(ChatScreenIntent.SendMessage(state.inputText)) },
            enabled = !state.isTyping
        )
    }
}
```

### android/app/src/main/java/com/tendertracker/ui/chat/components/MessageBubble.kt
```kotlin
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == MessageRole.USER

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier
                .widthIn(max = 280.dp)
                .combinedClickable(onLongClick = onLongClick, onClick = {})
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.content,
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUser) Color.White.copy(alpha = 0.7f)
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
```

### android/app/src/main/java/com/tendertracker/ui/chat/components/MessageInput.kt
```kotlin
@Composable
fun MessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("Type a message...") },
            modifier = Modifier.weight(1f),
            enabled = enabled,
            maxLines = 4,
            shape = RoundedCornerShape(24.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onSend,
            enabled = enabled && text.isNotBlank()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = if (enabled && text.isNotBlank()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        }
    }
}
```

### android/app/src/main/java/com/tendertracker/ui/chat/components/TypingIndicator.kt
```kotlin
@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(start = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(3) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "typing")
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(300, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 100)
                ),
                label = "dot_$index"
            )

            Box(
                modifier = Modifier
                    .offset(y = offsetY.dp)
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}
```

### android/app/src/main/java/com/tendertracker/ui/chat/ChatResponseGenerator.kt
```kotlin
class ChatResponseGenerator @Inject constructor(
    private val gemmaInferenceEngine: GemmaInferenceEngine,
    private val tenderRepository: TenderRepository,
    private val intentClassifier: IntentClassifier
) {
    suspend fun generateResponse(message: String, context: ChatContext): Result<String> {
        val intent = intentClassifier.classify(message)

        return when (intent) {
            UserIntent.SEARCH_TENDERS -> handleSearch(message)
            UserIntent.SUMMARIZE_TENDER -> handleSummarize(context.tenderId)
            UserIntent.EXPLAIN_SCORE -> handleExplainScore(context.tenderId)
            UserIntent.LIST_HIGH_PRIORITY -> handleHighPriority()
            UserIntent.LIST_CLOSING_SOON -> handleClosingSoon()
            UserIntent.GENERAL_QUESTION -> handleGeneral(message, context)
        }
    }

    private suspend fun handleSearch(query: String): Result<String> {
        // Extract search terms and query Room
        val tenders = tenderRepository.searchTenders(query).first()
        return if (tenders.isEmpty()) {
            Result.success("I couldn't find any tenders matching your search.")
        } else {
            val summary = tenders.take(5).joinToString("\n") {
                "• ${it.title} (${it.reference}) - Score: ${it.score ?: "N/A"}"
            }
            Result.success("Found ${tenders.size} tenders:\n\n$summary")
        }
    }

    private suspend fun handleSummarize(tenderId: String?): Result<String> {
        if (tenderId == null) {
            return Result.success("Please select a tender first, or specify which tender you'd like summarized.")
        }
        val tender = tenderRepository.getTenderById(tenderId).first()
            ?: return Result.success("Tender not found.")

        return gemmaInferenceEngine.generateText(
            "Summarize this tender in 3-4 sentences:\n\n" +
            "Title: ${tender.title}\n" +
            "Requirements: ${tender.requirementsSummary}"
        )
    }

    private suspend fun handleGeneral(message: String, context: ChatContext): Result<String> {
        return gemmaInferenceEngine.generateText(
            "Answer this question about tenders concisely:\n\n$message"
        )
    }
}
```

## MODIFIED

### android/app/src/main/java/com/tendertracker/ui/navigation/NavHost.kt
```diff
 composable(Screen.Chat.route) {
-    // Placeholder
+    ChatScreen(navController)
 }
```

### android/data/src/main/java/com/tendertracker/data/local/TenderDatabase.kt
```diff
 @Database(
-    entities = [TenderEntity::class],
+    entities = [TenderEntity::class, ChatMessageEntity::class],
     version = 2,
     exportSchema = true
 )
 abstract class TenderDatabase : RoomDatabase() {
     abstract fun tenderDao(): TenderDao
+    abstract fun chatMessageDao(): ChatMessageDao
 }
```

## REMOVED

None.
