package com.friday.assistant.domain.browser

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.friday.assistant.domain.apps.ExecutionResult
import java.net.URLEncoder

class BrowserAutomationEngine(private val context: Context) {

    /**
     * Executes web search in Chrome or default browser.
     */
    fun openSearch(query: String): ExecutionResult {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.google.com/search?q=$encoded"
            openUrl(url)
        } catch (e: Exception) {
            ExecutionResult(false, "Failed to perform web search: ${e.localizedMessage}")
        }
    }

    /**
     * Opens target web URL.
     */
    fun openUrl(url: String): ExecutionResult {
        return try {
            val validUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
                url
            } else {
                "https://$url"
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // Attempt to target Chrome if available
                setPackage("com.android.chrome")
            }

            // Verify if Chrome is installed, else use system chooser/default browser
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                context.startActivity(intent)
                ExecutionResult(true, "Opened in Chrome.", packageName = "com.android.chrome")
            } else {
                val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse(validUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(genericIntent)
                ExecutionResult(true, "Opened in browser.")
            }
        } catch (e: Exception) {
            ExecutionResult(false, "Failed to open URL: ${e.localizedMessage}")
        }
    }
}

