package com.wishat12.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.wishat12.app.data.entity.MessageHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM message_history ORDER BY firedAt DESC LIMIT 100")
    fun observeRecent(): Flow<List<MessageHistoryEntity>>

    @Insert
    suspend fun insert(item: MessageHistoryEntity)

    @Insert
    suspend fun insertAll(items: List<MessageHistoryEntity>)

    @Query("SELECT * FROM message_history ORDER BY firedAt DESC")
    suspend fun getAll(): List<MessageHistoryEntity>

    @Query("DELETE FROM message_history")
    suspend fun clear()
}
