package com.friday.assistant.ui.screens.automation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friday.assistant.data.local.FridayDatabase
import com.friday.assistant.data.local.entity.AutomationEntity
import com.friday.assistant.ui.components.GlassCard
import com.friday.assistant.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationHistoryScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { FridayDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()
    val automations by database.automationDao().getAllAutomations().collectAsState(initial = emptyList())

    Scaffold(
        containerColor = FridayBgDark,
        topBar = {
            TopAppBar(
                title = { Text("Automation Audit Trail", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = FridayCyan)
                    }
                },
                actions = {
                    if (automations.isNotEmpty()) {
                        IconButton(onClick = {
                            scope.launch(Dispatchers.IO) { database.automationDao().clearHistory() }
                        }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Audit", tint = TextSecondary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FridayBgDark)
            )
        }
    ) { padding ->
        if (automations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No automation events logged yet.", color = TextSecondary, style = FridayTypography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(automations) { auto ->
                    AutomationAuditCard(auto)
                }
            }
        }
    }
}

@Composable
fun AutomationAuditCard(entity: AutomationEntity) {
    val timeStr = SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", Locale.getDefault()).format(Date(entity.timestamp))
    val isSuccess = entity.executionResult == "VERIFIED"

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = FridaySurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSuccess) FridaySuccessGreen.copy(alpha = 0.4f) else FridayCrimsonBlock.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entity.targetApp.uppercase(),
                    style = FridayTypography.labelSmall,
                    color = FridayCyan,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSuccess) FridaySuccessGreen.copy(alpha = 0.15f) else FridayCrimsonBlock.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = entity.executionResult,
                        color = if (isSuccess) FridaySuccessGreen else FridayCrimsonBlock,
                        style = FridayTypography.labelSmall,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Action: ${entity.actionType}",
                style = FridayTypography.bodyLarge,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            if (entity.parameters.isNotBlank()) {
                Text(
                    text = "Payload: \"${entity.parameters}\"",
                    style = FridayTypography.bodyMedium,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Package: ${entity.packageName}",
                style = FridayTypography.bodyMedium,
                color = TextMuted,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = timeStr,
                style = FridayTypography.bodyMedium,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

