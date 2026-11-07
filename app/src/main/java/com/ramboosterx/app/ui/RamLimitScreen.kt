package com.ramboosterx.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ramboosterx.app.data.CleanResult
import com.ramboosterx.app.data.SettingsRepository
import com.ramboosterx.app.model.MemoryPressure
import com.ramboosterx.app.model.RamState
import com.ramboosterx.app.ui.theme.DangerRed
import com.ramboosterx.app.ui.theme.NeonCyan
import com.ramboosterx.app.ui.theme.NeonPurple
import com.ramboosterx.app.ui.theme.OkGreen
import com.ramboosterx.app.ui.theme.TextPrimary
import com.ramboosterx.app.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun RamLimitScreen(
    ramState: RamState,
    thresholdPercent: Int,
    onThresholdChange: (Int) -> Unit,
    onThresholdCommit: () -> Unit,
    onReset: () -> Unit,
    onRefresh: () -> Unit,
    autoCleanEnabled: Boolean,
    onAutoCleanChange: (Boolean) -> Unit,
    cleanRunning: Boolean,
    lastClean: CleanResult?,
    onCleanNow: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val info = ramState.info
    val error = ramState.error
    val exceeded = info != null && (info.usedPercent >= thresholdPercent || info.systemLowMemory)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader("RAM Limit Manager")

        // ---- Status ----
        if (info == null) {
            NeonCard(accent = if (error != null) DangerRed else NeonCyan) {
                Text(
                    if (error != null) "Memory information unavailable" else "Reading memory information…",
                    fontWeight = FontWeight.Bold
                )
                if (error != null) Text(error, color = TextSecondary)
            }
        } else {
            val pressure = info.pressure(thresholdPercent)
            NeonCard(accent = pressure.color()) {
                SectionTitle("Current status")
                Text(
                    if (exceeded) {
                        "Warning: estimated usage is ${info.usedPercent}%, at or above your ${thresholdPercent}% threshold."
                    } else {
                        "OK: estimated usage is ${info.usedPercent}%, below your ${thresholdPercent}% threshold."
                    },
                    fontWeight = FontWeight.Bold,
                    color = if (exceeded) pressure.color() else OkGreen
                )
                NeonProgressBar(
                    fraction = info.usedFraction,
                    color = pressure.color(),
                    markerFraction = thresholdPercent / 100f
                )
                StatRow("Estimated used", "${formatBytes(info.usedBytes)} of ${formatBytes(info.totalBytes)}")
                StatRow("Available", formatBytes(info.availableBytes), OkGreen)
                StatRow("Memory pressure", pressure.label, pressure.color())
                if (info.systemLowMemory) {
                    Text(
                        "Android is reporting a low-memory state right now.",
                        color = DangerRed,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    "The white marker shows your threshold. Used RAM is an estimate; Android manages memory automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // ---- Threshold ----
        NeonCard(accent = NeonPurple) {
            SectionTitle("Warning threshold")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Warn me at", color = TextSecondary)
                Text("$thresholdPercent%", fontWeight = FontWeight.Bold, color = NeonCyan,
                    style = MaterialTheme.typography.titleLarge)
            }
            NeonSlider(
                value = thresholdPercent.toFloat(),
                onValueChange = { onThresholdChange(it.roundToInt()) },
                onValueChangeFinished = onThresholdCommit,
                range = SettingsRepository.MIN_THRESHOLD.toFloat()..SettingsRepository.MAX_THRESHOLD.toFloat(),
                steps = SettingsRepository.MAX_THRESHOLD - SettingsRepository.MIN_THRESHOLD - 1
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${SettingsRepository.MIN_THRESHOLD}%", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("Default ${SettingsRepository.DEFAULT_THRESHOLD}%", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("${SettingsRepository.MAX_THRESHOLD}%", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Text(
                "This is the level that triggers warnings and optional auto-clean. It is not a hard RAM limit: Android " +
                    "does not let apps cap physical memory.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeonButton("Reset to ${SettingsRepository.DEFAULT_THRESHOLD}%", onReset, Modifier.weight(1f), filled = false)
                NeonButton("Refresh", onRefresh, Modifier.weight(1f), Icons.Default.Refresh)
            }
        }

        // ---- Automatic cleanup ----
        NeonCard {
            SectionTitle("Automatic cleanup")
            ToggleRow(
                "Auto-clean at threshold",
                "When estimated RAM use reaches $thresholdPercent% (or Android reports low memory), " +
                    "stop background processes of your listed apps automatically.",
                autoCleanEnabled,
                onAutoCleanChange
            )
            Text(
                "It runs when you open the app or press Refresh, at most once every 2 minutes. " +
                    "There is no background service, so nothing runs while the app is closed.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                "Cleanup asks Android to stop background processes (not a force stop). Android may restart " +
                    "apps, and RAM may not increase. The result below is measured.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            NeonButton(
                if (cleanRunning) "Cleaning…" else "Clean now",
                onCleanNow,
                Modifier.fillMaxWidth(),
                enabled = !cleanRunning
            )
            if (lastClean != null) CleanResultSummary(lastClean)
        }

        // ---- Recommendations ----
        NeonCard {
            SectionTitle(if (exceeded) "Recommended actions" else "Good habits")
            val tips: List<String> = if (exceeded) {
                listOf(
                    "Close apps you are finished with from the Recents screen.",
                    "Open Battery usage and look for apps that stay active in the background.",
                    "Use each app's App info page to restrict background activity or force stop it (system screen, your choice).",
                    "Disable heavy live wallpapers or widgets you do not need.",
                    "If the phone feels slow, restarting it clears cached processes.",
                    "Otherwise do nothing: Android automatically frees cached apps when memory is needed."
                )
            } else {
                listOf(
                    "No action needed. Free RAM near zero is normal on Android because it caches apps.",
                    "Uninstall or disable apps you never use.",
                    "Restart the phone now and then.",
                    "Press Refresh after launching a game or heavy app to see its effect."
                )
            }
            tips.forEach { tip ->
                Text("• $tip", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
            NeonButton("Open Android app list", { openAppList(context) }, Modifier.fillMaxWidth(), filled = false)
            NeonButton("Open battery usage", { openBatteryUsage(context) }, Modifier.fillMaxWidth(), filled = false)
        }

        Text(
            "Cleanup only asks Android to stop background processes. Android decides what actually happens to memory.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}
