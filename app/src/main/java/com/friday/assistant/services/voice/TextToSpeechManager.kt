package com.friday.assistant.services.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private val siriEngine = SiriNeuralVoiceEngine(context)
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var currentPitch = 1.0f
    private var currentRate = 1.0f
    private var currentLocale: Locale = Locale.US
    private var onCompleteCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            applyLanguageSettings()
            tts?.setPitch(currentPitch)
            tts?.setSpeechRate(currentRate)

            val audioAttributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ASSISTANT)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(audioAttributes)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    onCompleteCallback?.invoke()
                    onCompleteCallback = null
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    onCompleteCallback?.invoke()
                    onCompleteCallback = null
                }
            })
        }
    }

    /**
     * Inspects available TTS voices to strictly select a natural, smooth, non-robotic Siri-style female voice.
     * Prioritizes Google Pixel's WaveNet/Studio US Female Neural voices:
     * - en-us-x-sfg (Premier Assistant Siri-style)
     * - en-us-x-tpf (Studio Neural Female)
     * - en-gb-x-rjs (Authentic Stark UK Female)
     * - Strictly rejects robotic concatenative and male models
     */
    private fun applyLanguageSettings() {
        try {
            tts?.language = currentLocale
            val voices = tts?.voices ?: return
            val langVoices = voices.filter { it.locale.language == currentLocale.language }
            if (langVoices.isEmpty()) return

            val siriPriority = listOf(
                "en-us-x-sfg", // Google Premier US Female (Siri equivalent)
                "en-us-x-tpf", // Google Studio Neural Female
                "en-us-x-iol", // Google Journey Neural Female
                "en-gb-x-rjs", // UK F.R.I.D.A.Y. Female
                "en-gb-x-gba", // UK British Assistant Female
                "wavenet",
                "neural",
                "female"
            )

            val selectedVoice = langVoices.firstOrNull { voice ->
                val name = voice.name.lowercase()
                val isExplicitlyMale = (name.contains("male") && !name.contains("female")) || name.contains("#male") || name.contains("-male-")
                if (isExplicitlyMale || name.contains("cxx")) return@firstOrNull false

                siriPriority.any { name.contains(it) }
            } ?: langVoices.firstOrNull { voice ->
                val name = voice.name.lowercase()
                !name.contains("male") && !name.contains("cxx")
            } ?: langVoices.firstOrNull()

            if (selectedVoice != null) {
                tts?.voice = selectedVoice
            }
        } catch (e: Exception) {
            tts?.language = Locale.US
        }
    }

    fun setLanguage(langCode: String) {
        currentLocale = when (langCode.lowercase()) {
            "en-us", "us-english", "siri" -> Locale.US
            "en-gb", "uk-english", "friday" -> Locale.UK
            "ta", "ta-in", "tamil" -> Locale("ta", "IN")
            "en-in", "indian-english" -> Locale("en", "IN")
            else -> Locale.US
        }
        if (isInitialized) {
            applyLanguageSettings()
        }
    }

    /**
     * Unified Siri Neural Speech Synthesizer:
     * 1. Attempts crystal-clear, breathy, studio-grade Siri voice streaming (en-US-JennyNeural).
     * 2. If offline, instantly falls back to Google's on-device WaveNet neural voice at 1.0x human pitch.
     */
    fun speak(text: String, onDone: (() -> Unit)? = null) {
        val cleanSpeech = cleanMarkdownForSpeech(text)
        if (cleanSpeech.isBlank()) {
            onDone?.invoke()
            return
        }

        this.onCompleteCallback = onDone
        _isSpeaking.value = true

        CoroutineScope(Dispatchers.IO).launch {
            // 1. Try High-Definition Siri-grade Neural Stream
            val playedSiri = siriEngine.speakSiriNeural(cleanSpeech) {
                _isSpeaking.value = false
                onCompleteCallback?.invoke()
                onCompleteCallback = null
            }

            if (!playedSiri) {
                // 2. On-device local WaveNet neural fallback
                withContext(Dispatchers.Main) {
                    speakLocalTts(cleanSpeech)
                }
            }
        }
    }

    private fun speakLocalTts(cleanSpeech: String) {
        if (!isInitialized) {
            _isSpeaking.value = false
            onCompleteCallback?.invoke()
            return
        }

        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        tts?.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        siriEngine.stop()
        if (isInitialized) {
            tts?.stop()
            _isSpeaking.value = false
            onCompleteCallback = null
        }
    }

    fun setPitch(pitch: Float) {
        currentPitch = pitch
        if (isInitialized) {
            tts?.setPitch(pitch)
        }
    }

    fun setSpeechRate(rate: Float) {
        currentRate = rate
        if (isInitialized) {
            tts?.setSpeechRate(rate)
        }
    }

    fun shutdown() {
        siriEngine.stop()
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    private fun cleanMarkdownForSpeech(raw: String): String {
        return raw.replace(Regex("[*#_`~>\\[\\]]"), "")
            .replace(Regex("\\(https?://[^)]+\\)"), "")
            .replace(Regex("\\|"), " ")
            .replace(Regex("[-]{3,}"), "")
            .replace(Regex("\\n{2,}"), ". ")
            .trim()
    }
}
