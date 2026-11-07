package com.wishat12.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wishat12.app.data.entity.ReminderEntity
import com.wishat12.app.ui.components.EmptyState
import com.wishat12.app.ui.components.SectionTitle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RemindersScreen(
    reminders: List<ReminderEntity>,
    onEdit: (ReminderEntity) -> Unit,
    onDelete: (ReminderEntity) -> Unit,
    onComplete: (ReminderEntity) -> Unit,
    onToggle: (ReminderEntity, Boolean) -> Unit,
    onDuplicate: (ReminderEntity) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Upcoming") }
    val shown = reminders.filter { item ->
        val matches = item.recipientName.contains(query, true) || item.occasionType.contains(query, true) || item.message.contains(query, true)
        matches && when (filter) {
            "Completed" -> item.isCompleted
            "Paused" -> !item.isActive && !item.isCompleted
            "Repeating" -> item.isActive && !item.isCompleted && item.recurrenceType != "NEVER"
            else -> item.isActive && !item.isCompleted
        }
    }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle("Your reminders", "Every date that matters, in one place")
        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Search people, occasions…") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Upcoming", "Repeating", "Paused", "Completed").forEach { item ->
                FilterChip(selected = filter == item, onClick = { filter = item }, label = { Text(item) })
            }
        }
        if (shown.isEmpty()) {
            EmptyState("Nothing here yet", "Try another filter or create a new reminder.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
                items(shown, key = { it.id }) { reminder ->
                    Card(shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Cake, null, tint = MaterialTheme.colorScheme.primary)
                                Column(Modifier.weight(1f).clickable { onEdit(reminder) }) {
                                    Text(reminder.recipientName, style = MaterialTheme.typography.titleMedium)
                                    Text(reminder.customOccasion.ifBlank { reminder.occasionType }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
                                }
                                Text(Instant.ofEpochMilli(reminder.scheduledAtMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM · hh:mm a", Locale.getDefault())), style = MaterialTheme.typography.labelSmall)
                            }
                            Text(reminder.message, maxLines = 2, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                if (!reminder.isCompleted) {
                                    IconButton(onClick = { onToggle(reminder, !reminder.isActive) }) { Icon(if (reminder.isActive) Icons.Default.Done else Icons.Default.Schedule, "Pause or resume") }
                                    IconButton(onClick = { onComplete(reminder) }) { Icon(Icons.Default.Done, "Complete") }
                                }
                                IconButton(onClick = { onDuplicate(reminder) }) { Icon(Icons.Default.ContentCopy, "Duplicate") }
                                IconButton(onClick = { onEdit(reminder) }) { Icon(Icons.Default.Edit, "Edit") }
                                IconButton(onClick = { onDelete(reminder) }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
    }
}
