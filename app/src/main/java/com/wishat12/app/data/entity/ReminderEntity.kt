package com.wishat12.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: Long = System.currentTimeMillis(),
    val recipientName: String,
    val phoneNumber: String = "",
    val occasionType: String = "Birthday",
    val customOccasion: String = "",
    val message: String,
    val scheduledDate: String,
    val scheduledTime: String,
    val timezone: String,
    val scheduledAtMillis: Long,
    val recurrenceAnchorMillis: Long = scheduledAtMillis,
    val recurrenceType: String = "NEVER",
    val notes: String = "",
    val isActive: Boolean = true,
    val isCompleted: Boolean = false,
    val notificationEnabled: Boolean = true,
    val lastNotifiedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
