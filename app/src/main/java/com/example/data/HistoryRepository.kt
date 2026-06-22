package com.example.data

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {
    val allHistory: Flow<List<HistoryEntry>> = historyDao.getAllHistory()
    val totalBlocksCount: Flow<Int> = historyDao.getTotalBlocksCount()

    suspend fun insert(entry: HistoryEntry) {
        historyDao.insertEntry(entry)
    }

    suspend fun clearAll() {
        historyDao.clearHistory()
    }
}
