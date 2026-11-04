package com.friday.assistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,      // COMMAND, SECURITY_BLOCK, APP_LAUNCH, ACCESSIBILITY_ACTION, MATH_SOLVE
    val details: String,
    val packageName: String? = null,
    val status: String          // SUCCESS, BLOCKED, FAILED, CONFIRMATION_REQUIRED
)

