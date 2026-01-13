package com.tendertracker.app.gemma.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modelId = inputData.getString(KEY_MODEL_ID)
            ?: return@withContext Result.failure(
                workDataOf(KEY_ERROR to "Missing model ID")
            )

        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL)
            ?: return@withContext Result.failure(
                workDataOf(KEY_ERROR to "Missing download URL")
            )

        try {
            val modelDir = File(applicationContext.filesDir, "models/$modelId")
            modelDir.mkdirs()

            // For now, create placeholder - actual download will use HF API
            val placeholderFile = File(modelDir, "model.bin")

            setProgress(workDataOf(KEY_PROGRESS to 10))

            // Simulate download progress
            for (progress in 20..100 step 10) {
                setProgress(workDataOf(KEY_PROGRESS to progress))
                kotlinx.coroutines.delay(100)
            }

            // Create marker file for downloaded state
            placeholderFile.writeText("Model placeholder for $modelId")

            Result.success(
                workDataOf(
                    KEY_MODEL_PATH to modelDir.absolutePath
                )
            )
        } catch (e: Exception) {
            Result.failure(
                workDataOf(KEY_ERROR to (e.message ?: "Download failed"))
            )
        }
    }

    companion object {
        const val KEY_MODEL_ID = "model_id"
        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_PROGRESS = "progress"
        const val KEY_MODEL_PATH = "model_path"
        const val KEY_ERROR = "error"
    }
}
