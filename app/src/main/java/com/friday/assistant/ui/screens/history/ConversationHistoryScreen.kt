package com.friday.assistant.ui.screens.history

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friday.assistant.data.local.FridayDatabase
import com.friday.assistant.data.local.entity.ConversationEntity
import com.friday.assistant.ui.components.GlassCard
import com.friday.assistant.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationHistoryScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { FridayDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()
    val messages by database.conversationDao().getAllMessages().collectAsState(initial = emptyList())

    Scaffold(
        containerColor = FridayBgDark,
        topBar = {
            TopAppBar(
                title = { Text("Conversation History", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = FridayCyan)
                    }
                },
                actions = {
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = {
                            scope.launch(Dispatchers.IO) { database.conversationDao().clearHistory() }
                        }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear History", tint = TextSecondary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FridayBgDark)
            )
        }
    ) { padding ->
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No past interactions recorded.", color = TextSecondary, style = FridayTypography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { msg ->
                    ConversationBubble(msg)
                }
            }
        }
    }
}

@Composable
fun ConversationBubble(msg: ConversationEntity) {
    val isUser = msg.sender == "USER"
    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) FridaySurfaceLight else FridaySurface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (msg.isSecurityBlocked) FridayCrimsonBlock else if (isUser) FridayElectricBlue else FridayGlassBorder
            ),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isUser) "YOU" else "F.R.I.D.A.Y.",
                        style = FridayTypography.labelSmall,
                        color = if (msg.isSecurityBlocked) FridayCrimsonBlock else if (isUser) FridayElectricBlue else FridayCyan,
                        fontSize = 10.sp
                    )
                    Text(text = timeStr, style = FridayTypography.bodyMedium, color = TextMuted, fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = msg.content,
                    style = FridayTypography.bodyLarge,
                    color = TextPrimary
                )

                if (msg.intentType != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Intent: ${msg.intentType}",
                        style = FridayTypography.labelSmall,
                        color = FridayCyan,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

