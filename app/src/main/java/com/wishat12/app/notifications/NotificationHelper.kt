package com.wishat12.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wishat12.app.MainActivity
import com.wishat12.app.R
import com.wishat12.app.data.entity.ReminderEntity
import com.wishat12.app.whatsapp.WhatsAppLauncher

object NotificationHelper {
    const val CHANNEL_ID = "wishat12_reminders"
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Scheduled wishes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for your scheduled wishes and important dates"
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun showReminder(context: Context, reminder: ReminderEntity): Boolean {
        createChannel(context)
        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) return false
        val openApp = PendingIntent.getActivity(
            context,
            reminder.id.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("reminder_id", reminder.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val whatsappAction = WhatsAppLauncher.buildUrl(reminder.phoneNumber, reminder.message)?.let { url ->
            PendingIntent.getActivity(
                context,
                (reminder.id xor 0x57A12L).hashCode(),
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val title = "${reminder.occasionType} · ${reminder.recipientName}"
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(reminder.message.take(120))
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminder.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(whatsappAction ?: openApp)
        if (whatsappAction != null) {
            builder.addAction(android.R.drawable.ic_menu_send, "Open WhatsApp", whatsappAction)
        }
        return try {
            notificationManager.notify(reminder.id.hashCode(), builder.build())
            true
        } catch (_: SecurityException) {
            // Notification permission may have been revoked after the alarm was scheduled.
            false
        }
    }

    fun showMissed(context: Context, reminder: ReminderEntity) {
        createChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("You missed a reminder")
            .setContentText("${reminder.occasionType} for ${reminder.recipientName}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Your reminder was due while this phone was unavailable. Open WishAt12 to review it. No WhatsApp message was sent."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        try { NotificationManagerCompat.from(context).notify((reminder.id xor 0x771L).hashCode(), notification) }
        catch (_: SecurityException) { }
    }
}
