package com.ramboosterx.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.ramboosterx.app.data.CleanResult
import com.ramboosterx.app.ui.theme.DangerRed
import com.ramboosterx.app.ui.theme.OkGreen
import com.ramboosterx.app.ui.theme.TextSecondary
import com.ramboosterx.app.ui.theme.WarnAmber
import java.text.DateFormat
import java.util.Date

/** Shows the MEASURED outcome of a cleanup. Numbers come from Android, not from us. */
@Composable
fun CleanResultSummary(result: CleanResult) {
    if (result.error != null) {
        Text("Last cleanup failed: ${result.error}", color = DangerRed, style = MaterialTheme.typography.bodyMedium)
        return
    }
    val time = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(result.finishedAtMillis))
    val scope = if (result.usedSelectedList) "your list" else "all non-system launcher apps"
    Text(
        "Last cleanup at $time: asked Android to stop background processes of ${result.attemptedCount} apps ($scope)" +
            if (result.failedCount > 0) ", ${result.failedCount} failed." else ".",
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary
    )
    StatRow("Available before", formatBytes(result.availBeforeBytes))
    StatRow("Available after", formatBytes(result.availAfterBytes))
    val d = result.freedBytes
    when {
        d > 0L -> StatRow("Measured change", "+${formatBytes(d)}", OkGreen)
        d < 0L -> StatRow("Measured change", "-${formatBytes(-d)}", WarnAmber)
        else -> StatRow("Measured change", "none", WarnAmber)
    }
    if (d <= 0L) {
        Text(
            "No gain is normal: Android may restart apps right away or had little to stop.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}
