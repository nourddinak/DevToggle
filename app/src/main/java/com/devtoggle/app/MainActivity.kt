package com.devtoggle.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeveloperMode
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.devtoggle.app.data.Capability
import com.devtoggle.app.data.CapabilityKind
import com.devtoggle.app.data.CustomTileConfig
import com.devtoggle.app.data.DevToggleRepository
import com.devtoggle.app.data.ShizukuStatus
import com.devtoggle.app.ui.theme.DevToggleTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.LaunchedEffect
import com.devtoggle.app.data.Updater

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DevToggleTheme {
                DevToggleApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DevToggleApp() {
    val context = LocalContext.current
    val repository = remember { DevToggleRepository(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var status by remember { mutableStateOf(repository.shizukuStatus()) }
    var developerOptionsEnabled by remember { mutableStateOf(repository.isDeveloperOptionsEnabled()) }
    var usbDebuggingEnabled by remember { mutableStateOf(repository.isUsbDebuggingEnabled()) }
    var wirelessDebuggingEnabled by remember { mutableStateOf(repository.isWirelessDebuggingEnabled()) }
    var showAllCapabilities by remember { mutableStateOf(false) }
    var customTileConfig by remember { mutableStateOf(repository.getCustomTileConfig()) }
    var updateAvailable by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(Unit) {
        val update = Updater.checkForUpdates()
        if (update != null) {
            updateAvailable = update
        }
    }

    fun refresh() {
        status = repository.shizukuStatus()
        if (status.installed && status.running && status.permissionGranted && !status.hasNativePermissions) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                repository.grantPermissions()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    status = repository.shizukuStatus()
                }
            }
        }
        developerOptionsEnabled = repository.isDeveloperOptionsEnabled()
        usbDebuggingEnabled = repository.isUsbDebuggingEnabled()
        wirelessDebuggingEnabled = repository.isWirelessDebuggingEnabled()
        customTileConfig = repository.getCustomTileConfig()
    }

    DisposableEffect(lifecycleOwner) {
        val binderReceivedListener = rikka.shizuku.Shizuku.OnBinderReceivedListener { refresh() }
        val binderDeadListener = rikka.shizuku.Shizuku.OnBinderDeadListener { refresh() }
        val permissionResultListener = rikka.shizuku.Shizuku.OnRequestPermissionResultListener { _, _ -> refresh() }
        
        runCatching {
            rikka.shizuku.Shizuku.addBinderReceivedListener(binderReceivedListener)
            rikka.shizuku.Shizuku.addBinderDeadListener(binderDeadListener)
            rikka.shizuku.Shizuku.addRequestPermissionResultListener(permissionResultListener)
        }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose { 
            runCatching {
                rikka.shizuku.Shizuku.removeBinderReceivedListener(binderReceivedListener)
                rikka.shizuku.Shizuku.removeBinderDeadListener(binderDeadListener)
                rikka.shizuku.Shizuku.removeRequestPermissionResultListener(permissionResultListener)
            }
            lifecycleOwner.lifecycle.removeObserver(observer) 
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("DevToggle", fontWeight = FontWeight.Bold)
                        Text(
                            "A safer shortcut to Developer Options",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { refresh() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh status")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (updateAvailable != null) {
                item {
                    UpdateBanner(update = updateAvailable!!)
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
            item { ShizukuCard(status, repository, ::refresh) }
            item { 
                QuickSettingsCard(
                    developerOptionsEnabled = developerOptionsEnabled,
                    usbDebuggingEnabled = usbDebuggingEnabled,
                    wirelessDebuggingEnabled = wirelessDebuggingEnabled,
                    status = status,
                    repository = repository
                ) 
            }
            item {
                CustomTileConfigCard(
                    config = customTileConfig,
                    onConfigChanged = { newConfig ->
                        repository.saveCustomTileConfig(newConfig)
                        customTileConfig = newConfig
                    }
                )
            }
            item { SetupSection(status, repository, ::refresh) }
            item {
                CapabilitiesSection(
                    capabilities = repository.capabilities(),
                    showAll = showAllCapabilities,
                    onShowAll = { showAllCapabilities = !showAllCapabilities },
                )
            }
            item { SafetySection() }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun ShizukuCard(
    status: ShizukuStatus,
    repository: DevToggleRepository,
    refresh: () -> Unit,
) {
    val ready = status.ready
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (ready) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusIcon(
                    icon = if (ready) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                    tint = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (ready) "Setup Complete" else "Setup required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (status.hasNativePermissions) "DevToggle can toggle settings natively!"
                        else if (ready) "DevToggle can verify the tile state."
                        else "Complete the setup below to use the tile.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StatusRow("Native permissions", status.hasNativePermissions)
            StatusRow("Shizuku installed", status.installed)
            StatusRow("Shizuku running", status.running)
            StatusRow("Shizuku permission", status.permissionGranted)
            if (!status.installed) {
                Button(
                    onClick = { repository.openShizuku() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Learn about Shizuku")
                    Spacer(Modifier.size(8.dp))
                    Icon(Icons.Outlined.OpenInNew, contentDescription = null)
                }
            } else if (status.running && !status.permissionGranted) {
                Button(
                    onClick = {
                        repository.requestPermission()
                        refresh()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Grant permission")
                    Spacer(Modifier.size(8.dp))
                    Icon(Icons.Outlined.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, active: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            if (active) "Ready" else "Not ready",
            style = MaterialTheme.typography.labelLarge,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun QuickSettingsCard(
    developerOptionsEnabled: Boolean,
    usbDebuggingEnabled: Boolean,
    wirelessDebuggingEnabled: Boolean,
    status: ShizukuStatus,
    repository: DevToggleRepository,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeading(Icons.Outlined.DeveloperMode, "Quick Settings tiles")
            
            TileItemRow("Developer Options", developerOptionsEnabled, status, Icons.Outlined.DeveloperMode)
            TileItemRow("USB Debugging", usbDebuggingEnabled, status, Icons.Outlined.Usb)
            
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                TileItemRow("Wireless Debugging", wirelessDebuggingEnabled, status, Icons.Outlined.Wifi)
            }
            
            Text(
                "Add them from the edit button in your notification shade. Tapping a tile opens Android Settings for a user-confirmed change.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { repository.openDeveloperSettings() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open Developer Options")
                Spacer(Modifier.size(8.dp))
                Icon(Icons.Outlined.OpenInNew, contentDescription = null)
            }
        }
    }
}

@Composable
private fun TileItemRow(title: String, enabled: Boolean, status: ShizukuStatus, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (status.ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (status.ready) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                when {
                    !status.ready -> "Unavailable · Shizuku not ready"
                    enabled -> "Active · On"
                    else -> "Inactive · Off"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SetupSection(
    status: ShizukuStatus,
    repository: DevToggleRepository,
    refresh: () -> Unit,
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Setup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        SetupStep(
            number = 1,
            title = "Install Shizuku",
            description = if (status.installed) "Installed" else "Get the official Shizuku service",
            complete = status.installed,
            onClick = { repository.openShizuku() },
        )
        AnimatedVisibility(visible = !status.installed) {
            Row(
                modifier = Modifier.padding(start = 54.dp, bottom = 4.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"))) },
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Play Store", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/"))) },
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Website", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RikkaApps/Shizuku/releases/download/v13.6.0/shizuku-v13.6.0.r1086.2650830c-release.apk"))) },
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Direct APK", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        SetupStep(
            number = 2,
            title = "Start Shizuku",
            description = if (status.running) "Service is running" else "Start it from the Shizuku app",
            complete = status.running,
            onClick = { repository.openShizuku() },
        )
        SetupStep(
            number = 3,
            title = "Grant permission",
            description = if (status.permissionGranted) "DevToggle is authorized" else "Allow DevToggle in Shizuku",
            complete = status.permissionGranted,
            onClick = {
                repository.requestPermission()
                refresh()
            },
        )
    }
}

@Composable
private fun SetupStep(
    number: Int,
    title: String,
    description: String,
    complete: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (complete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                if (complete) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(number.toString(), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ArrowForward, contentDescription = "Open $title", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CapabilitiesSection(
    capabilities: List<Capability>,
    showAll: Boolean,
    onShowAll: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Capabilities", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                if (showAll) "Hide" else "See all",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .padding(6.dp),
            )
        }
        CapabilityItem(capabilities.first())
        AnimatedVisibility(showAll) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                capabilities.drop(1).forEach { CapabilityItem(it) }
            }
        }
        OutlinedButton(onClick = onShowAll, modifier = Modifier.fillMaxWidth()) {
            Text(if (showAll) "Show less" else "View device capability details")
            Spacer(Modifier.size(8.dp))
            Icon(Icons.Outlined.Info, contentDescription = null)
        }
    }
}

@Composable
private fun CapabilityItem(capability: Capability) {
    val (tint, icon) = when (capability.kind) {
        CapabilityKind.SUPPORTED -> MaterialTheme.colorScheme.primary to Icons.Outlined.CheckCircle
        CapabilityKind.SETTINGS -> MaterialTheme.colorScheme.tertiary to Icons.Outlined.OpenInNew
        CapabilityKind.DEPENDENT -> MaterialTheme.colorScheme.tertiary to Icons.Outlined.Info
        CapabilityKind.UNSUPPORTED -> MaterialTheme.colorScheme.error to Icons.Outlined.Warning
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(12.dp))
            Column {
                Text(capability.title, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.size(3.dp))
                Text(capability.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SafetySection() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Security, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.size(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Safety and privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "DevToggle does not collect data or modify other apps. Shizuku is used only to check service readiness. Protected system changes remain in Android Settings and require your explicit action.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    "No background changes · No accessibility workarounds · No hidden APIs",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun SectionHeading(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.size(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusIcon(icon: ImageVector, tint: Color) {
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
    }
}

@Composable
private fun ConfigCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CustomTileConfigCard(
    config: CustomTileConfig,
    onConfigChanged: (CustomTileConfig) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                "Custom Group Tile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Select which settings the custom tile controls. When toggled, they will all turn on/off together.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
            ConfigCheckboxRow(
                label = "Developer Options",
                checked = config.devOptions,
                onCheckedChange = { onConfigChanged(config.copy(devOptions = it)) }
            )
            ConfigCheckboxRow(
                label = "USB Debugging",
                checked = config.usbDebugging,
                onCheckedChange = { onConfigChanged(config.copy(usbDebugging = it)) }
            )
            ConfigCheckboxRow(
                label = "Wireless Debugging",
                checked = config.wirelessDebugging,
                onCheckedChange = { onConfigChanged(config.copy(wirelessDebugging = it)) }
            )
        }
    }
}

@Composable
private fun UpdateBanner(update: Pair<String, String>) {
    val context = LocalContext.current
    val (version, url) = update
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Update Available",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Version $version is ready to download.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Button(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    contentColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text("Download")
            }
        }
    }
}