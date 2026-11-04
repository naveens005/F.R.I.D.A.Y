package com.friday.assistant.ui.screens.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.friday.assistant.domain.memory.FridayMemoryManager
import com.friday.assistant.ui.components.GlassCard
import com.friday.assistant.ui.theme.FridayBgDark
import com.friday.assistant.ui.theme.FridayCyan
import com.friday.assistant.ui.theme.TextPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryManagerScreen(
    memoryManager: FridayMemoryManager,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val notes by memoryManager.allNotes.collectAsState(initial = emptyList())
    val reminders by memoryManager.allReminders.collectAsState(initial = emptyList())
    val memories by memoryManager.allMemories.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var newNoteText by remember { mutableStateOf("") }
    var newKeyText by remember { mutableStateOf("") }
    var newValueText by remember { mutableStateOf("") }

    Scaffold(
        containerColor = FridayBgDark,
        topBar = {
            TopAppBar(
                title = { Text("Memory & Notes Manager", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = FridayCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FridayBgDark)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .background(FridayBgDark)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "F.R.I.D.A.Y. long-term on-device database: personal facts, notes, and reminders.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF78909C)
                )
            }

            // Add Note Section
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Quick Note Entry", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        OutlinedTextField(
                            value = newNoteText,
                            onValueChange = { newNoteText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Note content...", color = Color(0xFF546E7A)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color(0xFF1E3A5F),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Button(
                            onClick = {
                                if (newNoteText.isNotBlank()) {
                                    scope.launch {
                                        memoryManager.saveNote("Quick Note", newNoteText)
                                        newNoteText = ""
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                            Text("Save Note", color = Color.Black)
                        }
                    }
                }
            }

            // Add Key-Value Memory Fact
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Store Knowledge Fact", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newKeyText,
                                onValueChange = { newKeyText = it },
                                placeholder = { Text("Key (e.g. car)", color = Color(0xFF546E7A)) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color(0xFF1E3A5F),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = newValueText,
                                onValueChange = { newValueText = it },
                                placeholder = { Text("Value", color = Color(0xFF546E7A)) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color(0xFF1E3A5F),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true
                            )
                        }
                        Button(
                            onClick = {
                                if (newKeyText.isNotBlank() && newValueText.isNotBlank()) {
                                    scope.launch {
                                        memoryManager.remember(newKeyText, newValueText)
                                        newKeyText = ""
                                        newValueText = ""
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Remember Fact", color = Color.Black)
                        }
                    }
                }
            }

            // Facts List
            item {
                Text("STORED USER FACTS (${memories.size})", style = MaterialTheme.typography.labelMedium, color = Color(0xFF00E5FF))
            }

            if (memories.isEmpty()) {
                item {
                    Text("No user facts recorded yet. Say 'Friday, remember that my car is red' to add.", color = Color(0xFF546E7A), style = MaterialTheme.typography.bodySmall)
                }
            } else {
                items(memories) { mem ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mem.key.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color(0xFF00E5FF))
                                Text(mem.value, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Notes List
            item {
                Text("NOTES (${notes.size})", style = MaterialTheme.typography.labelMedium, color = Color(0xFF00E5FF))
            }

            if (notes.isEmpty()) {
                item {
                    Text("No notes saved. Say 'Friday, take a note: buy groceries'.", color = Color(0xFF546E7A), style = MaterialTheme.typography.bodySmall)
                }
            } else {
                items(notes) { note ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(note.title, style = MaterialTheme.typography.titleSmall, color = Color(0xFF00E5FF))
                                Text(note.content, style = MaterialTheme.typography.bodySmall, color = Color.White)
                            }
                            IconButton(onClick = { scope.launch { memoryManager.deleteNote(note.id) } }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
                            }
                        }
                    }
                }
            }

            // Reminders List
            item {
                Text("SCHEDULED REMINDERS (${reminders.size})", style = MaterialTheme.typography.labelMedium, color = Color(0xFF00E5FF))
            }

            if (reminders.isEmpty()) {
                item {
                    Text("No reminders active. Say 'Friday, remind me in 10 minutes to call doctor'.", color = Color(0xFF546E7A), style = MaterialTheme.typography.bodySmall)
                }
            } else {
                items(reminders) { rem ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rem.title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                                Text(
                                    SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()).format(Date(rem.triggerTimestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF00E5FF)
                                )
                            }
                            Checkbox(
                                checked = rem.isCompleted,
                                onCheckedChange = { scope.launch { memoryManager.completeReminder(rem.id) } },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00E5FF))
                            )
                        }
                    }
                }
            }
        }
    }
}
