package com.wishat12.app.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wishat12.app.data.entity.ReminderEntity

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(reminder: ReminderEntity): Boolean {
        if (!reminder.isActive || reminder.isCompleted) return false
        cancel(reminder.id)
        val pendingIntent = pendingIntent(reminder.id)
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.scheduledAtMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.scheduledAtMillis, pendingIntent)
            }
            true
        } catch (_: SecurityException) {
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.scheduledAtMillis, pendingIntent)
                true
            } catch (_: Exception) { false }
        } catch (_: Exception) { false }
    }

    fun cancel(id: Long) {
        alarmManager.cancel(pendingIntent(id))
    }

    private fun pendingIntent(id: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_FIRE_REMINDER
            putExtra(EXTRA_REMINDER_ID, id)
            data = android.net.Uri.parse("wishat12://reminder/$id")
        }
        return PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_FIRE_REMINDER = "com.wishat12.app.ACTION_FIRE_REMINDER"
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}
