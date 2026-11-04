package com.friday.assistant.domain.calendar

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import java.util.Calendar

data class CalendarScheduleResult(
    val success: Boolean,
    val title: String,
    val formattedTime: String,
    val message: String
)

/**
 * Stark Autonomous Calendar & Schedule Engine.
 * Parses natural language scheduling directives and deploys them to Google Calendar.
 */
class StarkCalendarEngine(private val context: Context) {

    fun tryScheduleEvent(query: String): CalendarScheduleResult? {
        val q = query.trim().lowercase()
        if (!q.startsWith("schedule ") && !q.startsWith("add event ") && !q.startsWith("set meeting ") && !q.startsWith("create event ")) {
            return null
        }

        val clean = query.trim()
            .removePrefix("schedule ")
            .removePrefix("Schedule ")
            .removePrefix("add event ")
            .removePrefix("set meeting ")
            .removePrefix("create event ")
            .trim()

        val cal = Calendar.getInstance()
        var title = clean

        // Parse tomorrow vs today
        if (clean.contains(" tomorrow", ignoreCase = true)) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            title = title.replace(" tomorrow", "", ignoreCase = true).trim()
        }

        // Parse time: "at 4 pm", "at 10 am", "at 16:00"
        val timeRegex = Regex("""\bat\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)?\b""", RegexOption.IGNORE_CASE)
        val match = timeRegex.find(title)
        var timeStr = "Morning"

        if (match != null) {
            val hourStr = match.groupValues[1]
            val minStr = match.groupValues[2]
            val amPm = match.groupValues[3].lowercase()

            var hour = hourStr.toIntOrNull() ?: 9
            val min = minStr.toIntOrNull() ?: 0

            if (amPm == "pm" && hour < 12) hour += 12
            if (amPm == "am" && hour == 12) hour = 0

            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, min)
            cal.set(Calendar.SECOND, 0)
            timeStr = String.format("%02d:%02d", hour, min)

            title = title.replace(match.value, "").trim()
        } else {
            cal.set(Calendar.HOUR_OF_DAY, 10)
            cal.set(Calendar.MINUTE, 0)
        }

        title = title.removePrefix("for ").removePrefix("a ").trim()
        if (title.isBlank()) title = "Important Directive"

        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, cal.timeInMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, cal.timeInMillis + (60 * 60 * 1000L))
                putExtra(CalendarContract.Events.DESCRIPTION, "Scheduled via F.R.I.D.A.Y. Stark Assistant Protocol.")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            CalendarScheduleResult(
                success = true,
                title = title,
                formattedTime = timeStr,
                message = "Event \"$title\" scheduled for $timeStr. Deployed to Google Calendar, Boss."
            )
        } catch (e: Exception) {
            null
        }
    }
}

