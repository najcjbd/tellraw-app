package com.tellraw.app.data.repository

import com.tellraw.app.data.local.AppDatabase
import com.tellraw.app.data.local.CommandHistory
import com.tellraw.app.data.remote.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TellrawRepository @Inject constructor(
    private val apiService: ApiService,
    private val database: AppDatabase
) {
    
    private val commandHistoryDao = database.commandHistoryDao()
    
    // 验证选择器
    suspend fun validateSelector(selector: String): Result<SelectorValidationResult> {
        return try {
            val response = apiService.validateSelector(selector)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "验证失败"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("网络错误: ${e.message}"))
        } catch (e: HttpException) {
            Result.failure(Exception("服务器错误: ${e.message}"))
        }
    }
    
    // 获取命令建议
    suspend fun getCommandSuggestions(partialCommand: String): Result<List<String>> {
        return try {
            val response = apiService.getCommandSuggestions(partialCommand)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "获取建议失败"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("网络错误: ${e.message}"))
        } catch (e: HttpException) {
            Result.failure(Exception("服务器错误: ${e.message}"))
        }
    }
    
    // 同步命令到云端
    suspend fun syncCommand(command: String, version: String): Result<SyncResult> {
        return try {
            val request = SyncCommandRequest(
                command = command,
                version = version,
                timestamp = System.currentTimeMillis()
            )
            val response = apiService.syncCommand(request)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "同步失败"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("网络错误: ${e.message}"))
        } catch (e: HttpException) {
            Result.failure(Exception("服务器错误: ${e.message}"))
        }
    }
    
    // 检查版本更新
    suspend fun checkVersion(): Result<VersionInfo> {
        return try {
            val response = apiService.checkVersion()
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "检查版本失败"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("网络错误: ${e.message}"))
        } catch (e: HttpException) {
            Result.failure(Exception("服务器错误: ${e.message}"))
        }
    }
    
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
}