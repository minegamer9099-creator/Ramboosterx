package com.ramboosterx.app.data

import android.content.Context

/** Local-only preferences (SharedPreferences). Nothing leaves the device. */
class SettingsRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("ramboosterx_prefs", Context.MODE_PRIVATE)

    var threshold: Int
        get() = prefs.getInt(K_THRESHOLD, DEFAULT_THRESHOLD).coerceIn(MIN_THRESHOLD, MAX_THRESHOLD)
        set(value) {
            prefs.edit().putInt(K_THRESHOLD, value.coerceIn(MIN_THRESHOLD, MAX_THRESHOLD)).apply()
        }

    var targetBackgroundApps: Int
        get() = prefs.getInt(K_TARGET, DEFAULT_TARGET).coerceIn(MIN_TARGET, MAX_TARGET)
        set(value) {
            prefs.edit().putInt(K_TARGET, value.coerceIn(MIN_TARGET, MAX_TARGET)).apply()
        }

    var showSystemApps: Boolean
        get() = prefs.getBoolean(K_SHOW_SYSTEM, false)
        set(value) {
            prefs.edit().putBoolean(K_SHOW_SYSTEM, value).apply()
        }

    var showIcons: Boolean
        get() = prefs.getBoolean(K_SHOW_ICONS, true)
        set(value) {
            prefs.edit().putBoolean(K_SHOW_ICONS, value).apply()
        }

    var autoCleanEnabled: Boolean
        get() = prefs.getBoolean(K_AUTO_CLEAN, false)
        set(value) {
            prefs.edit().putBoolean(K_AUTO_CLEAN, value).apply()
        }

    var selectedPackages: Set<String>
        get() = prefs.getStringSet(K_SELECTED, emptySet())?.toSet() ?: emptySet()
        set(value) {
            prefs.edit().putStringSet(K_SELECTED, HashSet(value)).apply()
        }

    fun resetThreshold() {
        prefs.edit().remove(K_THRESHOLD).apply()
    }

    companion object {
        const val MIN_THRESHOLD = 50
        const val MAX_THRESHOLD = 95
        const val DEFAULT_THRESHOLD = 85
        const val MIN_TARGET = 1
        const val MAX_TARGET = 10
        const val DEFAULT_TARGET = 5

        private const val K_THRESHOLD = "ram_threshold"
        private const val K_TARGET = "bg_target"
        private const val K_SHOW_SYSTEM = "show_system_apps"
        private const val K_SHOW_ICONS = "show_icons"
        private const val K_AUTO_CLEAN = "auto_clean"
        private const val K_SELECTED = "selected_packages"
    }
}
