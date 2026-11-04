package com.friday.assistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friday_memory")
data class MemoryEntity(
    @PrimaryKey
    val key: String,
    val value: String,
    val category: String = "USER_FACT",
    val updatedAt: Long = System.currentTimeMillis()
)

