package com.friday.assistant.services.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Stark Industries Audio Synthesizer Engine.
 * Generates futuristic sci-fi sound effects mathematically via low-latency AudioTrack.
 * Zero asset dependencies, 100% offline, zero APK bloat.
 */
object StarkSoundEngine {

    private val scope = CoroutineScope(Dispatchers.Default)
    private const val SAMPLE_RATE = 44100

    enum class SoundType {
        ARC_REACTOR_POWER_UP,
        REPULSOR_BLAST,
        AFFIRMATIVE_CHIME,
        SECURITY_BLOCKED,
        TACTICAL_SONAR_PING,
        HUD_TARGET_LOCK
    }

    fun playSound(type: SoundType) {
        scope.launch {
            try {
                val samples = when (type) {
                    SoundType.ARC_REACTOR_POWER_UP -> generateArcReactorPowerUp()
                    SoundType.REPULSOR_BLAST -> generateRepulsorBlast()
                    SoundType.AFFIRMATIVE_CHIME -> generateAffirmativeChime()
                    SoundType.SECURITY_BLOCKED -> generateSecurityBlocked()
                    SoundType.TACTICAL_SONAR_PING -> generateSonarPing()
                    SoundType.HUD_TARGET_LOCK -> generateTargetLock()
                }
                playPcmSamples(samples)
            } catch (e: Exception) {
                // Silently fallback if audio hardware is occupied
            }
        }
    }

    private fun playPcmSamples(samples: ShortArray) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize, samples.size * 2)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(samples, 0, samples.size)
        audioTrack.play()
        
        // Wait for playback then release
        Thread.sleep((samples.size * 1000L) / SAMPLE_RATE + 50)
        audioTrack.release()
    }

    /**
     * Arc Reactor Power-Up: A smooth exponential pitch sweep from 150 Hz to 1200 Hz with harmonic resonance.
     */
    private fun generateArcReactorPowerUp(): ShortArray {
        val durationMs = 380
        val numSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(numSamples)
        var phase = 0.0

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val freq = 150.0 + (1050.0 * progress * progress)
            phase += 2.0 * PI * freq / SAMPLE_RATE
            
            // Envelope: fade in, sustain, gentle fade out
            val envelope = when {
                progress < 0.1 -> progress / 0.1
                progress > 0.85 -> (1.0 - progress) / 0.15
                else -> 1.0
            }

            // Fundamental + sub-harmonic + shimmer
            val wave = 0.6 * sin(phase) + 0.25 * sin(phase * 0.5) + 0.15 * sin(phase * 2.0)
            samples[i] = (wave * envelope * Short.MAX_VALUE * 0.7).toInt().toShort()
        }
        return samples
    }

    /**
     * Repulsor Blast: Sudden high-energy burst with rapid exponential decay and sub-bass impact.
     */
    private fun generateRepulsorBlast(): ShortArray {
        val durationMs = 300
        val numSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(numSamples)
        var phase = 0.0

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val freq = 800.0 * Math.exp(-progress * 6.0) + 60.0
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val decay = Math.exp(-progress * 5.0)
            val wave = sin(phase) + 0.3 * sin(phase * 1.5)
            samples[i] = (wave * decay * Short.MAX_VALUE * 0.8).toInt().toShort()
        }
        return samples
    }

    /**
     * Affirmative Stark Chime: Dual pleasant futuristic bells (C6 -> G6: 1046 Hz -> 1568 Hz).
     */
    private fun generateAffirmativeChime(): ShortArray {
        val durationMs = 250
        val numSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(numSamples)
        val split = numSamples / 2

        var phase1 = 0.0
        for (i in 0 until split) {
            val progress = i.toDouble() / split
            phase1 += 2.0 * PI * 1046.5 / SAMPLE_RATE
            val envelope = Math.exp(-progress * 4.0)
            samples[i] = (sin(phase1) * envelope * Short.MAX_VALUE * 0.6).toInt().toShort()
        }

        var phase2 = 0.0
        for (i in split until numSamples) {
            val progress = (i - split).toDouble() / split
            phase2 += 2.0 * PI * 1567.98 / SAMPLE_RATE
            val envelope = Math.exp(-progress * 3.5)
            samples[i] = (sin(phase2) * envelope * Short.MAX_VALUE * 0.65).toInt().toShort()
        }
        return samples
    }

    /**
     * Security Blocked: Low frequency dual dissonance warning (220 Hz + 233 Hz).
     */
    private fun generateSecurityBlocked(): ShortArray {
        val durationMs = 350
        val numSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(numSamples)
        var phaseA = 0.0
        var phaseB = 0.0

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            phaseA += 2.0 * PI * 220.0 / SAMPLE_RATE
            phaseB += 2.0 * PI * 233.08 / SAMPLE_RATE
            val envelope = Math.sin(progress * PI)
            val wave = 0.5 * sin(phaseA) + 0.5 * sin(phaseB)
            samples[i] = (wave * envelope * Short.MAX_VALUE * 0.75).toInt().toShort()
        }
        return samples
    }

    /**
     * Tactical Sonar Ping: High clean frequency ping with gentle harmonic echo.
     */
    private fun generateSonarPing(): ShortArray {
        val durationMs = 400
        val numSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(numSamples)
        var phase = 0.0

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            phase += 2.0 * PI * 2400.0 / SAMPLE_RATE
            val envelope = Math.exp(-progress * 5.0)
            samples[i] = (sin(phase) * envelope * Short.MAX_VALUE * 0.5).toInt().toShort()
        }
        return samples
    }

    /**
     * HUD Target Lock: Rapid triple click pulse.
     */
    private fun generateTargetLock(): ShortArray {
        val durationMs = 180
        val numSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(numSamples)
        var phase = 0.0

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val freq = if ((i / 800) % 2 == 0) 1800.0 else 900.0
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val envelope = Math.exp(-progress * 3.0)
            samples[i] = (sin(phase) * envelope * Short.MAX_VALUE * 0.6).toInt().toShort()
        }
        return samples
    }
}

