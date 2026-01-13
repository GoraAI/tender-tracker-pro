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

@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun downloadModel(model: GemmaModel): String {
        val workName = "download_${model.id}"

        val inputData = Data.Builder()
            .putString(ModelDownloadWorker.KEY_MODEL_ID, model.id)
            .putString(ModelDownloadWorker.KEY_DOWNLOAD_URL, model.downloadUrl)
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

        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.KEEP,
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
                    val progress = workInfo.progress.getInt(
                        ModelDownloadWorker.KEY_PROGRESS, 0
                    )
                    ModelState.Downloading(progress)
                }
                WorkInfo.State.SUCCEEDED -> ModelState.Downloaded
                WorkInfo.State.FAILED -> {
                    val error = workInfo.outputData.getString(
                        ModelDownloadWorker.KEY_ERROR
                    ) ?: "Download failed"
                    ModelState.Error(error)
                }
                WorkInfo.State.CANCELLED -> ModelState.NotDownloaded
                else -> ModelState.NotDownloaded
            }
        }
    }

    fun cancelDownload(modelId: String) {
        workManager.cancelUniqueWork("download_$modelId")
    }

    fun getModelDirectory(): java.io.File {
        return java.io.File(context.filesDir, "models").apply {
            if (!exists()) mkdirs()
        }
    }

    fun getModelPath(modelId: String): String {
        return java.io.File(getModelDirectory(), modelId).absolutePath
    }

    fun isModelDownloaded(modelId: String): Boolean {
        val modelDir = java.io.File(getModelDirectory(), modelId)
        return modelDir.exists() && modelDir.isDirectory && modelDir.listFiles()?.isNotEmpty() == true
    }

    fun deleteModel(modelId: String): Boolean {
        val modelDir = java.io.File(getModelDirectory(), modelId)
        return modelDir.deleteRecursively()
    }
}
