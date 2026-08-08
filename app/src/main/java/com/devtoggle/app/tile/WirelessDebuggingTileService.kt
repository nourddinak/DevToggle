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

class WirelessDebuggingTileService : TileService() {
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

    private fun refreshTile() {
        val tile = qsTile ?: return
        tile.icon = Icon.createWithResource(this, R.drawable.ic_wifi)
        val state = repository.specificTileState(repository.isWirelessDebuggingEnabled(), requiresApi30 = true)
        when (state) {
            TileState.ON -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = getString(R.string.tile_label_wifi_on)
                tile.contentDescription = getString(R.string.tile_description_wifi)
                setSubtitle(tile, R.string.tile_subtitle_on)
            }
            TileState.OFF -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.tile_label_wifi_off)
                tile.contentDescription = getString(R.string.tile_description_wifi)
                setSubtitle(tile, R.string.tile_subtitle_off)
            }
            TileState.UNAVAILABLE -> {
                tile.state = Tile.STATE_UNAVAILABLE
                tile.label = getString(R.string.tile_label_wifi)
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
        val currentState = repository.isWirelessDebuggingEnabled()
        repository.toggleWirelessDebugging(!currentState)
        refreshTile()
    }
}
