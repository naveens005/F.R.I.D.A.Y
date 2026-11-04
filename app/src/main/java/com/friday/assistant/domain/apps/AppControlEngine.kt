package com.friday.assistant.domain.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import com.friday.assistant.core.security.MasterSwitchManager
import com.friday.assistant.core.security.SecurityBlocklist
import java.net.URLEncoder

data class ExecutionResult(
    val success: Boolean,
    val message: String,
    val isSecurityBlocked: Boolean = false,
    val packageName: String? = null
)

class AppControlEngine(private val context: Context) {

    companion object {
        // Broad dictionary of popular applications and aliases
        private val COMMON_APP_MAP = mapOf(
            "whatsapp" to "com.whatsapp",
            "insta" to "com.instagram.android",
            "instagram" to "com.instagram.android",
            "youtube" to "com.google.android.youtube",
            "yt" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "google" to "com.google.android.googlequicksearchbox",
            "google search" to "com.google.android.googlequicksearchbox",
            "play store" to "com.android.vending",
            "playstore" to "com.android.vending",
            "google play" to "com.android.vending",
            "maps" to "com.google.android.apps.maps",
            "google maps" to "com.google.android.apps.maps",
            "map" to "com.google.android.apps.maps",
            "camera" to "com.google.android.GoogleCamera",
            "photos" to "com.google.android.apps.photos",
            "gallery" to "com.google.android.apps.photos",
            "google photos" to "com.google.android.apps.photos",
            "settings" to "com.android.settings",
            "calendar" to "com.google.android.calendar",
            "google calendar" to "com.google.android.calendar",
            "files" to "com.google.android.documentsui",
            "file manager" to "com.google.android.documentsui",
            "my files" to "com.google.android.documentsui",
            "phone" to "com.google.android.dialer",
            "dialer" to "com.google.android.dialer",
            "call" to "com.google.android.dialer",
            "contacts" to "com.google.android.contacts",
            "messages" to "com.google.android.apps.messaging",
            "message" to "com.google.android.apps.messaging",
            "sms" to "com.google.android.apps.messaging",
            "gmail" to "com.google.android.gm",
            "mail" to "com.google.android.gm",
            "email" to "com.google.android.gm",
            "clock" to "com.google.android.deskclock",
            "alarm" to "com.google.android.deskclock",
            "timer" to "com.google.android.deskclock",
            "calculator" to "com.google.android.calculator",
            "recorder" to "com.google.android.apps.recorder",
            "voice recorder" to "com.google.android.apps.recorder",
            "drive" to "com.google.android.apps.docs",
            "google drive" to "com.google.android.apps.docs",
            "keep" to "com.google.android.keep",
            "keep notes" to "com.google.android.keep",
            "notes" to "com.google.android.keep",
            "youtube music" to "com.google.android.apps.youtube.music",
            "yt music" to "com.google.android.apps.youtube.music",
            "meet" to "com.google.android.apps.meetings",
            "google meet" to "com.google.android.apps.meetings",
            "spotify" to "com.spotify.music",
            "netflix" to "com.netflix.mediaclient",
            "telegram" to "org.telegram.messenger",
            "twitter" to "com.twitter.android",
            "x" to "com.twitter.android",
            "linkedin" to "com.linkedin.android",
            "uber" to "com.ubercab",
            "swiggy" to "in.swiggy.android",
            "zomato" to "com.application.zomato",
            "prime video" to "com.amazon.avod.thirdpartyclient",
            "amazon" to "in.amazon.mShop.android.shopping",
            "hotstar" to "in.startv.hotstar",
            "disney hotstar" to "in.startv.hotstar",
            "snapchat" to "com.snapchat.android",
            "pinterest" to "com.pinterest",
            "reddit" to "com.reddit.frontpage",
            "facebook" to "com.facebook.katana",
            "fb" to "com.facebook.katana",
            "threads" to "com.instagram.barcelona",
            "vlc" to "org.videolan.vlc",
            "truecaller" to "com.truecaller",
            "zoom" to "us.zoom.videomeetings"
        )
    }

