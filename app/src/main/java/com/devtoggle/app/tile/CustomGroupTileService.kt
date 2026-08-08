package com.devtoggle.app.tile

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.devtoggle.app.MainActivity
import com.devtoggle.app.data.DevToggleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CustomGroupTileService : TileService() {
    private val repository by lazy { DevToggleRepository(this) }

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        
        if (!repository.hasNativePermissions()) {
            tile.state = Tile.STATE_UNAVAILABLE
            tile.subtitle = "Setup Required"
            tile.updateTile()
            return
        }

        val config = repository.getCustomTileConfig()
        
        var isAnyOn = false
        var hasActiveConfig = false

        if (config.devOptions) {
            hasActiveConfig = true
            if (repository.isDeveloperOptionsEnabled()) isAnyOn = true
        }
        if (config.usbDebugging) {
            hasActiveConfig = true
            if (repository.isUsbDebuggingEnabled()) isAnyOn = true
        }
        if (config.wirelessDebugging) {
            hasActiveConfig = true
            if (repository.isWirelessDebuggingEnabled()) isAnyOn = true
        }

        if (!hasActiveConfig) {
            tile.state = Tile.STATE_INACTIVE
            tile.subtitle = "Not Configured"
            tile.updateTile()
            return
        }

        // If any selected toggle is ON, the group tile is considered ON.
        if (isAnyOn) {
            tile.state = Tile.STATE_ACTIVE
            tile.subtitle = "ON"
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.subtitle = "OFF"
        }
        
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
        
        if (!repository.hasNativePermissions()) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = android.app.PendingIntent.getActivity(
                    this, 0, intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
            return
        }

        executeGroupToggle()
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private fun executeGroupToggle() {
        val config = repository.getCustomTileConfig()
        val tile = qsTile ?: return
        val targetState = tile.state == Tile.STATE_INACTIVE

        scope.launch {
            if (targetState) {
                if (config.devOptions) repository.toggleDeveloperOptions(true)
                if (config.usbDebugging) repository.toggleUsbDebugging(true)
                if (config.wirelessDebugging) repository.toggleWirelessDebugging(true)
            } else {
                if (config.devOptions) repository.toggleDeveloperOptions(false)
                if (config.usbDebugging) repository.toggleUsbDebugging(false)
                if (config.wirelessDebugging) repository.toggleWirelessDebugging(false)
            }
            withContext(Dispatchers.Main) {
                refreshTile()
                val context = applicationContext
                TileService.requestListeningState(context, ComponentName(context, DeveloperOptionsTileService::class.java))
                TileService.requestListeningState(context, ComponentName(context, UsbDebuggingTileService::class.java))
                TileService.requestListeningState(context, ComponentName(context, WirelessDebuggingTileService::class.java))
            }
        }
    }
}
