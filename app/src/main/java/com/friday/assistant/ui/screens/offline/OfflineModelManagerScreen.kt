package com.friday.assistant.ui.screens.offline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.friday.assistant.data.preferences.PreferencesManager
import com.friday.assistant.domain.ai.FridayAIModelRouter
import com.friday.assistant.ui.components.GlassCard
import com.friday.assistant.ui.theme.FridayBgDark
import com.friday.assistant.ui.theme.FridayCyan
import com.friday.assistant.ui.theme.TextPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineModelManagerScreen(
    modelRouter: FridayAIModelRouter,
    preferencesManager: PreferencesManager,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val forceOffline by preferencesManager.forceOffline.collectAsState(initial = false)
    val connectivity by modelRouter.connectivityMode.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = FridayBgDark,
        topBar = {
            TopAppBar(
                title = { Text("Offline AI Architecture", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = FridayCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FridayBgDark)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .background(FridayBgDark)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Independent, 100% on-device neural & procedural execution without internet requirement.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF78909C)
                )
            }

            // Active State Card
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Force Offline Execution", style = MaterialTheme.typography.titleSmall, color = Color.White)
                                Text("Routes 100% of tasks to local device engines", style = MaterialTheme.typography.bodySmall, color = Color(0xFF78909C))
                            }
                            Switch(
                                checked = forceOffline,
                                onCheckedChange = { scope.launch { preferencesManager.setForceOffline(it) } },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                            )
                        }

                        Divider(color = Color(0xFF1E3A5F))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Active Engine Status:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF78909C))
                            Text(
                                text = if (forceOffline) "FORCED OFFLINE (LOCAL)" else connectivity.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (forceOffline || connectivity.name == "OFFLINE") Color(0xFF00E5FF) else Color(0xFF00E676)
                            )
                        }
                    }
                }
            }

            // Offline Capabilities Checklist
            item {
                Text(
                    text = "VERIFIED OFFLINE SUBSYSTEMS",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF00E5FF)
                )
            }

            val capabilities = listOf(
                "Telephony Engine" to "Contacts search, phone dialing, and automated speakerphone toggle.",
                "Application Control" to "Launch any installed app via Android Intent/Package resolver.",
                "Deterministic Mathematics" to "PEMDAS equations, percentages, roots, powers, trigonometry, and unit conversions.",
                "General Knowledge Bank" to "Instant offline retrieval for science, technology, capitals, and Marvel lore.",
                "Device Hardware Telemetry" to "Battery capacity check, flashlight toggle, time/date synchronization.",
                "Persistent Memory & Notes" to "Room DB backed storage for notes, reminders, and custom facts.",
                "Defensive Security Guard" to "Permanent fail-closed blocking of Google Pay, PhonePe, Paytm, and banking."
            )

            items(capabilities.size) { i ->
                val (title, desc) = capabilities[i]
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = Color(0xFFB0BEC5))
                        }
                    }
                }
            }
        }
    }
}
