package com.wishat12.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wishat12.app.data.database.AppDatabase
import com.wishat12.app.data.entity.ContactEntity
import com.wishat12.app.data.entity.MessageTemplateEntity
import com.wishat12.app.data.entity.ReminderEntity
import com.wishat12.app.domain.RecurrenceCalculator
import com.wishat12.app.scheduling.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class WishAt12ViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.get(application)
    val reminders = db.reminderDao().observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val templates = db.templateDao().observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val history = db.historyDao().observeRecent().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val contacts = db.contactDao().observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveReminder(reminder: ReminderEntity) = viewModelScope.launch(Dispatchers.IO) {
        val normalized = reminder.copy(updatedAt = System.currentTimeMillis())
        db.reminderDao().upsert(normalized)
        if (normalized.phoneNumber.isNotBlank()) {
            val current = db.contactDao().findByPhone(normalized.phoneNumber)
            if (current == null) db.contactDao().upsert(ContactEntity(name = normalized.recipientName, phoneNumber = normalized.phoneNumber))
            else db.contactDao().upsert(current.copy(name = normalized.recipientName))
        }
        if (normalized.isActive && !normalized.isCompleted) AlarmScheduler(getApplication()).schedule(normalized)
    }

    fun duplicateReminder(reminder: ReminderEntity) = viewModelScope.launch(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val duplicate = reminder.copy(
            id = maxOf(now, reminder.id + 1),
            isActive = true,
            isCompleted = false,
            lastNotifiedAt = null,
            createdAt = now,
            updatedAt = now
        )
        db.reminderDao().upsert(duplicate)
        AlarmScheduler(getApplication()).schedule(duplicate)
    }

    fun deleteReminder(reminder: ReminderEntity) = viewModelScope.launch(Dispatchers.IO) {
        AlarmScheduler(getApplication()).cancel(reminder.id)
        db.reminderDao().deleteById(reminder.id)
    }

    fun completeReminder(reminder: ReminderEntity) = viewModelScope.launch(Dispatchers.IO) {
        AlarmScheduler(getApplication()).cancel(reminder.id)
        db.reminderDao().update(reminder.copy(isActive = false, isCompleted = true, updatedAt = System.currentTimeMillis()))
    }

    fun toggleReminder(reminder: ReminderEntity, active: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        val changed = reminder.copy(isActive = active, isCompleted = false, updatedAt = System.currentTimeMillis())
        db.reminderDao().update(changed)
        if (active) AlarmScheduler(getApplication()).schedule(changed) else AlarmScheduler(getApplication()).cancel(reminder.id)
    }

    fun toggleFavorite(contact: ContactEntity) = viewModelScope.launch(Dispatchers.IO) {
        db.contactDao().upsert(contact.copy(isFavorite = !contact.isFavorite))
    }

    fun saveTemplate(template: MessageTemplateEntity) = viewModelScope.launch(Dispatchers.IO) {
        db.templateDao().upsert(template)
    }

    fun deleteTemplate(template: MessageTemplateEntity) = viewModelScope.launch(Dispatchers.IO) {
        db.templateDao().deleteById(template.id)
    }

    fun rescheduleAll() = viewModelScope.launch(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        db.reminderDao().getActive().forEach { reminder ->
            if (reminder.scheduledAtMillis > now) AlarmScheduler(getApplication()).schedule(reminder)
            else if (reminder.recurrenceType != RecurrenceCalculator.NEVER) {
                val zone = runCatching { ZoneId.of(reminder.timezone) }.getOrDefault(ZoneId.systemDefault())
                val next = RecurrenceCalculator.nextOccurrence(reminder.recurrenceAnchorMillis, reminder.recurrenceType, zone, now)
                if (next != null) {
                    val local = Instant.ofEpochMilli(next).atZone(zone)
                    val updated = reminder.copy(
                        scheduledDate = local.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        scheduledTime = local.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                        scheduledAtMillis = next
                    )
                    db.reminderDao().update(updated)
                    AlarmScheduler(getApplication()).schedule(updated)
                }
            }
        }
    }
}