    /**
     * Executes mobile application launching and deep linking with strict security verification.
     */
    fun executeAppAction(
        packageName: String?,
        appName: String?,
        actionType: String? = null,
        query: String? = null
    ): ExecutionResult {
        // 1. Master Automation Switch Check
        if (!MasterSwitchManager.isEnabled()) {
            return ExecutionResult(
                success = false,
                message = "Automation is currently disabled via the Master Switch."
            )
        }

        // 2. Strict Permanent Security Blocklist Enforcement
        val rawTarget = cleanTargetName(packageName ?: appName ?: query ?: "")
        val securityCheck = SecurityBlocklist.verifySecurity(rawTarget, query)
        if (!securityCheck.isAllowed) {
            return ExecutionResult(
                success = false,
                message = securityCheck.refusalMessage,
                isSecurityBlocked = true,
                packageName = packageName
            )
        }

        // 3. Resolve target app identifier
        val resolvedTarget = resolveTargetApp(packageName, appName, query)

        return try {
            when {
                // Specialized App Handling
                resolvedTarget.contains("whatsapp") -> handleWhatsApp(query)
                resolvedTarget.contains("instagram") -> handleInstagram(query)
                resolvedTarget.contains("youtube") -> handleYouTube(query)
                resolvedTarget.contains("maps") -> handleMaps(query)
                resolvedTarget.contains("chrome") -> handleChrome(query)
                resolvedTarget.contains("dialer") || resolvedTarget.contains("phone") -> handleDialer(query)
                resolvedTarget.contains("camera") -> handleCamera()
                resolvedTarget.contains("photos") || resolvedTarget.contains("gallery") -> handleGallery()
                resolvedTarget.contains("settings") -> handleSettings()
                resolvedTarget.contains("calendar") -> handleCalendar()
                resolvedTarget.contains("documentsui") || resolvedTarget.contains("files") -> handleFiles()
                resolvedTarget.contains("calculator") -> handleCalculator()
                resolvedTarget.contains("deskclock") || resolvedTarget.contains("clock") -> handleClock()
                resolvedTarget.contains("gmail") || resolvedTarget.contains("mail") -> handleGmail(query)
                resolvedTarget.contains("messaging") || resolvedTarget.contains("sms") -> handleMessages(query)

                // Package name provided directly
                !packageName.isNullOrBlank() -> launchGenericPackage(packageName, appName)

                // Dynamic Discovery of installed apps on the device (full device scan)
                else -> discoverAndLaunchApp(resolvedTarget, appName ?: resolvedTarget)
            }
        } catch (e: Exception) {
            ExecutionResult(false, "Failed to launch application: ${e.localizedMessage}")
        }
    }

    private fun cleanTargetName(raw: String): String {
        return raw.lowercase().trim()
            .removePrefix("open ")
            .removePrefix("launch ")
            .removePrefix("start ")
            .removePrefix("the ")
            .removeSuffix(" app")
            .removeSuffix(" application")
            .trim()
    }

    /**
     * Resolves app name / target from any available parameter.
     */
    private fun resolveTargetApp(packageName: String?, appName: String?, query: String?): String {
        if (!packageName.isNullOrBlank()) return packageName.trim().lowercase()

        val nameTarget = cleanTargetName(appName ?: "")
        if (nameTarget.isNotBlank()) {
            COMMON_APP_MAP[nameTarget]?.let { return it }
            return nameTarget
        }

        val qTarget = cleanTargetName(query ?: "")
        if (qTarget.isNotBlank()) {
            COMMON_APP_MAP[qTarget]?.let { return it }
            for ((key, value) in COMMON_APP_MAP) {
                if (qTarget == key || qTarget.startsWith("$key ") || qTarget.contains(" $key")) {
                    return value
                }
            }
            return qTarget
        }
        return ""
    }

