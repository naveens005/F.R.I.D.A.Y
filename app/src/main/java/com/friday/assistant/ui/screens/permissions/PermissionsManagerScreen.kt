package com.friday.assistant.ui.screens.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.friday.assistant.ui.components.GlassCard
import com.friday.assistant.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsManagerScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var isMicrophoneGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isContactsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isPhoneCallGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isNotificationGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    Scaffold(
        containerColor = FridayBgDark,
        topBar = {
            TopAppBar(
                title = { Text("Permissions & Trust", color = TextPrimary, fontWeight = FontWeight.Bold) },
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
            Text("SUBSYSTEM PRIVACY & ACCESS CONTROL", style = FridayTypography.labelSmall, color = TextSecondary)

            PermissionItemCard(
                icon = Icons.Default.Mic,
                title = "Audio Recording (Microphone)",
                description = "Required to recognize user speech directives and stream audio levels.",
                isGranted = isMicrophoneGranted,
                actionLabel = "Open App Settings",
                onAction = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            )

            PermissionItemCard(
                icon = Icons.Default.Contacts,
                title = "Contacts Integration",
                description = "Allows calling your contacts (e.g. 'Call Home Minister') by looking up phone numbers in your address book.",
                isGranted = isContactsGranted,
                actionLabel = "Grant in Settings",
                onAction = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            )

            PermissionItemCard(
                icon = Icons.Default.Phone,
                title = "Phone Calling & Speakerphone",
                description = "Enables placing outgoing calls directly and toggling speakerphone as commanded.",
                isGranted = isPhoneCallGranted,
                actionLabel = "Grant in Settings",
                onAction = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            )

            PermissionItemCard(
                icon = Icons.Default.Notifications,
                title = "Foreground Notifications",
                description = "Displays the active voice service notification with microphone indicator.",
                isGranted = isNotificationGranted,
                actionLabel = "Manage Notifications",
                onAction = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }
            )

            // Security Commitment Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = FridaySurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, FridaySuccessGreen.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = "Verified", tint = FridaySuccessGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Strict Privacy Guarantee", style = FridayTypography.titleMedium, color = FridaySuccessGreen, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "• Voice processing is user-initiated only.\n• Sensitive credentials, OTPs, passwords, and banking apps are never monitored or recorded.\n• All audit logs are encrypted and persisted locally on the device.",
                        style = FridayTypography.bodyMedium,
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionItemCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = FridaySurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isGranted) FridayGlassBorder.copy(alpha = 0.3f) else FridayGoldAlert.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(icon, contentDescription = title, tint = if (isGranted) FridayCyan else FridayGoldAlert)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(title, style = FridayTypography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isGranted) FridaySuccessGreen.copy(alpha = 0.15f) else FridayGoldAlert.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isGranted) "GRANTED" else "REQUIRED",
                        color = if (isGranted) FridaySuccessGreen else FridayGoldAlert,
                        style = FridayTypography.labelSmall,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(description, style = FridayTypography.bodyMedium, color = TextSecondary, fontSize = 12.sp)

            if (!isGranted) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = FridayGoldAlert),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(actionLabel, color = FridayBgDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

