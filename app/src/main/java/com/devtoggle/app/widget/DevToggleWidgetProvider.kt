package com.devtoggle.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.devtoggle.app.R
import com.devtoggle.app.data.DevToggleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DevToggleWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE_DEV_OPTIONS = "com.devtoggle.app.widget.TOGGLE_DEV_OPTIONS"
        const val ACTION_TOGGLE_USB_DEBUGGING = "com.devtoggle.app.widget.TOGGLE_USB_DEBUGGING"
        const val ACTION_TOGGLE_WIRELESS_DEBUGGING = "com.devtoggle.app.widget.TOGGLE_WIRELESS_DEBUGGING"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val repository = DevToggleRepository(context)
            val views = RemoteViews(context.packageName, R.layout.widget_devtoggle)

            val devOptionsIntent = Intent(context, DevToggleWidgetProvider::class.java).apply { action = ACTION_TOGGLE_DEV_OPTIONS }
            val usbIntent = Intent(context, DevToggleWidgetProvider::class.java).apply { action = ACTION_TOGGLE_USB_DEBUGGING }
            val wirelessIntent = Intent(context, DevToggleWidgetProvider::class.java).apply { action = ACTION_TOGGLE_WIRELESS_DEBUGGING }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            views.setOnClickPendingIntent(R.id.btn_dev_options, PendingIntent.getBroadcast(context, 0, devOptionsIntent, flags))
            views.setOnClickPendingIntent(R.id.btn_usb_debugging, PendingIntent.getBroadcast(context, 1, usbIntent, flags))
            views.setOnClickPendingIntent(R.id.btn_wireless_debugging, PendingIntent.getBroadcast(context, 2, wirelessIntent, flags))

            if (repository.hasNativePermissions()) {
                val devOn = repository.isDeveloperOptionsEnabled()
                val usbOn = repository.isUsbDebuggingEnabled()
                val wirelessOn = repository.isWirelessDebuggingEnabled()

                views.setInt(R.id.btn_dev_options, "setBackgroundResource", if (devOn) R.drawable.widget_button_bg_on_circle else R.drawable.widget_button_bg_circle)
                views.setInt(R.id.btn_usb_debugging, "setBackgroundResource", if (usbOn) R.drawable.widget_button_bg_on_circle else R.drawable.widget_button_bg_circle)
                views.setInt(R.id.btn_wireless_debugging, "setBackgroundResource", if (wirelessOn) R.drawable.widget_button_bg_on_circle else R.drawable.widget_button_bg_circle)
            } else {
                views.setInt(R.id.btn_dev_options, "setBackgroundResource", R.drawable.widget_button_bg_circle)
                views.setInt(R.id.btn_usb_debugging, "setBackgroundResource", R.drawable.widget_button_bg_circle)
                views.setInt(R.id.btn_wireless_debugging, "setBackgroundResource", R.drawable.widget_button_bg_circle)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        
        if (action == ACTION_TOGGLE_DEV_OPTIONS || 
            action == ACTION_TOGGLE_USB_DEBUGGING || 
            action == ACTION_TOGGLE_WIRELESS_DEBUGGING) {
            
            val pendingResult = goAsync()
            val repository = DevToggleRepository(context)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (repository.hasNativePermissions()) {
                        when (action) {
                            ACTION_TOGGLE_DEV_OPTIONS -> {
                                val current = repository.isDeveloperOptionsEnabled()
                                repository.toggleDeveloperOptions(!current)
                            }
                            ACTION_TOGGLE_USB_DEBUGGING -> {
                                val current = repository.isUsbDebuggingEnabled()
                                repository.toggleUsbDebugging(!current)
                            }
                            ACTION_TOGGLE_WIRELESS_DEBUGGING -> {
                                val current = repository.isWirelessDebuggingEnabled()
                                repository.toggleWirelessDebugging(!current)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val componentName = ComponentName(context, DevToggleWidgetProvider::class.java)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                    for (id in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, id)
                    }
                    pendingResult.finish()
                }
            }
        }
    }
}
