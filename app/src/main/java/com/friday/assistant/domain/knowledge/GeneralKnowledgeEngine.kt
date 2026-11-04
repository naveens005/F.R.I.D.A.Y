package com.friday.assistant.domain.knowledge

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class GeneralKnowledgeEngine(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    // Curated Instant Offline Knowledge Base
    private val OFFLINE_KNOWLEDGE = mapOf(
        "artificial intelligence" to "Artificial intelligence is a discipline of computer science dedicated to developing systems capable of performing tasks that typically require human cognition, such as visual perception, natural language understanding, and automated reasoning.",
        "machine learning" to "Machine learning is a subset of artificial intelligence focused on training statistical algorithms to identify patterns and make predictions from data without explicit procedural programming.",
        "quantum computing" to "Quantum computing harnesses the phenomena of quantum mechanics, specifically superposition and entanglement, to execute complex calculations exponentially faster than classical computers for specialized problems.",
        "photosynthesis" to "Photosynthesis is the biological process used by plants and other organisms to convert light energy into chemical energy stored in glucose molecules, consuming carbon dioxide and releasing oxygen.",
        "speed of light" to "The speed of light in a vacuum is approximately 299,792,458 metres per second, or about 300,000 kilometres per second.",
        "gravity" to "Gravity is a fundamental natural interaction that causes all objects with mass or energy to attract one another. In general relativity, it is described as the curvature of spacetime caused by mass.",
        "theory of relativity" to "Albert Einstein's theory of relativity consists of special relativity, which dictates that the laws of physics are invariant and light speed is constant, and general relativity, which explains gravitation as the curvature of spacetime.",
        "dna" to "Deoxyribonucleic acid (DNA) is a polymer composed of two polynucleotide chains that coil around each other to form a double helix, carrying genetic instructions for the development and functioning of all living organisms.",
        "prime minister of india" to "The Prime Minister of India is the head of government and chief executive of the Republic of India.",
        "president of the united states" to "The President of the United States is the head of state and head of government of the United States of America.",
        "tony stark" to "Anthony Edward 'Tony' Stark, known as Iron Man, is an industrialist, genius inventor, and founding member of the Avengers who built powered exoskeletons and the initial Jarvis and Friday artificial intelligences.",
        "friday" to "F.R.I.D.A.Y. (Female Replacement Intelligent Digital Assistant Youth) is your advanced mobile AI operating system, engineered for phone automation, high-speed computation, and defensive security.",
        "avengers" to "The Avengers are Earth's mightiest heroes, assembled to combat threats beyond the capability of conventional defense forces.",
        "arc reactor" to "The Arc Reactor is a clean fusion power core developed by Stark Industries, providing high-density electrical energy to sustain Mark armor systems and defense telemetry.",
        "capital of france" to "The capital of France is Paris.",
        "capital of india" to "The capital of India is New Delhi.",
        "capital of the united states" to "The capital of the United States is Washington, D.C.",
        "capital of japan" to "The capital of Japan is Tokyo.",
        "capital of germany" to "The capital of Germany is Berlin.",
        "capital of united kingdom" to "The capital of the United Kingdom is London."
    )

    /**
     * Answers general knowledge questions with instant offline lookup and fast live Wikipedia/DuckDuckGo fallback.
     */
    suspend fun answerQuestion(query: String): String? {
        val qLower = query.lowercase().trim()
            .removePrefix("who is ")
            .removePrefix("what is ")
            .removePrefix("what are ")
            .removePrefix("explain ")
            .removePrefix("tell me about ")
            .removePrefix("define ")
            .removeSuffix("?")
            .trim()

        // 1. Instant Offline Knowledge Lookup
        for ((key, value) in OFFLINE_KNOWLEDGE) {
            if (qLower == key || qLower.contains(key)) {
                return value
            }
        }

        // 2. Fast Live Knowledge Lookup via DuckDuckGo Instant Answer API (No API Key Required)
        return withContext(Dispatchers.IO) {
            try {
                val encodedQ = URLEncoder.encode(query, "UTF-8")
                val ddgUrl = "https://api.duckduckgo.com/?q=$encodedQ&format=json&no_html=1&skip_disambig=1"
                val request = Request.Builder().url(ddgUrl).build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val abstractText = json.optString("AbstractText", "")
                        if (abstractText.isNotBlank()) {
                            return@withContext abstractText
                        }

                        val heading = json.optString("Heading", "")
                        val related = json.optJSONArray("RelatedTopics")
                        if (related != null && related.length() > 0) {
                            val first = related.getJSONObject(0)
                            val text = first.optString("Text", "")
                            if (text.isNotBlank()) {
                                return@withContext text
                            }
                        }
                    }
                }

                // 3. Fallback to Wikipedia REST Summary API
                val cleanTopic = qLower.replace(" ", "_")
                val wikiUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/$cleanTopic"
                val wikiReq = Request.Builder().url(wikiUrl).build()

                httpClient.newCall(wikiReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: ""
                        val json = JSONObject(body)
                        val extract = json.optString("extract", "")
                        if (extract.isNotBlank()) {
                            return@withContext extract
                        }
                    }
                }
                null
            } catch (e: Exception) {
                null
            }
        }
    }
}

