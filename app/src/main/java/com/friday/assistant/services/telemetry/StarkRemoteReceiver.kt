package com.friday.assistant.services.telemetry

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.friday.assistant.services.overlay.FridayOverlayService
import com.friday.assistant.services.sound.StarkSoundEngine
import com.friday.assistant.ui.MainActivity

/**
 * Stark Remote Broadcast Engine.
 * Allows Wear OS smartwatches, Bluetooth peripherals, ADB, and automation apps (Tasker/Macrodroid)
 * to interact with F.R.I.D.A.Y. securely.
 */
class StarkRemoteReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_VOICE = "com.friday.assistant.ACTION_TRIGGER_VOICE"
        const val ACTION_TOGGLE_OVERLAY = "com.friday.assistant.ACTION_TOGGLE_OVERLAY"
        const val ACTION_EXECUTE_PROTOCOL = "com.friday.assistant.ACTION_EXECUTE_PROTOCOL"
        const val EXTRA_PROTOCOL_NAME = "extra_protocol_name"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_TRIGGER_VOICE -> {
                StarkSoundEngine.playSound(StarkSoundEngine.SoundType.AFFIRMATIVE_CHIME)
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("AUTO_LISTEN", true)
                }
                context.startActivity(launchIntent)
            }
            ACTION_TOGGLE_OVERLAY -> {
                FridayOverlayService.start(context)
            }
            ACTION_EXECUTE_PROTOCOL -> {
                val protocol = intent.getStringExtra(EXTRA_PROTOCOL_NAME) ?: "Morning Briefing"
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("EXECUTE_PROTOCOL", protocol)
                }
                context.startActivity(launchIntent)
            }
        }
    }
}

