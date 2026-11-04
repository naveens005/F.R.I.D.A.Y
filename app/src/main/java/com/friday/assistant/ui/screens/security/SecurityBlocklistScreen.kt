package com.friday.assistant.ui.screens.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friday.assistant.core.security.MasterSwitchManager
import com.friday.assistant.core.security.SecurityBlocklist
import com.friday.assistant.ui.components.GlassCard
import com.friday.assistant.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SecurityBlocklistScreen(
    onNavigateBack: () -> Unit
) {
    val isMasterAutomationEnabled by MasterSwitchManager.isAutomationEnabled.collectAsState()

    Scaffold(
        containerColor = FridayBgDark,
        topBar = {
            TopAppBar(
                title = { Text("Security & Blocklist", color = TextPrimary, fontWeight = FontWeight.Bold) },
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master Automation Switch Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (isMasterAutomationEnabled) FridayCyan else FridayGoldAlert
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "MASTER AUTOMATION SWITCH",
                                style = FridayTypography.labelSmall,
                                color = if (isMasterAutomationEnabled) FridayCyan else FridayGoldAlert
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isMasterAutomationEnabled) "Automation Enabled" else "Automation Stopped",
                                style = FridayTypography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Instant kill-switch for all app intents and accessibility actions.",
                                style = FridayTypography.bodyMedium,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Switch(
                            checked = isMasterAutomationEnabled,
                            onCheckedChange = { MasterSwitchManager.setAutomationEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = FridayBgDark,
                                checkedTrackColor = FridayCyan,
                                uncheckedThumbColor = FridayBgDark,
                                uncheckedTrackColor = TextMuted
                            )
                        )
                    }
                }
            }

            // Zero-Override Policy Notice
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = FridayCrimsonBlock.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FridayCrimsonBlock.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = "Immutable", tint = FridayCrimsonBlock)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PERMANENT IMMUTABLE RESTRICTION",
                                style = FridayTypography.labelSmall,
                                color = FridayCrimsonBlock,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "F.R.I.D.A.Y. cannot access, automate, or read banking, UPI, or financial payment applications. This blocklist is permanently hardcoded and cannot be bypassed, disabled, or modified under any condition.",
                            style = FridayTypography.bodyMedium,
                            color = TextPrimary,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Standard Refusal: \"${SecurityBlocklist.REFUSAL_MESSAGE}\"",
                            style = FridayTypography.bodyMedium,
                            color = FridayGoldAlert,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Blocked Categories Overview
            item {
                Text(
                    text = "BLOCKED FINANCIAL APPLICATIONS (${SecurityBlocklist.BLOCKED_PACKAGES.size})",
                    style = FridayTypography.labelSmall,
                    color = TextSecondary
                )
            }

            items(SecurityBlocklist.BLOCKED_PACKAGES.toList()) { pkg ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FridaySurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, FridayGlassBorder.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = "Shield",
                            tint = FridayCrimsonBlock,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = pkg,
                                style = FridayTypography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Permanently Excluded from Automation",
                                style = FridayTypography.bodyMedium,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Prohibited Keywords
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "BLOCKED TRANSACTION KEYWORDS",
                    style = FridayTypography.labelSmall,
                    color = TextSecondary
                )
            }

            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecurityBlocklist.BLOCKED_KEYWORDS.forEach { keyword ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = FridaySurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, FridayCrimsonBlock.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = keyword,
                                color = TextPrimary,
                                style = FridayTypography.bodyMedium,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

