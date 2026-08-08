package com.devtoggle.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    var hasCompletedOnboarding by remember { mutableStateOf(repository.hasCompletedOnboarding()) }

    LaunchedEffect(Unit) {
        val update = Updater.checkForUpdates(context)
        if (update != null) {
            updateAvailable = update
        }
    }

    fun refresh() {
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
        scope.launch {
            val newStatus = repository.shizukuStatus()
            val newDev = repository.isDeveloperOptionsEnabled()
            val newUsb = repository.isUsbDebuggingEnabled()
            val newWifi = repository.isWirelessDebuggingEnabled()
            val newConfig = repository.getCustomTileConfig()
            
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                status = newStatus
                developerOptionsEnabled = newDev
                usbDebuggingEnabled = newUsb
                wirelessDebuggingEnabled = newWifi
                customTileConfig = newConfig

                if (status.installed && status.running && status.permissionGranted && !status.hasNativePermissions) {
                    scope.launch {
                        repository.grantPermissions()
                        val finalStatus = repository.shizukuStatus()
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            status = finalStatus
                        }
                    }
                }
            }
        }
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

    if (!hasCompletedOnboarding) {
        com.devtoggle.app.ui.OnboardingScreen(
            status = status,
            repository = repository,
            refresh = { refresh() },
            onComplete = {
                repository.setCompletedOnboarding(true)
                hasCompletedOnboarding = true
            }
        )
        return
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
            item { ShizukuSection(status, repository, ::refresh) }
            item { 
                QuickSettingsSection(
                    developerOptionsEnabled = developerOptionsEnabled,
                    usbDebuggingEnabled = usbDebuggingEnabled,
                    wirelessDebuggingEnabled = wirelessDebuggingEnabled,
                    status = status,
                    repository = repository
                ) 
            }
            item {
                CustomTileConfigSection(
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
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun ShizukuSection(
    status: ShizukuStatus,
    repository: DevToggleRepository,
    refresh: () -> Unit,
) {
    val ready = status.ready
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                StatusIcon(
                    icon = if (ready) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                    tint = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.size(12.dp))
                Text(
                    if (ready) "Setup Complete" else "Setup Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    if (status.hasNativePermissions) "DevToggle can toggle settings natively!"
                    else if (ready) "DevToggle can verify the tile state."
                    else "Complete the setup below to use the tile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
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
private fun QuickSettingsSection(
    developerOptionsEnabled: Boolean,
    usbDebuggingEnabled: Boolean,
    wirelessDebuggingEnabled: Boolean,
    status: ShizukuStatus,
    repository: DevToggleRepository,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.DeveloperMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Spacer(Modifier.size(8.dp))
                Text(
                    "Quick Settings Tiles",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    "Add them from the edit button in your notification shade. Tapping a tile opens Android Settings for a user-confirmed change.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
            TileItemRow("Developer Options", developerOptionsEnabled, status, Icons.Outlined.DeveloperMode)
            TileItemRow("USB Debugging", usbDebuggingEnabled, status, Icons.Outlined.Usb)
            
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                TileItemRow("Wireless Debugging", wirelessDebuggingEnabled, status, Icons.Outlined.Wifi)
            }
            
            Button(
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
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (status.ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.size(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(
                when {
                    !status.ready -> "Unavailable · Shizuku not ready"
                    enabled -> "Active · On"
                    else -> "Inactive · Off"
                },
                style = MaterialTheme.typography.bodyMedium,
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
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (complete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (complete) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(number.toString(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ArrowForward, contentDescription = "Open $title", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(start = 74.dp))
    }
}

@Composable
private fun CapabilitiesSection(
    capabilities: List<Capability>,
    showAll: Boolean,
    onShowAll: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Spacer(Modifier.size(8.dp))
                Text(
                    "Capabilities",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            CapabilityItem(capabilities.first())
            AnimatedVisibility(showAll) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    capabilities.drop(1).forEach { CapabilityItem(it) }
                }
            }
            Button(
                onClick = onShowAll,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showAll) "Hide details" else "See all capabilities")
            }
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
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.size(16.dp))
        Column {
            Text(capability.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.size(4.dp))
            Text(capability.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SafetySection() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Security, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
            Spacer(Modifier.size(12.dp))
            Text("Safety and Privacy", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.size(8.dp))
            Text(
                "DevToggle does not collect data or modify other apps. Shizuku is used only to check service readiness. Protected system changes remain in Android Settings and require your explicit action.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.size(8.dp))
            Text(
                "No background changes · No accessibility workarounds · No hidden APIs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
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
private fun CustomTileConfigSection(
    config: CustomTileConfig,
    onConfigChanged: (CustomTileConfig) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Spacer(Modifier.size(8.dp))
                Text(
                    "Custom Group Tile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    "Select which settings the custom tile controls. When toggled, they will all turn on/off together.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
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