package com.friday.assistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val sender: String,         // USER or FRIDAY
    val content: String,
    val intentType: String? = null,
    val actionType: String? = null,
    val isSecurityBlocked: Boolean = false
)

