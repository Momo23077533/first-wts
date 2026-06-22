package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM blocking_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: HistoryEntry)

    @Query("DELETE FROM blocking_history")
    suspend fun clearHistory()
    
    @Query("SELECT COUNT(*) FROM blocking_history")
    fun getTotalBlocksCount(): Flow<Int>
}
