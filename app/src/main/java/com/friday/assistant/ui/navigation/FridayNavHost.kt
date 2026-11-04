package com.friday.assistant.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.friday.assistant.ui.screens.apps.ApplicationManagerScreen
import com.friday.assistant.ui.screens.automation.AutomationHistoryScreen
import com.friday.assistant.ui.screens.browser.BrowserAgentScreen
import com.friday.assistant.ui.screens.history.ConversationHistoryScreen
import com.friday.assistant.ui.screens.main.DashboardViewModel
import com.friday.assistant.ui.screens.main.MainDashboardScreen
import com.friday.assistant.ui.screens.memory.MemoryManagerScreen
import com.friday.assistant.ui.screens.models.AiModelConfigScreen
import com.friday.assistant.ui.screens.offline.OfflineModelManagerScreen
import com.friday.assistant.ui.screens.permissions.PermissionsManagerScreen
import com.friday.assistant.ui.screens.security.SecurityBlocklistScreen
import com.friday.assistant.ui.screens.vision.CameraVisionScreen
import com.friday.assistant.ui.screens.voice.VoiceSettingsScreen
import com.friday.assistant.ui.theme.*

@Composable
fun FridayNavHost(
    shouldAutoListen: Boolean = false,
    viewModel: DashboardViewModel = viewModel()
) {
    val navController = rememberNavController()

    Scaffold(
        containerColor = FridayBgDark
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                MainDashboardScreen(
                    viewModel = viewModel,
                    shouldAutoListen = shouldAutoListen,
                    onNavigateToHistory = { navController.navigate(Screen.History.route) },
                    onNavigateToApps = { navController.navigate(Screen.Apps.route) },
                    onNavigateToBrowser = { navController.navigate(Screen.Browser.route) },
                    onNavigateToMemory = { navController.navigate(Screen.Memory.route) },
                    onNavigateToSecurity = { navController.navigate(Screen.Security.route) },
                    onNavigateToModels = { navController.navigate(Screen.Models.route) },
                    onNavigateToVoice = { navController.navigate(Screen.Voice.route) },
                    onNavigateToPermissions = { navController.navigate(Screen.Permissions.route) },
                    onNavigateToOffline = { navController.navigate(Screen.Offline.route) },
                    onNavigateToVision = { navController.navigate(Screen.Vision.route) },
                    onNavigateToAutomation = { navController.navigate(Screen.Automation.route) }
                )
            }
            composable(Screen.History.route) {
                ConversationHistoryScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Apps.route) {
                ApplicationManagerScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Browser.route) {
                BrowserAgentScreen(
                    browserAgent = viewModel.taskOrchestrator.browserEngine,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Memory.route) {
                MemoryManagerScreen(
                    memoryManager = viewModel.taskOrchestrator.memoryManager,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Security.route) {
                SecurityBlocklistScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Models.route) {
                AiModelConfigScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Voice.route) {
                VoiceSettingsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Permissions.route) {
                PermissionsManagerScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Offline.route) {
                OfflineModelManagerScreen(
                    modelRouter = viewModel.taskOrchestrator.modelRouter,
                    preferencesManager = viewModel.taskOrchestrator.preferencesManager,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Vision.route) {
                CameraVisionScreen(
                    knowledgeEngine = viewModel.taskOrchestrator.knowledgeEngine,
                    ttsManager = null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Automation.route) {
                AutomationHistoryScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
