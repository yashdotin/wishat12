package com.wishat12.app.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wishat12.app.data.entity.ReminderEntity
import com.wishat12.app.data.entity.ContactEntity
import com.wishat12.app.data.entity.MessageTemplateEntity
import com.wishat12.app.domain.RecurrenceCalculator
import com.wishat12.app.domain.PhoneUtils
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val occasions = listOf("Birthday", "Anniversary", "Festival", "Friendship day", "Special occasion", "Custom event")
private val recurrences = listOf("Never", "Every week", "Every month", "Every year")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditorDialog(
    existing: ReminderEntity? = null,
    initialTemplate: String? = null,
    templates: List<MessageTemplateEntity> = emptyList(),
    favoriteContacts: List<ContactEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (ReminderEntity) -> Unit
) {
    val context = LocalContext.current
    val zone = remember { ZoneId.systemDefault() }
    var recipient by remember { mutableStateOf(existing?.recipientName.orEmpty()) }
    var phone by remember { mutableStateOf(existing?.phoneNumber.orEmpty()) }
    var occasion by remember { mutableStateOf(existing?.occasionType ?: "Birthday") }
    var customOccasion by remember { mutableStateOf(existing?.customOccasion.orEmpty()) }
    var message by remember { mutableStateOf(existing?.message ?: initialTemplate.orEmpty()) }
    var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }
    var recurrence by remember { mutableStateOf(existing?.recurrenceType ?: if (occasion == "Birthday") "YEARLY" else "NEVER") }
    var notify by remember { mutableStateOf(existing?.notificationEnabled ?: true) }
    var date by remember { mutableStateOf(existing?.let { LocalDate.parse(it.scheduledDate) } ?: LocalDate.now()) }
    var time by remember { mutableStateOf(existing?.let { LocalTime.parse(it.scheduledTime) } ?: LocalTime.MIDNIGHT) }
    var occasionExpanded by remember { mutableStateOf(false) }
    var recurrenceExpanded by remember { mutableStateOf(false) }
    var templateExpanded by remember { mutableStateOf(false) }
    var favoriteExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Create a wish" else "Edit reminder") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 580.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(recipient, { recipient = it }, label = { Text("Recipient name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(phone, { phone = it }, label = { Text("WhatsApp number (with country code)") }, placeholder = { Text("+91 98765 43210") }, supportingText = { Text("Include the country code; leave blank if you do not need WhatsApp.") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (favoriteContacts.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = favoriteExpanded, onExpandedChange = { favoriteExpanded = it }) {
                        OutlinedTextField("Choose a favorite contact", {}, readOnly = true, label = { Text("Quick fill") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(favoriteExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded = favoriteExpanded, onDismissRequest = { favoriteExpanded = false }) {
                            favoriteContacts.forEach { contact ->
                                DropdownMenuItem(text = { Text("${contact.name} · ${contact.phoneNumber}") }, onClick = {
                                    recipient = contact.name
                                    phone = contact.phoneNumber
                                    favoriteExpanded = false
                                })
                            }
                        }
                    }
                }
                ExposedDropdownMenuBox(expanded = occasionExpanded, onExpandedChange = { occasionExpanded = it }) {
                    OutlinedTextField(occasion, {}, readOnly = true, label = { Text("Occasion") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(occasionExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = occasionExpanded, onDismissRequest = { occasionExpanded = false }) {
                        occasions.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { occasion = item; occasionExpanded = false }) }
                    }
                }
                if (occasion == "Custom event") OutlinedTextField(customOccasion, { customOccasion = it }, label = { Text("Custom occasion name") }, modifier = Modifier.fillMaxWidth())
                if (templates.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = templateExpanded, onExpandedChange = { templateExpanded = it }) {
                        OutlinedTextField("Choose a template", {}, readOnly = true, label = { Text("Start from a template") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(templateExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded = templateExpanded, onDismissRequest = { templateExpanded = false }) {
                            templates.forEach { template ->
                                DropdownMenuItem(text = { Text(template.title) }, onClick = {
                                    message = template.content
                                    templateExpanded = false
                                })
                            }
                        }
                    }
                }
                OutlinedTextField(message, { message = it }, label = { Text("Message *") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = {
                        DatePickerDialog(context, { _, y, m, d -> date = LocalDate.of(y, m + 1, d) }, date.year, date.monthValue - 1, date.dayOfMonth).show()
                    }, modifier = Modifier.weight(1f)) { Text(date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault()))) }
                    OutlinedButton(onClick = {
                        TimePickerDialog(context, { _, h, m -> time = LocalTime.of(h, m) }, time.hour, time.minute, false).show()
                    }, modifier = Modifier.weight(1f)) { Text("Custom · " + time.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))) }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(LocalTime.MIDNIGHT, LocalTime.of(9, 0), LocalTime.NOON, LocalTime.of(18, 0)).forEach { preset ->
                        FilterChip(selected = time == preset, onClick = { time = preset }, label = { Text(preset.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))) })
                    }
                }
                ExposedDropdownMenuBox(expanded = recurrenceExpanded, onExpandedChange = { recurrenceExpanded = it }) {
                    OutlinedTextField(recurrences.firstOrNull { it.secondCode() == recurrence } ?: "Never", {}, readOnly = true, label = { Text("Repeat") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(recurrenceExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = recurrenceExpanded, onDismissRequest = { recurrenceExpanded = false }) {
                        recurrences.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { recurrence = item.secondCode(); recurrenceExpanded = false }) }
                    }
                }
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Reminder notification", style = MaterialTheme.typography.bodyMedium)
                        Text("Show a notification at the scheduled time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
                    }
                    Switch(checked = notify, onCheckedChange = { notify = it })
                }
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (recipient.isBlank() || message.isBlank()) { error = "Add a recipient and a message to continue."; return@Button }
                if (phone.isNotBlank() && !PhoneUtils.isValid(phone)) { error = "Enter a valid international phone number (8–15 digits), including country code."; return@Button }
                val scheduledAt = RecurrenceCalculator.atLocalDateTime(date, time.hour, time.minute, zone)
                if (scheduledAt <= System.currentTimeMillis() && existing == null) { error = "Choose a date and time in the future."; return@Button }
                val timeText = time.format(DateTimeFormatter.ofPattern("HH:mm"))
                val preservedAnchor = existing
                    ?.takeIf { it.scheduledDate == date.format(DateTimeFormatter.ISO_LOCAL_DATE) && it.scheduledTime == timeText }
                    ?.recurrenceAnchorMillis
                    ?: scheduledAt
                val item = ReminderEntity(
                    id = existing?.id ?: System.currentTimeMillis(), recipientName = recipient.trim(), phoneNumber = PhoneUtils.normalize(phone),
                    occasionType = occasion, customOccasion = customOccasion.trim(), message = message.trim().replace("{name}", recipient.trim()),
                    scheduledDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE), scheduledTime = timeText,
                    timezone = zone.id, scheduledAtMillis = scheduledAt, recurrenceAnchorMillis = preservedAnchor, recurrenceType = recurrence, notes = notes.trim(),
                    isActive = true, isCompleted = false, notificationEnabled = notify,
                    lastNotifiedAt = existing?.lastNotifiedAt, createdAt = existing?.createdAt ?: System.currentTimeMillis()
                )
                onSave(item)
            }) { Text("Save reminder") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun String.secondCode(): String = when (this) {
    "Every year" -> RecurrenceCalculator.YEARLY
    "Every month" -> RecurrenceCalculator.MONTHLY
    "Every week" -> RecurrenceCalculator.WEEKLY
    else -> RecurrenceCalculator.NEVER
}
