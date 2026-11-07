package com.wishat12.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wishat12.app.data.dao.ContactDao
import com.wishat12.app.data.dao.HistoryDao
import com.wishat12.app.data.dao.ReminderDao
import com.wishat12.app.data.dao.TemplateDao
import com.wishat12.app.data.entity.ContactEntity
import com.wishat12.app.data.entity.MessageHistoryEntity
import com.wishat12.app.data.entity.MessageTemplateEntity
import com.wishat12.app.data.entity.ReminderEntity

@Database(
    entities = [ReminderEntity::class, MessageTemplateEntity::class, ContactEntity::class, MessageHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun templateDao(): TemplateDao
    abstract fun contactDao(): ContactDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "wishat12.db"
            ).build().also { INSTANCE = it }
        }
    }
}
