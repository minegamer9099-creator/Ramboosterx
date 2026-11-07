package com.ramboosterx.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

/** Opens a system screen; shows a message instead of crashing if the device lacks it. */
fun safeStart(context: Context, intent: Intent, failMessage: String): Boolean {
    return try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, failMessage, Toast.LENGTH_LONG).show()
        false
    } catch (e: SecurityException) {
        Toast.makeText(context, failMessage, Toast.LENGTH_LONG).show()
        false
    }
}

fun openAppInfo(context: Context, packageName: String) {
    safeStart(
        context,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")),
        "App info is not available for this app on your device."
    )
}

fun openAppList(context: Context) {
    safeStart(
        context,
        Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS),
        "The Android app list is not available on this device."
    )
}

fun openBatteryUsage(context: Context) {
    safeStart(
        context,
        Intent(Intent.ACTION_POWER_USAGE_SUMMARY),
        "Battery usage settings are not available on this device."
    )
}

fun openDeveloperOptions(context: Context) {
    safeStart(
        context,
        Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
        "Developer options are not enabled or not available on this device."
    )
}
