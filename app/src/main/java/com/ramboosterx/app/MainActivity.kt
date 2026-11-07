package com.ramboosterx.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.ramboosterx.app.data.BackgroundCleaner
import com.ramboosterx.app.data.CleanResult
import com.ramboosterx.app.data.InstalledAppsRepository
import com.ramboosterx.app.data.RamRepository
import com.ramboosterx.app.data.SettingsRepository
import com.ramboosterx.app.model.RamState
import com.ramboosterx.app.ui.BackgroundAppsScreen
import com.ramboosterx.app.ui.BottomNavBar
import com.ramboosterx.app.ui.DashboardScreen
import com.ramboosterx.app.ui.NavItem
import com.ramboosterx.app.ui.RamLimitScreen
import com.ramboosterx.app.ui.theme.BgDeep
import com.ramboosterx.app.ui.theme.BgNavy
import com.ramboosterx.app.ui.theme.RamBoosterXTheme
import com.ramboosterx.app.ui.theme.TextPrimary
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Bumped in onResume so memory is re-read once each time the app is opened (no polling).
    private var resumeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = SettingsRepository(applicationContext)
        val ram = RamRepository(applicationContext)
        val apps = InstalledAppsRepository(applicationContext)
        val cleaner = BackgroundCleaner(applicationContext, ram, apps)
        setContent {
            RamBoosterXTheme {
                RamBoosterXApp(settings, ram, apps, cleaner, resumeTick)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }
}

private enum class Route { DASHBOARD, APPS, LIMIT }

private val NAV_ITEMS = listOf(
    NavItem("Home", Icons.Default.Home),
    NavItem("Apps", Icons.AutoMirrored.Filled.List),
    NavItem("Limit", Icons.Default.Settings)
)

@Composable
private fun RamBoosterXApp(
    settings: SettingsRepository,
    ramRepository: RamRepository,
    appsRepository: InstalledAppsRepository,
    cleaner: BackgroundCleaner,
    resumeTick: Int
) {
    var routeName by rememberSaveable { mutableStateOf(Route.DASHBOARD.name) }
    val route = Route.valueOf(routeName)

    var ramState by remember { mutableStateOf(RamState(null, null)) }
    var threshold by remember { mutableIntStateOf(settings.threshold) }
    var target by remember { mutableIntStateOf(settings.targetBackgroundApps) }
    var showSystem by remember { mutableStateOf(settings.showSystemApps) }
    var showIcons by remember { mutableStateOf(settings.showIcons) }
    var selected by remember { mutableStateOf(settings.selectedPackages) }
    var autoClean by remember { mutableStateOf(settings.autoCleanEnabled) }

    var cleanRunning by remember { mutableStateOf(false) }
    var lastClean by remember { mutableStateOf<CleanResult?>(null) }
    var lastAutoMillis by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()

    val refresh: () -> Unit = { ramState = ramRepository.read() }

    val runClean: () -> Unit = {
        if (!cleanRunning) {
            cleanRunning = true
            scope.launch {
                try {
                    lastClean = cleaner.clean(selected)
                    ramState = ramRepository.read()
                } finally {
                    cleanRunning = false
                }
            }
        }
    }

    // Re-read memory once whenever the app is opened.
    LaunchedEffect(resumeTick) { ramState = ramRepository.read() }

    // Auto-clean: evaluated only when a new reading arrives (open / Refresh), never by polling.
    LaunchedEffect(ramState, autoClean, threshold) {
        val info = ramState.info
        if (autoClean && info != null && !cleanRunning &&
            (info.usedPercent >= threshold || info.systemLowMemory)
        ) {
            val now = System.currentTimeMillis()
            if (now - lastAutoMillis > 120_000L) {
                lastAutoMillis = now
                runClean()
            }
        }
    }

    BackHandler(enabled = route != Route.DASHBOARD) { routeName = Route.DASHBOARD.name }

    CompositionLocalProvider(LocalContentColor provides TextPrimary) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BgDeep, BgNavy)))
        ) {
            Box(Modifier.weight(1f)) {
                Crossfade(targetState = route, animationSpec = tween(200), label = "nav") { r ->
                    when (r) {
                        Route.DASHBOARD -> DashboardScreen(
                            ramState = ramState,
                            thresholdPercent = threshold,
                            autoCleanEnabled = autoClean,
                            selectedCount = selected.size,
                            cleanRunning = cleanRunning,
                            lastClean = lastClean,
                            onBoost = runClean,
                            onRefresh = refresh
                        )

                        Route.APPS -> BackgroundAppsScreen(
                            appsRepository = appsRepository,
                            ramState = ramState,
                            thresholdPercent = threshold,
                            targetCount = target,
                            onTargetChange = { target = it },
                            onTargetCommit = { settings.targetBackgroundApps = target },
                            showSystemApps = showSystem,
                            onShowSystemApps = { showSystem = it; settings.showSystemApps = it },
                            showIcons = showIcons,
                            onShowIcons = { showIcons = it; settings.showIcons = it },
                            selected = selected,
                            onToggleApp = { pkg ->
                                val next = if (pkg in selected) selected - pkg else selected + pkg
                                selected = next
                                settings.selectedPackages = next
                            },
                            onClearSelection = {
                                selected = emptySet()
                                settings.selectedPackages = emptySet()
                            },
                            cleanRunning = cleanRunning,
                            onCleanNow = runClean,
                            onBack = { routeName = Route.DASHBOARD.name }
                        )

                        Route.LIMIT -> RamLimitScreen(
                            ramState = ramState,
                            thresholdPercent = threshold,
                            onThresholdChange = { threshold = it },
                            onThresholdCommit = { settings.threshold = threshold },
                            onReset = {
                                settings.resetThreshold()
                                threshold = settings.threshold
                            },
                            onRefresh = refresh,
                            autoCleanEnabled = autoClean,
                            onAutoCleanChange = { autoClean = it; settings.autoCleanEnabled = it },
                            cleanRunning = cleanRunning,
                            lastClean = lastClean,
                            onCleanNow = runClean,
                            onBack = { routeName = Route.DASHBOARD.name }
                        )
                    }
                }
            }
            BottomNavBar(
                items = NAV_ITEMS,
                selectedIndex = route.ordinal,
                onSelect = { routeName = Route.values()[it].name }
            )
        }
    }
}
