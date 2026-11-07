package com.ramboosterx.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.ramboosterx.app.data.AppEntry
import com.ramboosterx.app.data.AppLoadState
import com.ramboosterx.app.data.InstalledAppsRepository
import com.ramboosterx.app.data.SettingsRepository
import com.ramboosterx.app.model.MemoryPressure
import com.ramboosterx.app.model.RamState
import com.ramboosterx.app.ui.theme.BgDeep
import com.ramboosterx.app.ui.theme.CardBg
import com.ramboosterx.app.ui.theme.DangerRed
import com.ramboosterx.app.ui.theme.NeonCyan
import com.ramboosterx.app.ui.theme.NeonPurple
import com.ramboosterx.app.ui.theme.TextPrimary
import com.ramboosterx.app.ui.theme.TextSecondary
import com.ramboosterx.app.ui.theme.TrackColor
import com.ramboosterx.app.ui.theme.WarnAmber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun BackgroundAppsScreen(
    appsRepository: InstalledAppsRepository,
    ramState: RamState,
    thresholdPercent: Int,
    targetCount: Int,
    onTargetChange: (Int) -> Unit,
    onTargetCommit: () -> Unit,
    showSystemApps: Boolean,
    onShowSystemApps: (Boolean) -> Unit,
    showIcons: Boolean,
    onShowIcons: (Boolean) -> Unit,
    selected: Set<String>,
    onToggleApp: (String) -> Unit,
    onClearSelection: () -> Unit,
    cleanRunning: Boolean,
    onCleanNow: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var reloadKey by remember { mutableIntStateOf(0) }
    var loadState by remember { mutableStateOf<AppLoadState>(AppLoadState.Loading) }
    var query by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(reloadKey) {
        loadState = AppLoadState.Loading
        loadState = appsRepository.loadLaunchableApps()
    }

    val pressure = ramState.info?.pressure(thresholdPercent)
    val highPressure = pressure != null && pressure >= MemoryPressure.HIGH

    val visibleApps: List<AppEntry> = remember(loadState, showSystemApps, query) {
        val s = loadState
        if (s is AppLoadState.Loaded) {
            val q = query.trim()
            s.apps.filter { app ->
                (showSystemApps || !app.isSystem) &&
                    (q.isEmpty() ||
                        app.label.contains(q, ignoreCase = true) ||
                        app.packageName.contains(q, ignoreCase = true))
            }
        } else {
            emptyList()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader("Background App Manager") }

        item {
            NeonCard(accent = NeonPurple) {
                SectionTitle("What this screen can do")
                Text(
                    "Android does not allow normal apps to list running processes or force-stop other apps, and " +
                        "RAM Booster X does not work around that. Build a list of apps here; cleanup asks Android " +
                        "to stop their background processes, and each app's Info page lets you restrict it yourself.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    "Selecting an app only adds it to your list. Nothing is stopped until cleanup runs, and apps may restart.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (highPressure) {
            item {
                NeonCard(accent = DangerRed) {
                    Text(
                        "Memory pressure is ${pressure?.label?.lowercase()}",
                        fontWeight = FontWeight.Bold,
                        color = DangerRed
                    )
                    Text(
                        "Based on real RAM statistics (usage vs. your $thresholdPercent% threshold). " +
                            "Consider reviewing the apps in your list below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        item {
            NeonCard {
                SectionTitle("Preferred background-app count")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Target", color = TextSecondary)
                    Text(
                        "$targetCount",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
                NeonSlider(
                    value = targetCount.toFloat(),
                    onValueChange = { onTargetChange(it.roundToInt()) },
                    onValueChangeFinished = onTargetCommit,
                    range = SettingsRepository.MIN_TARGET.toFloat()..SettingsRepository.MAX_TARGET.toFloat(),
                    steps = SettingsRepository.MAX_TARGET - SettingsRepository.MIN_TARGET - 1
                )
                Text(
                    "A personal goal for how many apps you want to keep active. Android cannot enforce it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    "Apps in your list: ${selected.size}" +
                        if (selected.size > targetCount) " (more than your target of $targetCount)" else "",
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected.size > targetCount) WarnAmber else TextPrimary
                )
            }
        }

        item {
            NeonCard {
                SectionTitle("Preferences")
                ToggleRow(
                    "Show system apps",
                    "Include preinstalled apps that have a launcher icon.",
                    showSystemApps,
                    onShowSystemApps
                )
                ToggleRow(
                    "Show app icons",
                    "Turn off for a lighter, faster list.",
                    showIcons,
                    onShowIcons
                )
            }
        }

        item {
            NeonCard(accent = NeonPurple) {
                SectionTitle("How to restrict an app manually")
                val steps = listOf(
                    "Tap Info next to an app to open its Android App info page.",
                    "Open Battery (wording varies by phone) and choose Restricted, or tap Force stop if shown.",
                    "Many phones also have Settings > Battery > Background usage limits.",
                    "Android may restart a stopped app when you open it or when the system needs it."
                )
                steps.forEachIndexed { i, s ->
                    Text("${i + 1}. $s", style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    "Advanced: Developer options > Limit background processes is a system-wide setting. " +
                        "It can hurt performance or stability; change it only if you understand it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                NeonButton("Open Android app list", { openAppList(context) }, Modifier.fillMaxWidth(), filled = false)
                NeonButton("Open battery usage", { openBatteryUsage(context) }, Modifier.fillMaxWidth(), filled = false)
                NeonButton("Open Developer options", { openDeveloperOptions(context) }, Modifier.fillMaxWidth(), filled = false)
            }
        }

        item {
            NeonCard {
                SectionTitle("Your app list")
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search apps") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = TextSecondary,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = NeonCyan
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeonButton(
                        "Refresh apps",
                        { reloadKey++ },
                        Modifier.weight(1f),
                        Icons.Default.Refresh
                    )
                    NeonButton(
                        "Clear list",
                        onClearSelection,
                        Modifier.weight(1f),
                        filled = false
                    )
                }
                NeonButton(
                    if (cleanRunning) "Cleaning…" else "Stop background processes of listed apps",
                    onCleanNow,
                    Modifier.fillMaxWidth(),
                    enabled = !cleanRunning && selected.isNotEmpty()
                )
                val s = loadState
                if (s is AppLoadState.Loaded) {
                    Text(
                        "Showing ${visibleApps.size} of ${s.apps.size} visible apps. Only apps with a launcher icon " +
                            "are visible to RAM Booster X (Android package-visibility rules); background " +
                            "processes are never listed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        when (val s = loadState) {
            is AppLoadState.Loading -> item {
                Text("Loading apps…", color = TextSecondary)
            }
            is AppLoadState.Failed -> item {
                NeonCard(accent = DangerRed) {
                    Text("Could not load apps", fontWeight = FontWeight.Bold, color = DangerRed)
                    Text(s.message, color = TextSecondary)
                    Text(
                        "You can still manage apps from Android's own app list.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    NeonButton("Open Android app list", { openAppList(context) }, Modifier.fillMaxWidth())
                }
            }
            is AppLoadState.Loaded -> {
                if (visibleApps.isEmpty()) {
                    item {
                        NeonCard {
                            Text("No apps to show", fontWeight = FontWeight.Bold)
                            Text(
                                if (s.apps.isEmpty()) {
                                    "Android did not expose any launcher apps to this app. Use the Android app list instead."
                                } else {
                                    "Nothing matches your search or filters. Try clearing the search or enabling system apps."
                                },
                                color = TextSecondary
                            )
                            NeonButton("Open Android app list", { openAppList(context) }, Modifier.fillMaxWidth(), filled = false)
                        }
                    }
                } else {
                    items(visibleApps, key = { it.packageName }) { app ->
                        AppRow(
                            app = app,
                            checked = app.packageName in selected,
                            showIcon = showIcons,
                            onToggle = { onToggleApp(app.packageName) },
                            onInfo = { openAppInfo(context, app.packageName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(
    app: AppEntry,
    checked: Boolean,
    showIcon: Boolean,
    onToggle: () -> Unit,
    onInfo: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (showIcon) {
            AppIcon(app.packageName, Modifier.size(40.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                app.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                app.packageName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        TextButton(onClick = onInfo) { Text("Info", color = NeonCyan) }
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = NeonCyan,
                uncheckedColor = TextSecondary,
                checkmarkColor = BgDeep
            )
        )
    }
}

/** Loads one icon lazily at small size so the list stays light on memory. */
@Composable
private fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, packageName) {
        value = withContext(Dispatchers.Default) {
            try {
                context.packageManager.getApplicationIcon(packageName).toBitmap(96, 96).asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }
    val bmp = bitmap
    if (bmp != null) {
        Image(bitmap = bmp, contentDescription = null, modifier = modifier)
    } else {
        Box(modifier = modifier.clip(CircleShape).background(TrackColor))
    }
}
