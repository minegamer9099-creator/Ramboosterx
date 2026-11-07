package com.ramboosterx.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.ramboosterx.app.data.InstalledAppsRepository
import com.ramboosterx.app.data.RamRepository
import com.ramboosterx.app.data.SettingsRepository
import com.ramboosterx.app.model.RamState
import com.ramboosterx.app.ui.BackgroundAppsScreen
import com.ramboosterx.app.ui.DashboardScreen
import com.ramboosterx.app.ui.RamLimitScreen
import com.ramboosterx.app.ui.theme.BgDeep
import com.ramboosterx.app.ui.theme.BgNavy
import com.ramboosterx.app.ui.theme.RamBoosterXTheme
import com.ramboosterx.app.ui.theme.TextPrimary

class MainActivity : ComponentActivity() {

    // Bumped in onResume so memory is re-read once each time the app is opened (no polling).
    private var resumeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = SettingsRepository(applicationContext)
        val ram = RamRepository(applicationContext)
        val apps = InstalledAppsRepository(applicationContext)
        setContent {
            RamBoosterXTheme {
                RamBoosterXApp(settings, ram, apps, resumeTick)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }
}

private enum class Route { DASHBOARD, APPS, LIMIT }

@Composable
private fun RamBoosterXApp(
    settings: SettingsRepository,
    ramRepository: RamRepository,
    appsRepository: InstalledAppsRepository,
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

    LaunchedEffect(resumeTick) { ramState = ramRepository.read() }

    val refresh: () -> Unit = { ramState = ramRepository.read() }

    BackHandler(enabled = route != Route.DASHBOARD) { routeName = Route.DASHBOARD.name }

    CompositionLocalProvider(LocalContentColor provides TextPrimary) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BgDeep, BgNavy)))
        ) {
            Crossfade(targetState = route, animationSpec = tween(200), label = "nav") { r ->
                when (r) {
                    Route.DASHBOARD -> DashboardScreen(
                        ramState = ramState,
                        thresholdPercent = threshold,
                        selectedCount = selected.size,
                        onRefresh = refresh,
                        onOpenApps = { routeName = Route.APPS.name },
                        onOpenLimit = { routeName = Route.LIMIT.name }
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
                        onBack = { routeName = Route.DASHBOARD.name }
                    )
                }
            }
        }
    }
}
