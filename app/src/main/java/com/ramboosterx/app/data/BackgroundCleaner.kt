package com.ramboosterx.app.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class CleanResult(
    val attemptedCount: Int,
    val failedCount: Int,
    val availBeforeBytes: Long,
    val availAfterBytes: Long,
    val finishedAtMillis: Long,
    val usedSelectedList: Boolean,
    val error: String?
) {
    /** Measured difference in available RAM. Can be zero or negative: Android may restart apps. */
    val freedBytes: Long get() = availAfterBytes - availBeforeBytes
}

/**
 * Asks Android to stop BACKGROUND processes of other apps through the public
 * ActivityManager.killBackgroundProcesses() API (permission KILL_BACKGROUND_PROCESSES).
 * This is NOT a force-stop: foreground/system-critical processes are untouched and
 * Android may restart apps at any time. The result is measured, never assumed.
 */
class BackgroundCleaner(
    private val context: Context,
    private val ramRepository: RamRepository,
    private val appsRepository: InstalledAppsRepository
) {

    suspend fun clean(selected: Set<String>): CleanResult = withContext(Dispatchers.Default) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val before = ramRepository.read().info?.availableBytes
        if (am == null || before == null) {
            return@withContext CleanResult(
                0, 0, 0L, 0L, System.currentTimeMillis(), selected.isNotEmpty(),
                "Memory information is not available."
            )
        }

        val useSelected = selected.isNotEmpty()
        val targets: Set<String> = if (useSelected) {
            selected
        } else {
            val s = appsRepository.loadLaunchableApps()
            if (s is AppLoadState.Loaded) {
                s.apps.filter { !it.isSystem }.map { it.packageName }.toSet()
            } else {
                emptySet()
            }
        }

        val protectedPackages = protectedPackages()
        var attempted = 0
        var failed = 0
        for (pkg in targets) {
            if (pkg == context.packageName || pkg in protectedPackages) continue
            try {
                am.killBackgroundProcesses(pkg)
                attempted++
            } catch (e: Exception) {
                failed++
            }
        }

        delay(1500) // give Android a moment before measuring
        val after = ramRepository.read().info?.availableBytes ?: before
        CleanResult(attempted, failed, before, after, System.currentTimeMillis(), useSelected, null)
    }

    private fun protectedPackages(): Set<String> {
        return try {
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            @Suppress("DEPRECATION")
            val pkg = context.packageManager
                .resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
            if (pkg != null) setOf(pkg) else emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }
}
