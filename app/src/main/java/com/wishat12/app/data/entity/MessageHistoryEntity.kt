package com.wishat12.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_history")
data class MessageHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reminderId: Long,
    val recipientName: String,
    val occasion: String,
    val message: String,
    val firedAt: Long,
    val status: String
)