    /**
     * Deep device discovery: Iterates ALL launcher activities and installed packages on the device.
     */
    private fun discoverAndLaunchApp(query: String, displayName: String): ExecutionResult {
        val cleanQ = cleanTargetName(query)
        if (cleanQ.isBlank()) {
            return ExecutionResult(false, "No target application specified.")
        }

        // Check common dictionary again
        COMMON_APP_MAP[cleanQ]?.let { mappedPkg ->
            val launchIntent = context.packageManager.getLaunchIntentForPackage(mappedPkg)
            if (launchIntent != null) {
                return launchGenericPackage(mappedPkg, displayName)
            }
        }

        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveList = pm.queryIntentActivities(mainIntent, 0)
        val normalizedQuery = cleanQ.replace(" ", "").replace("-", "").replace("_", "")

        var bestPkg: String? = null
        var bestLabel: String? = null

        // Pass 1: Exact label match
        for (info in resolveList) {
            val pkg = info.activityInfo.packageName
            val label = info.loadLabel(pm).toString()
            val lowerLabel = label.lowercase()

            if (SecurityBlocklist.isPackageBlocked(pkg) || SecurityBlocklist.isPackageBlocked(lowerLabel)) continue

            if (lowerLabel == cleanQ) {
                bestPkg = pkg
                bestLabel = label
                break
            }
        }

        // Pass 2: Normalized label match (removes spaces, symbols)
        if (bestPkg == null) {
            for (info in resolveList) {
                val pkg = info.activityInfo.packageName
                val label = info.loadLabel(pm).toString()
                val normLabel = label.lowercase().replace(" ", "").replace("-", "").replace("_", "")

                if (SecurityBlocklist.isPackageBlocked(pkg) || SecurityBlocklist.isPackageBlocked(label)) continue

                if (normLabel == normalizedQuery || normLabel.contains(normalizedQuery) || normalizedQuery.contains(normLabel)) {
                    bestPkg = pkg
                    bestLabel = label
                    break
                }
            }
        }

        // Pass 3: Contains substring match
        if (bestPkg == null) {
            for (info in resolveList) {
                val pkg = info.activityInfo.packageName
                val label = info.loadLabel(pm).toString()
                val lowerLabel = label.lowercase()

                if (SecurityBlocklist.isPackageBlocked(pkg) || SecurityBlocklist.isPackageBlocked(lowerLabel)) continue

                if (lowerLabel.contains(cleanQ) || cleanQ.contains(lowerLabel)) {
                    bestPkg = pkg
                    bestLabel = label
                    break
                }
            }
        }

        // Pass 4: Package name match
        if (bestPkg == null) {
            for (info in resolveList) {
                val pkg = info.activityInfo.packageName.lowercase()
                if (SecurityBlocklist.isPackageBlocked(pkg)) continue
                if (pkg.contains(cleanQ) || pkg.contains(normalizedQuery)) {
                    bestPkg = info.activityInfo.packageName
                    bestLabel = info.loadLabel(pm).toString()
                    break
                }
            }
        }

        // Pass 5: Fallback scan of all installed applications
        if (bestPkg == null) {
            try {
                val installedPackages = pm.getInstalledPackages(0)
                for (p in installedPackages) {
                    val pkg = p.packageName
                    if (SecurityBlocklist.isPackageBlocked(pkg)) continue
                    val launchIntent = pm.getLaunchIntentForPackage(pkg) ?: continue
                    val appInfo = p.applicationInfo ?: continue
                    val label = appInfo.loadLabel(pm).toString()
                    val lowerLabel = label.lowercase()
                    val normLabel = lowerLabel.replace(" ", "").replace("-", "").replace("_", "")

                    if (lowerLabel == cleanQ || normLabel == normalizedQuery ||
                        lowerLabel.contains(cleanQ) || cleanQ.contains(lowerLabel) ||
                        pkg.lowercase().contains(cleanQ) || pkg.lowercase().contains(normalizedQuery)
                    ) {
                        bestPkg = pkg
                        bestLabel = label
                        break
                    }
                }
            } catch (e: Exception) {
                // Fallback scan error handled
            }
        }

        return if (bestPkg != null) {
            launchGenericPackage(bestPkg, bestLabel ?: displayName)
        } else {
            ExecutionResult(
                success = false,
                message = "$displayName is not installed on this device."
            )
        }
    }

