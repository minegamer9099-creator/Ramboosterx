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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
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
import com.ramboosterx.app.model.RamState
import com.ramboosterx.app.ui.theme.DangerRed
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
    selectedCount: Int,
    onRefresh: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenLimit: () -> Unit
) {
    val info = ramState.info
    val error = ramState.error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = "RAM Booster X logo",
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(TrackColor)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "RAM Booster X",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Text(
                    "Manual memory monitor",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
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

            NeonCard(accent = pressure.color()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(pressure.color())
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Memory pressure: ${pressure.label}",
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(pressure.summary, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }

            NeonCard {
                SectionTitle("System RAM")
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    UsageRing(
                        fraction = info.usedFraction,
                        centerTop = "${info.usedPercent}%",
                        centerBottom = "used (estimate)",
                        color = pressure.color()
                    )
                }
                StatRow("Total physical RAM", formatBytes(info.totalBytes))
                StatRow("Available RAM", formatBytes(info.availableBytes), OkGreen)
                StatRow("Estimated used RAM", formatBytes(info.usedBytes), pressure.color())
                StatRow("Android low-memory level", formatBytes(info.systemThresholdBytes))
                StatRow(
                    "Last refreshed",
                    DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(info.readAtMillis))
                )
                Text(
                    "Used RAM is an estimate (total minus available). Android deliberately keeps memory " +
                        "filled with cached apps and manages it automatically, so high usage is normal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            NeonButton("Refresh", onRefresh, Modifier.fillMaxWidth(), Icons.Default.Refresh)

            NeonCard(accent = NeonPurple) {
                SectionTitle("This app's own memory")
                StatRow(
                    "Process memory (PSS)",
                    info.appPssBytes?.let { formatBytes(it) } ?: "Unavailable"
                )
                StatRow("Java heap in use", formatBytes(info.appHeapUsedBytes))
                StatRow("Java heap limit", formatBytes(info.appHeapMaxBytes))
                Text(
                    "These numbers describe only RAM Booster X, not the whole system.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        NeonButton(
            "Background App Manager" + if (selectedCount > 0) " ($selectedCount in list)" else "",
            onOpenApps,
            Modifier.fillMaxWidth(),
            Icons.Default.Settings,
            filled = false
        )
        NeonButton(
            "RAM Limit Manager (warning at $thresholdPercent%)",
            onOpenLimit,
            Modifier.fillMaxWidth(),
            Icons.Default.Info,
            filled = false
        )

        NeonCard(accent = NeonPurple) {
            SectionTitle("Permissions & capabilities")
            CapabilityRow(
                "RAM statistics",
                "Read through ActivityManager.MemoryInfo.",
                info != null
            )
            CapabilityRow(
                "Memory-pressure status",
                "Derived from Android's low-memory flag and your own warning threshold.",
                info != null
            )
            CapabilityRow(
                "App discovery",
                "Apps with a launcher icon, via package-visibility rules. No QUERY_ALL_PACKAGES.",
                true
            )
            CapabilityRow(
                "Permissions requested",
                "None. No root, Accessibility, overlay, usage-access or internet access.",
                true
            )
            CapabilityRow(
                "Background monitoring",
                "Off by design: no services, no polling. Updates only on open or Refresh.",
                true
            )
            CapabilityRow(
                "Force-stopping or clearing other apps' RAM",
                "Not available to normal Android apps, so it is not implemented. " +
                    "Use the system App info screens instead.",
                false
            )
        }

        Text(
            "Android manages memory automatically. RAM Booster X shows real numbers and guides you " +
                "to safe system settings; it does not free memory by itself.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun CapabilityRow(title: String, detail: String, available: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            if (available) "●" else "○",
            color = if (available) OkGreen else WarnAmber
        )
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}
