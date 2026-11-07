package com.wishat12.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wishat12.app.data.entity.MessageTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM message_templates ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MessageTemplateEntity>>

    @Query("SELECT * FROM message_templates ORDER BY createdAt DESC")
    suspend fun getAll(): List<MessageTemplateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(template: MessageTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MessageTemplateEntity>)

    @Query("DELETE FROM message_templates WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM message_templates")
    suspend fun clear()
}
