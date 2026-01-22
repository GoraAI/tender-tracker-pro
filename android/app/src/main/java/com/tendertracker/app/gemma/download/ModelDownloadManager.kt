package com.tendertracker.app.gemma.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.tendertracker.app.gemma.model.GemmaModel
import com.tendertracker.app.gemma.model.ModelState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for model downloads using WorkManager.
 * Based on Google AI Edge Gallery DownloadRepository pattern.
 */
@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun downloadModel(model: GemmaModel): String {
        val workName = model.id // Use model.id as unique work name (like Google uses model.name)

        // Build input data with Google's exact KEY constants
        val inputData = Data.Builder()
            .putString(KEY_MODEL_NAME, model.name)
            .putString(KEY_MODEL_URL, model.downloadUrl)
            .putString(KEY_MODEL_COMMIT_HASH, model.version)
            .putString(KEY_MODEL_DOWNLOAD_MODEL_DIR, model.id) // normalized name
            .putString(KEY_MODEL_DOWNLOAD_FILE_NAME, "model.litertlm") // our filename
            .putBoolean(KEY_MODEL_IS_ZIP, false) // we don't use zip
            .putLong(KEY_MODEL_TOTAL_BYTES, model.sizeBytes)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()

        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(model.id)
            .build()

        // Use REPLACE policy like Google (allows retry)
        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            request
        )

        return request.id.toString()
    }

    fun observeDownloadProgress(modelId: String): Flow<ModelState> {
        return workManager.getWorkInfosByTagFlow(modelId).map { workInfos ->
            val workInfo = workInfos.firstOrNull()
            when (workInfo?.state) {
                WorkInfo.State.ENQUEUED -> ModelState.Downloading(0)
                WorkInfo.State.RUNNING -> {
                    val progress = workInfo.progress.getLong(
                        KEY_MODEL_DOWNLOAD_RECEIVED_BYTES, 0
                    )
                    val total = workInfo.inputData.getLong(KEY_MODEL_TOTAL_BYTES, 0)
                    val percentage = if (total > 0) ((progress * 100) / total).toInt() else 0
                    ModelState.Downloading(percentage)
                }
                WorkInfo.State.SUCCEEDED -> ModelState.Downloaded
                WorkInfo.State.FAILED -> {
                    val error = workInfo.outputData.getString(
                        KEY_MODEL_DOWNLOAD_ERROR_MESSAGE
                    ) ?: "Download failed"
                    ModelState.Error(error)
                }
                WorkInfo.State.CANCELLED -> ModelState.NotDownloaded
                else -> ModelState.NotDownloaded
            }
        }
    }

    fun cancelDownload(modelId: String) {
        workManager.cancelUniqueWork(modelId)
    }

    fun getModelDirectory(): java.io.File {
        return java.io.File(context.filesDir, "models").apply {
            if (!exists()) mkdirs()
        }
    }

    fun getModelPath(modelId: String): String {
        // Google's pattern: {externalFilesDir}/{normalizedName}/{version}/{fileName}
        // Our pattern: {filesDir}/models/{modelId}/{version}/model.litertlm
        val modelVersion = "v1" // For now we use static version
        return java.io.File(
            java.io.File(java.io.File(getModelDirectory(), modelId), modelVersion),
            "model.litertlm"
        ).absolutePath
    }

    fun isModelDownloaded(modelId: String): Boolean {
        val modelFile = java.io.File(getModelPath(modelId))
        return modelFile.exists() && modelFile.isFile && modelFile.length() > 0
    }

    fun deleteModel(modelId: String): Boolean {
        val modelDir = java.io.File(getModelDirectory(), modelId)
        return modelDir.deleteRecursively()
    }
}
