package com.tendertracker.app.gemma.model

sealed class ModelState {
    data object NotDownloaded : ModelState()
    data class Downloading(val progress: Int) : ModelState()
    data object Downloaded : ModelState()
    data object Loading : ModelState()
    data object Ready : ModelState()
    data class Error(val message: String) : ModelState()

    fun isReady(): Boolean = this is Ready
    fun isDownloaded(): Boolean = this is Downloaded || this is Loading || this is Ready
}

data class ModelInfo(
    val model: GemmaModel,
    val state: ModelState,
    val localPath: String? = null,
    val downloadedAt: Long? = null
)
