package com.wishat12.app.scheduling

import android.content.Context
import com.wishat12.app.data.database.AppDatabase
import com.wishat12.app.data.entity.MessageHistoryEntity
import com.wishat12.app.data.entity.ReminderEntity
import com.wishat12.app.domain.RecurrenceCalculator
import com.wishat12.app.domain.ReminderDeliveryPolicy
import com.wishat12.app.notifications.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ReminderProcessor {
    suspend fun process(context: Context, reminderId: Long) = withContext(Dispatchers.IO) {
        val db = AppDatabase.get(context)
        val reminder = db.reminderDao().getById(reminderId) ?: return@withContext
        if (!reminder.isActive || reminder.isCompleted) return@withContext
        val dueAt = reminder.scheduledAtMillis
        if (!ReminderDeliveryPolicy.shouldProcess(reminder.isActive, reminder.isCompleted, reminder.lastNotifiedAt, dueAt)) return@withContext
        val now = System.currentTimeMillis()
        if (dueAt > now) {
            AlarmScheduler(context).schedule(reminder)
            return@withContext
        }
        val notificationShown = reminder.notificationEnabled && NotificationHelper.showReminder(context, reminder)
        val deliveryStatus = when {
            !reminder.notificationEnabled -> "NOTIFICATION_DISABLED"
            notificationShown -> "NOTIFICATION_SHOWN"
            else -> "NOTIFICATION_BLOCKED"
        }
        db.historyDao().insert(
            MessageHistoryEntity(
                reminderId = reminder.id,
                recipientName = reminder.recipientName,
                occasion = reminder.customOccasion.ifBlank { reminder.occasionType },
                message = reminder.message,
                firedAt = now,
                status = deliveryStatus
            )
        )
        val zone = runCatching { ZoneId.of(reminder.timezone) }.getOrDefault(ZoneId.systemDefault())
        val nextAt = RecurrenceCalculator.nextOccurrence(reminder.recurrenceAnchorMillis, reminder.recurrenceType, zone, now)
        val updated = if (nextAt != null) {
            val local = Instant.ofEpochMilli(nextAt).atZone(zone)
            reminder.copy(
                scheduledDate = local.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                scheduledTime = local.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                scheduledAtMillis = nextAt,
                lastNotifiedAt = dueAt,
                updatedAt = now
            )
        } else {
            reminder.copy(isActive = false, lastNotifiedAt = dueAt, updatedAt = now)
        }
        db.reminderDao().update(updated)
        AlarmScheduler(context).cancel(reminder.id)
        if (updated.isActive) AlarmScheduler(context).schedule(updated)
    }

    suspend fun recover(context: Context, timezoneChanged: Boolean = false) = withContext(Dispatchers.IO) {
        val db = AppDatabase.get(context)
        val scheduler = AlarmScheduler(context)
        val now = System.currentTimeMillis()
        val active = db.reminderDao().getActive()
        val currentZone = ZoneId.systemDefault()
        active.forEach { reminder ->
            val storedZone = runCatching { ZoneId.of(reminder.timezone) }.getOrDefault(currentZone)
            val needsRebase = timezoneChanged || storedZone.id != currentZone.id
            if (needsRebase) {
                val recalculated = runCatching {
                    RecurrenceCalculator.nextDateTimeFromFields(reminder.scheduledDate, reminder.scheduledTime, currentZone)
                }.getOrDefault(reminder.scheduledAtMillis)
                val anchorLocal = Instant.ofEpochMilli(reminder.recurrenceAnchorMillis).atZone(storedZone).toLocalDateTime()
                val newAnchor = anchorLocal.atZone(currentZone).toInstant().toEpochMilli()
                val changed = reminder.copy(
                    timezone = currentZone.id,
                    scheduledAtMillis = recalculated,
                    recurrenceAnchorMillis = newAnchor,
                    updatedAt = now
                )
                db.reminderDao().update(changed)
                recoverOne(context, changed, scheduler, now)
            } else {
                recoverOne(context, reminder, scheduler, now)
            }
        }
    }

    private suspend fun recoverOne(context: Context, reminder: ReminderEntity, scheduler: AlarmScheduler, now: Long) {
        if (reminder.scheduledAtMillis > now) {
            scheduler.schedule(reminder)
        } else if (reminder.recurrenceType != RecurrenceCalculator.NEVER) {
            val zone = runCatching { ZoneId.of(reminder.timezone) }.getOrDefault(ZoneId.systemDefault())
            val nextAt = RecurrenceCalculator.nextOccurrence(reminder.recurrenceAnchorMillis, reminder.recurrenceType, zone, now)
            if (nextAt != null) {
                val local = Instant.ofEpochMilli(nextAt).atZone(zone)
                val updated = reminder.copy(
                    scheduledDate = local.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    scheduledTime = local.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                    scheduledAtMillis = nextAt,
                    updatedAt = now
                )
                AppDatabase.get(context).reminderDao().update(updated)
                scheduler.schedule(updated)
            }
        } else {
            val age = now - reminder.scheduledAtMillis
            if (ReminderDeliveryPolicy.missedSingleShot(age) == ReminderDeliveryPolicy.MissedSingleShot.SHOW_CATCH_UP) {
                NotificationHelper.showMissed(context, reminder)
            }
            AppDatabase.get(context).reminderDao().update(reminder.copy(isActive = false, updatedAt = now))
            scheduler.cancel(reminder.id)
        }
    }
}
