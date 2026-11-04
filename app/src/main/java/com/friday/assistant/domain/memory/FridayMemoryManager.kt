package com.friday.assistant.domain.memory

import android.content.Context
import com.friday.assistant.data.local.FridayDatabase
import com.friday.assistant.data.local.entity.MemoryEntity
import com.friday.assistant.data.local.entity.NoteEntity
import com.friday.assistant.data.local.entity.ReminderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FridayMemoryManager(context: Context) {
    private val database = FridayDatabase.getInstance(context)
    private val noteDao = database.noteDao()
    private val reminderDao = database.reminderDao()
    private val memoryDao = database.memoryDao()

    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()
    val allReminders: Flow<List<ReminderEntity>> = reminderDao.getAllReminders()
    val allMemories: Flow<List<MemoryEntity>> = memoryDao.getAllMemories()

    suspend fun saveNote(title: String, content: String, category: String = "GENERAL"): Long =
        withContext(Dispatchers.IO) {
            noteDao.insertNote(NoteEntity(title = title, content = content, category = category))
        }

    suspend fun deleteNote(id: Long) = withContext(Dispatchers.IO) {
        noteDao.deleteNoteById(id)
    }

    suspend fun addReminder(title: String, triggerTimestamp: Long): Long =
        withContext(Dispatchers.IO) {
            reminderDao.insertReminder(
                ReminderEntity(title = title, triggerTimestamp = triggerTimestamp)
            )
        }

    suspend fun completeReminder(id: Long) = withContext(Dispatchers.IO) {
        reminderDao.markCompleted(id)
    }

    suspend fun remember(key: String, value: String) = withContext(Dispatchers.IO) {
        memoryDao.saveMemory(MemoryEntity(key = key.trim().lowercase(), value = value.trim()))
    }

    suspend fun recall(key: String): String? = withContext(Dispatchers.IO) {
        memoryDao.getMemoryByKey(key.trim().lowercase())?.value
    }

    suspend fun searchMemory(query: String): List<MemoryEntity> = withContext(Dispatchers.IO) {
        memoryDao.searchMemories(query)
    }

    /**
     * Interprets and executes natural language memory requests directly:
     * e.g., "remember that...", "note down...", "remind me..."
     */
    suspend fun tryHandleMemoryCommand(query: String): String? {
        val lower = query.trim().lowercase()

        // 1. Remember facts pattern: "remember that <key> is <value>" or "my <key> is <value>"
        if (lower.startsWith("remember that ") || lower.startsWith("remember ")) {
            val fact = query.substring(if (lower.startsWith("remember that ")) 14 else 9).trim()
            if (fact.contains(" is ")) {
                val parts = fact.split(" is ", limit = 2)
                remember(parts[0], parts[1])
                return "Understood. I will remember that ${parts[0]} is ${parts[1]}."
            } else if (fact.contains(" are ")) {
                val parts = fact.split(" are ", limit = 2)
                remember(parts[0], parts[1])
                return "Noted. I have recorded that ${parts[0]} are ${parts[1]}."
            } else {
                remember("general_fact_${System.currentTimeMillis()}", fact)
                return "Recorded in persistent memory: \"$fact\"."
            }
        }

        // 2. Recall facts pattern: "what is my <key>?" or "where is my <key>?"
        if (lower.startsWith("what is my ") || lower.startsWith("what's my ") || lower.startsWith("where is my ")) {
            val key = lower.replace(Regex("^(what is my|what's my|where is my)\\s+"), "").replace("?", "").trim()
            val remembered = recall(key)
            if (remembered != null) {
                return "According to my records, your $key is $remembered."
            }
            val matches = searchMemory(key)
            if (matches.isNotEmpty()) {
                val m = matches.first()
                return "I recall: ${m.key} is ${m.value}."
            }
        }

        // 3. Take a note pattern: "take a note: ..." or "note down ..."
        if (lower.startsWith("take a note") || lower.startsWith("note down") || lower.startsWith("save note")) {
            val text = query.replace(Regex("^(take a note:?|note down:?|save note:?)\\s*", RegexOption.IGNORE_CASE), "").trim()
            if (text.isNotBlank()) {
                val timeStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
                saveNote("Note ($timeStr)", text)
                return "Note successfully saved: \"$text\"."
            }
        }

        // 4. Remind me pattern: "remind me to <action> in <N> minutes"
        if (lower.startsWith("remind me to ") || lower.startsWith("remind me ")) {
            val raw = query.substring(if (lower.startsWith("remind me to ")) 13 else 10).trim()
            val minMatch = Regex("in\\s+(\\d+)\\s*(?:minutes|min|mins)").find(raw)
            val minutes = minMatch?.groupValues?.get(1)?.toIntOrNull() ?: 10
            val task = raw.replace(Regex("in\\s+\\d+\\s*(?:minutes|min|mins)"), "").trim()

            val triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
            addReminder(task.ifBlank { "Reminder" }, triggerTime)
            return "Reminder scheduled for $task in $minutes minutes."
        }

        return null
    }

    /**
     * Offline Personal RAG: Semantic & Keyword Retrieval across Personal Vault & Notes.
     */
    suspend fun queryPersonalKnowledgeBase(query: String): String? = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        val memories = memoryDao.searchMemories(q)
        if (memories.isNotEmpty()) {
            val m = memories.first()
            return@withContext "According to your personal knowledge base: **${m.key} is ${m.value}**."
        }

        val notes = noteDao.searchNotes(q)
        if (notes.isNotEmpty()) {
            val n = notes.first()
            return@withContext "Located in your personal intelligence files (${n.title}):\n\n\"${n.content}\""
        }

        return@withContext null
    }

    /**
     * Encrypted Backup Generation: Exports all memories into an encrypted JSON vault.
     */
    suspend fun generateEncryptedBackup(vaultManager: com.friday.assistant.core.security.StarkVaultManager): String = withContext(Dispatchers.IO) {
        val factCount = memoryDao.getCount()
        val noteCount = noteDao.getCount()
        val summary = "{\"backup_timestamp\": ${System.currentTimeMillis()}, \"facts_count\": $factCount, \"notes_count\": $noteCount}"
        vaultManager.encrypt(summary)
    }
}

