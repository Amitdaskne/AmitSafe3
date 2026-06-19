package com.amitdas.amitsafe.ui.screens

import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amitdas.amitsafe.ui.theme.*
import com.amitdas.amitsafe.viewmodel.VaultViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun LockScreen(
    viewModel: VaultViewModel,
    onUnlockSuccess: () -> Unit
) {
    val context = LocalContext.current
    val isRegistered by viewModel.isRegistered.collectAsState()
    val lockoutSeconds by viewModel.lockoutTimeRemaining.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var pinText by remember { mutableStateOf("") }
    var setupStep by remember { mutableStateOf(1) } // 1: Create, 2: Confirm
    var firstEnteredPin by remember { mutableStateOf("") }
    var shakeTrigger by remember { mutableStateOf(false) }

    // Collect wrong PIN failures to trigger shake and buzz
    LaunchedEffect(viewModel.wrongPinTrigger) {
        viewModel.wrongPinTrigger.collectLatest {
            shakeTrigger = true
            triggerHaptic(context)
            pinText = ""
            delay(500)
            shakeTrigger = false
        }
    }

    // Horizontal Shake translation
    val shakeOffset by animateFloatAsState(
        targetValue = if (shakeTrigger) 12f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
    )

    // Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        // Futuristic mesh drawing in the background
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(36.dp)
        ) {
            // Cyber visual badge icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(CyberPurple.copy(alpha = 0.15f), CyberCyan.copy(alpha = 0.02f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRegistered) Icons.Filled.Lock else Icons.Filled.Security,
                    contentDescription = "Secured Vault Logo",
                    tint = CyberCyan,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Headings based on Setup or Authentication
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "AmitSafe",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = if (!isRegistered) {
                        if (setupStep == 1) "CREATE 4-DIGIT PIN" else "CONFIRM 4-DIGIT PIN"
                    } else {
                        if (lockoutSeconds > 0) "VAULT LOCKEDOUT DUE TO ATTEMPTS" else "LOG-IN SECURE PIN"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (lockoutSeconds > 0) CyberPink else TextSecondary,
                        letterSpacing = 1.5.sp
                    )
                )
            }

            // Lockout banner / Countdown display
            if (lockoutSeconds > 0) {
                Text(
                    text = "Retry available in $lockoutSeconds seconds",
                    color = CyberPink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // PIN indicator circles
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.offset(x = if (shakeTrigger) shakeOffset.dp else 0.dp)
                ) {
                    for (i in 0 until 4) {
                        val isActive = i < pinText.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .border(
                                    2.dp,
                                    if (isActive) CyberCyan else TextMuted,
                                    CircleShape
                                )
                                .background(
                                    if (isActive) CyberCyan else Color.Transparent
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Numeric Keypad
            NumericKeypad(
                onKeyClick = { key ->
                    if (lockoutSeconds > 0) return@NumericKeypad
                    if (pinText.length < 4) {
                        pinText += key
                        if (pinText.length == 4) {
                            if (!isRegistered) {
                                if (setupStep == 1) {
                                    firstEnteredPin = pinText
                                    pinText = ""
                                    setupStep = 2
                                } else {
                                    if (pinText == firstEnteredPin) {
                                        viewModel.setPIN(pinText)
                                        onUnlockSuccess()
                                    } else {
                                        // Mismatch reset
                                        shakeTrigger = true
                                        triggerHaptic(context)
                                        pinText = ""
                                        setupStep = 1
                                        firstEnteredPin = ""
                                        coroutineScope.launch {
                                            delay(500)
                                            shakeTrigger = false
                                        }
                                    }
                                }
                            } else {
                                val ok = viewModel.verifyAndUnlock(pinText)
                                if (ok) {
                                    onUnlockSuccess()
                                } else {
                                    pinText = ""
                                }
                            }
                        }
                    }
                },
                onBackspaceClick = {
                    if (pinText.isNotEmpty() && lockoutSeconds <= 0) {
                        pinText = pinText.substring(0, pinText.length - 1)
                    }
                }
            )
        }
    }
}

@Composable
fun NumericKeypad(
    onKeyClick: (String) -> Unit,
    onBackspaceClick: () -> Unit
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in keys) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (key in row) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.25f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (key.isNotEmpty()) {
                            if (key == "DEL") {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .clickable { onBackspaceClick() }
                                        .testTag("delete_backspace_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Delete key",
                                        tint = CyberPink,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .border(
                                            1.dp,
                                            BorderDark,
                                            CircleShape
                                        )
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(CardDark, Color.Black)
                                            )
                                        )
                                        .clickable { onKeyClick(key) }
                                        .testTag("pin_key_$key"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun triggerHaptic(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            val vibrator = vibratorManager?.defaultVibrator
            vibrator?.vibrate(android.os.VibrationEffect.createOneShot(120, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(120)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
