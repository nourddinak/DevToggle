package com.devtoggle.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import rikka.shizuku.Shizuku
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class TileState {
    ON,
    OFF,
    UNAVAILABLE,
}

data class ShizukuStatus(
    val installed: Boolean,
    val running: Boolean,
    val permissionGranted: Boolean,
    val hasNativePermissions: Boolean = false,
) {
    val ready: Boolean
        get() = hasNativePermissions || (installed && running && permissionGranted)
}

data class Capability(
    val title: String,
    val description: String,
    val kind: CapabilityKind,
)

enum class CapabilityKind {
    SUPPORTED,
    DEPENDENT,
    SETTINGS,
    UNSUPPORTED,
}

data class CustomTileConfig(
    val devOptions: Boolean = false,
    val usbDebugging: Boolean = false,
    val wirelessDebugging: Boolean = false
)

class DevToggleRepository(private val context: Context) {
    companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        const val SHIZUKU_REQUEST_CODE = 100
        private const val DEVELOPMENT_SETTINGS_ENABLED = "development_settings_enabled"
        private const val ADB_ENABLED = Settings.Global.ADB_ENABLED
        private const val ADB_WIFI_ENABLED = "adb_wifi_enabled" // API 30+
    }

    private val prefs = context.getSharedPreferences("dev_toggle_prefs", Context.MODE_PRIVATE)

    fun getCustomTileConfig(): CustomTileConfig {
        return CustomTileConfig(
            devOptions = prefs.getBoolean("custom_dev_options", false),
            usbDebugging = prefs.getBoolean("custom_usb_debugging", false),
            wirelessDebugging = prefs.getBoolean("custom_wireless_debugging", false)
        )
    }

    fun saveCustomTileConfig(config: CustomTileConfig) {
        prefs.edit().apply {
            putBoolean("custom_dev_options", config.devOptions)
            putBoolean("custom_usb_debugging", config.usbDebugging)
            putBoolean("custom_wireless_debugging", config.wirelessDebugging)
        }.apply()
    }

    fun hasCompletedOnboarding(): Boolean {
        return prefs.getBoolean("has_completed_onboarding", false)
    }

    fun setCompletedOnboarding(completed: Boolean) {
        prefs.edit().putBoolean("has_completed_onboarding", completed).apply()
    }



    fun shizukuStatus(): ShizukuStatus {
        val native = hasNativePermissions()
        val installed = isShizukuInstalled()
        val running = installed && runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        val permissionGranted = installed && running &&
            runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }
                .getOrDefault(false)
        return ShizukuStatus(installed, running, permissionGranted, native)
    }

    fun isDeveloperOptionsEnabled(): Boolean =
        runCatching {
            Settings.Global.getInt(
                context.contentResolver,
                DEVELOPMENT_SETTINGS_ENABLED,
                0,
            ) == 1
        }.getOrDefault(false)

    fun isUsbDebuggingEnabled(): Boolean =
        runCatching {
            Settings.Global.getInt(
                context.contentResolver,
                ADB_ENABLED,
                0,
            ) == 1
        }.getOrDefault(false)

    fun isWirelessDebuggingEnabled(): Boolean =
        runCatching {
            Settings.Global.getInt(
                context.contentResolver,
                ADB_WIFI_ENABLED,
                0,
            ) == 1
        }.getOrDefault(false)

    fun tileState(): TileState =
        if (!hasNativePermissions()) TileState.UNAVAILABLE
        else if (isDeveloperOptionsEnabled()) TileState.ON else TileState.OFF

    fun specificTileState(enabled: Boolean, requiresApi30: Boolean = false): TileState =
        if (!hasNativePermissions() || (requiresApi30 && android.os.Build.VERSION.SDK_INT < 30)) TileState.UNAVAILABLE
        else if (enabled) TileState.ON else TileState.OFF

    fun openShizuku(): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return runCatching {
                context.startActivity(intent)
                true
            }.getOrDefault(false)
        }
        return false
    }

    fun openUrl(url: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun openDeveloperSettings(): Boolean {
        val primary = Intent("android.settings.APPLICATION_DEVELOPMENT_SETTINGS")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val fallback = Intent(Settings.ACTION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(primary)
            true
        }.getOrElse {
            runCatching {
                context.startActivity(fallback)
                true
            }.getOrDefault(false)
        }
    }

    fun openAppDetails(): Boolean =
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:$SHIZUKU_PACKAGE")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
            true
        }.getOrDefault(false)

    fun isShizukuInstalled(): Boolean =
        runCatching {
            context.packageManager.getApplicationInfo(SHIZUKU_PACKAGE, 0)
            true
        }.getOrDefault(false)

    fun requestPermission() {
        if (shizukuStatus().running && !shizukuStatus().permissionGranted) {
            Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
        }
    }

    private suspend fun executeShizukuCommand(command: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
            val newProcessMethod = shizukuClass.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            // Prevent pipe buffer deadlocks
            process.inputStream.bufferedReader().readText()
            process.errorStream.bufferedReader().readText()
            process.waitFor() == 0
        }.getOrDefault(false)
    }

    fun hasNativePermissions(): Boolean {
        return androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            "android.permission.WRITE_SECURE_SETTINGS"
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    suspend fun grantPermissions() = withContext(Dispatchers.IO) {
        if (!shizukuStatus().ready || hasNativePermissions()) return@withContext
        
        runCatching {
            val pkg = context.packageName
            executeShizukuCommand("pm grant $pkg android.permission.WRITE_SECURE_SETTINGS")
            executeShizukuCommand("appops set $pkg WRITE_SETTINGS allow")
        }
    }

    private suspend fun toggleSetting(namespace: String, key: String, enable: Boolean): Boolean = withContext(Dispatchers.IO) {
        val value = if (enable) 1 else 0
        val nativeSuccess = runCatching {
            if (namespace == "global") {
                Settings.Global.putInt(context.contentResolver, key, value)
            } else {
                Settings.System.putInt(context.contentResolver, key, value)
            }
            true
        }.getOrDefault(false)
        
        if (!nativeSuccess && isShizukuInstalled() && runCatching { Shizuku.pingBinder() }.getOrDefault(false)) {
            return@withContext executeShizukuCommand("settings put $namespace $key $value")
        }
        return@withContext nativeSuccess
    }

    suspend fun toggleDeveloperOptions(enable: Boolean): Boolean =
        toggleSetting("global", DEVELOPMENT_SETTINGS_ENABLED, enable)

    suspend fun toggleUsbDebugging(enable: Boolean): Boolean =
        toggleSetting("global", ADB_ENABLED, enable)

    suspend fun toggleWirelessDebugging(enable: Boolean): Boolean =
        toggleSetting("global", ADB_WIFI_ENABLED, enable)

    fun capabilities(): List<Capability> = listOf(
        Capability(
            "Read Developer Options state",
            "Available on Android versions that expose the system setting to apps.",
            CapabilityKind.SUPPORTED,
        ),
        Capability(
            "Shizuku service and permission checks",
            "The app can verify whether Shizuku is installed, running, and authorized.",
            CapabilityKind.SUPPORTED,
        ),
        Capability(
            "Enable or disable Developer Options directly",
            "Android protects this setting from third-party apps, including this app.",
            CapabilityKind.SETTINGS,
        ),
        Capability(
            "Open the system Developer Options page",
            "Always available as a user-controlled fallback when a direct operation is restricted.",
            CapabilityKind.SUPPORTED,
        ),
    )
}