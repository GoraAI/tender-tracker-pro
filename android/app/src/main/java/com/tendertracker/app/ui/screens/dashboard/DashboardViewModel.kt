package com.tendertracker.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tendertracker.app.gemma.engine.GemmaInferenceEngine
import com.tendertracker.app.ui.components.AttentionItem
import com.tendertracker.app.ui.components.AttentionPriority
import com.tendertracker.domain.model.Tender
import com.tendertracker.domain.repository.AnalyticsRepository
import com.tendertracker.domain.repository.TenderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardState(
    val totalTenders: Int = 0,
    val highPriorityCount: Int = 0,
    val avgScore: Float = 0f,
    val modelStatus: ModelStatus = ModelStatus.NOT_LOADED,
    val recentTenders: List<Tender> = emptyList(),
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val syncProgress: Int = 0,
    val error: String? = null,
    val syncMessage: String? = null,

    // Attention-prioritized data
    val attentionItems: List<AttentionItem> = emptyList(),

    // Compliance summary
    val complianceValidCount: Int = 0,
    val complianceTotalCount: Int = 0,
    val complianceDaysToExpiry: Int = 0,
    val complianceNextExpiryItem: String = "",

    // Pipeline summary
    val pipelineActiveCount: Int = 0,
    val pipelineDueThisWeek: Int = 0,
    val pipelineHasConflicts: Boolean = false,

    // Funnel summary
    val funnelDiscovered: Int = 0,
    val funnelQualified: Int = 0,
    val funnelSubmitted: Int = 0,
    val funnelAwarded: Int = 0,

    // Empty states
    val complianceIsEmpty: Boolean = true,
    val pipelineIsEmpty: Boolean = true,
    val funnelIsEmpty: Boolean = true
)

enum class ModelStatus(val displayName: String) {
    NOT_LOADED("Not Loaded"),
    LOADING("Loading..."),
    READY("Ready"),
    ERROR("Error")
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val tenderRepository: TenderRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val inferenceEngine: GemmaInferenceEngine
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Load analytics data from API
            loadAnalyticsSummaries()

