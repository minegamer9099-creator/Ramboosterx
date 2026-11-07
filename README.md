# RAM Booster X

Native Android app (Kotlin, Jetpack Compose, Material 3). Offline, no permissions, no accounts, no analytics.

- Package: `com.ramboosterx.app` · minSdk 26 · targetSdk/compileSdk 34
- Gradle task: `:app:assembleDebug`

## What it does (and honestly does not do)

| Feature | Status |
|---|---|
| Real RAM stats (total, available, estimated used, %) via `ActivityManager.MemoryInfo` | Implemented |
| Memory-pressure status (Android low-memory flag + your threshold) | Implemented |
| Warning threshold 50–95 % (default 85 %), saved locally, reset button | Implemented |
| App's own memory (PSS / heap), shown separately from system RAM | Implemented |
| Launcher-app list with icons, search, system-app filter, personal "review list" | Implemented (uses `<queries>`, no `QUERY_ALL_PACKAGES`) |
| Shortcuts to App info, app list, battery usage, Developer options | Implemented |
| Force-stopping other apps / clearing other apps' RAM / listing running processes | **Not possible for normal apps; not implemented, not faked** |

Used RAM is an estimate (total − available). Android caches apps on purpose and manages memory itself.
Selecting an app only adds it to your own list; it does not stop or restrict the app.

## Build with AndroidIDE (phone only)

1. Copy `RAMBoosterX.zip` to the phone and extract it (e.g. into `AndroidIDEProjects/`).
2. In AndroidIDE choose **Open project** and select the `RAMBoosterX` folder.
3. Wait for Gradle sync to finish (first sync downloads dependencies, so you need internet once).
4. If AndroidIDE says the SDK is missing, install it from its setup screen: **Android SDK Platform 34** and Build-Tools 34.x. Use JDK 17.
5. Open the Gradle/quick-run menu and run **`:app:assembleDebug`**.
6. The APK is at `app/build/outputs/apk/debug/app-debug.apk`. Open it with a file manager and install (allow "install unknown apps" for that file manager if asked).

## Troubleshooting

- **Sync fails / cannot resolve dependencies**: check internet; `google()` and `mavenCentral()` must be reachable. Retry sync.
- **"Unsupported Gradle / AGP version"**: AndroidIDE bundles specific Gradle versions. Keep the AGP/Kotlin/Compose triple consistent: AGP 8.2.2 + Kotlin 1.9.22 + Compose compiler 1.5.10 (set in `build.gradle.kts` and `app/build.gradle.kts`). Gradle wrapper is 8.2 (`gradle/wrapper/gradle-wrapper.properties`).
- **"Compose compiler requires Kotlin x"**: the compiler extension version must match the Kotlin version (1.5.10 ↔ 1.9.22).
- **SDK location not found**: create `local.properties` containing `sdk.dir=/path/to/Android/sdk` (AndroidIDE normally creates it).
- **Out of memory during build**: close other apps; lower `org.gradle.jvmargs` in `gradle.properties` (e.g. `-Xmx1024m`).
- **Install blocked**: enable installing from your file manager in Android settings; uninstall older debug builds signed with another key.

## Project layout

```
app/src/main/java/com/ramboosterx/app/
  MainActivity.kt                navigation + state
  model/RamInfo.kt               data models, pressure logic
  data/RamRepository.kt          ActivityManager readings
  data/SettingsRepository.kt     SharedPreferences
  data/InstalledAppsRepository.kt  launcher-app discovery
  ui/DashboardScreen.kt  BackgroundAppsScreen.kt  RamLimitScreen.kt
  ui/Components.kt  SystemIntents.kt  theme/Theme.kt
```
