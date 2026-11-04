package com.friday.assistant.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.friday.assistant.data.local.dao.AuditLogDao
import com.friday.assistant.data.local.dao.AutomationDao
import com.friday.assistant.data.local.dao.ConversationDao
import com.friday.assistant.data.local.dao.MemoryDao
import com.friday.assistant.data.local.dao.NoteDao
import com.friday.assistant.data.local.dao.ReminderDao
import com.friday.assistant.data.local.entity.AuditLogEntity
import com.friday.assistant.data.local.entity.AutomationEntity
import com.friday.assistant.data.local.entity.ConversationEntity
import com.friday.assistant.data.local.entity.MemoryEntity
import com.friday.assistant.data.local.entity.NoteEntity
import com.friday.assistant.data.local.entity.ReminderEntity

@Database(
    entities = [
        AuditLogEntity::class,
        ConversationEntity::class,
        AutomationEntity::class,
        NoteEntity::class,
        ReminderEntity::class,
        MemoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class FridayDatabase : RoomDatabase() {

    abstract fun auditLogDao(): AuditLogDao
    abstract fun conversationDao(): ConversationDao
    abstract fun automationDao(): AutomationDao
    abstract fun noteDao(): NoteDao
    abstract fun reminderDao(): ReminderDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: FridayDatabase? = null

        fun getInstance(context: Context): FridayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FridayDatabase::class.java,
                    "friday_assistant.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
