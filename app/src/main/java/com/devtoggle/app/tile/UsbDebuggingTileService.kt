package com.devtoggle.app.tile

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.devtoggle.app.R
import com.devtoggle.app.data.DevToggleRepository
import com.devtoggle.app.data.TileState
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UsbDebuggingTileService : TileService() {
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
        tile.icon = Icon.createWithResource(this, R.drawable.ic_usb)
        val state = repository.specificTileState(repository.isUsbDebuggingEnabled())
        when (state) {
            TileState.ON -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = getString(R.string.tile_label_usb_on)
                tile.contentDescription = getString(R.string.tile_description_usb)
                setSubtitle(tile, R.string.tile_subtitle_on)
            }
            TileState.OFF -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.tile_label_usb_off)
                tile.contentDescription = getString(R.string.tile_description_usb)
                setSubtitle(tile, R.string.tile_subtitle_off)
            }
            TileState.UNAVAILABLE -> {
                tile.state = Tile.STATE_UNAVAILABLE
                tile.label = getString(R.string.tile_label_usb)
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

    private val scope = CoroutineScope(Dispatchers.IO)

    private fun executeToggle() {
        val currentState = repository.isUsbDebuggingEnabled()
        scope.launch {
            repository.toggleUsbDebugging(!currentState)
            withContext(Dispatchers.Main) {
                refreshTile()
            }
        }
    }
}
