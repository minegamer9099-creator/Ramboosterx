package com.ramboosterx.app.data

import android.app.ActivityManager
import android.content.Context
import android.os.Process
import com.ramboosterx.app.model.RamInfo
import com.ramboosterx.app.model.RamState

/** Reads memory statistics on demand. No polling, no background work. */
class RamRepository(private val context: Context) {

    fun read(): RamState {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return RamState(null, "ActivityManager is not available on this device.")

            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            if (mi.totalMem <= 0L) {
                return RamState(null, "Android did not report total memory.")
            }

            val pssBytes: Long? = try {
                val pssKb = am.getProcessMemoryInfo(intArrayOf(Process.myPid())).firstOrNull()?.totalPss
                if (pssKb != null && pssKb > 0) pssKb.toLong() * 1024L else null
            } catch (e: Exception) {
                null
            }

            val rt = Runtime.getRuntime()
            RamState(
                info = RamInfo(
                    totalBytes = mi.totalMem,
                    availableBytes = mi.availMem,
                    systemThresholdBytes = mi.threshold,
                    systemLowMemory = mi.lowMemory,
                    appPssBytes = pssBytes,
                    appHeapUsedBytes = rt.totalMemory() - rt.freeMemory(),
                    appHeapMaxBytes = rt.maxMemory(),
                    readAtMillis = System.currentTimeMillis()
                ),
                error = null
            )
        } catch (e: Exception) {
            RamState(null, "Could not read memory information: ${e.message ?: e.javaClass.simpleName}")
        }
    }
}
