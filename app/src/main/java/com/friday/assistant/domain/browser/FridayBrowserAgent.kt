package com.friday.assistant.domain.browser

import android.content.Context
import com.friday.assistant.domain.apps.ExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class OfflineArticle(
    val url: String,
    val title: String,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis()
)

class FridayBrowserAgent(private val context: Context) {
    private val browserEngine = BrowserAutomationEngine(context)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val _cachedArticles = MutableStateFlow<List<OfflineArticle>>(emptyList())
    val cachedArticles: StateFlow<List<OfflineArticle>> = _cachedArticles.asStateFlow()

    fun openSearch(query: String): ExecutionResult {
        return browserEngine.openSearch(query)
    }

    fun openUrl(url: String): ExecutionResult {
        return browserEngine.openUrl(url)
    }

    /**
     * Fetches, strips, and summarizes online articles or web content.
     */
    suspend fun fetchAndSummarize(url: String): String = withContext(Dispatchers.IO) {
        val validUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        try {
            val request = Request.Builder().url(validUrl).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "Unable to retrieve webpage (HTTP ${response.code})."
                }

                val html = response.body?.string() ?: ""
                val title = extractTitle(html).ifBlank { url }
                val text = cleanHtmlToPlainText(html)

                // Summarize first 600 characters or key paragraphs
                val summary = if (text.length > 500) {
                    text.take(500) + "..."
                } else {
                    text.ifBlank { "Page loaded successfully, but contains dynamic JavaScript content." }
                }

                // Cache for offline reading
                val article = OfflineArticle(url = validUrl, title = title, summary = summary)
                _cachedArticles.value = listOf(article) + _cachedArticles.value.take(19)

                return@withContext summary
            }
        } catch (e: Exception) {
            return@withContext "Could not fetch webpage: ${e.localizedMessage}. Webpage might require local browser rendering."
        }
    }

    private fun extractTitle(html: String): String {
        val titleMatch = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE).find(html)
        return titleMatch?.groupValues?.get(1)?.trim() ?: ""
    }

    private fun cleanHtmlToPlainText(html: String): String {
        return html
            .replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

