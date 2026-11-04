package com.friday.assistant.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Dashboard : Screen("dashboard", "F.R.I.D.A.Y.")
    object Apps : Screen("apps", "Applications")
    object Browser : Screen("browser", "Browser Agent")
    object Memory : Screen("memory", "Memory & Notes")
    object Security : Screen("security", "Security Guard")
    object Models : Screen("models", "AI Model Router")
    object Voice : Screen("voice", "Voice Engine")
    object Permissions : Screen("permissions", "Permissions")
    object Offline : Screen("offline", "Offline Models")
    object Automation : Screen("automation", "Audit & History")
    object Vision : Screen("vision", "HUD Vision Optics")
    object History : Screen("history", "Conversation")
}
