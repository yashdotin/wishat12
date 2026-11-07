package com.wishat12.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.wishat12.app.data.entity.ReminderEntity
import com.wishat12.app.ui.components.SectionTitle
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(reminders: List<ReminderEntity>, onOpen: (ReminderEntity) -> Unit) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf(LocalDate.now()) }
    val days = month.lengthOfMonth()
    val firstOffset = month.atDay(1).dayOfWeek.value % 7
    val byDate = reminders.filter { it.isActive && !it.isCompleted }.groupBy {
        Instant.ofEpochMilli(it.scheduledAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    val selectedItems = byDate[selected].orEmpty()
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle("Calendar", "A clear view of what’s coming")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.Default.ChevronLeft, "Previous month") }
            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())), style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.Default.ChevronRight, "Next month") }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { Text(it, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f)) }
        }
        val totalCells = ((firstOffset + days + 6) / 7) * 7
        for (week in 0 until totalCells / 7) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val day = week * 7 + col - firstOffset + 1
                    if (day in 1..days) {
                        val date = month.atDay(day)
                        val has = byDate[date].orEmpty().isNotEmpty()
                        val isSelected = selected == date
                        Column(Modifier.weight(1f).padding(vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.size(38.dp).clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (has) 1f else .34f))
                                    .clickable { selected = date },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(day.toString(), color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(Modifier.height(3.dp))
                            Box(Modifier.size(5.dp).clip(CircleShape).background(if (has) MaterialTheme.colorScheme.secondary else androidx.compose.ui.graphics.Color.Transparent))
                        }
                    } else Spacer(Modifier.weight(1f))
                }
            }
        }
        HorizontalDivider()
        Text(selected.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())), style = MaterialTheme.typography.titleMedium)
        if (selectedItems.isEmpty()) Text("No reminders on this date.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f), style = MaterialTheme.typography.bodySmall)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(selectedItems, key = { it.id }) { item ->
                ListItem(
                    headlineContent = { Text(item.recipientName) },
                    supportingContent = { Text(item.customOccasion.ifBlank { item.occasionType } + " · " + item.scheduledTime) },
                    modifier = Modifier.clickable { onOpen(item) }
                )
            }
        }
    }
}
