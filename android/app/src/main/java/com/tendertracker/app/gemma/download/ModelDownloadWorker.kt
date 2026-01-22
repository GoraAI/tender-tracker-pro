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
 * Based on Google AI Edge Gallery DownloadWorker.kt implementation.
 *
 * Reference: https://github.com/google-ai-edge/gallery
 */
@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val externalFilesDir = context.getExternalFilesDir(null)

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Unique notification id based on worker ID
    private val notificationId: Int = params.id.hashCode()

    companion object {
        private const val TAG = "ModelDownloadWorker"
        private const val FOREGROUND_NOTIFICATION_CHANNEL_ID = "model_download_channel_foreground"
        private var channelCreated = false
    }

    init {
        if (!channelCreated) {
            // Create notification channel for download progress (same as Google)
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

    override suspend fun doWork(): Result {
        val fileUrl = inputData.getString(KEY_MODEL_URL)
        val modelName = inputData.getString(KEY_MODEL_NAME) ?: "Model"
        val version = inputData.getString(KEY_MODEL_COMMIT_HASH)!!
        val fileName = inputData.getString(KEY_MODEL_DOWNLOAD_FILE_NAME)
        val modelDir = inputData.getString(KEY_MODEL_DOWNLOAD_MODEL_DIR)!!
        val isZip = inputData.getBoolean(KEY_MODEL_IS_ZIP, false)
        val unzippedDir = inputData.getString(KEY_MODEL_UNZIPPED_DIR)
        val extraDataFileUrls = inputData.getString(KEY_MODEL_EXTRA_DATA_URLS)?.split(",") ?: listOf()
        val extraDataFileNames =
            inputData.getString(KEY_MODEL_EXTRA_DATA_DOWNLOAD_FILE_NAMES)?.split(",") ?: listOf()
        val totalBytes = inputData.getLong(KEY_MODEL_TOTAL_BYTES, 0L)
        val accessToken = inputData.getString(KEY_MODEL_DOWNLOAD_ACCESS_TOKEN)

        return withContext(Dispatchers.IO) {
            if (fileUrl == null || fileName == null) {
                Result.failure()
            } else {
                return@withContext try {
                    // Set the worker as a foreground service immediately (like Google)
                    setForeground(createForegroundInfo(progress = 0, modelName = modelName))

                    // Collect data for all files
                    val allFiles: MutableList<UrlAndFileName> = mutableListOf()
                    allFiles.add(UrlAndFileName(url = fileUrl, fileName = fileName))
                    for (index in extraDataFileUrls.indices) {
                        allFiles.add(
                            UrlAndFileName(url = extraDataFileUrls[index], fileName = extraDataFileNames[index])
                        )
                    }
                    Log.d(TAG, "About to download: $allFiles")

                    // Download them in sequence (same as Google - they mention parallel as TODO)
                    var downloadedBytes = 0L
                    val bytesReadSizeBuffer: MutableList<Long> = mutableListOf()
                    val bytesReadLatencyBuffer: MutableList<Long> = mutableListOf()

                    for (file in allFiles) {
                        val url = URL(file.url)

                        val connection = url.openConnection() as HttpURLConnection
                        if (accessToken != null) {
                            Log.d(TAG, "Using access token: ${accessToken.subSequence(0, 10)}...")
                            connection.setRequestProperty("Authorization", "Bearer $accessToken")
                        }

                        // Prepare output file's dir
                        val outputDir =
                            File(
                                applicationContext.getExternalFilesDir(null),
                                listOf(modelDir, version).joinToString(separator = File.separator),
                            )
                        if (!outputDir.exists()) {
                            outputDir.mkdirs()
                        }

                        // Read the tmp file and see if it is partially downloaded (RESUMABLE DOWNLOAD)
                        val outputTmpFile =
                            File(
                                applicationContext.getExternalFilesDir(null),
                                listOf(modelDir, version, "${file.fileName}.$TMP_FILE_EXT")
                                    .joinToString(separator = File.separator),
                            )
                        val outputFileBytes = outputTmpFile.length()
                        if (outputFileBytes > 0) {
                            Log.d(
                                TAG,
                                "File '${outputTmpFile.name}' partial size: ${outputFileBytes}. Trying to resume download",
                            )
                            connection.setRequestProperty("Range", "bytes=${outputFileBytes}-")
                        }
                        connection.connect()
                        Log.d(TAG, "response code: ${connection.responseCode}")

                        if (
                            connection.responseCode == HttpURLConnection.HTTP_OK ||
                            connection.responseCode == HttpURLConnection.HTTP_PARTIAL
                        ) {
                            val contentRange = connection.getHeaderField("Content-Range")

                            if (contentRange != null) {
                                // Parse the Content-Range header
                                val rangeParts = contentRange.substringAfter("bytes ").split("/")
                                val byteRange = rangeParts[0].split("-")
                                val startByte = byteRange[0].toLong()
                                val endByte = byteRange[1].toLong()

                                Log.d(
                                    TAG,
                                    "Content-Range: $contentRange. Start bytes: ${startByte}, end bytes: $endByte",
                                )

                                downloadedBytes += startByte
                            } else {
                                Log.d(TAG, "Download starts from beginning.")
                            }
                        } else {
                            throw IOException("HTTP error code: ${connection.responseCode}")
                        }

                        val inputStream = connection.inputStream
                        val outputStream = FileOutputStream(outputTmpFile, true /* append */)

                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var bytesRead: Int
                        var lastSetProgressTs: Long = 0
                        var deltaBytes = 0L

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            deltaBytes += bytesRead

                            // Report progress every 200 ms (same as Google)
                            val curTs = System.currentTimeMillis()
                            if (curTs - lastSetProgressTs > 200) {
                                // Calculate download rate (exact same algorithm as Google)
                                var bytesPerMs = 0f
                                if (lastSetProgressTs != 0L) {
                                    if (bytesReadSizeBuffer.size == 5) {
                                        bytesReadSizeBuffer.removeAt(0)
                                    }
                                    bytesReadSizeBuffer.add(deltaBytes)
                                    if (bytesReadLatencyBuffer.size == 5) {
                                        bytesReadLatencyBuffer.removeAt(0)
                                    }
                                    bytesReadLatencyBuffer.add(curTs - lastSetProgressTs)
                                    deltaBytes = 0L
                                    bytesPerMs = bytesReadSizeBuffer.sum().toFloat() / bytesReadLatencyBuffer.sum()
                                }

                                // Calculate remaining seconds
                                var remainingMs = 0f
                                if (bytesPerMs > 0f && totalBytes > 0L) {
                                    remainingMs = (totalBytes - downloadedBytes) / bytesPerMs
                                }

                                setProgress(
                                    Data.Builder()
                                        .putLong(KEY_MODEL_DOWNLOAD_RECEIVED_BYTES, downloadedBytes)
                                        .putLong(KEY_MODEL_DOWNLOAD_RATE, (bytesPerMs * 1000).toLong())
                                        .putLong(KEY_MODEL_DOWNLOAD_REMAINING_MS, remainingMs.toLong())
                                        .build()
                                )
                                setForeground(
                                    createForegroundInfo(
                                        progress = (downloadedBytes * 100 / totalBytes).toInt(),
                                        modelName = modelName,
                                    )
                                )
                                Log.d(TAG, "downloadedBytes: $downloadedBytes")
                                lastSetProgressTs = curTs
                            }
                        }

                        outputStream.close()
                        inputStream.close()

                        // Rename the tmp file to the original file name by removing the tmp file ext
                        val originalFilePath = outputTmpFile.absolutePath.replace(".$TMP_FILE_EXT", "")
                        val originalFile = File(originalFilePath)
                        if (originalFile.exists()) {
                            originalFile.delete()
                        }
                        outputTmpFile.renameTo(originalFile)
                        Log.d(TAG, "Download done")

                        // TODO: Unzip if the downloaded file is a zip (skipping for now as we don't use zip)
                    }
                    Result.success()
                } catch (e: IOException) {
                    Result.failure(
                        Data.Builder().putString(KEY_MODEL_DOWNLOAD_ERROR_MESSAGE, e.message).build()
                    )
                }
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        // Initial progress is 0
        return createForegroundInfo(0)
    }

    /**
     * Creates a ForegroundInfo object for the download worker's ongoing notification.
     * Exact copy from Google AI Edge Gallery implementation.
     */
    private fun createForegroundInfo(progress: Int, modelName: String? = null): ForegroundInfo {
        var title = "Downloading model"
        if (modelName != null) {
            title = "Downloading \"$modelName\""
        }
        val content = "Downloading in progress: $progress%"

        val intent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName)
            ?.apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

        val pendingIntent =
            PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val notification =
            NotificationCompat.Builder(applicationContext, FOREGROUND_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true) // Makes the notification non-dismissable
                .setProgress(100, progress, false) // Show progress
                .setContentIntent(pendingIntent)
                .build()

        return ForegroundInfo(
            notificationId,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }
}

/**
 * Data class for URL and filename pairs.
 * Copied from Google AI Edge Gallery.
 */
data class UrlAndFileName(val url: String, val fileName: String)
