package com.friday.assistant.ui.screens.models

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friday.assistant.data.preferences.PreferencesManager
import com.friday.assistant.data.remote.FridayApiClient
import com.friday.assistant.ui.components.GlassCard
import com.friday.assistant.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiModelConfigScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()

    val currentUrl by prefs.backendUrl.collectAsState(initial = PreferencesManager.DEFAULT_BACKEND_URL)
    val currentModel by prefs.selectedModel.collectAsState(initial = "gemini")
    val currentApiKey by prefs.geminiApiKey.collectAsState(initial = "")

    var inputUrl by remember { mutableStateOf("") }
    var inputApiKey by remember { mutableStateOf("") }
    var connectionStatus by remember { mutableStateOf("UNTESTED") }
    var latencyMs by remember { mutableStateOf<Long?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    LaunchedEffect(currentUrl, currentApiKey) {
        inputUrl = currentUrl
        inputApiKey = currentApiKey
    }

    Scaffold(
        containerColor = FridayBgDark,
        topBar = {
            TopAppBar(
                title = { Text("AI Model Architecture", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = FridayCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FridayBgDark)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Backend Server Configuration
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "BACKEND GATEWAY ENDPOINT",
                        style = FridayTypography.labelSmall,
                        color = FridayCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "FastAPI AI Brain hosting SymPy, Gemini & OpenAI Routing.",
                        style = FridayTypography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FridayCyan,
                            unfocusedBorderColor = FridayGlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true,
                        label = { Text("Gateway URL", color = TextSecondary) }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch { prefs.setBackendUrl(inputUrl) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FridayCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save URL", color = FridayBgDark, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                isChecking = true
                                scope.launch {
                                    val start = System.currentTimeMillis()
                                    try {
                                        val service = FridayApiClient.getService(inputUrl)
                                        val res = withContext(Dispatchers.IO) { service.checkHealth() }
                                        val end = System.currentTimeMillis()
                                        latencyMs = end - start
                                        connectionStatus = if (res.isSuccessful) "ONLINE" else "ERROR ${res.code()}"
                                    } catch (e: Exception) {
                                        connectionStatus = "OFFLINE (Local Reasoner Active)"
                                        latencyMs = null
                                    } finally {
                                        isChecking = false
                                    }
                                }
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, FridayCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isChecking) "Pinging..." else "Test Health", color = FridayCyan)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Status Indicator
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = FridaySurfaceLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Gateway Status:", color = TextSecondary, fontSize = 12.sp)
                            Text(
                                text = connectionStatus + (if (latencyMs != null) " (${latencyMs}ms)" else ""),
                                color = if (connectionStatus == "ONLINE") FridaySuccessGreen else FridayGoldAlert,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Direct Google Gemini API Key (ChatGPT / Gemini Mode)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "DIRECT GOOGLE GEMINI CLOUD AI",
                        style = FridayTypography.labelSmall,
                        color = FridayCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Optional: Paste your Gemini API Key to enable live cloud chatbot reasoning directly on your phone without needing a PC server.",
                        style = FridayTypography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputApiKey,
                        onValueChange = { inputApiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FridayCyan,
                            unfocusedBorderColor = FridayGlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true,
                        placeholder = { Text("AIzaSy...", color = TextMuted) },
                        label = { Text("Gemini API Key", color = TextSecondary) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            scope.launch { prefs.setGeminiApiKey(inputApiKey.trim()) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FridayCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (inputApiKey.isNotBlank()) "Save Cloud Key" else "Clear / Use Local AI", color = FridayBgDark, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Modular Routing Subsystems
            Text("ACTIVE REASONING ENGINES", style = FridayTypography.labelSmall, color = TextSecondary)

            EngineStatusCard(
                name = "Google Gemini Reasoning",
                description = "Multimodal natural language understanding & ecosystem routing",
                status = "ACTIVE"
            )

            EngineStatusCard(
                name = "OpenAI Planning Subsystem",
                description = "Multi-step complex decomposition & tool planning",
                status = "STANDBY"
            )

            EngineStatusCard(
                name = "SymPy Deterministic Math",
                description = "Exact algebraic solutions, derivatives, integrals & statistics",
                status = "VERIFIED DETERMINISTIC"
            )

            EngineStatusCard(
                name = "Webpage Intelligence & Browser",
                description = "Chrome intent automation, organic web search & article extraction",
                status = "INTEGRATED"
            )
        }
    }
}

@Composable
fun EngineStatusCard(name: String, description: String, status: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = FridaySurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, FridayGlassBorder.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = FridayTypography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, style = FridayTypography.bodyMedium, color = TextSecondary, fontSize = 12.sp)
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = FridayCyan.copy(alpha = 0.15f)
            ) {
                Text(
                    text = status,
                    color = FridayCyan,
                    style = FridayTypography.labelSmall,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

