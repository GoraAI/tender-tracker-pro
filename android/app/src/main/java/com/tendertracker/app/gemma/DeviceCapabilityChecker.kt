package com.tendertracker.app.gemma

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Three-tier device capability detection for Gemma LLM deployment.
 *
 * Tier 1 (FULL_SUPPORT): 8GB+ RAM, Android 12+ - Full AI features with optimal performance
 * Tier 2 (DEGRADED): 4-6GB RAM, Android 12+ - AI works but may be slower, show warnings
 * Tier 3 (UNSUPPORTED): <4GB RAM or Android <12 - Disable AI features, show clear message
 */
@Singleton
class DeviceCapabilityChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {

    data class Capability(
        val tier: DeviceTier,
        val hasMinAPILevel: Boolean,
        val hasEnoughRAM: Boolean,
        val hasEnoughStorage: Boolean,
        val ramMB: Long,
        val storageByte: Long,
        val apiLevel: Int,
        val warningMessage: String?
    )

    enum class DeviceTier {
        FULL_SUPPORT,   // Tier 1: 8GB+ RAM, Android 12+ - Optimal
        DEGRADED,       // Tier 2: 4-6GB RAM, Android 12+ - May be slow
        UNSUPPORTED     // Tier 3: <4GB RAM or Android <12 - Disabled
    }

    companion object {
        const val MIN_RAM_MB = 4096                     // 4 GB (Tier 2 minimum)
        const val RECOMMENDED_RAM_MB = 8192             // 8 GB (Tier 1 minimum)
        const val MIN_STORAGE_BYTES = 5_000_000_000L    // 5 GB
        const val MIN_API_LEVEL = 31                    // Android 12
    }

    /**
     * Check device capability and return tier classification with detailed info.
     */
    fun checkCapability(): Capability {
        val ramMB = getTotalRAM()
        val apiLevel = Build.VERSION.SDK_INT
        val storageBytes = getAvailableStorage()

        return when {
            // Tier 3: Android version too old
            apiLevel < MIN_API_LEVEL -> Capability(
                tier = DeviceTier.UNSUPPORTED,
                hasMinAPILevel = false,
                hasEnoughRAM = ramMB >= MIN_RAM_MB,
                hasEnoughStorage = storageBytes >= MIN_STORAGE_BYTES,
                ramMB = ramMB,
                storageByte = storageBytes,
                apiLevel = apiLevel,
                warningMessage = "AI features require Android 12 or newer. Your device is running Android ${getAndroidVersionName(apiLevel)}."
            )

            // Tier 3: RAM too low
            ramMB < MIN_RAM_MB -> Capability(
                tier = DeviceTier.UNSUPPORTED,
                hasMinAPILevel = true,
                hasEnoughRAM = false,
                hasEnoughStorage = storageBytes >= MIN_STORAGE_BYTES,
                ramMB = ramMB,
                storageByte = storageBytes,
                apiLevel = apiLevel,
                warningMessage = "AI features require at least 4GB RAM. Your device has ${ramMB/1024}GB RAM."
            )

            // Tier 3: Storage too low
            storageBytes < 2_000_000_000L -> Capability(
                tier = DeviceTier.UNSUPPORTED,
                hasMinAPILevel = true,
                hasEnoughRAM = true,
                hasEnoughStorage = false,
                ramMB = ramMB,
                storageByte = storageBytes,
                apiLevel = apiLevel,
                warningMessage = "Not enough storage. AI features require 5GB free space. Please free up storage and try again."
            )

            // Tier 2: Mid-tier device (4-6GB RAM) - degraded performance
            ramMB < RECOMMENDED_RAM_MB -> Capability(
                tier = DeviceTier.DEGRADED,
                hasMinAPILevel = true,
                hasEnoughRAM = true,
                hasEnoughStorage = storageBytes >= MIN_STORAGE_BYTES,
                ramMB = ramMB,
                storageByte = storageBytes,
                apiLevel = apiLevel,
                warningMessage = "AI features may be slower on this device (${ramMB/1024}GB RAM detected). Recommended: 8GB+ RAM for optimal performance."
            )

            // Tier 1: Full support (8GB+ RAM, Android 12+)
            else -> Capability(
                tier = DeviceTier.FULL_SUPPORT,
                hasMinAPILevel = true,
                hasEnoughRAM = true,
                hasEnoughStorage = storageBytes >= MIN_STORAGE_BYTES,
                ramMB = ramMB,
                storageByte = storageBytes,
                apiLevel = apiLevel,
                warningMessage = null
            )
        }
    }

    /**
     * Get total device RAM in MB.
     */
    fun getTotalRAM(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.totalMem / (1024 * 1024) // Convert bytes to MB
    }

    /**
     * Get available storage space in bytes for internal storage.
     */
    fun getAvailableStorage(): Long {
        val statFs = StatFs(context.filesDir.absolutePath)
        return statFs.availableBytes
    }

    /**
     * Get current Android API level.
     */
    fun getAPILevel(): Int = Build.VERSION.SDK_INT

    /**
     * Convert API level to human-readable Android version name.
     */
    private fun getAndroidVersionName(apiLevel: Int): String = when (apiLevel) {
        30 -> "11"
        31, 32 -> "12"
        33 -> "13"
        34 -> "14"
        35 -> "15"
        else -> if (apiLevel > 35) "15+" else apiLevel.toString()
    }
}
