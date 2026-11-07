package com.ramboosterx.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgDeep = Color(0xFF05070F)
val BgNavy = Color(0xFF0A1020)
val CardBg = Color(0xFF0E1630)
val TrackColor = Color(0xFF1B2747)
val NeonCyan = Color(0xFF00E5FF)
val ElectricBlue = Color(0xFF2979FF)
val NeonPurple = Color(0xFF9C5CFF)
val TextPrimary = Color(0xFFE8F1FF)
val TextSecondary = Color(0xFF8FA3C7)
val OkGreen = Color(0xFF29E6A0)
val WarnAmber = Color(0xFFFFB74D)
val OrangeHigh = Color(0xFFFF8A50)
val DangerRed = Color(0xFFFF5370)

private val GamingColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = BgDeep,
    secondary = ElectricBlue,
    tertiary = NeonPurple,
    background = BgDeep,
    onBackground = TextPrimary,
    surface = CardBg,
    onSurface = TextPrimary,
    surfaceVariant = TrackColor,
    onSurfaceVariant = TextSecondary,
    error = DangerRed
)

@Composable
fun RamBoosterXTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GamingColorScheme,
        typography = Typography(),
        content = content
    )
}
