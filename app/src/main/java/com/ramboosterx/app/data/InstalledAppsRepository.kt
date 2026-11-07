package com.ramboosterx.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppEntry(
    val packageName: String,
    val label: String,
    val isSystem: Boolean
)

sealed interface AppLoadState {
    object Loading : AppLoadState
    data class Loaded(val apps: List<AppEntry>) : AppLoadState
    data class Failed(val message: String) : AppLoadState
}

/**
 * Lists apps that have a launcher icon, using the package-visibility <queries> entry
 * declared in the manifest. It does NOT list running processes (Android does not allow that).
 */
class InstalledAppsRepository(private val context: Context) {

    suspend fun loadLaunchableApps(): AppLoadState = withContext(Dispatchers.IO) {
        loadBlocking()
    }

    private fun loadBlocking(): AppLoadState {
        return try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolved = if (Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
            }
            val apps = resolved
                .mapNotNull { ri ->
                    val ai = ri.activityInfo?.applicationInfo ?: return@mapNotNull null
                    if (ai.packageName == context.packageName) return@mapNotNull null
                    val label = ri.loadLabel(pm).toString()
                    AppEntry(
                        packageName = ai.packageName,
                        label = if (label.isBlank()) ai.packageName else label,
                        isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }
            AppLoadState.Loaded(apps)
        } catch (e: Exception) {
            AppLoadState.Failed("Could not read installed apps: ${e.message ?: e.javaClass.simpleName}")
        }
    }
}
