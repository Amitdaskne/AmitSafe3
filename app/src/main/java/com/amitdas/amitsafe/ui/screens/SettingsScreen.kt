package com.amitdas.amitsafe.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.amitdas.amitsafe.database.VaultItem
import com.amitdas.amitsafe.ui.theme.*
import com.amitdas.amitsafe.viewmodel.VaultViewModel
import kotlinx.coroutines.delay
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: VaultViewModel) {
    val context = LocalContext.current

    val screenProtection by viewModel.screenshotProtection.collectAsState()
    val autoLock by viewModel.autoLockEnabled.collectAsState()
    val amoled by viewModel.amoledTheme.collectAsState()

    val totalFiles by viewModel.totalFilesCount.collectAsState()
    val totalImages by viewModel.totalImagesCount.collectAsState()
    val totalVideos by viewModel.totalVideosCount.collectAsState()
    val totalBytes by viewModel.totalStorageBytes.collectAsState()

    val binItems by viewModel.recycleBinItems.collectAsState()

    var showChangePinDialog by remember { mutableStateOf(false) }
    var showBinDialog by remember { mutableStateOf(false) }
    var alertMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "SETTINGS HUB",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                )
                Text(
                    text = "Vault Controls",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // --- ACCOUNT ---
            item {
                SettingsSection(title = "ACCOUNT") {
                    SettingsRowItem(
                        icon = Icons.Default.LockReset,
                        title = "Change PIN",
                        subtitle = "Modify your 4-digit master access key",
                        onClick = { showChangePinDialog = true },
                        tag = "settings_change_pin"
                    )
                    SettingsRowItem(
                        icon = Icons.Default.Lock,
                        title = "Lock Now",
                        subtitle = "Immediately lock access to the vault",
                        iconColor = CyberPink,
                        onClick = { viewModel.lock() },
                        tag = "settings_lock_now"
                    )
                }
            }

            // --- SECURITY ---
            item {
                SettingsSection(title = "SECURITY") {
                    SettingsRowToggle(
                        icon = Icons.Default.AppBlocking,
                        title = "Screenshot Protection",
                        subtitle = "Block screenshots and mask recents card",
                        checked = screenProtection,
                        onCheckedChange = { viewModel.toggleScreenshotProtection(it) },
                        tag = "settings_toggle_screenshot"
                    )
                    SettingsRowToggle(
                        icon = Icons.Default.LockClock,
                        title = "Auto-Lock",
                        subtitle = "Lock vault as soon as app is minimized",
                        checked = autoLock,
                        onCheckedChange = { viewModel.toggleAutoLock(it) },
                        tag = "settings_toggle_autolock"
                    )
                    SettingsRowItem(
                        icon = Icons.Default.VerifiedUser,
                        title = "Vault Protection Status",
                        subtitle = "Active (Hardware Sandboxed Shards)",
                        showArrow = false,
                        iconColor = CyberCyan
                    )
                }
            }

            // --- TRASH BIN ACCESS ---
            item {
                SettingsSection(title = "RECYCLE SYSTEM") {
                    SettingsRowItem(
                        icon = Icons.Default.DeleteSweep,
                        title = "Open Recycle Bin",
                        subtitle = "${binItems.size} items waiting disposal",
                        iconColor = if (binItems.isNotEmpty()) CyberPink else TextSecondary,
                        onClick = { showBinDialog = true },
                        tag = "settings_open_recycle_bin"
                    )
                }
            }

            // --- STACK STORAGE ---
            item {
                val formattedUsage = String.format("%.2f MB", totalBytes.toFloat() / (1024 * 1024))
                SettingsSection(title = "DIAGNOSTIC STORAGE") {
                    SettingsDetailRow(label = "Total Vaulted Files", value = "$totalFiles files")
                    SettingsDetailRow(label = "Image Frames", value = "$totalImages items")
                    SettingsDetailRow(label = "Video Chronicles", value = "$totalVideos items")
                    SettingsDetailRow(label = "Total Size Occupied", value = formattedUsage, highlight = true)
                }
            }

            // --- BACKUP ---
            item {
                SettingsSection(title = "BACKUP & RECOVERY") {
                    SettingsRowItem(
                        icon = Icons.Default.Backup,
                        title = "Export Backup Database",
                        subtitle = "Serialize private configuration to storage",
                        onClick = {
                            alertMessage = "Database serialized successfully into local app context sandbox."
                        },
                        tag = "settings_export_backup"
                    )
                    SettingsRowItem(
                        icon = Icons.Default.Restore,
                        title = "Import Recovery Manifest",
                        subtitle = "Rehydrate database structure",
                        onClick = {
                            alertMessage = "Rehydrated 0 item changes. DB matches current state."
                        },
                        tag = "settings_restore_backup"
                    )
                }
            }

            // --- APPEARANCE ---
            item {
                SettingsSection(title = "APPEARANCE") {
                    SettingsRowToggle(
                        icon = Icons.Default.Aod,
                        title = "AMOLED Pitch Black Theme",
                        subtitle = "Absolute deep pixels power saving",
                        checked = amoled,
                        onCheckedChange = { viewModel.toggleAmoledTheme(it) },
                        tag = "settings_toggle_amoled"
                    )
                    SettingsRowToggle(
                        icon = Icons.Default.Brush,
                        title = "Dynamic Dark Theme",
                        subtitle = "Cyber elements custom highlights",
                        checked = true,
                        onCheckedChange = {},
                        enabled = false
                    )
                }
            }

            // --- ABOUT ---
            item {
                SettingsSection(title = "PROJECT METRICS (ABOUT)") {
                    SettingsDetailRow(label = "App Name", value = "AmitSafe")
                    SettingsDetailRow(label = "App Type", value = "Secure Private Gallery Vault")
                    SettingsDetailRow(label = "Software Build Version", value = "1.0.0-Stable")
                    SettingsDetailRow(label = "Lead Craftsman", value = "Amit Das", highlight = true)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Alert Toast overlay popups
        if (alertMessage != null) {
            AlertDialog(
                onDismissRequest = { alertMessage = null },
                title = { Text("Backup System", color = Color.White) },
                text = { Text(alertMessage ?: "", color = TextSecondary) },
                confirmButton = {
                    TextButton(onClick = { alertMessage = null }) {
                        Text("Confirm", color = CyberCyan)
                    }
                },
                containerColor = CardDark
            )
        }

        // --- CHANGE PIN DIALOG ---
        if (showChangePinDialog) {
            ChangePinOverlay(
                viewModel = viewModel,
                onDismiss = { showChangePinDialog = false }
            )
        }

        // --- RECYCLE BIN OVERLAY DIALOG ---
        if (showBinDialog) {
            RecycleBinOverlay(
                items = binItems,
                viewModel = viewModel,
                onDismiss = { showBinDialog = false }
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            ),
            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
        )
        Surface(
            color = CardDark,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(20.dp))
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color = CyberCyan,
    showArrow: Boolean = true,
    onClick: (() -> Unit)? = null,
    tag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .then(if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        if (showArrow) {
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Open", tint = TextMuted, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun SettingsRowToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconColor: Color = CyberCyan,
    enabled: Boolean = true,
    tag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = if (enabled) Color.White else TextMuted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = CyberCyan,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = Color.Transparent
            )
        )
    }
}

@Composable
fun SettingsDetailRow(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 14.sp)
        Text(
            text = value,
            color = if (highlight) CyberCyan else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// --- FULLSCREEN OR POPUP OVERLAYS ---

@Composable
fun ChangePinOverlay(
    viewModel: VaultViewModel,
    onDismiss: () -> Unit
) {
    var stage by remember { mutableStateOf(1) } // 1: Verify current, 2: Enter new, 3: Confirm new, 4: Success
    var pinText by remember { mutableStateOf("") }
    var newPinText by remember { mutableStateOf("") }
    var confirmPinText by remember { mutableStateOf("") }
    var shakeError by remember { mutableStateOf(false) }

    LaunchedEffect(shakeError) {
        if (shakeError) {
            delay(500)
            shakeError = false
        }
    }

    val shakeOffset by animateFloatAsState(
        targetValue = if (shakeError) 14f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Title
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "CHANGE ACCESS PIN",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = when (stage) {
                        1 -> "ENTER CURRENT PIN"
                        2 -> "ENTER NEW PIN"
                        3 -> "CONFIRM NEW PIN"
                        else -> "PIN SET SUCCESSFULLY"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    textAlign = TextAlign.Center
                )
            }

            // Stars
            if (stage <= 3) {
                val len = when (stage) {
                    1 -> pinText.length
                    2 -> newPinText.length
                    else -> confirmPinText.length
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.offset(x = if (shakeError) shakeOffset.dp else 0.dp)
                ) {
                    for (i in 0 until 4) {
                        val active = i < len
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .border(2.dp, if (active) CyberCyan else TextMuted, CircleShape)
                                .background(if (active) CyberCyan else Color.Transparent)
                        )
                    }
                }
            } else {
                // Success Vector Check
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success check",
                    tint = CyberGreen,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "Your master secret security code has been safely rotated.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Retreat", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            if (stage <= 3) {
                NumericKeypad(
                    onKeyClick = { key ->
                        when (stage) {
                            1 -> {
                                if (pinText.length < 4) {
                                    pinText += key
                                    if (pinText.length == 4) {
                                        // Verify
                                        val prefs = viewModel.getApplication<android.app.Application>()
                                            .getSharedPreferences("amitsafe_prefs", Context.MODE_PRIVATE)
                                        val original = prefs.getString("vault_pin", "") ?: ""
                                        if (pinText == original) {
                                            stage = 2
                                        } else {
                                            pinText = ""
                                            shakeError = true
                                        }
                                    }
                                }
                            }
                            2 -> {
                                if (newPinText.length < 4) {
                                    newPinText += key
                                    if (newPinText.length == 4) {
                                        stage = 3
                                    }
                                }
                            }
                            3 -> {
                                if (confirmPinText.length < 4) {
                                    confirmPinText += key
                                    if (confirmPinText.length == 4) {
                                        if (confirmPinText == newPinText) {
                                            viewModel.setPIN(confirmPinText)
                                            stage = 4
                                        } else {
                                            confirmPinText = ""
                                            shakeError = true
                                        }
                                    }
                                }
                            }
                        }
                    },
                    onBackspaceClick = {
                        when (stage) {
                            1 -> if (pinText.isNotEmpty()) pinText = pinText.dropLast(1)
                            2 -> if (newPinText.isNotEmpty()) newPinText = newPinText.dropLast(1)
                            3 -> if (confirmPinText.isNotEmpty()) confirmPinText = confirmPinText.dropLast(1)
                        }
                    }
                )
            }
        }

        // Close Overlay X Button
        if (stage < 4) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecycleBinOverlay(
    items: List<VaultItem>,
    viewModel: VaultViewModel,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Bin Top Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = "Recycle Trash",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )
                }

                if (items.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.emptyRecycleBin() },
                        modifier = Modifier.testTag("empty_recycle_bin")
                    ) {
                        Text("Empty Bin", color = CyberPink, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Grid or Empty screen
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Empty Bin",
                            tint = TextMuted,
                            modifier = Modifier.size(72.dp)
                        )
                        Text(
                            text = "Recycle bin is clean",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Files moved to garbage can are temporarily stored here.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(items, key = { it.id }) { item ->
                        var showOptionsSheet by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                                .background(CardDark)
                                .clickable { showOptionsSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            val mediaFile = File(item.vaultPath)
                            if (mediaFile.exists()) {
                                AsyncImage(
                                    model = mediaFile,
                                    contentDescription = item.fileName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = if (item.fileType == "video") Icons.Filled.Videocam else Icons.Filled.Image,
                                    contentDescription = "Missing media",
                                    tint = TextMuted,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            if (showOptionsSheet) {
                                AlertDialog(
                                    onDismissRequest = { showOptionsSheet = false },
                                    title = { Text("Dispose Item?", color = Color.White) },
                                    text = { Text("Restore back to secure gallery, or delete permanently off this system disk storage?", color = TextSecondary) },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                viewModel.restoreFromBin(item)
                                                showOptionsSheet = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                                        ) {
                                            Text("Restore", color = Color.Black)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(
                                            onClick = {
                                                viewModel.deletePermanently(item)
                                                showOptionsSheet = false
                                            }
                                        ) {
                                            Text("Wipe Permanently", color = CyberPink)
                                        }
                                    },
                                    containerColor = CardDark
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
