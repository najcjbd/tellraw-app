package com.tellraw.app.data.repository

import com.tellraw.app.data.local.AppDatabase
import com.tellraw.app.data.local.CommandHistory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TellrawRepository @Inject constructor(
    private val database: AppDatabase
) {
    
    private val commandHistoryDao = database.commandHistoryDao()
    
    // 本地数据库操作
    
    // 保存命令到历史记录
    suspend fun saveCommandToHistory(
        selector: String,
        message: String,
        javaCommand: String,
        bedrockCommand: String
    ) {
        val history = CommandHistory(
            selector = selector,
            message = message,
            javaCommand = javaCommand,
            bedrockCommand = bedrockCommand,
            timestamp = System.currentTimeMillis()
        )
        commandHistoryDao.insert(history)
    }
    
    // 获取命令历史记录
    fun getCommandHistory(): Flow<List<CommandHistory>> {
        return commandHistoryDao.getAll()
    }
    
    // 删除历史记录
    suspend fun deleteCommandHistory(history: CommandHistory) {
        commandHistoryDao.delete(history)
    }
    
    // 清空历史记录
    suspend fun clearAllHistory() {
        commandHistoryDao.deleteAll()
    }
    
    // 根据ID获取历史记录
    suspend fun getHistoryById(id: Long): CommandHistory? {
        return commandHistoryDao.getById(id)
    }
    
    // 搜索历史记录
    fun searchHistory(query: String): Flow<List<CommandHistory>> {
        return commandHistoryDao.searchByQuery(query)
    }
}