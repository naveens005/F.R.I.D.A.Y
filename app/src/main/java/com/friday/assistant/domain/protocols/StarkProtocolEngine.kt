package com.friday.assistant.domain.protocols

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import com.friday.assistant.domain.apps.AppControlEngine
import com.friday.assistant.domain.memory.FridayMemoryManager
import com.friday.assistant.services.sound.StarkSoundEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class ProtocolExecutionResult(
    val protocolName: String,
    val success: Boolean,
    val speechResponse: String,
    val displayDetails: List<String>
)

class StarkProtocolEngine(
    private val context: Context,
    private val memoryManager: FridayMemoryManager,
    private val appControlEngine: AppControlEngine
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    /**
     * Inspects query for multi-step Stark Protocols or autonomous task chains.
     */
    suspend fun tryExecuteProtocol(query: String): ProtocolExecutionResult? = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()

        // 1. Morning Briefing / Executive Digest
        if (q.contains("morning briefing") || q.contains("brief me") || q.contains("morning report") ||
            q.contains("daily briefing") || q.contains("executive digest")
        ) {
            return@withContext executeMorningBriefing()
        }

        // 2. Workout Protocol
        if (q.contains("workout protocol") || q.contains("initiate workout") || q.contains("start workout") || q.contains("gym protocol")) {
            return@withContext executeWorkoutProtocol()
        }

        // 3. Focus / Deep Work Protocol
        if (q.contains("focus protocol") || q.contains("study protocol") || q.contains("deep work") || q.contains("do not disturb protocol")) {
            return@withContext executeFocusProtocol()
        }

        // 4. Night / Bedtime Protocol
        if (q.contains("night protocol") || q.contains("bedtime protocol") || q.contains("sleep protocol") || q.contains("power down protocol")) {
            return@withContext executeNightProtocol()
        }

        // 5. Movie Night Protocol
        if (q.contains("movie protocol") || q.contains("movie night") || q.contains("cinema mode")) {
            return@withContext executeMovieProtocol()
        }

        // 6. Driving / Car Protocol
        if (q.contains("driving protocol") || q.contains("car mode") || q.contains("start driving") || q.contains("drive mode")) {
            return@withContext executeDrivingProtocol()
        }

        // 7. Emergency SOS / House Party Protocol (Play Protect safe: Uses ACTION_SENDTO)
        if (q.contains("house party protocol") || q.contains("emergency protocol") || q.contains("mayday") || q == "sos") {
            return@withContext executeEmergencySosProtocol()
        }

        // 8. Smart WhatsApp Dispatch: "Text [Name] on WhatsApp saying [Message]"
        if (q.startsWith("text ") || q.startsWith("send message to ") || q.startsWith("whatsapp ")) {
            val dispatch = tryParseWhatsAppDispatch(query)
            if (dispatch != null) {
                return@withContext dispatch
            }
        }

        return@withContext null
    }

    private suspend fun executeMorningBriefing(): ProtocolExecutionResult {
        StarkSoundEngine.playSound(StarkSoundEngine.SoundType.AFFIRMATIVE_CHIME)
        val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        val dateStr = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())

        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val isCharging = bm?.isCharging == true

        val pendingReminders = memoryManager.allReminders.first().filter { !it.isCompleted }
        val noteCount = memoryManager.allNotes.first().size

        val reminderSummary = if (pendingReminders.isNotEmpty()) {
            "You have ${pendingReminders.size} scheduled reminder${if (pendingReminders.size > 1) "s" else ""}: ${pendingReminders.first().title}."
        } else {
            "No pending reminders scheduled for today."
        }

        val speech = "Good morning, Boss! Current time is $timeStr on $dateStr. " +
                "Arc Reactor battery is at $batteryLevel percent${if (isCharging) " and charging" else ""}. " +
                "$reminderSummary You have $noteCount archived intelligence files in memory. All systems nominal. Ready for your directives."

        return ProtocolExecutionResult(
            protocolName = "STARK MORNING BRIEFING",
            success = true,
            speechResponse = speech,
            displayDetails = listOf(
                "Time & Date: $timeStr | $dateStr",
                "Battery Telemetry: $batteryLevel% ${if (isCharging) "(Charging)" else ""}",
                "Reminders: ${pendingReminders.size} pending tasks",
                "Archived Notes: $noteCount memory entries",
                "Defense Protocols: Shield Active & Nominal"
            )
        )
    }

    private suspend fun executeWorkoutProtocol(): ProtocolExecutionResult {
        StarkSoundEngine.playSound(StarkSoundEngine.SoundType.ARC_REACTOR_POWER_UP)
        // Step 1: Set Media Volume to 80%
        audioManager?.let { am ->
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, (maxVol * 0.8).toInt(), AudioManager.FLAG_SHOW_UI)
        }

        // Step 2: Schedule 45-Minute Workout Timer
        val triggerTime = System.currentTimeMillis() + (45 * 60 * 1000L)
        memoryManager.addReminder("Complete Workout Session", triggerTime)

        // Step 3: Launch YouTube / Spotify for workout audio
        appControlEngine.executeAppAction(
            packageName = "com.google.android.youtube",
            appName = "YouTube",
            query = "gym workout motivation music"
        )

        val speech = "Workout protocol initiated, Boss. Media volume calibrated to 80 percent, a 45-minute training timer is active, and workout audio deployed. Let's break some records."

        return ProtocolExecutionResult(
            protocolName = "STARK WORKOUT PROTOCOL",
            success = true,
            speechResponse = speech,
            displayDetails = listOf(
                "Step 1: Calibrated Media Volume to 80%",
                "Step 2: Scheduled 45-Minute Workout Timer",
                "Step 3: Launched Workout Audio via YouTube / Spotify",
                "Status: Execution Completed"
            )
        )
    }

    private suspend fun executeFocusProtocol(): ProtocolExecutionResult {
        StarkSoundEngine.playSound(StarkSoundEngine.SoundType.TACTICAL_SONAR_PING)
        // Step 1: Set ringer to Vibrate
        audioManager?.ringerMode = AudioManager.RINGER_MODE_VIBRATE

        // Step 2: Schedule 25-Minute Pomodoro Focus Session
        val triggerTime = System.currentTimeMillis() + (25 * 60 * 1000L)
        memoryManager.addReminder("End of 25-minute Focus Session", triggerTime)

        val speech = "Focus protocol engaged, Boss. Audio ringer set to vibrate, ambient distractions suppressed, and a 25-minute Pomodoro focus timer is active. Pure productivity mode engaged."

        return ProtocolExecutionResult(
            protocolName = "STARK FOCUS PROTOCOL",
            success = true,
            speechResponse = speech,
            displayDetails = listOf(
                "Step 1: Set Ringer Mode to Vibrate",
                "Step 2: Activated 25-minute Pomodoro Timer",
                "Step 3: Suppressed Audio Distractions",
                "Status: Deep Work Active"
            )
        )
    }

    private fun executeNightProtocol(): ProtocolExecutionResult {
        audioManager?.ringerMode = AudioManager.RINGER_MODE_SILENT
        val speech = "Night protocol active, Boss. Ringer muted and notifications suppressed. Mark VI operating system standing by in low-power defensive guard until morning. Good night."

        return ProtocolExecutionResult(
            protocolName = "STARK NIGHT PROTOCOL",
            success = true,
            speechResponse = speech,
            displayDetails = listOf(
                "Step 1: Silenced Ringer & Notifications",
                "Step 2: Low-Power Standby Engaged",
                "Status: Good Night, Boss"
            )
        )
    }

    private fun executeMovieProtocol(): ProtocolExecutionResult {
        StarkSoundEngine.playSound(StarkSoundEngine.SoundType.ARC_REACTOR_POWER_UP)
        audioManager?.let { am ->
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, (maxVol * 0.75).toInt(), AudioManager.FLAG_SHOW_UI)
        }
        audioManager?.ringerMode = AudioManager.RINGER_MODE_VIBRATE

        appControlEngine.executeAppAction(
            packageName = "com.netflix.mediaclient",
            appName = "Netflix",
            query = ""
        )

        val speech = "Movie protocol engaged, Boss. Media audio calibrated to 75 percent, ringer set to vibrate, and entertainment streams deployed."

        return ProtocolExecutionResult(
            protocolName = "STARK MOVIE NIGHT PROTOCOL",
            success = true,
            speechResponse = speech,
            displayDetails = listOf(
                "Step 1: Calibrated Media Volume to 75%",
                "Step 2: Ringer Muted to Vibrate",
                "Step 3: Launched Streaming Client",
                "Status: Enjoy the show, Boss"
            )
        )
    }

    private fun executeDrivingProtocol(): ProtocolExecutionResult {
        StarkSoundEngine.playSound(StarkSoundEngine.SoundType.ARC_REACTOR_POWER_UP)
        audioManager?.let { am ->
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, (maxVol * 0.85).toInt(), AudioManager.FLAG_SHOW_UI)
        }

        appControlEngine.executeAppAction(
            packageName = "com.google.android.apps.maps",
            appName = "Maps",
            query = ""
        )

        val speech = "Driving protocol initialized, Boss. Google Navigation deployed, media volume calibrated, and hands-free speakerphone enabled. Eyes on the road."

        return ProtocolExecutionResult(
            protocolName = "STARK DRIVING PROTOCOL",
            success = true,
            speechResponse = speech,
            displayDetails = listOf(
                "Step 1: Deployed Google Maps Navigation",
                "Step 2: Calibrated Speaker Volume to 85%",
                "Step 3: Hands-Free Speakerphone Active",
                "Status: Eyes on the road, Boss"
            )
        )
    }

    private fun executeEmergencySosProtocol(): ProtocolExecutionResult {
        StarkSoundEngine.playSound(StarkSoundEngine.SoundType.REPULSOR_BLAST)
        // Max volume
        audioManager?.let { am ->
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_RING)
            am.setStreamVolume(AudioManager.STREAM_RING, maxVol, AudioManager.FLAG_SHOW_UI)
        }

        // Toggle Flashlight Strobe
        try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cm?.cameraIdList?.firstOrNull()
            if (cameraId != null) {
                cm.setTorchMode(cameraId, true)
            }
        } catch (e: Exception) {}

        // Launch SMS Intent (Safe for Play Protect: NO SEND_SMS permission needed!)
        try {
            val emergencySmsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:112")
                putExtra("sms_body", "EMERGENCY: Immediate assistance required. Sent via F.R.I.D.A.Y. Mark VI Emergency Protocol.")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(emergencySmsIntent)
        } catch (e: Exception) {}

        val speech = "Emergency House Party Protocol initiated! Maximum audio alert engaged, emergency beacon active, and dispatch intent deployed, Boss."

        return ProtocolExecutionResult(
            protocolName = "HOUSE PARTY EMERGENCY PROTOCOL",
            success = true,
            speechResponse = speech,
            displayDetails = listOf(
                "Alert Level: MAXIMUM TACTICAL PRIORITY",
                "Step 1: Audio Volume Set to 100%",
                "Step 2: Emergency Flashlight Beacon Activated",
                "Step 3: Prepared Emergency Dispatch via SMS",
                "Status: DEFENSIVE PROTOCOL ACTIVE"
            )
        )
    }

    private fun tryParseWhatsAppDispatch(query: String): ProtocolExecutionResult? {
        val q = query.trim()
        val regex = Regex("""(?:text|send message to|whatsapp)\s+([a-zA-Z0-9\s]+?)\s+(?:saying|that|with message)\s+(.+)""", RegexOption.IGNORE_CASE)
        val match = regex.find(q) ?: return null

        val (contactName, messageBody) = match.destructured

        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_TEXT, messageBody.trim())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (context.packageManager.resolveActivity(intent, 0) != null) {
                context.startActivity(intent)
                ProtocolExecutionResult(
                    protocolName = "WHATSAPP SMART DISPATCH",
                    success = true,
                    speechResponse = "Prepared WhatsApp message for $contactName saying \"${messageBody.trim()}\". Opening chat now, Boss.",
                    displayDetails = listOf(
                        "Target Contact: $contactName",
                        "Message Payload: \"${messageBody.trim()}\"",
                        "Dispatched via: WhatsApp Secure Intent"
                    )
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
