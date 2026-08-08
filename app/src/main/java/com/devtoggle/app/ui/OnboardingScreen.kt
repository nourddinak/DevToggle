package com.devtoggle.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devtoggle.app.data.DevToggleRepository
import com.devtoggle.app.data.ShizukuStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    status: ShizukuStatus,
    repository: DevToggleRepository,
    refresh: () -> Unit,
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 6 })
    val coroutineScope = rememberCoroutineScope()

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> WelcomePage()
                    1 -> ShizukuSetupPage(status, repository, refresh)
                    2 -> CustomGroupTilePage()
                    3 -> QuickSettingsSetupPage()
                    4 -> HomeScreenWidgetSetupPage()
                    5 -> AllSetPage(onComplete)
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(6) { index ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }

                if (pagerState.currentPage < 5) {
                    val canAdvance = when (pagerState.currentPage) {
                        1 -> status.ready || status.hasNativePermissions
                        else -> true
                    }
                    Button(
                        onClick = {
                            if (canAdvance) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        enabled = canAdvance
                    ) {
                        Text("Next")
                        Spacer(Modifier.size(8.dp))
                        Icon(Icons.Outlined.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.TouchApp,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            "Welcome to DevToggle",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Toggle Developer Options, USB Debugging, and more directly from your Quick Settings.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShizukuSetupPage(status: ShizukuStatus, repository: DevToggleRepository, refresh: () -> Unit) {
    var showDownloadSheet by remember { mutableStateOf(false) }

    if (showDownloadSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showDownloadSheet = false },
            sheetState = sheetState
        ) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Download Shizuku", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                
                ListItem(
                    headlineContent = { Text("Google Play Store") },
                    leadingContent = { Icon(Icons.Outlined.Shop, contentDescription = null) },
                    modifier = Modifier.clickable { 
                        repository.openUrl("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api") 
                        showDownloadSheet = false
                    }
                )
                ListItem(
                    headlineContent = { Text("GitHub Releases") },
                    leadingContent = { Icon(Icons.Outlined.Code, contentDescription = null) },
                    modifier = Modifier.clickable { 
                        repository.openUrl("https://github.com/RikkaApps/Shizuku/releases") 
                        showDownloadSheet = false
                    }
                )
                ListItem(
                    headlineContent = { Text("Official Website") },
                    leadingContent = { Icon(Icons.Outlined.Public, contentDescription = null) },
                    modifier = Modifier.clickable { 
                        repository.openUrl("https://shizuku.rikka.app/") 
                        showDownloadSheet = false
                    }
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val ready = status.ready || status.hasNativePermissions
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(if (ready) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (ready) Icons.Outlined.CheckCircle else Icons.Outlined.Security,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            if (ready) "Shizuku is Ready" else "Shizuku Required",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            if (status.hasNativePermissions) "DevToggle already has native permissions! You're good to go."
            else if (ready) "DevToggle has been granted permission to check settings."
            else "DevToggle requires Shizuku to toggle system settings without root.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        if (!ready) {
            if (!status.installed) {
                Button(onClick = { showDownloadSheet = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Install Shizuku")
                }
            } else if (status.running && !status.permissionGranted) {
                Button(onClick = { 
                    repository.requestPermission()
                    refresh()
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Grant Permission")
                }
            } else if (!status.running) {
                Button(onClick = { repository.openShizuku() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Start Shizuku")
                }
            }
        }
    }
}

@Composable
private fun CustomGroupTilePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.DeveloperMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Box(Modifier.width(40.dp).height(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                }
                Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Usb, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Box(Modifier.width(40.dp).height(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                }
                Icon(Icons.Outlined.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(
                    modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            "Custom Group Tile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Toggle Developer Options, USB Debugging, and more all at once with a single Custom Group Tile.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun QuickSettingsSetupPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                .padding(24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.width(60.dp).height(4.dp).background(Color.Gray.copy(alpha = 0.5f), CircleShape)
                )
                Spacer(Modifier.height(32.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    repeat(3) {
                        Box(
                            modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.background, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (it == 1) {
                                Icon(Icons.Outlined.DeveloperMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            } else {
                                Box(Modifier.size(24.dp).background(Color.Gray.copy(alpha=0.3f), CircleShape))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
                Icon(Icons.Outlined.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            "Add the Tile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Swipe down your notification shade, tap the edit (pencil) icon, and drag the DevToggle tile into your active tiles.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HomeScreenWidgetSetupPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .background(
                                if (index == 1) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when (index) {
                            0 -> Icons.Outlined.DeveloperMode
                            1 -> Icons.Outlined.Usb
                            else -> Icons.Outlined.Wifi
                        }
                        Icon(icon, contentDescription = null, tint = if (index == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            "Home Screen Widget",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Long-press your home screen to add the DevToggle Widget. Instantly access your developer tools right from your home screen!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AllSetPage(onComplete: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            "You're All Set!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "DevToggle is ready to use. You can change your tile configuration anytime in the app.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Let's Go!")
        }
    }
}
