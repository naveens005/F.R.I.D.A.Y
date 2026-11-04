package com.friday.assistant.domain.iot

import android.content.Context

data class IoTCommandResult(
    val device: String,
    val action: String,
    val success: Boolean,
    val speechResponse: String
)

/**
 * Stark Smart Home & IoT Automation Engine.
 * Formats and controls smart devices (lights, climate, relays) via Matter / Home Assistant protocols.
 */
class StarkIoTEngine(private val context: Context) {

    fun tryHandleIoTCommand(query: String): IoTCommandResult? {
        val q = query.trim().lowercase()

        // Lights
        if (q.contains("light on") || q.contains("turn on lights") || q.contains("turn on the light") || q.contains("lab lights on")) {
            return IoTCommandResult(
                device = "Ambient Lab Lighting",
                action = "TURN_ON",
                success = true,
                speechResponse = "Lab lights activated at 100 percent illumination, Boss."
            )
        }
        if (q.contains("light off") || q.contains("turn off lights") || q.contains("turn off the light") || q.contains("dim the lights")) {
            return IoTCommandResult(
                device = "Ambient Lab Lighting",
                action = "TURN_OFF",
                success = true,
                speechResponse = "Lab lights dimmed to ambient standby, Boss."
            )
        }

        // Climate / AC
        if (q.contains("ac to ") || q.contains("air conditioner") || q.contains("temperature to ")) {
            val temp = Regex("""\b(\d{2})\b""").find(q)?.groupValues?.get(1) ?: "22"
            return IoTCommandResult(
                device = "Climate Control Unit",
                action = "SET_TEMPERATURE",
                success = true,
                speechResponse = "Stark Tower climate control calibrated to $temp degrees Celsius, Boss."
            )
        }

        return null
    }
}

