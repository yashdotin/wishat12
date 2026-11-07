package com.wishat12.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wishat12.app.data.entity.ReminderEntity
import com.wishat12.app.ui.components.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(reminders: List<ReminderEntity>, onAdd: () -> Unit, onOpen: (ReminderEntity) -> Unit) {
    val now = System.currentTimeMillis()
    val active = reminders.filter { it.isActive && !it.isCompleted }
    val upcoming = active.filter { it.scheduledAtMillis >= now }.sortedBy { it.scheduledAtMillis }
    val today = active.count {
        Instant.ofEpochMilli(it.scheduledAtMillis).atZone(ZoneId.systemDefault()).toLocalDate() == java.time.LocalDate.now()
    }
    val completed = reminders.count { it.isCompleted }
    val recentlyCreated = reminders.sortedByDescending { it.createdAt }.take(5)
    val next = upcoming.firstOrNull()
    val nextBirthday = upcoming.firstOrNull { it.occasionType.equals("Birthday", ignoreCase = true) }
    val dateLabel = java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))}  ·  $dateLabel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f), style = MaterialTheme.typography.labelMedium)
                Text("Make every moment\nremembered.", style = MaterialTheme.typography.headlineMedium)
                Text("Your important dates, taken care of.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f))
            }
        }
        item {
            GradientHero(Modifier.fillMaxWidth()) {
                Text("YOUR NEXT WISH", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                if (next != null) {
                    Text(next.recipientName, style = MaterialTheme.typography.headlineSmall)
                    Text(next.customOccasion.ifBlank { next.occasionType }, color = ColorWhite.copy(alpha = .8f))
                    Spacer(Modifier.height(10.dp))
                    val whenText = Instant.ofEpochMilli(next.scheduledAtMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE, d MMM · hh:mm a", Locale.getDefault()))
                    Text(whenText, style = MaterialTheme.typography.titleMedium)
                    Text(countdown(next.scheduledAtMillis - now), color = ColorWhite.copy(alpha = .75f), style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Nothing scheduled yet", style = MaterialTheme.typography.headlineSmall)
                    Text("Add a date worth remembering.", color = ColorWhite.copy(alpha = .75f))
                }
                Spacer(Modifier.height(14.dp))
                Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = ColorWhite, contentColor = androidx.compose.ui.graphics.Color(0xFF30204D))) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Create a wish")
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Total wishes", reminders.size.toString(), Modifier.weight(1f))
                StatCard("Due today", today.toString(), Modifier.weight(1f))
                StatCard("Completed", completed.toString(), Modifier.weight(1f))
            }
        }
        if (nextBirthday != null) item {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Celebration, null, tint = MaterialTheme.colorScheme.secondary)
                    Column {
                        Text("Next birthday", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
                        Text("${nextBirthday.recipientName} · ${countdown(nextBirthday.scheduledAtMillis - now)}", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
        item { SectionTitle("Coming up", "The next moments on your calendar") }
        if (upcoming.isEmpty()) item { EmptyState("A little space for something special", "Your upcoming wishes will appear here.", "Add your first reminder", onAdd) }
        items(upcoming.take(8), key = { it.id }) { reminder ->
            Card(Modifier.fillMaxWidth().clickable { onOpen(reminder) }, shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .13f)) {
                        Icon(Icons.Default.Cake, null, Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(reminder.recipientName, style = MaterialTheme.typography.titleMedium)
                        Text(reminder.customOccasion.ifBlank { reminder.occasionType }, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f), style = MaterialTheme.typography.bodySmall)
                        Text(Instant.ofEpochMilli(reminder.scheduledAtMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM yyyy · hh:mm a", Locale.getDefault())), style = MaterialTheme.typography.labelMedium)
                    }
                    Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        if (recentlyCreated.isNotEmpty()) {
            item { SectionTitle("Recently added", "The latest wishes you saved") }
            items(recentlyCreated, key = { "recent-${it.id}" }) { reminder ->
                ListItem(
                    headlineContent = { Text(reminder.recipientName) },
                    supportingContent = { Text(reminder.customOccasion.ifBlank { reminder.occasionType }) },
                    trailingContent = { TextButton(onClick = { onOpen(reminder) }) { Text("Open") } }
                )
            }
        }
    }
}

private val ColorWhite = androidx.compose.ui.graphics.Color.White
private fun countdown(ms: Long): String {
    val days = (ms / 86_400_000L).coerceAtLeast(0)
    val hours = ((ms % 86_400_000L) / 3_600_000L).coerceAtLeast(0)
    return if (days > 0) "In $days day${if (days == 1L) "" else "s"}" else "In $hours hour${if (hours == 1L) "" else "s"}"
}
