package com.friday.assistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val triggerTimestamp: Long,
    val isCompleted: Boolean = false,
    val priority: String = "NORMAL"
)

