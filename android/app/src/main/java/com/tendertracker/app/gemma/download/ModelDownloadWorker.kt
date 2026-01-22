package com.tendertracker.app.gemma.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * WorkManager worker for downloading AI models with resumable download support.
 * Based on Google AI Edge Gallery implementation.
 *
 * Key features:
 * - Resumable downloads using HTTP Range header
 * - Foreground service to prevent termination
 * - Temporary file pattern (.tmp) for partial downloads
 * - Progress tracking every 200ms
 */
@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Unique notification id based on worker ID
    private val notificationId: Int = workerParams.id.hashCode()

    companion object {
        private const val TAG = "ModelDownloadWorker"
        private const val TMP_FILE_EXT = "tmp"
        private const val FOREGROUND_NOTIFICATION_CHANNEL_ID = "model_download_channel_foreground"
        private var channelCreated = false

        // Input data keys
        const val KEY_MODEL_ID = "model_id"
        const val KEY_MODEL_NAME = "model_name"
        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_FALLBACK_URL = "fallback_url"
        const val KEY_TOTAL_BYTES = "total_bytes"

        // Output/Progress data keys
        const val KEY_PROGRESS = "progress"
        const val KEY_DOWNLOADED_BYTES = "downloaded_bytes"
        const val KEY_DOWNLOAD_RATE = "download_rate"
        const val KEY_REMAINING_MS = "remaining_ms"
        const val KEY_MODEL_PATH = "model_path"
        const val KEY_ERROR = "error"
    }

    init {
        if (!channelCreated) {
            // Create notification channel for download progress
            val channel = NotificationChannel(
                FOREGROUND_NOTIFICATION_CHANNEL_ID,
                "Model Downloading",
                NotificationManager.IMPORTANCE_LOW // Silent notification
            ).apply {
                description = "Notifications for model downloading"
            }
            notificationManager.createNotificationChannel(channel)
            channelCreated = true
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modelId = inputData.getString(KEY_MODEL_ID)
            ?: return@withContext Result.failure(
                workDataOf(KEY_ERROR to "Missing model ID")
            )

        val modelName = inputData.getString(KEY_MODEL_NAME) ?: "Model"

        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL)
            ?: return@withContext Result.failure(
                workDataOf(KEY_ERROR to "Missing download URL")
            )

        val fallbackUrl = inputData.getString(KEY_FALLBACK_URL)
        val totalBytes = inputData.getLong(KEY_TOTAL_BYTES, 0L)

        try {
            // Set worker as foreground service immediately
            setForeground(createForegroundInfo(progress = 0, modelName = modelName))

            val modelDir = File(applicationContext.filesDir, "models/$modelId")
            modelDir.mkdirs()

            val finalFile = File(modelDir, "model.litertlm")
            val tmpFile = File(modelDir, "model.litertlm.$TMP_FILE_EXT")

            setProgress(workDataOf(KEY_PROGRESS to 0))

            // Try primary URL first
            var downloadResult = downloadFromUrl(
                urlString = downloadUrl,
                tmpFile = tmpFile,
                finalFile = finalFile,
                modelName = modelName,
                totalBytes = totalBytes
            )

            // If primary fails and fallback exists, try fallback
            if (!downloadResult && fallbackUrl != null) {
                Log.w(TAG, "Primary URL failed, trying fallback URL")
                downloadResult = downloadFromUrl(
                    urlString = fallbackUrl,
                    tmpFile = tmpFile,
                    finalFile = finalFile,
                    modelName = modelName,
                    totalBytes = totalBytes
                )
            }

            if (downloadResult) {
                // Clean up tmp file if it exists
                if (tmpFile.exists()) {
                    tmpFile.delete()
                }

                Log.i(TAG, "Model download succeeded: ${finalFile.absolutePath}")
                Result.success(
                    workDataOf(
                        KEY_MODEL_PATH to modelDir.absolutePath
                    )
                )
            } else {
                // Keep tmp file for resume, only delete final file
                if (finalFile.exists()) {
                    finalFile.delete()
                }
                Log.e(TAG, "Download failed from all sources")
                Result.failure(
                    workDataOf(KEY_ERROR to "Download failed from all sources")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed with exception", e)
            Result.failure(
                workDataOf(KEY_ERROR to (e.message ?: "Download failed"))
            )
        }
    }

    /**
     * Downloads a file from URL with resumable download support.
     * Based on Google AI Edge Gallery implementation.
     */
    private suspend fun downloadFromUrl(
        urlString: String,
        tmpFile: File,
        finalFile: File,
        modelName: String,
        totalBytes: Long
    ): Boolean {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "TenderTracker/1.0")

            // Check if tmp file exists and has partial download
            val existingBytes = if (tmpFile.exists()) tmpFile.length() else 0L
            if (existingBytes > 0) {
                Log.d(TAG, "Found partial download: $existingBytes bytes. Resuming...")
                connection.setRequestProperty("Range", "bytes=$existingBytes-")
            }

            connection.connect()
            val responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")

            // Accept both 200 (full) and 206 (partial content)
            if (responseCode != HttpURLConnection.HTTP_OK &&
                responseCode != HttpURLConnection.HTTP_PARTIAL
            ) {
                Log.e(TAG, "HTTP error code: $responseCode")
                connection.disconnect()
                return false
            }

            // Parse Content-Range header if present
            var downloadedBytes = existingBytes
            val contentRange = connection.getHeaderField("Content-Range")
            if (contentRange != null) {
                // Content-Range: bytes 1000000-2999999/3000000
                val rangeParts = contentRange.substringAfter("bytes ").split("/")
                val byteRange = rangeParts[0].split("-")
                val startByte = byteRange[0].toLong()
                Log.d(TAG, "Server confirmed resume from byte: $startByte")
                downloadedBytes = startByte
            } else {
                Log.d(TAG, "Download starting from beginning")
                // If no Content-Range but we requested Range, server doesn't support resume
                if (existingBytes > 0) {
                    Log.w(TAG, "Server doesn't support resume, starting over")
                    tmpFile.delete()
                    downloadedBytes = 0L
                }
            }

            // Open output stream in append mode if resuming
            val outputStream = FileOutputStream(tmpFile, existingBytes > 0)
            val inputStream = connection.inputStream

            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var bytesRead: Int
            var lastProgressUpdateMs = 0L
            var deltaBytes = 0L

            // For calculating download rate
            val bytesReadSizeBuffer = mutableListOf<Long>()
            val bytesReadLatencyBuffer = mutableListOf<Long>()

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead
                deltaBytes += bytesRead

                // Report progress every 200ms
                val currentTimeMs = System.currentTimeMillis()
                if (currentTimeMs - lastProgressUpdateMs > 200) {
                    // Calculate download rate (moving average over last 5 measurements)
                    var bytesPerMs = 0f
                    if (lastProgressUpdateMs != 0L) {
                        if (bytesReadSizeBuffer.size == 5) {
                            bytesReadSizeBuffer.removeAt(0)
                        }
                        bytesReadSizeBuffer.add(deltaBytes)

                        if (bytesReadLatencyBuffer.size == 5) {
                            bytesReadLatencyBuffer.removeAt(0)
                        }
                        bytesReadLatencyBuffer.add(currentTimeMs - lastProgressUpdateMs)
                        deltaBytes = 0L

                        bytesPerMs = bytesReadSizeBuffer.sum().toFloat() / bytesReadLatencyBuffer.sum()
                    }

                    // Calculate remaining time
                    var remainingMs = 0L
                    if (bytesPerMs > 0f && totalBytes > 0L) {
                        remainingMs = ((totalBytes - downloadedBytes) / bytesPerMs).toLong()
                    }

                    // Update progress data
                    val progress = if (totalBytes > 0) {
                        ((downloadedBytes * 100) / totalBytes).toInt()
                    } else 0

                    setProgress(
                        workDataOf(
                            KEY_PROGRESS to progress,
                            KEY_DOWNLOADED_BYTES to downloadedBytes,
                            KEY_DOWNLOAD_RATE to (bytesPerMs * 1000).toLong(),
                            KEY_REMAINING_MS to remainingMs
                        )
                    )

                    // Update foreground notification
                    setForeground(
                        createForegroundInfo(
                            progress = progress,
                            modelName = modelName
                        )
                    )

                    Log.d(TAG, "Progress: $progress% ($downloadedBytes / $totalBytes bytes)")
                    lastProgressUpdateMs = currentTimeMs
                }
            }

            outputStream.close()
            inputStream.close()
            connection.disconnect()

            // Rename tmp file to final file
            if (finalFile.exists()) {
                finalFile.delete()
            }
            val renamed = tmpFile.renameTo(finalFile)
            if (!renamed) {
                Log.e(TAG, "Failed to rename tmp file to final file")
                return false
            }

            Log.i(TAG, "Download completed successfully")
            true
        } catch (e: IOException) {
            Log.e(TAG, "IOException during download", e)
            // Keep tmp file for resume
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception during download", e)
            false
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        // Initial progress is 0
        return createForegroundInfo(0)
    }

    /**
     * Creates a ForegroundInfo object for the download worker's ongoing notification.
     * Based on Google AI Edge Gallery implementation.
     */
    private fun createForegroundInfo(progress: Int, modelName: String? = null): ForegroundInfo {
        val title = if (modelName != null) {
            "Downloading \"$modelName\""
        } else {
            "Downloading model"
        }
        val content = "Download in progress: $progress%"

        val intent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName)
            ?.apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(
            applicationContext,
            FOREGROUND_NOTIFICATION_CHANNEL_ID
        )
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true) // Makes notification non-dismissable
            .setProgress(100, progress, false) // Show progress bar
            .setContentIntent(pendingIntent)
            .build()

        return ForegroundInfo(
            notificationId,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }
}
