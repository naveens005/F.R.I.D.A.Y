package com.friday.assistant.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.friday.assistant.data.local.entity.AutomationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutomation(entity: AutomationEntity): Long

    @Query("SELECT * FROM automation_history ORDER BY timestamp DESC")
    fun getAllAutomations(): Flow<List<AutomationEntity>>

    @Query("DELETE FROM automation_history")
    suspend fun clearHistory()
}

