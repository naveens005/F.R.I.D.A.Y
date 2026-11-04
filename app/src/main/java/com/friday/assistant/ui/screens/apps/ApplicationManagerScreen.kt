package com.friday.assistant.ui.screens.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friday.assistant.domain.apps.AppCategory
import com.friday.assistant.domain.apps.AppControlEngine
import com.friday.assistant.domain.apps.AppItem
import com.friday.assistant.domain.apps.InstalledAppsManager
import com.friday.assistant.ui.components.GlassCard
import com.friday.assistant.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationManagerScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val appsManager = remember { InstalledAppsManager(context) }
    val appControlEngine = remember { AppControlEngine(context) }
    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<AppCategory?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        apps = appsManager.getInstalledLaunchableApps()
    }

    val filteredApps = remember(apps, selectedCategory) {
        if (selectedCategory == null) apps else apps.filter { it.category == selectedCategory }
    }

    Scaffold(
        containerColor = FridayBgDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Application Manager", color = TextPrimary, fontWeight = FontWeight.Bold) },
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
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // Category Filter Row
            ScrollableTabRow(
                selectedTabIndex = if (selectedCategory == null) 0 else AppCategory.values().indexOf(selectedCategory) + 1,
                containerColor = FridaySurface,
                contentColor = FridayCyan,
                edgePadding = 16.dp,
                divider = {}
            ) {
                Tab(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    text = { Text("All (${apps.size})", fontSize = 12.sp) }
                )
                AppCategory.values().forEach { cat ->
                    val count = apps.count { it.category == cat }
                    if (count > 0) {
                        Tab(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            text = { Text("${cat.displayName} ($count)", fontSize = 12.sp) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredApps) { app ->
                    AppRowItem(
                        app = app,
                        onLaunch = {
                            val res = appControlEngine.executeAppAction(app.packageName, app.appName)
                            // Display result feedback
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppRowItem(
    app: AppItem,
    onLaunch: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = FridaySurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (app.isBlocked) FridayCrimsonBlock.copy(alpha = 0.5f) else FridayGlassBorder.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (app.isBlocked) FridayCrimsonBlock.copy(alpha = 0.2f) else FridaySurfaceLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (app.isBlocked) Icons.Default.Block else Icons.Default.Apps,
                        contentDescription = app.appName,
                        tint = if (app.isBlocked) FridayCrimsonBlock else FridayCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = app.appName,
                        style = FridayTypography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = app.packageName,
                        style = FridayTypography.bodyMedium,
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            if (app.isBlocked) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = FridayCrimsonBlock.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "BLOCKED",
                        color = FridayCrimsonBlock,
                        style = FridayTypography.labelSmall,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                IconButton(onClick = onLaunch) {
                    Icon(
                        imageVector = Icons.Default.Launch,
                        contentDescription = "Launch",
                        tint = FridayCyan
                    )
                }
            }
        }
    }
}

