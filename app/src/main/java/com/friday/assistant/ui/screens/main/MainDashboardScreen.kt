package com.friday.assistant.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friday.assistant.core.state.AssistantOrbState
import com.friday.assistant.core.state.MessageSender
import com.friday.assistant.ui.components.FridayOrb
import com.friday.assistant.ui.components.GlassCard
import com.friday.assistant.ui.components.SecurityConfirmationDialog
import com.friday.assistant.ui.components.WaveformVisualizer
import com.friday.assistant.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    viewModel: DashboardViewModel,
    shouldAutoListen: Boolean = false,
    onNavigateToHistory: () -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToBrowser: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToModels: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToOffline: () -> Unit,
    onNavigateToVision: () -> Unit,
    onNavigateToAutomation: () -> Unit
) {
    val orbState by viewModel.orbState.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var showHudMenu by remember { mutableStateOf(false) }

    val greeting = remember { viewModel.getGreeting() }
    val currentTime = remember {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    }

    LaunchedEffect(shouldAutoListen) {
        if (shouldAutoListen) {
            viewModel.startVoiceListening()
        }
    }

    val quickActions = listOf(
        "Morning briefing",
        "Audit security posture",
        "Schedule meeting tomorrow at 4 pm",
        "Arrived at home",
        "Turn on the lights",
        "Where am I?",
        "Export backup",
        "Initiate workout protocol",
        "Focus protocol",
        "Movie night protocol",
        "Driving protocol",
        "House party protocol",
        "Translate hello to Tamil",
        "Enable floating arc reactor",
        "Call Home Minister on speaker",
        "Open WhatsApp",
        "What is quantum computing?",
        "Calculate 15% of 850",
        "What is my battery status?",
        "Turn on flashlight"
    )

    // Security Confirmation Modal
    pendingConfirmation?.let { conf ->
        SecurityConfirmationDialog(
            title = conf.title,
            description = conf.description,
            onConfirm = conf.onConfirm,
            onDismiss = conf.onCancel
        )
    }

    // Top HUD Menu (Control Center) Modal Bottom Sheet
    if (showHudMenu) {
        ModalBottomSheet(
            onDismissRequest = { showHudMenu = false },
            containerColor = FridaySurface,
            contentColor = TextPrimary,
            scrimColor = Color.Black.copy(alpha = 0.7f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "F.R.I.D.A.Y. CONTROL CENTER",
                    style = FridayTypography.labelSmall,
                    color = FridayCyan,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Mark VI Armor Subsystems & Operational Directives",
                    style = FridayTypography.bodyMedium,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(20.dp))

                HudMenuTile(
                    title = "Security & Permanent Blocklist",
                    subtitle = "Strict financial fraud protection (Google Pay, UPI, Banking)",
                    icon = Icons.Default.Shield,
                    iconTint = FridaySuccessGreen
                ) {
                    showHudMenu = false
                    onNavigateToSecurity()
                }

                HudMenuTile(
                    title = "Application Control Manager",
                    subtitle = "Manage permitted mobile apps & launch overrides",
                    icon = Icons.Default.Apps,
                    iconTint = FridayCyan
                ) {
                    showHudMenu = false
                    onNavigateToApps()
                }

                HudMenuTile(
                    title = "Browser Research Agent",
                    subtitle = "Live web search, page summarization & offline article reader",
                    icon = Icons.Default.Language,
                    iconTint = Color(0xFF00E5FF)
                ) {
                    showHudMenu = false
                    onNavigateToBrowser()
                }

                HudMenuTile(
                    title = "Persistent Memory & Notes",
                    subtitle = "On-device Room DB: personal facts, notes, and reminders",
                    icon = Icons.Default.Psychology,
                    iconTint = Color(0xFF69F0AE)
                ) {
                    showHudMenu = false
                    onNavigateToMemory()
                }

                HudMenuTile(
                    title = "On-Device Offline AI Models",
                    subtitle = "Local offline reasoner, deterministic math, and offline capabilities",
                    icon = Icons.Default.Memory,
                    iconTint = Color(0xFFFFAB40)
                ) {
                    showHudMenu = false
                    onNavigateToOffline()
                }

                HudMenuTile(
                    title = "AI Brain & Architecture Config",
                    subtitle = "Google Gemini API key, OpenAI & Gateway endpoint",
                    icon = Icons.Default.Psychology,
                    iconTint = FridayGoldAlert
                ) {
                    showHudMenu = false
                    onNavigateToModels()
                }

                HudMenuTile(
                    title = "Voice & Neural Speech Engine",
                    subtitle = "Pitch, speech rate, and female neural profile",
                    icon = Icons.Default.GraphicEq,
                    iconTint = FridayElectricBlue
                ) {
                    showHudMenu = false
                    onNavigateToVoice()
                }

                HudMenuTile(
                    title = "System Permissions Manager",
                    subtitle = "Accessibility, microphone, and foreground services",
                    icon = Icons.Default.Lock,
                    iconTint = Color(0xFF00FFFF)
                ) {
                    showHudMenu = false
                    onNavigateToPermissions()
                }

                HudMenuTile(
                    title = "Conversation & Directives Log",
                    subtitle = "Encrypted conversation audit records",
                    icon = Icons.Default.History,
                    iconTint = TextSecondary
                ) {
                    showHudMenu = false
                    onNavigateToHistory()
                }

                HudMenuTile(
                    title = "Automation Task History",
                    subtitle = "Verified mobile execution trail",
                    icon = Icons.Default.Bolt,
                    iconTint = FridayElectricBlue
                ) {
                    showHudMenu = false
                    onNavigateToAutomation()
                }

                HudMenuTile(
                    title = "HUD Vision Optics Scanner",
                    subtitle = "Camera targeting reticle & optical mathematical solver",
                    icon = Icons.Default.CenterFocusStrong,
                    iconTint = FridayCyan
                ) {
                    showHudMenu = false
                    onNavigateToVision()
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    Scaffold(
        containerColor = FridayBgDark,
        bottomBar = {
            // Pure, Decluttered Futuristic Command Dock (Only Text Field + Glowing Mic Button)
            Surface(
                color = FridaySurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, FridayCyan.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                if (voiceState.isListening) "Listening to voice directive..." else "Command or instruct F.R.I.D.A.Y...",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    if (textInput.isNotBlank()) {
                        IconButton(
                            onClick = {
                                viewModel.submitTextCommand(textInput)
                                textInput = ""
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send Command",
                                tint = FridayCyan
                            )
                        }
                    }

                    // Floating Glowing Arc Reactor Styled Mic Button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (voiceState.isListening) FridayCrimsonBlock else FridayCyan
                            )
                            .clickable { viewModel.toggleVoiceListening() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (voiceState.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = FridayBgDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Iron Man HUD Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "F.R.I.D.A.Y.",
                            style = FridayTypography.headlineMedium,
                            color = FridayCyan,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(FridaySuccessGreen)
                        )
                    }
                    Text(
                        text = "$greeting • $currentTime",
                        style = FridayTypography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Security Shield Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = FridaySurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, FridaySuccessGreen.copy(alpha = 0.6f)),
                        modifier = Modifier.clickable { onNavigateToSecurity() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Security Status",
                                tint = FridaySuccessGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "SHIELD ACTIVE",
                                style = FridayTypography.labelSmall,
                                color = FridaySuccessGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // HUD Menu / Settings Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = FridaySurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, FridayCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable { showHudMenu = true }
                    ) {
                        Box(
                            modifier = Modifier.padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Widgets,
                                contentDescription = "HUD Control Center",
                                tint = FridayCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Signature Mark VI Arc Reactor Centerpiece (Matches Reference Image)
            FridayOrb(
                state = orbState,
                audioLevel = voiceState.audioRmsDb,
                size = 260.dp,
                onClick = { viewModel.toggleVoiceListening() }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Dynamic Arc Reactor Telemetry Badge
            val (statusText, statusColor) = when (orbState) {
                AssistantOrbState.IDLE -> Pair("ARC REACTOR STABLE // TAP TO ENGAGE", FridayCyan)
                AssistantOrbState.LISTENING -> Pair("LISTENING // VOCAL TELEMETRY ACTIVE", FridayNeonBlue)
                AssistantOrbState.THINKING -> Pair("REASONING & COMPUTING DIRECTIVE...", FridayGoldAlert)
                AssistantOrbState.SPEAKING -> Pair("VOCAL SYNTHESIZER TRANSMITTING...", FridayCyan)
                AssistantOrbState.EXECUTING -> Pair("EXECUTING MOBILE ACTION...", FridayElectricBlue)
                AssistantOrbState.BLOCKED -> Pair("RESTRICTION ENFORCED // ACTION BLOCKED", FridayCrimsonBlock)
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = FridaySurfaceLight,
                border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.6f))
            ) {
                Text(
                    text = statusText,
                    style = FridayTypography.labelSmall,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Real-Time Waveform Visualizer
            WaveformVisualizer(
                isListening = voiceState.isListening,
                audioRmsDb = voiceState.audioRmsDb,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            // Streaming Voice Transcript Preview
            AnimatedVisibility(
                visible = voiceState.partialText.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = "\"${voiceState.partialText}\"",
                    style = FridayTypography.bodyLarge,
                    color = FridayCyan,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Recent Conversation Response Card
            val lastAssistantMsg = messages.lastOrNull { it.sender == MessageSender.FRIDAY }
            if (lastAssistantMsg != null) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (lastAssistantMsg.isSecurityBlocked) FridayCrimsonBlock else FridayCyan.copy(alpha = 0.4f)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (lastAssistantMsg.isSecurityBlocked) "SECURITY RESTRICTION" else "F.R.I.D.A.Y. RESPONSE",
                                style = FridayTypography.labelSmall,
                                color = if (lastAssistantMsg.isSecurityBlocked) FridayCrimsonBlock else FridayCyan,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(
                                onClick = { viewModel.stopSpeaking() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeMute,
                                    contentDescription = "Mute Voice",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = lastAssistantMsg.text,
                            style = FridayTypography.bodyLarge,
                            color = TextPrimary
                        )

                        // Math step-by-step rendering
                        if (lastAssistantMsg.mathSteps.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Derivation Steps:",
                                style = FridayTypography.titleMedium,
                                color = FridayCyan,
                                fontSize = 12.sp
                            )
                            lastAssistantMsg.mathSteps.forEach { step ->
                                Text(
                                    text = "• $step",
                                    style = FridayTypography.bodyMedium,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Quick Directives Carousel
            Text(
                text = "QUICK DIRECTIVES",
                style = FridayTypography.labelSmall,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickActions) { action ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = FridaySurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, FridayGlassBorder.copy(alpha = 0.45f)),
                        modifier = Modifier.clickable { viewModel.submitTextCommand(action) }
                    ) {
                        Text(
                            text = action,
                            style = FridayTypography.bodyMedium,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HudMenuTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF0C1926),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, FridayGlassBorder.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = FridayTypography.titleMedium,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = FridayTypography.bodyMedium,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
