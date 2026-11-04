package com.friday.assistant.services.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okio.ByteString
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * High-Definition Siri-Grade Neural Voice Engine.
 * Streams ultra-realistic, expressive female neural speech (en-US-JennyNeural / en-US-AriaNeural)
 * matching Apple Siri's acoustic warmth, breathiness, and natural human intonation.
 */
class SiriNeuralVoiceEngine(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private var mediaPlayer: MediaPlayer? = null

    companion object {
        private const val WS_URL =
            "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1?TrustedClientToken=6A5AA1D4EA6547818E00C335752625"
    }

    /**
     * Synthesizes and plays Siri-style neural audio.
     * Returns true if successfully played online neural audio, false if network failed.
     */
    suspend fun speakSiriNeural(text: String, onDone: (() -> Unit)? = null): Boolean = withContext(Dispatchers.IO) {
        val cleanText = cleanForSpeech(text)
        if (cleanText.isBlank()) {
            onDone?.invoke()
            return@withContext true
        }

        val outputFile = File(context.cacheDir, "siri_voice_${System.currentTimeMillis()}.mp3")
        val success = fetchNeuralAudio(cleanText, outputFile)

        if (!success || !outputFile.exists() || outputFile.length() < 100) {
            outputFile.delete()
            return@withContext false
        }

        // Play via MediaPlayer
        withContext(Dispatchers.Main) {
            try {
                stop()
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    setDataSource(outputFile.absolutePath)
                    prepare()
                    setOnCompletionListener {
                        stop()
                        outputFile.delete()
                        onDone?.invoke()
                    }
                    setOnErrorListener { _, _, _ ->
                        stop()
                        outputFile.delete()
                        onDone?.invoke()
                        true
                    }
                    start()
                }
            } catch (e: Exception) {
                outputFile.delete()
                onDone?.invoke()
            }
        }
        return@withContext true
    }

    private fun fetchNeuralAudio(text: String, outputFile: File): Boolean {
        val latch = CountDownLatch(1)
        var receivedAudio = false
        val fos = FileOutputStream(outputFile)

        val request = Request.Builder()
            .url(WS_URL)
            .addHeader("Pragma", "no-cache")
            .addHeader("Cache-Control", "no-cache")
            .addHeader(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0"
            )
            .addHeader("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
            .build()

        val ws = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // 1. Send Speech Config
                val config = "Content-Type:application/json; charset=utf-8\r\nPath:speech.config\r\n\r\n" +
                        "{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":{\"sentenceBoundaryEnabled\":\"false\",\"wordBoundaryEnabled\":\"false\"},\"outputFormat\":\"audio-24khz-48kbitrate-mono-mp3\"}}}}"
                webSocket.send(config)

                // 2. Send SSML with en-US-JennyNeural (Siri Voice Profile)
                val reqId = UUID.randomUUID().toString().replace("-", "")
                val escapedText = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
                val ssml = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'>" +
                        "<voice name='en-US-JennyNeural'>" +
                        "<prosody pitch='+0Hz' rate='+0%'>$escapedText</prosody>" +
                        "</voice></speak>"
                val ssmlMsg = "X-RequestId:$reqId\r\nContent-Type:application/ssml+xml\r\nPath:ssml\r\n\r\n$ssml"
                webSocket.send(ssmlMsg)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val byteArray = bytes.toByteArray()
                val headerMarker = "Path:audio\r\n".toByteArray(Charsets.US_ASCII)
                val index = indexOf(byteArray, headerMarker)
                if (index != -1) {
                    val audioStart = index + headerMarker.size
                    if (audioStart < byteArray.size) {
                        fos.write(byteArray, audioStart, byteArray.size - audioStart)
                        receivedAudio = true
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.contains("Path:turn.end")) {
                    webSocket.close(1000, "Done")
                    latch.countDown()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                latch.countDown()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                latch.countDown()
            }
        })

        val completed = try {
            latch.await(3500, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            false
        }

        try {
            fos.flush()
            fos.close()
        } catch (e: Exception) {}

        if (!completed) {
            ws.cancel()
        }

        return receivedAudio && outputFile.length() > 500
    }

    fun stop() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {}
    }

    private fun indexOf(source: ByteArray, target: ByteArray): Int {
        if (target.isEmpty()) return 0
        outer@ for (i in 0..source.size - target.size) {
            for (j in target.indices) {
                if (source[i + j] != target[j]) {
                    continue@outer
                }
            }
            return i
        }
        return -1
    }

    private fun cleanForSpeech(raw: String): String {
        return raw.replace(Regex("[*#_`~>\\[\\]]"), "")
            .replace(Regex("\\(https?://[^)]+\\)"), "")
            .replace(Regex("\\|"), " ")
            .replace(Regex("[-]{3,}"), "")
            .replace(Regex("\\n{2,}"), ". ")
            .trim()
    }
}

