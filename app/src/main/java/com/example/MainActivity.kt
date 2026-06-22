package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.ui.DashboardViewModel
import com.example.ui.ScreenState
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notification permission if Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), 101)
            }
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = NavyDark
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        val screenState by viewModel.screenState.collectAsState()
                        
                        AnimatedContent(
                            targetState = screenState,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "screen_transition"
                        ) { state ->
                            when (state) {
                                ScreenState.Home -> HomeScreen(viewModel)
                                ScreenState.PinSetup -> PinSetupScreen(viewModel)
                                ScreenState.PinVerify -> PinVerifyScreen(viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: DashboardViewModel) {
    val context = LocalContext.current
    val isEnabled by viewModel.isBlockingEnabled.collectAsState()
    val isServiceActive by viewModel.isServiceActive.collectAsState()
    val isPinSet by viewModel.isPinSet.collectAsState()
    val totalBlocks by viewModel.totalBlocksCount.collectAsState()
    val showPrompt by viewModel.showAccessibilityPrompt.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // App header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "FocusGuard",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = "WhatsApp Blocker",
                    fontSize = 14.sp,
                    color = TextGray
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(NavyMedium, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings Mode Indicator",
                    tint = if (isEnabled && isServiceActive) EmeraldGreen else TextGray
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Generated 3D glassmorphism logo
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(NavyLight, NavyDark)
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_guard_shield_1782084628433),
                contentDescription = "Guard Art Banner",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop
            )
        }

        // Active blocking toggle banner clickable row
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("toggle_block_button_container"),
            colors = CardDefaults.cardColors(containerColor = NavyMedium),
            shape = RoundedCornerShape(16.dp),
            onClick = { viewModel.toggleBlocking() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (isEnabled) EmeraldGreen.copy(alpha = 0.2f) else ActiveRed.copy(alpha = 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock State Icon",
                            tint = if (isEnabled) EmeraldGreen else ActiveRed
                        )
                    }
                    Column {
                        Text(
                            text = "WhatsApp Protection",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = if (isEnabled) "Shield Activated — Blocked" else "Shield Deactivated — Unlocked",
                            fontSize = 12.sp,
                            color = TextGray
                        )
                    }
                }

                // If PIN is set and blocking is ON, display LOCK icon to show password gate
                if (isPinSet && isEnabled) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "PIN Protected Blocker",
                        tint = AccentTeal,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { viewModel.toggleBlocking() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EmeraldGreen,
                            checkedTrackColor = EmeraldGreen.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("toggle_block_button")
                    )
                }
            }
        }

        // Real-time Service status indicators
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NavyMedium),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "System Monitor",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextGray
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (isServiceActive) EmeraldGreen else ActiveRed,
                                    CircleShape
                                )
                        )
                        Text(
                            text = "Accessibility Blocker Service",
                            fontSize = 14.sp,
                            color = TextWhite
                        )
                    }
                    Text(
                        text = if (isServiceActive) "Active" else "Inactive",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isServiceActive) EmeraldGreen else ActiveRed
                    )
                }

                if (!isServiceActive) {
                    Text(
                        text = "Android OS requires manual accessibility service activation to intercept and shield WhatsApp stories tab.",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("activate_service_setup")
                    ) {
                        Text("Enable Service", color = NavyDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Room persistence counters stats
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NavyMedium),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Total Distractions Shielded",
                        fontSize = 14.sp,
                        color = TextGray,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$totalBlocks times",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (totalBlocks > 0) EmeraldGreen else TextWhite
                    )
                }
                
                if (totalBlocks > 0) {
                    IconButton(
                        onClick = { viewModel.clearAllHistory() },
                        modifier = Modifier.testTag("clear_history_logs")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Clear logs",
                            tint = TextGray
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Protection PIN setup trigger
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.navigateToPinSetup() },
                modifier = Modifier
                    .weight(1f)
                    .testTag("setup_pin_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(if (isPinSet) "Change PIN" else "Set Security PIN")
            }
        }
    }

    // Interactive custom settings navigation helper
    if (showPrompt) {
        Dialog(onDismissRequest = { viewModel.showAccessibilityPrompt.value = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavyMedium),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(EmeraldGreen.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(
                        text = "Accessibility Permission Required",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = TextWhite
                    )
                    Text(
                        text = "FocusGuard requires Android's Accessibility Service permission to block WhatsApp updates. No private conversations are processed.",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = TextGray
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = { viewModel.showAccessibilityPrompt.value = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Later", color = TextGray)
                        }
                        Button(
                            onClick = {
                                viewModel.showAccessibilityPrompt.value = false
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Activate", color = NavyDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinSetupScreen(viewModel: DashboardViewModel) {
    val pin by viewModel.pinSetupInput.collectAsState()
    val confirm by viewModel.pinSetupConfirmInput.collectAsState()
    val error by viewModel.pinSetupError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateToHome() }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
            }
            Text("Cancel Setup", color = TextWhite)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .size(72.dp)
                .background(AccentTeal.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = AccentTeal,
                modifier = Modifier.size(36.dp)
            )
        }

        Text(
            text = "Configure Shield PIN",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Register a 4 to 6-digit PIN. Disabling updates protection will be restricted to authorized entry.",
            fontSize = 13.sp,
            color = TextGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) viewModel.pinSetupInput.value = it },
            label = { Text("Choose PIN", color = TextGray) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedBorderColor = AccentTeal,
                unfocusedBorderColor = NavyLight
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pin_setup_input")
        )

        OutlinedTextField(
            value = confirm,
            onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) viewModel.pinSetupConfirmInput.value = it },
            label = { Text("Confirm PIN", color = TextGray) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedBorderColor = AccentTeal,
                unfocusedBorderColor = NavyLight
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pin_setup_confirm")
        )

        if (error != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ActiveRed.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = "Error", tint = ActiveRed)
                    Text(text = error!!, color = ActiveRed, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.handlePinSetupSave() },
            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("save_pin_button")
        ) {
            Text("Save & Guard", color = NavyDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun PinVerifyScreen(viewModel: DashboardViewModel) {
    val input by viewModel.pinVerifyInput.collectAsState()
    val error by viewModel.pinVerifyError.collectAsState()
    val timeLeft by viewModel.lockoutTimeLeft.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateToHome() }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
            }
            Text("Return Home", color = TextWhite)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(72.dp)
                .background(ActiveRed.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = ActiveRed,
                modifier = Modifier.size(36.dp)
            )
        }

        Text(
            text = "Verification Gate",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Enter your custom Shield PIN to modify your focus preferences.",
            fontSize = 13.sp,
            color = TextGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        OutlinedTextField(
            value = input,
            onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) viewModel.pinVerifyInput.value = it },
            label = { Text("Enter 4-6 Digit PIN", color = TextGray) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedBorderColor = ActiveRed,
                unfocusedBorderColor = NavyLight
            ),
            singleLine = true,
            enabled = timeLeft <= 0,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pin_input")
        )

        if (timeLeft > 0) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ActiveRed.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "LOCKOUT ACTIVE",
                        fontWeight = FontWeight.Bold,
                        color = ActiveRed,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Too many wrong attempts. Try again in $timeLeft seconds.",
                        color = TextWhite,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (error != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ActiveRed.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = "Error", tint = ActiveRed)
                    Text(text = error!!, color = ActiveRed, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.handlePinVerify() },
            colors = ButtonDefaults.buttonColors(containerColor = ActiveRed),
            enabled = timeLeft <= 0 && input.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("verify_pin_button")
        ) {
            Text("Unlock & Modify", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
