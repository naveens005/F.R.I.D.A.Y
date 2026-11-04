package com.friday.assistant.domain.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.friday.assistant.core.security.SecurityBlocklist

data class AppItem(
    val appName: String,
    val packageName: String,
    val category: AppCategory,
    val icon: Drawable? = null,
    val isBlocked: Boolean = false
)

enum class AppCategory(val displayName: String) {
    SOCIAL("Social & Chat"),
    BROWSER("Browsers & Search"),
    MEDIA("Media & Streaming"),
    PRODUCTIVITY("Productivity & Work"),
    NAVIGATION("Maps & Navigation"),
    COMMUNICATION("Phone & Contacts"),
    UTILITY("Utilities & Tools"),
    SECURITY_EXCLUDED("Security Blocklist"),
    OTHER("Applications")
}

class InstalledAppsManager(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * Discovers all user-launchable applications on the device.
     */
    fun getInstalledLaunchableApps(): List<AppItem> {
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
        val appList = mutableListOf<AppItem>()

        for (resolveInfo in resolveInfos) {
            val packageName = resolveInfo.activityInfo.packageName
            val appName = resolveInfo.loadLabel(packageManager).toString()
            val icon = resolveInfo.loadIcon(packageManager)
            val isBlocked = SecurityBlocklist.isPackageBlocked(packageName)

            val category = if (isBlocked) {
                AppCategory.SECURITY_EXCLUDED
            } else {
                categorizePackage(packageName)
            }

            appList.add(
                AppItem(
                    appName = appName,
                    packageName = packageName,
                    category = category,
                    icon = icon,
                    isBlocked = isBlocked
                )
            )
        }

        // Fallback discovery if resolveList is incomplete
        if (appList.size < 10) {
            try {
                val installedPackages = packageManager.getInstalledPackages(0)
                val existingPkgs = appList.map { it.packageName }.toSet()
                for (p in installedPackages) {
                    val packageName = p.packageName
                    if (existingPkgs.contains(packageName)) continue
                    val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: continue
                    val appInfo = p.applicationInfo ?: continue
                    val appName = appInfo.loadLabel(packageManager).toString()
                    val icon = appInfo.loadIcon(packageManager)
                    val isBlocked = SecurityBlocklist.isPackageBlocked(packageName)
                    val category = if (isBlocked) AppCategory.SECURITY_EXCLUDED else categorizePackage(packageName)
                    appList.add(AppItem(appName, packageName, category, icon, isBlocked))
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        return appList.sortedWith(compareBy({ it.isBlocked }, { it.appName }))
    }

    private fun categorizePackage(pkg: String): AppCategory {
        val p = pkg.lowercase()
        return when {
            p.contains("whatsapp") || p.contains("instagram") || p.contains("facebook") ||
                    p.contains("twitter") || p.contains("telegram") || p.contains("snapchat") -> AppCategory.SOCIAL

            p.contains("chrome") || p.contains("browser") || p.contains("firefox") ||
                    p.contains("opera") || p.contains("searchbox") -> AppCategory.BROWSER

            p.contains("youtube") || p.contains("spotify") || p.contains("music") ||
                    p.contains("video") || p.contains("photos") || p.contains("gallery") -> AppCategory.MEDIA

            p.contains("gm") || p.contains("email") || p.contains("calendar") ||
                    p.contains("docs") || p.contains("sheets") || p.contains("drive") -> AppCategory.PRODUCTIVITY

            p.contains("maps") || p.contains("navigation") || p.contains("uber") ||
                    p.contains("ola") -> AppCategory.NAVIGATION

            p.contains("dialer") || p.contains("phone") || p.contains("contacts") ||
                    p.contains("messaging") || p.contains("sms") -> AppCategory.COMMUNICATION

            p.contains("camera") || p.contains("calculator") || p.contains("clock") ||
                    p.contains("settings") || p.contains("documentsui") || p.contains("files") -> AppCategory.UTILITY

            else -> AppCategory.OTHER
        }
    }
}

