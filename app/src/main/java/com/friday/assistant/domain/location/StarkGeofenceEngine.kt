package com.friday.assistant.domain.location

import android.content.Context
import android.media.AudioManager
import com.friday.assistant.domain.memory.FridayMemoryManager

data class GeofenceEventResult(
    val locationName: String,
    val speechResponse: String,
    val details: List<String>
)

/**
 * Stark Geofence & Location Telemetry Engine.
 * Handles location arrival & departure protocols, profile switching, and perimeter telemetry.
 */
class StarkGeofenceEngine(
    private val context: Context,
    private val memoryManager: FridayMemoryManager
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun handleLocationDirective(query: String): GeofenceEventResult? {
        val q = query.trim().lowercase()

        // 1. Arrived Home
        if (q.contains("arrived at home") || q.contains("reached home") || q.contains("i am home") || q.contains("i'm home")) {
            audioManager?.ringerMode = AudioManager.RINGER_MODE_NORMAL
            val speech = "Welcome back to Stark Tower, Boss! Home perimeter network connected, audio profile set to normal, and all defense systems nominal."
            return GeofenceEventResult(
                locationName = "HOME / STARK TOWER",
                speechResponse = speech,
                details = listOf(
                    "Perimeter: Home Wi-Fi Synchronized",
                    "Ringer: Normal Audio Profile",
                    "Status: Welcome home, Boss"
                )
            )
        }

        // 2. Arrived at Office / College
        if (q.contains("arrived at work") || q.contains("arrived at office") || q.contains("reached office") || q.contains("reached college")) {
            audioManager?.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            val speech = "Work perimeter entered, Boss. Ringer muted to vibrate, ambient notifications suppressed, and focus protocols standing by."
            return GeofenceEventResult(
                locationName = "OFFICE / WORKPLACE",
                speechResponse = speech,
                details = listOf(
                    "Perimeter: Office Zone Detected",
                    "Ringer: Vibrate Only",
                    "Status: Productivity Mode Engaged"
                )
            )
        }

        // 3. Leaving Work / Transit
        if (q.contains("leaving office") || q.contains("leaving work") || q.contains("heading out")) {
            val speech = "Transit protocol initialized, Boss. Navigation and traffic telemetry standing by. Have a safe journey."
            return GeofenceEventResult(
                locationName = "TRANSIT PROTOCOL",
                speechResponse = speech,
                details = listOf(
                    "Perimeter: Departure Logged",
                    "Status: Safe travels, Boss"
                )
            )
        }

        return null
    }
}

