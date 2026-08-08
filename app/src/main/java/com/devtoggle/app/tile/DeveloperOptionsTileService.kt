package com.devtoggle.app.tile

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.devtoggle.app.R
import com.devtoggle.app.data.DevToggleRepository
import com.devtoggle.app.data.TileState
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeveloperOptionsTileService : TileService() {
    private val repository by lazy { DevToggleRepository(this) }

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        refreshTile()

        if (!repository.hasNativePermissions()) {
            startActivityAndCollapse(
                android.content.Intent(this, com.devtoggle.app.MainActivity::class.java),
            )
        } else if (isLocked) {
            unlockAndRun { executeToggle() }
        } else {
            executeToggle()
        }
    }

    override fun onTileAdded() {
        super.onTileAdded()
        refreshTile()
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        tile.icon = Icon.createWithResource(this, R.drawable.ic_devtoggle)
        when (repository.tileState()) {
            TileState.ON -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = getString(R.string.tile_label_on)
                tile.contentDescription = getString(R.string.tile_description_on)
                setSubtitle(tile, R.string.tile_subtitle_on)
            }
            TileState.OFF -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.tile_label_off)
                tile.contentDescription = getString(R.string.tile_description_off)
                setSubtitle(tile, R.string.tile_subtitle_off)
            }
            TileState.UNAVAILABLE -> {
                tile.state = Tile.STATE_UNAVAILABLE
                tile.label = getString(R.string.tile_label)
                tile.contentDescription = getString(R.string.tile_description_unavailable)
                setSubtitle(tile, R.string.tile_subtitle_unavailable)
            }
        }
        tile.updateTile()
    }

    private fun setSubtitle(tile: Tile, resourceId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(resourceId)
        }
    }

    private fun executeToggle() {
        val currentState = repository.isDeveloperOptionsEnabled()
        val newState = !currentState
        repository.toggleDeveloperOptions(newState)
        refreshTile()
        
        // If developer options are turned off, we explicitly turn off the other features 
        // to match user expectations.
        if (!newState) {
            if (repository.isUsbDebuggingEnabled()) repository.toggleUsbDebugging(false)
            if (repository.isWirelessDebuggingEnabled()) repository.toggleWirelessDebugging(false)

            val context = applicationContext
            android.service.quicksettings.TileService.requestListeningState(
                context,
                android.content.ComponentName(context, UsbDebuggingTileService::class.java)
            )
            android.service.quicksettings.TileService.requestListeningState(
                context,
                android.content.ComponentName(context, WirelessDebuggingTileService::class.java)
            )
        }
    }
}