            // Load all tenders from local database
            tenderRepository.getAllTenders()
                .catch { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load tenders"
                        )
                    }
                }
                .collect { tenders ->
                    val totalCount = tenders.size
                    val highPriority = tenders.filter { (it.score ?: 0) >= 70 }
                    val avgScore = if (tenders.isNotEmpty()) {
                        tenders.mapNotNull { it.score }.average().toFloat()
                    } else 0f

                    val recentHighPriority = highPriority
                        .sortedByDescending { it.score }
                        .take(5)

                    val modelStatus = when {
                        inferenceEngine.isReady() -> ModelStatus.READY
                        else -> ModelStatus.NOT_LOADED
                    }

                    // Generate attention items based on real data
                    val attentionItems = generateAttentionItems(highPriority)

                    _state.update {
                        it.copy(
                            totalTenders = totalCount,
                            highPriorityCount = highPriority.size,
                            avgScore = avgScore,
                            modelStatus = modelStatus,
                            recentTenders = recentHighPriority,
                            isLoading = false,
                            attentionItems = attentionItems
                        )
                    }
                }
        }
    }

    private suspend fun loadAnalyticsSummaries() {
        // Load compliance status
        analyticsRepository.getComplianceStatus().onSuccess { status ->
            _state.update {
                it.copy(
                    complianceValidCount = status.validCount,
                    complianceTotalCount = status.totalCount,
                    complianceDaysToExpiry = status.daysToNextExpiry ?: 0,
                    complianceNextExpiryItem = status.nextExpiryItem ?: "",
                    complianceIsEmpty = status.isEmpty
                )
            }
        }

        // Load pipeline status
        analyticsRepository.getPipelineStatus().onSuccess { status ->
            _state.update {
                it.copy(
                    pipelineActiveCount = status.activeCount,
                    pipelineDueThisWeek = status.dueThisWeek,
                    pipelineHasConflicts = status.hasConflicts,
                    pipelineIsEmpty = status.isEmpty
                )
            }
        }

        // Load funnel analytics
        analyticsRepository.getFunnelAnalytics(12).onSuccess { funnel ->
            val discovered = funnel.stages.find { it.name == "Discovered" }?.count ?: 0
            val qualified = funnel.stages.find { it.name == "Qualified" }?.count ?: 0
            val submitted = funnel.stages.find { it.name == "Submitted" }?.count ?: 0
            val awarded = funnel.stages.find { it.name == "Awarded" }?.count ?: 0

            _state.update {
                it.copy(
                    funnelDiscovered = discovered,
                    funnelQualified = qualified,
                    funnelSubmitted = submitted,
                    funnelAwarded = awarded,
                    funnelIsEmpty = funnel.isEmpty
                )
            }
        }
    }

    private fun generateAttentionItems(highPriorityTenders: List<Tender>): List<AttentionItem> {
        val attentionItems = mutableListOf<AttentionItem>()
        val currentState = _state.value

        // Check for urgent compliance from real data
        if (!currentState.complianceIsEmpty && currentState.complianceDaysToExpiry > 0 && currentState.complianceDaysToExpiry < 30) {
            attentionItems.add(
                AttentionItem(
                    id = "compliance-expiry",
                    title = "${currentState.complianceNextExpiryItem} expires in ${currentState.complianceDaysToExpiry} days",
                    description = "Affects: ${currentState.pipelineActiveCount} active tenders",
                    priority = if (currentState.complianceDaysToExpiry < 14) AttentionPriority.CRITICAL else AttentionPriority.WARNING,
                    actionLabel = "Start Renewal"
                )
            )
        }

        // Check for tenders with upcoming deadlines
        highPriorityTenders.take(1).forEach { tender ->
            tender.deadline?.let {
                attentionItems.add(
                    AttentionItem(
                        id = "tender-${tender.id}",
                        title = tender.title.take(40),
                        description = "Due: ${tender.deadline} | Score: ${tender.score}",
                        priority = AttentionPriority.WARNING,
                        actionLabel = "View Details",
                        secondaryActionLabel = "Complete Checklist"
                    )
                )
            }
        }

        // Check for pipeline conflicts
        if (currentState.pipelineHasConflicts) {
            attentionItems.add(
                AttentionItem(
                    id = "pipeline-conflicts",
                    title = "Schedule conflicts detected",
                    description = "${currentState.pipelineDueThisWeek} tenders due this week",
                    priority = AttentionPriority.WARNING,
                    actionLabel = "Review Pipeline"
                )
            )
        }

        return attentionItems
    }

    /**
     * Run the pipeline to discover new tenders, then sync to local database.
     */
    fun runPipelineSync() {
        if (_state.value.isSyncing) return // Prevent multiple syncs

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSyncing = true,
                    syncProgress = 0,
                    syncMessage = "Starting pipeline...",
                    error = null
                )
            }

            tenderRepository.runPipelineAndSync(
                sourceId = "joburg",
                onProgress = { progress ->
                    val message = when {
                        progress < 10 -> "Starting pipeline..."
                        progress < 30 -> "Discovering tenders..."
                        progress < 50 -> "Downloading PDFs..."
                        progress < 70 -> "Extracting content..."
                        progress < 85 -> "Scoring relevance..."
                        progress < 100 -> "Syncing tenders..."
                        else -> "Complete!"
                    }
                    _state.update {
                        it.copy(syncProgress = progress, syncMessage = message)
                    }
                }
            ).onSuccess { count ->
                _state.update {
                    it.copy(
                        isSyncing = false,
                        syncProgress = 100,
                        syncMessage = "Synced $count tenders"
                    )
                }
                // Reload dashboard data to show updated counts
                loadDashboardData()
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        isSyncing = false,
                        syncProgress = 0,
                        syncMessage = null,
                        error = "Sync failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun refreshModelStatus() {
        val status = when {
            inferenceEngine.isReady() -> ModelStatus.READY
            else -> ModelStatus.NOT_LOADED
        }
        _state.update { it.copy(modelStatus = status) }
    }
}
