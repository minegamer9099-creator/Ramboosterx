package com.ramboosterx.app.model

import kotlin.math.roundToInt

/** Real values read from Android APIs. Nothing here is simulated. */
data class RamInfo(
    val totalBytes: Long,
    val availableBytes: Long,
    val systemThresholdBytes: Long,
    val systemLowMemory: Boolean,
    val appPssBytes: Long?,
    val appHeapUsedBytes: Long,
    val appHeapMaxBytes: Long,
    val readAtMillis: Long
) {
    /** Estimate only: total minus available. Android counts cached apps as "used". */
    val usedBytes: Long get() = (totalBytes - availableBytes).coerceIn(0L, totalBytes)

    val usedFraction: Float
        get() = if (totalBytes > 0L) (usedBytes.toDouble() / totalBytes.toDouble()).toFloat().coerceIn(0f, 1f) else 0f

    val usedPercent: Int get() = (usedFraction * 100f).roundToInt()

    fun pressure(thresholdPercent: Int): MemoryPressure = when {
        systemLowMemory -> MemoryPressure.CRITICAL
        usedPercent >= thresholdPercent -> MemoryPressure.HIGH
        usedPercent >= thresholdPercent - 10 -> MemoryPressure.ELEVATED
        else -> MemoryPressure.NORMAL
    }
}

enum class MemoryPressure(val label: String, val summary: String) {
    NORMAL(
        "Normal",
        "Estimated usage is below your warning threshold. Android manages memory automatically."
    ),
    ELEVATED(
        "Elevated",
        "Estimated usage is approaching your warning threshold. This is common and usually harmless."
    ),
    HIGH(
        "High",
        "Estimated usage has reached your warning threshold. Consider closing apps you no longer need."
    ),
    CRITICAL(
        "Critical",
        "Android reports a low-memory state. Close unneeded apps; Android may also stop background apps on its own."
    )
}

/** Either a reading or an error message. Both null means "not read yet". */
data class RamState(val info: RamInfo?, val error: String?)
