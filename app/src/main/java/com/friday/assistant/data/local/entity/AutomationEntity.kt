package com.friday.assistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "automation_history")
data class AutomationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val targetApp: String,
    val packageName: String,
    val actionType: String,     // LAUNCH, DEEP_LINK, SEARCH, ACCESSIBILITY_CLICK, ACCESSIBILITY_SCROLL
    val parameters: String,
    val executionResult: String // SUCCESS, VERIFIED, FAILED, ABORTED
)

