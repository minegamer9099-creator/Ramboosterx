package com.ramboosterx.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ramboosterx.app.R
import com.ramboosterx.app.data.CleanResult
import com.ramboosterx.app.model.RamState
import com.ramboosterx.app.ui.theme.DangerRed
import com.ramboosterx.app.ui.theme.ElectricBlue
import com.ramboosterx.app.ui.theme.NeonCyan
import com.ramboosterx.app.ui.theme.NeonPurple
import com.ramboosterx.app.ui.theme.OkGreen
import com.ramboosterx.app.ui.theme.TextPrimary
import com.ramboosterx.app.ui.theme.TextSecondary
import com.ramboosterx.app.ui.theme.TrackColor
import com.ramboosterx.app.ui.theme.WarnAmber
import java.text.DateFormat
import java.util.Date

@Composable
fun DashboardScreen(
    ramState: RamState,
    thresholdPercent: Int,
    autoCleanEnabled: Boolean,
    selectedCount: Int,
    cleanRunning: Boolean,
    lastClean: CleanResult?,
    onBoost: () -> Unit,
    onRefresh: () -> Unit
) {
    val info = ramState.info
    val error = ramState.error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ---- Header ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = "RAM Booster X logo",
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(TrackColor)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "RAM BOOSTER X",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonCyan
                )
                Text("Manual memory monitor", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            if (info != null) {
                val pr = info.pressure(thresholdPercent)
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(pr.color().copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(pr.color()))
                    Spacer(Modifier.width(6.dp))
                    Text(pr.label, color = pr.color(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        if (info == null) {
            NeonCard(accent = if (error != null) DangerRed else NeonCyan) {
                Text(
                    if (error != null) "Memory information unavailable" else "Reading memory information…",
                    fontWeight = FontWeight.Bold
                )
                if (error != null) {
                    Text(error, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
            NeonButton("Try again", onRefresh, Modifier.fillMaxWidth(), Icons.Default.Refresh)
        } else {
            val pressure = info.pressure(thresholdPercent)

            // ---- Hero ----
            NeonCard(accent = pressure.color()) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    UsageRing(
                        fraction = info.usedFraction,
                        centerTop = "${info.usedPercent}%",
                        centerBottom = "RAM used (est.)",
                        color = pressure.color(),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip("Total", formatBytes(info.totalBytes), ElectricBlue, Modifier.weight(1f))
                    StatChip("Available", formatBytes(info.availableBytes), OkGreen, Modifier.weight(1f))
                    StatChip("Used", formatBytes(info.usedBytes), pressure.color(), Modifier.weight(1f))
                }
                NeonProgressBar(
                    fraction = info.usedFraction,
                    color = pressure.color(),
                    markerFraction = thresholdPercent / 100f
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Warning at $thresholdPercent%", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(
                        "Updated " + DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(info.readAtMillis)),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Text(pressure.summary, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }

            // ---- Actions ----
            NeonButton(
                text = if (cleanRunning) "Cleaning…" else "Clean background apps",
                onClick = onBoost,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                icon = Icons.Default.PlayArrow,
                enabled = !cleanRunning
            )
            NeonButton("Refresh", onRefresh, Modifier.fillMaxWidth(), Icons.Default.Refresh, filled = false)

            // ---- Cleanup status ----
            NeonCard(accent = NeonPurple) {
                SectionTitle("Cleanup")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(10.dp).clip(CircleShape)
                            .background(if (autoCleanEnabled) OkGreen else WarnAmber)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (autoCleanEnabled) "Auto-clean ON at $thresholdPercent% or more (while the app is open)"
                        else "Auto-clean OFF (turn it on in the Limit tab)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    if (selectedCount > 0) "Targets: the $selectedCount apps in your list."
                    else "Targets: all non-system launcher apps (your list is empty).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (lastClean != null) {
                    CleanResultSummary(lastClean)
                } else {
                    Text("No cleanup run yet in this session.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Text(
                    "Cleanup asks Android to stop background processes. It is not a force stop; Android may " +
                        "restart apps and free RAM may not increase.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            NeonCard {
                SectionTitle("This app's own memory")
                StatRow("Process memory (PSS)", info.appPssBytes?.let { formatBytes(it) } ?: "Unavailable")
                StatRow("Java heap in use", formatBytes(info.appHeapUsedBytes))
                StatRow("Java heap limit", formatBytes(info.appHeapMaxBytes))
                Text(
                    "Only RAM Booster X, not total system RAM.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        NeonCard(accent = NeonPurple) {
            SectionTitle("Permissions & capabilities")
            CapabilityRow("RAM statistics", "Read through ActivityManager.MemoryInfo.", info != null)
            CapabilityRow(
                "Stop background processes",
                "Uses the public killBackgroundProcesses API with the normal KILL_BACKGROUND_PROCESSES permission.",
                true
            )
            CapabilityRow(
                "App discovery",
                "Apps with a launcher icon, via package-visibility rules. No QUERY_ALL_PACKAGES.",
                true
            )
            CapabilityRow(
                "Background monitoring",
                "Off by design: no services or polling. Checks happen when you open the app or press Refresh.",
                true
            )
            CapabilityRow(
                "True force-stop / root / Accessibility",
                "Not used and not available to normal apps.",
                false
            )
        }

        Text(
            "Used RAM is an estimate. Android manages memory automatically.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun CapabilityRow(title: String, detail: String, available: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (available) "●" else "○", color = if (available) OkGreen else WarnAmber)
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}
