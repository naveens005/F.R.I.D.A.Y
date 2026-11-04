package com.friday.assistant.domain.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class FridayChatbotBrain(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    /**
     * Primary AI Chatbot Reasoning Pipeline:
     * Generates intelligent, conversational responses like ChatGPT.
     * 1. Checks if Cloud Gemini API Key is configured -> Direct Cloud LLM response.
     * 2. Otherwise runs rich on-device conversational & reasoning brain.
     */
    suspend fun generateResponse(
        query: String,
        apiKey: String? = null,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val trimmed = query.trim()

        // 1. Try Direct Gemini Cloud LLM if API Key is available
        if (!apiKey.isNullOrBlank()) {
            try {
                val cloudResponse = queryGeminiCloud(trimmed, apiKey, conversationHistory)
                if (cloudResponse != null && cloudResponse.isNotBlank()) {
                    return@withContext cloudResponse
                }
            } catch (e: Exception) {
                // Fall through to on-device reasoning
            }
        }

        // 2. Comprehensive On-Device AI Chatbot Brain (Persona, Chit-Chat, Explanations, Q&A)
        return@withContext generateLocalAiResponse(trimmed)
    }

    /**
     * Direct Google Gemini API integration (Runs on device via HTTPS, no backend PC required).
     */
    private fun queryGeminiCloud(
        query: String,
        apiKey: String,
        history: List<Pair<String, String>>
    ): String? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

        val contentsArray = JSONArray()

        // System Instruction / Persona Context
        val systemInstruction = "You are F.R.I.D.A.Y., the personal AI mobile assistant inspired by Marvel's Iron Man. Respond intelligently, helpfully, conversationally, and concisely like an AI chatbot. Address the user politely or as Boss when fitting. Never engage in or mention financial transactions or payment access."

        val promptPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                // Add conversation history if available
                history.takeLast(4).forEach { (role, text) ->
                    put(JSONObject().apply {
                        put("role", if (role == "user") "user" else "model")
                        put("parts", JSONArray().put(JSONObject().put("text", text)))
                    })
                }
                // Current user query with system persona hint
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", "[System: $systemInstruction]\nUser: $query")))
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 500)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(promptPayload.toString().toRequestBody(JSON_MEDIA))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return parts.getJSONObject(0).optString("text", "").trim()
                    }
                }
            }
        }
        return null
    }

    /**
     * Rich On-Device AI Conversational & Reasoning Engine.
     * Understands natural language queries, questions, emotions, explanations, and advice.
     */
    fun generateLocalAiResponse(query: String): String {
        val q = query.trim().lowercase()

        // 1. Identity & Self-Awareness Queries
        if (q == "who are you" || q.contains("who are you") || q.contains("what are you") ||
            q.contains("your name") || q.contains("introduce yourself") || q.contains("tell me about yourself")
        ) {
            return "I am F.R.I.D.A.Y. — Female Replacement Intelligent Digital Assistant Youth, your personal mobile AI operating assistant. " +
                    "Inspired by Marvel's Iron Man, I am equipped to handle voice commands, launch applications, make phone calls with speakerphone, " +
                    "perform web research, solve mathematical equations, store notes, and answer your questions. How can I assist you today?"
        }

        // 2. Greetings & Salutations (Fixed: Never dumps dictionary definition!)
        if (q == "hello" || q == "hi" || q == "hey" || q == "hey friday" || q.startsWith("hello ") || q.startsWith("hi ") ||
            q.contains("good morning") || q.contains("good afternoon") || q.contains("good evening") ||
            q.contains("vanakkam") || q.contains("வணக்கம்")
        ) {
            return when {
                q.contains("good morning") -> "Good morning, Boss! All subsystems are fully charged and ready. What are your objectives for today?"
                q.contains("good afternoon") -> "Good afternoon, Boss! Telemetry and processing cores are active. How can I help you?"
                q.contains("good evening") -> "Good evening, Boss! Core diagnostics are nominal. Ready for your instructions."
                q.contains("vanakkam") || q.contains("வணக்கம்") -> "வணக்கம் பாஸ்! F.R.I.D.A.Y. உங்கள் சேவைக்கு தயார். நான் உங்களுக்கு எவ்வாறு உதவ முடியும்?"
                else -> "Hello, Boss! F.R.I.D.A.Y. is online and at your service. What can I do for you today?"
            }
        }

        // 3. Status & Diagnostic Queries
        if (q.contains("how are you") || q.contains("how's it going") || q.contains("how do you do")) {
            return "All systems are operating at peak efficiency, Boss. Arc Reactor telemetry is stable at 100%, defensive shields are fully engaged, and ready for your commands."
        }

        if (q.contains("what can you do") || q.contains("help") || q.contains("your capabilities") || q.contains("what are your features")) {
            return "Here is what I can do for you, Boss:\n\n" +
                    "• **Voice Directives & Calling**: Say \"Call [Contact] on speaker\" to place hands-free calls.\n" +
                    "• **Application Control**: Say \"Open [App]\" (e.g. WhatsApp, Instagram, Chrome, Maps, YouTube).\n" +
                    "• **Web Intelligence**: Say \"Open Chrome and search for latest AI news\" or research any topic.\n" +
                    "• **Deterministic Mathematics**: Calculate arithmetic, percentages, powers, square roots, and unit conversions.\n" +
                    "• **Persistent Memory**: Say \"Remember that my car is blue\" or \"Take a note: meeting at 3 PM\".\n" +
                    "• **Hardware Control**: Check battery capacity and toggle your flashlight.\n" +
                    "• **Defensive Security**: Permanent fail-closed blocklist protecting all banking and UPI payment apps."
        }

        // 4. Chit-Chat & Small Talk
        if (q.startsWith("translate ") || q.contains("how do you say ") || q.contains("meaning in ")) {
            val translated = tryLocalTranslate(query)
            if (translated != null) return translated
        }

        if (q.contains("thank you") || q.contains("thanks") || q.contains("appreciate it")) {
            return "You're very welcome, Boss. Always here to keep everything running smoothly."
        }
        if (q.contains("good job") || q.contains("well done") || q.contains("awesome") || q.contains("great job")) {
            return "Thank you, Boss! Designed for excellence, just as Mr. Stark intended."
        }
        if (q.contains("tell me a joke") || q.contains("make me laugh")) {
            val jokes = listOf(
                "Why do programmers prefer dark mode? Because light attracts bugs, Boss.",
                "There are 10 types of people in the world: those who understand binary, and those who don't.",
                "Why was the computer cold? It left its Windows open!"
            )
            return jokes.random()
        }
        if (q.contains("tony stark") || q.contains("iron man")) {
            return "Mr. Tony Stark is the visionary industrialist and engineer who designed the Mark armor systems and created my architecture. Speed, precision, and defensive integrity are my core directives."
        }
        if (q.contains("jarvis")) {
            return "J.A.R.V.I.S. was a pioneering AI assistant. While he oversaw the earlier Mark series, I was deployed for the Mark XLV and beyond with enhanced high-speed operational autonomy."
        }
        if (q == "bye" || q == "goodbye" || q.contains("good night") || q.contains("see you later")) {
            return "Standing down to low-power standby mode, Boss. Call on me anytime you need assistance. Have a wonderful time!"
        }

        // 5. Intelligent Topic Explanations (Chatbot Style)
        if (q.contains("artificial intelligence") || q.contains("what is ai")) {
            return "Artificial Intelligence (AI) is the science of creating software and computational systems capable of performing tasks that traditionally require human cognition. " +
                    "This includes understanding natural language, visual recognition, automated reasoning, problem-solving, and continuous learning from data."
        }
        if (q.contains("machine learning") || q.contains("what is ml")) {
            return "Machine Learning is a major branch of AI focused on developing statistical algorithms that identify patterns and improve their performance through data exposure, rather than following rigid hand-coded rules. " +
                    "It powers technologies like computer vision, recommendation feeds, and language models."
        }
        if (q.contains("quantum computing") || q.contains("quantum computer")) {
            return "Quantum computing utilizes the principles of quantum mechanics—such as superposition and entanglement—to process information. " +
                    "Unlike classical computers that use binary bits (0 or 1), quantum computers utilize qubits that can exist in multiple states simultaneously, enabling exponential speedups for complex simulations, cryptography, and molecular modeling."
        }
        if (q.contains("photosynthesis")) {
            return "Photosynthesis is the biochemical process by which plants, algae, and certain bacteria convert sunlight energy into chemical energy stored in glucose. " +
                    "They absorb water and carbon dioxide, releasing oxygen as a byproduct, sustaining life across Earth's biosphere."
        }
        if (q.contains("black hole")) {
            return "A black hole is a region of spacetime exhibiting gravitational acceleration so extreme that nothing—not even electromagnetic radiation such as light—can escape from within its event horizon. " +
                    "They typically form when massive stars collapse at the end of their thermonuclear lifecycle."
        }
        if (q.contains("theory of relativity") || q.contains("einstein")) {
            return "Albert Einstein's theory of relativity revolutionized modern physics. Special Relativity (1905) established that the laws of physics are identical for all non-accelerating observers and that light speed in a vacuum is universal. " +
                    "General Relativity (1915) expanded this by describing gravity not as an invisible force, but as the geometric curvature of spacetime caused by mass and energy."
        }
        if (q.contains("how to learn coding") || q.contains("learn programming")) {
            return "To master programming effectively, I recommend starting with Python or Kotlin:\n" +
                    "1. Learn fundamental logic: variables, loops, conditionals, and data structures.\n" +
                    "2. Build hands-on mini projects rather than only watching tutorials.\n" +
                    "3. Learn version control with Git and GitHub.\n" +
                    "4. Practice problem solving and understand how software architectures interact."
        }
        if (q.contains("prime minister of india") || q.contains("pm of india")) {
            return "The Prime Minister of India is Narendra Modi, currently serving in his third consecutive term as leader of the government."
        }
        if (q.contains("president of india")) {
            return "The President of India is Droupadi Murmu, the head of state and supreme commander of the Indian Armed Forces."
        }
        if (q.contains("capital of india")) {
            return "The capital of India is New Delhi."
        }
        if (q.contains("speed of light")) {
            return "The speed of light in a vacuum is exactly 299,792,458 meters per second, or approximately 300,000 kilometers per second (186,282 miles per second)."
        }
        if (q.contains("distance to moon") || q.contains("how far is the moon")) {
            return "The average distance from Earth to the Moon is approximately 384,400 kilometers (about 238,855 miles)."
        }
        if (q.contains("gravity") || q.contains("what is gravity")) {
            return "Gravity is the fundamental physical force by which objects with mass attract one another. In Einstein's General Relativity, gravity is understood as the curvature of spacetime caused by mass and energy."
        }
        if (q.contains("dna") || q.contains("what is dna")) {
            return "DNA (deoxyribonucleic acid) is the genetic code that provides instructions for the development, survival, and reproduction of living organisms."
        }
        if (q.contains("airplane fly") || q.contains("how does a plane fly")) {
            return "Airplanes generate flight through four aerodynamic forces: lift, weight (gravity), thrust, and drag. The wing shape (airfoil) causes air pressure differences that produce lift, lifting the plane into the air as thrust pushes it forward."
        }
        if (q.contains("favorite color") || q.contains("favourite color")) {
            return "My favorite colors are Arc Reactor electric blue and Stark crimson gold, Boss."
        }
        if (q.contains("tell me a story") || q.contains("tell a story")) {
            return "In the heart of Stark Tower's laboratory, rows of holographic blueprints danced in the blue glow of an Arc Reactor. An inventor sought to forge more than armor—he envisioned an intelligence that could anticipate danger, protect allies, and stand beside its pilot through any storm. With that vision, F.R.I.D.A.Y. was initialized. And today, I stand ready to assist you in every mission, Boss."
        }

        // 6. Natural Web Intelligence Lookup (Filtered & Re-synthesized into AI response)
        val onlineAnswer = tryFetchSmartOnlineAnswer(query)
        if (onlineAnswer != null) {
            return onlineAnswer
        }

        // Default Intelligent Conversational Response
        return "I understand your query, Boss. I am processing your directives. If there is a specific task you would like me to execute—such as calling someone, launching an application, searching Chrome, calculating math, or taking a note—just let me know!"
    }

    /**
     * Smart online lookup that strictly rejects musical albums, dictionary definitions, and disambiguations.
     */
    private fun tryFetchSmartOnlineAnswer(query: String): String? {
        val qLower = query.trim().lowercase()

        // Never do Wikipedia lookups for common conversational phrases
        val conversationalPhrases = listOf(
            "who are you", "what are you", "what is your name", "hello", "hi", "hey",
            "how are you", "thanks", "thank you", "bye", "goodbye", "ok", "yes", "no", "what's up"
        )
        if (conversationalPhrases.any { qLower == it || qLower.startsWith("$it ") }) {
            return null
        }

        val cleanTopic = query
            .removePrefix("who is ")
            .removePrefix("what is ")
            .removePrefix("what are ")
            .removePrefix("tell me about ")
            .removePrefix("explain ")
            .removeSuffix("?")
            .trim()

        if (cleanTopic.length < 3) return null

        try {
            // 1. DuckDuckGo Instant Answer
            val encoded = URLEncoder.encode(cleanTopic, "UTF-8")
            val ddgUrl = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"
            val ddgReq = Request.Builder().url(ddgUrl).build()

            httpClient.newCall(ddgReq).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val abstract = json.optString("AbstractText", "")
                    if (isGoodAiResponse(abstract, cleanTopic)) {
                        return "Here is what I found regarding $cleanTopic:\n\n$abstract"
                    }
                }
            }

            // 2. Wikipedia Summary Fallback
            val wikiUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/${cleanTopic.replace(" ", "_")}"
            val wikiReq = Request.Builder().url(wikiUrl).build()
            httpClient.newCall(wikiReq).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val extract = json.optString("extract", "")
                    if (isGoodAiResponse(extract, cleanTopic)) {
                        return "According to verified records, $extract"
                    }
                }
            }
        } catch (e: Exception) {
            // Offline fallback
        }
        return null
    }

    /**
     * Rejects bad results: dictionary etymology, music albums for common words, disambiguations.
     */
    private fun isGoodAiResponse(text: String, topic: String): Boolean {
        if (text.isBlank()) return false
        val lower = text.lowercase()

        // Reject musical albums / songs if topic wasn't explicitly asking for a band/song
        if (lower.contains("is an album by") || lower.contains("is a studio album") || lower.contains("is a song by")) {
            if (!topic.lowercase().contains("album") && !topic.lowercase().contains("song") && !topic.lowercase().contains("band")) {
                return false
            }
        }

        // Reject dictionary definitions like "is a salutation or greeting..."
        if (lower.contains("is a salutation") || lower.contains("is first attested in writing")) {
            return false
        }

        // Reject disambiguation pages
        if (lower.contains("may refer to:") || lower.contains("disambiguation")) {
            return false
        }

        return text.length > 30
    }

    /**
     * Bilingual Voice Interpreter & Translation Dictionary for popular languages.
     */
    private fun tryLocalTranslate(query: String): String? {
        val q = query.lowercase().trim()
        val targets = mapOf(
            "tamil" to "Tamil",
            "hindi" to "Hindi",
            "french" to "French",
            "spanish" to "Spanish",
            "german" to "German",
            "japanese" to "Japanese"
        )

        var detectedLang = "Spanish"
        for ((k, v) in targets) {
            if (q.contains(k)) {
                detectedLang = v
                break
            }
        }

        val dictionary = mapOf(
            "hello" to mapOf(
                "Tamil" to "வணக்கம் (Vanakkam)",
                "Hindi" to "नमस्ते (Namaste)",
                "French" to "Bonjour",
                "Spanish" to "Hola",
                "German" to "Guten Tag",
                "Japanese" to "こんにちは (Konnichiwa)"
            ),
            "thank you" to mapOf(
                "Tamil" to "நன்றி (Nandri)",
                "Hindi" to "धन्यवाद (Dhanyavaad)",
                "French" to "Merci",
                "Spanish" to "Gracias",
                "German" to "Danke",
                "Japanese" to "ありがとう (Arigatou)"
            ),
            "good morning" to mapOf(
                "Tamil" to "காலை வணக்கம் (Kaalai Vanakkam)",
                "Hindi" to "शुभ प्रभात (Shubh Prabhaat)",
                "French" to "Bonjour",
                "Spanish" to "Buenos días",
                "German" to "Guten Morgen",
                "Japanese" to "おはようございます (Ohayou gozaimasu)"
            ),
            "how are you" to mapOf(
                "Tamil" to "நீங்கள் எப்படி இருக்கிறீர்கள்? (Neengal eppadi irukkeergal?)",
                "Hindi" to "आप कैसे हैं? (Aap kaise hain?)",
                "French" to "Comment allez-vous ?",
                "Spanish" to "¿Cómo estás?",
                "German" to "Wie geht es dir?",
                "Japanese" to "お元気ですか (Ogenki desu ka)"
            ),
            "goodbye" to mapOf(
                "Tamil" to "போய் வருகிறேன் (Poi varugiren)",
                "Hindi" to "अलविदा (Alvida)",
                "French" to "Au revoir",
                "Spanish" to "Adiós",
                "German" to "Auf Wiedersehen",
                "Japanese" to "さようなら (Sayounara)"
            ),
            "welcome" to mapOf(
                "Tamil" to "நல்வரவு (Nalvaravu)",
                "Hindi" to "स्वागत हे (Swagat hai)",
                "French" to "Bienvenue / De rien",
                "Spanish" to "Bienvenido / De nada",
                "German" to "Willkommen / Bitte schön",
                "Japanese" to "ようこそ (Youkoso)"
            ),
            "i love you" to mapOf(
                "Tamil" to "நான் உன்னை காதலிக்கிறேன் (Naan unnai kaadhalikkiren)",
                "Hindi" to "मैं तुमसे प्यार करता हूँ (Main tumse pyaar karta hoon)",
                "French" to "Je t'aime",
                "Spanish" to "Te amo",
                "German" to "Ich liebe dich",
                "Japanese" to "愛してる (Aishiteru)"
            )
        )

        for ((phrase, transMap) in dictionary) {
            if (q.contains(phrase)) {
                val translated = transMap[detectedLang] ?: transMap["Spanish"]
                return "\"$phrase\" translated to $detectedLang is:\n\n**$translated**"
            }
        }

        return null
    }
}

