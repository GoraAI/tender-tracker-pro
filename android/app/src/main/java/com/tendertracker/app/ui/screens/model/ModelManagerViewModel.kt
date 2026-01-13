package com.tendertracker.app.ui.screens.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tendertracker.app.gemma.download.ModelDownloadManager
import com.tendertracker.app.gemma.engine.GemmaInferenceEngine
import com.tendertracker.app.gemma.model.GemmaModel
import com.tendertracker.app.gemma.model.GemmaModelRegistry
import com.tendertracker.app.gemma.model.ModelInfo
import com.tendertracker.app.gemma.model.ModelState
import com.tendertracker.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModelManagerState(
    val models: List<ModelInfo> = emptyList(),
    val activeModelId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ModelManagerViewModel @Inject constructor(
    private val downloadManager: ModelDownloadManager,
    private val inferenceEngine: GemmaInferenceEngine,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ModelManagerState())
    val state: StateFlow<ModelManagerState> = _state.asStateFlow()

    init {
        loadModelStates()
        observeActiveModel()
    }

    private fun loadModelStates() {
        val models = GemmaModelRegistry.ALL_MODELS.map { model ->
            val isDownloaded = downloadManager.isModelDownloaded(model.id)
            val state = if (isDownloaded) {
                if (inferenceEngine.getCurrentModel()?.id == model.id) {
                    ModelState.Ready
                } else {
                    ModelState.Downloaded
                }
            } else {
                ModelState.NotDownloaded
            }
            ModelInfo(
                model = model,
                state = state,
                localPath = if (isDownloaded) {
                    downloadManager.getModelPath(model.id)
                } else null
            )
        }
        _state.update { it.copy(models = models) }
    }

    private fun observeActiveModel() {
        viewModelScope.launch {
            settingsRepository.getSelectedModelId().collect { modelId ->
                _state.update { it.copy(activeModelId = modelId) }
            }
        }
    }

    fun downloadModel(model: GemmaModel) {
        viewModelScope.launch {
            updateModelState(model.id, ModelState.Downloading(0))
            downloadManager.downloadModel(model)

            // Observe download progress
            downloadManager.observeDownloadProgress(model.id).collect { downloadState ->
                updateModelState(model.id, downloadState)
            }
        }
    }

    fun loadModel(model: GemmaModel) {
        viewModelScope.launch {
            updateModelState(model.id, ModelState.Loading)

            val modelPath = downloadManager.getModelPath(model.id)
            val result = inferenceEngine.initialize(model, modelPath)

            result.onSuccess {
                updateModelState(model.id, ModelState.Ready)
                settingsRepository.setSelectedModelId(model.id)
            }.onFailure { e ->
                updateModelState(
                    model.id,
                    ModelState.Error(e.message ?: "Failed to load model")
                )
            }
        }
    }

    fun deleteModel(model: GemmaModel) {
        viewModelScope.launch {
            if (inferenceEngine.getCurrentModel()?.id == model.id) {
                inferenceEngine.cleanup()
                settingsRepository.setSelectedModelId(null)
            }

            downloadManager.deleteModel(model.id)
            updateModelState(model.id, ModelState.NotDownloaded)
        }
    }

    private fun updateModelState(modelId: String, newState: ModelState) {
        _state.update { currentState ->
            currentState.copy(
                models = currentState.models.map { modelInfo ->
                    if (modelInfo.model.id == modelId) {
                        modelInfo.copy(state = newState)
                    } else {
                        modelInfo
                    }
                }
            )
        }
    }
}
