package com.wishat12.app.util

import android.content.Context
import android.net.Uri
import com.wishat12.app.data.database.AppDatabase
import androidx.room.withTransaction
import com.wishat12.app.data.entity.ContactEntity
import com.wishat12.app.data.entity.MessageHistoryEntity
import com.wishat12.app.data.entity.MessageTemplateEntity
import com.wishat12.app.data.entity.ReminderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object BackupManager {
    suspend fun export(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val db = AppDatabase.get(context)
        val root = JSONObject()
        root.put("format", "WishAt12Backup")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("reminders", JSONArray().apply { db.reminderDao().getAll().forEach { put(it.toJson()) } })
        root.put("templates", JSONArray().apply { db.templateDao().getAll().forEach { put(it.toJson()) } })
        root.put("contacts", JSONArray().apply { db.contactDao().getAll().forEach { put(it.toJson()) } })
        root.put("history", JSONArray().apply { db.historyDao().getAll().forEach { put(it.toJson()) } })
        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(root.toString(2)) }
            ?: error("Could not open the selected backup destination")
    }

    suspend fun import(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Could not read the selected backup")
        val root = JSONObject(text)
        require(root.optString("format") == "WishAt12Backup") { "This is not a WishAt12 backup file." }
        val db = AppDatabase.get(context)
        val reminders = root.optJSONArray("reminders").toModels { it.toReminder() }
        val templates = root.optJSONArray("templates").toModels { it.toTemplate() }
        val contacts = root.optJSONArray("contacts").toModels { it.toContact() }
        val history = root.optJSONArray("history").toModels { it.toHistory() }
        db.withTransaction {
            db.reminderDao().clear(); db.templateDao().clear(); db.contactDao().clear(); db.historyDao().clear()
            db.reminderDao().insertAll(reminders)
            db.templateDao().insertAll(templates)
            db.contactDao().insertAll(contacts)
            db.historyDao().insertAll(history)
        }
    }

    private fun ReminderEntity.toJson() = JSONObject().apply {
        put("id", id); put("recipientName", recipientName); put("phoneNumber", phoneNumber); put("occasionType", occasionType)
        put("customOccasion", customOccasion); put("message", message); put("scheduledDate", scheduledDate); put("scheduledTime", scheduledTime)
        put("timezone", timezone); put("scheduledAtMillis", scheduledAtMillis); put("recurrenceAnchorMillis", recurrenceAnchorMillis); put("recurrenceType", recurrenceType); put("notes", notes)
        put("isActive", isActive); put("isCompleted", isCompleted); put("notificationEnabled", notificationEnabled)
        put("lastNotifiedAt", lastNotifiedAt ?: JSONObject.NULL); put("createdAt", createdAt); put("updatedAt", updatedAt)
    }
    private fun MessageTemplateEntity.toJson() = JSONObject().apply { put("id", id); put("title", title); put("category", category); put("content", content); put("createdAt", createdAt) }
    private fun ContactEntity.toJson() = JSONObject().apply { put("id", id); put("name", name); put("phoneNumber", phoneNumber); put("isFavorite", isFavorite) }
    private fun MessageHistoryEntity.toJson() = JSONObject().apply { put("id", id); put("reminderId", reminderId); put("recipientName", recipientName); put("occasion", occasion); put("message", message); put("firedAt", firedAt); put("status", status) }

    private fun JSONObject.toReminder() = ReminderEntity(
        id = getLong("id"), recipientName = getString("recipientName"), phoneNumber = optString("phoneNumber"),
        occasionType = optString("occasionType", "Birthday"), customOccasion = optString("customOccasion"), message = getString("message"),
        scheduledDate = getString("scheduledDate"), scheduledTime = getString("scheduledTime"), timezone = optString("timezone", "UTC"),
        scheduledAtMillis = getLong("scheduledAtMillis"), recurrenceAnchorMillis = optLong("recurrenceAnchorMillis", getLong("scheduledAtMillis")), recurrenceType = optString("recurrenceType", "NEVER"), notes = optString("notes"),
        isActive = optBoolean("isActive", true), isCompleted = optBoolean("isCompleted"), notificationEnabled = optBoolean("notificationEnabled", true),
        lastNotifiedAt = if (isNull("lastNotifiedAt")) null else optLong("lastNotifiedAt"), createdAt = optLong("createdAt"), updatedAt = optLong("updatedAt")
    )
    private fun JSONObject.toTemplate() = MessageTemplateEntity(getLong("id"), getString("title"), getString("category"), getString("content"), optLong("createdAt"))
    private fun JSONObject.toContact() = ContactEntity(getLong("id"), getString("name"), getString("phoneNumber"), optBoolean("isFavorite"))
    private fun JSONObject.toHistory() = MessageHistoryEntity(getLong("id"), getLong("reminderId"), getString("recipientName"), getString("occasion"), getString("message"), getLong("firedAt"), getString("status"))

    private fun <T> JSONArray?.toModels(block: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return (0 until length()).map { block(getJSONObject(it)) }
    }
}