    private fun launchGenericPackage(packageName: String, appName: String?): ExecutionResult {
        // Double-check security
        if (SecurityBlocklist.isPackageBlocked(packageName)) {
            return ExecutionResult(false, SecurityBlocklist.REFUSAL_MESSAGE, isSecurityBlocked = true)
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            ExecutionResult(
                success = true,
                message = "Opened ${appName ?: packageName}.",
                packageName = packageName
            )
        } else {
            ExecutionResult(
                success = false,
                message = "${appName ?: packageName} is not installed on this device.",
                packageName = packageName
            )
        }
    }

    private fun handleWhatsApp(query: String?): ExecutionResult {
        val cleanMsg = cleanAppQuery(query, "whatsapp")
        val intent = if (!cleanMsg.isNullOrBlank()) {
            val encodedMsg = URLEncoder.encode(cleanMsg, "UTF-8")
            Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?text=$encodedMsg")).apply {
                setPackage("com.whatsapp")
            }
        } else {
            context.packageManager.getLaunchIntentForPackage("com.whatsapp")
        }
        return launchIntentSafely(intent, "WhatsApp", "com.whatsapp")
    }

    private fun handleInstagram(query: String?): ExecutionResult {
        val cleanQuery = cleanAppQuery(query, "instagram")
        val intent = if (!cleanQuery.isNullOrBlank()) {
            val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/explore/tags/$encoded/")).apply {
                setPackage("com.instagram.android")
            }
        } else {
            context.packageManager.getLaunchIntentForPackage("com.instagram.android")
        }
        return launchIntentSafely(intent, "Instagram", "com.instagram.android")
    }

    private fun handleYouTube(query: String?): ExecutionResult {
        val cleanQuery = cleanAppQuery(query, "youtube")
        val intent = if (!cleanQuery.isNullOrBlank()) {
            Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", cleanQuery)
            }
        } else {
            context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
        }
        return launchIntentSafely(intent, "YouTube", "com.google.android.youtube")
    }

    private fun handleMaps(query: String?): ExecutionResult {
        val cleanQuery = cleanAppQuery(query, "maps")
        val intent = if (!cleanQuery.isNullOrBlank()) {
            val encodedQuery = URLEncoder.encode(cleanQuery, "UTF-8")
            Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$encodedQuery")).apply {
                setPackage("com.google.android.apps.maps")
            }
        } else {
            context.packageManager.getLaunchIntentForPackage("com.google.android.apps.maps")
        }
        return launchIntentSafely(intent, "Google Maps", "com.google.android.apps.maps")
    }

    private fun handleChrome(query: String?): ExecutionResult {
        val cleanQuery = cleanAppQuery(query, "chrome")
        val url = if (!cleanQuery.isNullOrBlank()) {
            if (cleanQuery.startsWith("http://") || cleanQuery.startsWith("https://")) {
                cleanQuery
            } else {
                "https://www.google.com/search?q=" + URLEncoder.encode(cleanQuery, "UTF-8")
            }
        } else {
            "https://www.google.com"
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage("com.android.chrome")
        }
        return launchIntentSafely(intent, "Chrome", "com.android.chrome")
    }

    private fun handleDialer(query: String?): ExecutionResult {
        val cleanNum = query?.filter { it.isDigit() || it == '+' }
        val intent = if (!cleanNum.isNullOrBlank()) {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNum"))
        } else {
            Intent(Intent.ACTION_DIAL)
        }
        return launchIntentSafely(intent, "Phone", "com.google.android.dialer")
    }

    private fun handleCamera(): ExecutionResult {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        return launchIntentSafely(intent, "Camera", "com.google.android.GoogleCamera")
    }

    private fun handleGallery(): ExecutionResult {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            type = "image/*"
        }
        return launchIntentSafely(intent, "Gallery / Photos", "com.google.android.apps.photos")
    }

    private fun handleSettings(): ExecutionResult {
        val intent = Intent(Settings.ACTION_SETTINGS)
        return launchIntentSafely(intent, "Settings", "com.android.settings")
    }

    private fun handleCalendar(): ExecutionResult {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("content://com.android.calendar/time")
        }
        return launchIntentSafely(intent, "Calendar", "com.google.android.calendar")
    }

    private fun handleFiles(): ExecutionResult {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
        }
        return launchIntentSafely(intent, "Files", "com.google.android.documentsui")
    }

    private fun handleCalculator(): ExecutionResult {
        val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.calculator")
            ?: Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_CALCULATOR) }
        return launchIntentSafely(intent, "Calculator", "com.google.android.calculator")
    }

    private fun handleClock(): ExecutionResult {
        val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.deskclock")
            ?: Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS)
        return launchIntentSafely(intent, "Clock", "com.google.android.deskclock")
    }

    private fun handleGmail(query: String?): ExecutionResult {
        val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.gm")
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("mailto:"))
        return launchIntentSafely(intent, "Gmail", "com.google.android.gm")
    }

    private fun handleMessages(query: String?): ExecutionResult {
        val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.messaging")
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("sms:"))
        return launchIntentSafely(intent, "Messages", "com.google.android.apps.messaging")
    }

    private fun cleanAppQuery(raw: String?, appKeyword: String): String? {
        if (raw.isNullOrBlank()) return null
        val lower = raw.trim()
        val cleaned = lower
            .removePrefix("open $appKeyword and search for ")
            .removePrefix("open $appKeyword and search ")
            .removePrefix("open $appKeyword and play ")
            .removePrefix("open $appKeyword and text ")
            .removePrefix("open $appKeyword and ")
            .removePrefix("open $appKeyword ")
            .removePrefix("search for ")
            .removePrefix("search ")
            .removePrefix("play ")
            .trim()
        return if (cleaned.isBlank() || cleaned.equals(appKeyword, ignoreCase = true)) null else cleaned
    }

    private fun launchIntentSafely(intent: Intent?, appName: String, fallbackPackage: String?): ExecutionResult {
        if (intent == null && fallbackPackage != null) {
            val fallbackLaunch = context.packageManager.getLaunchIntentForPackage(fallbackPackage)
            if (fallbackLaunch != null) {
                fallbackLaunch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(fallbackLaunch)
                return ExecutionResult(true, "Opened $appName.", packageName = fallbackPackage)
            }
            return ExecutionResult(false, "$appName is not installed on this device.", packageName = fallbackPackage)
        }

        if (intent == null) {
            return ExecutionResult(false, "$appName is not installed.")
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val resolve = context.packageManager.resolveActivity(intent, 0)
        return if (resolve != null) {
            context.startActivity(intent)
            ExecutionResult(true, "Opened $appName.", packageName = fallbackPackage)
        } else if (fallbackPackage != null) {
            val fallbackLaunch = context.packageManager.getLaunchIntentForPackage(fallbackPackage)
            if (fallbackLaunch != null) {
                fallbackLaunch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(fallbackLaunch)
                ExecutionResult(true, "Opened $appName.", packageName = fallbackPackage)
            } else {
                ExecutionResult(false, "$appName is not installed on this device.", packageName = fallbackPackage)
            }
        } else {
            ExecutionResult(false, "Could not open $appName.")
        }
    }
}
