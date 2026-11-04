package com.friday.assistant.services.telemetry

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import com.friday.assistant.services.sound.StarkSoundEngine
import com.friday.assistant.services.voice.TextToSpeechManager

/**
 * Stark Telemetry & Ambient Power Monitor.
 * Monitors battery coupling, fast-charge status, power reserves, and Bluetooth car connectivity.
 */
class StarkTelemetryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                StarkSoundEngine.playSound(StarkSoundEngine.SoundType.ARC_REACTOR_POWER_UP)
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
                val speech = if (batteryPct >= 0) {
                    "Arc Reactor power coupling established. Battery at $batteryPct percent, charging initiated, Boss."
                } else {
                    "Power coupling established. High-density charging online, Boss."
                }
                speakBriefly(context, speech)
            }

            Intent.ACTION_POWER_DISCONNECTED -> {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
                val speech = if (batteryPct in 1..20) {
                    "Power coupling detached. Warning Boss: Battery is at $batteryPct percent. Core energy reserves are low."
                } else {
                    "Power coupling detached. Mark VI systems operating on internal battery reserves, Boss."
                }
                speakBriefly(context, speech)
            }

            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                val name = try { device?.name } catch (e: SecurityException) { null } ?: "Audio Device"
                StarkSoundEngine.playSound(StarkSoundEngine.SoundType.AFFIRMATIVE_CHIME)
                val lowerName = name.lowercase()
                val isCar = lowerName.contains("car") || lowerName.contains("auto") || lowerName.contains("sync") ||
                        lowerName.contains("bmw") || lowerName.contains("audi") || lowerName.contains("mercedes") ||
                        lowerName.contains("honda") || lowerName.contains("toyota") || lowerName.contains("hyundai")

                val speech = if (isCar) {
                    "Vehicle Bluetooth link synchronized with $name. Driving protocols and hands-free speakerphone active, Boss."
                } else {
                    "Bluetooth peripheral connected: $name."
                }
                speakBriefly(context, speech)
            }
        }
    }

    private fun speakBriefly(context: Context, text: String) {
        try {
            val tts = TextToSpeechManager(context.applicationContext)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                tts.speak(text)
            }, 750)
        } catch (e: Exception) {
            // Handled gracefully
        }
    }
}
