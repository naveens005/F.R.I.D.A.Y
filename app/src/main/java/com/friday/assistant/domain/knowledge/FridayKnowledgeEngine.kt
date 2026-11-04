package com.friday.assistant.domain.knowledge

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.*

data class MathEvaluationResult(
    val expression: String,
    val result: Double,
    val formattedResult: String,
    val steps: List<String>
)

class FridayKnowledgeEngine(private val context: Context) {

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
        "president of india" to "The President of India is the ceremonial head of state of the Republic of India and commander-in-chief of the Indian Armed Forces.",
        "tony stark" to "Anthony Edward 'Tony' Stark, known as Iron Man, is an industrialist, genius inventor, and founding member of the Avengers who built powered exoskeletons and the initial Jarvis and Friday artificial intelligences.",
        "friday" to "F.R.I.D.A.Y. (Female Replacement Intelligent Digital Assistant Youth) is your advanced mobile AI operating system, engineered for phone automation, high-speed computation, and defensive security.",
        "avengers" to "The Avengers are Earth's mightiest heroes, assembled to combat threats beyond the capability of conventional defense forces.",
        "arc reactor" to "The Arc Reactor is a clean fusion power core developed by Stark Industries, providing high-density electrical energy to sustain Mark armor systems and defense telemetry.",
        "capital of france" to "The capital of France is Paris.",
        "capital of india" to "The capital of India is New Delhi.",
        "capital of the united states" to "The capital of the United States is Washington, D.C.",
        "capital of japan" to "The capital of Japan is Tokyo.",
        "capital of germany" to "The capital of Germany is Berlin.",
        "capital of united kingdom" to "The capital of the United Kingdom is London.",
        "capital of australia" to "The capital of Australia is Canberra.",
        "capital of russia" to "The capital of Russia is Moscow.",
        "capital of china" to "The capital of China is Beijing."
    )

    /**
     * Deterministic Mathematical Evaluator:
     * Evaluates expressions accurately without LLM hallucinations.
     */
    fun evaluateMath(query: String): MathEvaluationResult? {
        val clean = query.trim().lowercase()
            .removePrefix("calculate")
            .removePrefix("solve")
            .removePrefix("what is")
            .removePrefix("what's")
            .removePrefix("compute")
            .removeSuffix("?")
            .trim()

        // 1. Percentage calculations: "what is 15 percent of 400"
        val pctRegex = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:percent|%)\\s*(?:of|\\*)\\s*(\\d+(?:\\.\\d+)?)")
        val pctMatch = pctRegex.find(clean)
        if (pctMatch != null) {
            val p = pctMatch.groupValues[1].toDoubleOrNull() ?: return null
            val total = pctMatch.groupValues[2].toDoubleOrNull() ?: return null
            val res = (p / 100.0) * total
            return MathEvaluationResult(
                expression = "$p% of $total",
                result = res,
                formattedResult = formatNumber(res),
                steps = listOf(
                    "Identified percentage query: $p% of $total",
                    "Convert percentage to decimal: $p / 100 = ${p / 100.0}",
                    "Multiply: ${p / 100.0} × $total = ${formatNumber(res)}"
                )
            )
        }

        // 2. Square Root: "square root of 144" or "sqrt 144"
        val sqrtRegex = Regex("(?:square root of|sqrt)\\s*(\\d+(?:\\.\\d+)?)")
        val sqrtMatch = sqrtRegex.find(clean)
        if (sqrtMatch != null) {
            val num = sqrtMatch.groupValues[1].toDoubleOrNull() ?: return null
            val res = sqrt(num)
            return MathEvaluationResult(
                expression = "sqrt($num)",
                result = res,
                formattedResult = formatNumber(res),
                steps = listOf(
                    "Identify square root calculation for $num",
                    "Compute √$num = ${formatNumber(res)}"
                )
            )
        }

        // 3. Exponentiation / Power: "2 power 8" or "2 ^ 8" or "2 raised to 8"
        val powRegex = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:power|raised to|\\^)\\s*(\\d+(?:\\.\\d+)?)")
        val powMatch = powRegex.find(clean)
        if (powMatch != null) {
            val base = powMatch.groupValues[1].toDoubleOrNull() ?: return null
            val exp = powMatch.groupValues[2].toDoubleOrNull() ?: return null
            val res = base.pow(exp)
            return MathEvaluationResult(
                expression = "$base ^ $exp",
                result = res,
                formattedResult = formatNumber(res),
                steps = listOf(
                    "Base = $base, Exponent = $exp",
                    "Compute $base ^ $exp = ${formatNumber(res)}"
                )
            )
        }

        // 4. Standard Arithmetic: handles +, -, *, /, x, ÷
        val normalized = clean
            .replace(" multiplied by ", " * ")
            .replace(" times ", " * ")
            .replace(" x ", " * ")
            .replace(" divided by ", " / ")
            .replace(" plus ", " + ")
            .replace(" minus ", " - ")
            .replace("over", "/")

        // Check if string contains math characters
        if (normalized.any { it in "+-*/" } && normalized.any { it.isDigit() }) {
            try {
                val parser = SimpleMathParser(normalized)
                val res = parser.parse()
                return MathEvaluationResult(
                    expression = normalized,
                    result = res,
                    formattedResult = formatNumber(res),
                    steps = listOf(
                        "Input equation: $normalized",
                        "Evaluate operations via standard order (PEMDAS/BODMAS)",
                        "Result: ${formatNumber(res)}"
                    )
                )
            } catch (e: Exception) {
                // Not a valid math equation
            }
        }

        return null
    }

    /**
     * Answers general knowledge questions with offline instant bank + online Wikipedia lookup.
     */
    suspend fun answerGeneralQuestion(query: String): String? {
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

        // 2. Fast DuckDuckGo API (Online)
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
                        if (abstractText.isNotBlank()) return@withContext abstractText
                    }
                }

                // Wikipedia REST Summary Fallback
                val cleanTopic = qLower.replace(" ", "_")
                val wikiUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/$cleanTopic"
                val wikiReq = Request.Builder().url(wikiUrl).build()

                httpClient.newCall(wikiReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: ""
                        val json = JSONObject(body)
                        val extract = json.optString("extract", "")
                        if (extract.isNotBlank()) return@withContext extract
                    }
                }
            } catch (e: Exception) {
                // Offline fallback
            }
            null
        }
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format(java.util.Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')
        }
    }

    /**
     * Recursive descent parser for evaluating standard expressions: +, -, *, /, parentheses.
     */
    private class SimpleMathParser(val str: String) {
        var pos = -1
        var ch = 0

        fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
            return x
        }

        fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                if (eat('+'.code)) x += parseTerm()
                else if (eat('-'.code)) x -= parseTerm()
                else return x
            }
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                if (eat('*'.code)) x *= parseFactor()
                else if (eat('/'.code)) x /= parseFactor()
                else return x
            }
        }

        fun parseFactor(): Double {
            if (eat('+'.code)) return +parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = this.pos
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                x = str.substring(startPos, this.pos).toDouble()
            } else {
                throw RuntimeException("Unexpected: " + ch.toChar())
            }

            return x
        }
    }
}

