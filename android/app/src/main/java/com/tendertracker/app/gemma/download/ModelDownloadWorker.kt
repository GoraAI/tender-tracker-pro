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
import java.io.FileOutputStream
import java.net.HttpURLConnection
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

        val fallbackUrl = inputData.getString(KEY_FALLBACK_URL)

        try {
            val modelDir = File(applicationContext.filesDir, "models/$modelId")
            modelDir.mkdirs()

            val modelFile = File(modelDir, "model.litertlm")

            setProgress(workDataOf(KEY_PROGRESS to 0))

            // Try primary URL first
            var downloadResult = downloadFromUrl(downloadUrl, modelFile)

            // If primary fails and fallback exists, try fallback
            if (!downloadResult && fallbackUrl != null) {
                downloadResult = downloadFromUrl(fallbackUrl, modelFile)
            }

            if (downloadResult) {
                Result.success(
                    workDataOf(
                        KEY_MODEL_PATH to modelDir.absolutePath
                    )
                )
            } else {
                modelFile.delete()
                Result.failure(
                    workDataOf(KEY_ERROR to "Download failed from all sources")
                )
            }
        } catch (e: Exception) {
            Result.failure(
                workDataOf(KEY_ERROR to (e.message ?: "Download failed"))
            )
        }
    }

    private suspend fun downloadFromUrl(urlString: String, targetFile: File): Boolean {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "TenderTracker/1.0")

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                return false
            }

            val totalSize = connection.contentLengthLong
            var downloadedSize = 0L

            connection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedSize += bytesRead

                        // Update progress
                        if (totalSize > 0) {
                            val progress = ((downloadedSize * 100) / totalSize).toInt()
                            setProgress(workDataOf(KEY_PROGRESS to progress))
                        }
                    }
                }
            }

            connection.disconnect()
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        const val KEY_MODEL_ID = "model_id"
        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_FALLBACK_URL = "fallback_url"
        const val KEY_PROGRESS = "progress"
        const val KEY_MODEL_PATH = "model_path"
        const val KEY_ERROR = "error"
    }
}
