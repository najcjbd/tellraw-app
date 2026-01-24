package com.tellraw.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 历史记录项数据类
 */
data class HistoryItem(
    val selector: String,
    val message: String,
    val javaCommand: String,
    val bedrockCommand: String,
    val timestamp: Long
)

/**
 * 历史记录Repository
 * 负责处理历史记录txt文件的读写
 */
@Singleton
class HistoryRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val DEFAULT_FILENAME = "TellrawCommand.txt"
        private const val CONFIG_FILENAME = "tellraw_config.json"
        private const val SEPARATOR = "========================================"
        private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
    }
    
    private val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
    
    // 存储目录路径（用户通过文件管理器选择）
    private var _storageUri: MutableStateFlow<String?> = MutableStateFlow(null)
    val storageUri: Flow<String?> = _storageUri.asStateFlow()
    
    // 存储文件名
    private var _storageFilename: MutableStateFlow<String> = MutableStateFlow(DEFAULT_FILENAME)
    val storageFilename: Flow<String> = _storageFilename.asStateFlow()
    
    // 历史记录列表缓存
    private val _historyList = MutableStateFlow<List<HistoryItem>>(emptyList())
    val historyList: Flow<List<HistoryItem>> = _historyList.asStateFlow()
    
    /**
     * 初始化配置
     */
    suspend fun init() {
        loadConfig()
        loadHistory()
    }
    
    /**
     * 加载配置
     */
    private suspend fun loadConfig() {
        withContext(Dispatchers.IO) {
            try {
                val configFile = File(context.filesDir, CONFIG_FILENAME)
                if (configFile.exists()) {
                    val json = configFile.readText()
                    val storageUri = extractJsonValue(json, "history_storage_uri")
                    val storageFilename = extractJsonValue(json, "history_storage_filename")
                    
                    _storageUri.value = storageUri?.takeIf { it.isNotEmpty() }
                    _storageFilename.value = storageFilename?.takeIf { it.isNotEmpty() } ?: DEFAULT_FILENAME
                }
            } catch (e: Exception) {
                // 加载失败，使用默认值
            }
        }
    }
    
    /**
     * 保存配置
     */
    suspend fun saveConfig() {
        withContext(Dispatchers.IO) {
            try {
                val json = buildConfigJson()
                val configFile = File(context.filesDir, CONFIG_FILENAME)
                configFile.writeText(json)
            } catch (e: Exception) {
                // 保存失败
            }
        }
    }
    
    /**
     * 构建配置JSON
     */
    private fun buildConfigJson(): String {
        val uri = _storageUri.value ?: ""
        val filename = _storageFilename.value
        return """
            {
              "history_storage_uri": "$uri",
              "history_storage_filename": "$filename"
            }
        """.trimIndent()
    }
    
    /**
     * 从JSON中提取值
     */
    private fun extractJsonValue(jsonString: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        val match = pattern.find(jsonString)
        return match?.groupValues?.get(1)?.trim()
    }
    
    /**
     * 获取历史记录文件
     */
    private fun getHistoryFile(): File {
        val uri = _storageUri.value
        val filename = _storageFilename.value
        
        return if (uri != null) {
            // 用户选择了存储目录，使用文件路径
            File(context.filesDir, "${filename}_cache") // 临时缓存文件
        } else {
            // 未选择存储目录，使用应用沙盒
            File(context.filesDir, filename)
        }
    }
    
    /**
     * 加载历史记录
     */
    suspend fun loadHistory() {
        withContext(Dispatchers.IO) {
            try {
                val file = getHistoryFile()
                if (file.exists()) {
                    val content = file.readText()
                    val history = parseHistoryContent(content)
                    _historyList.value = history
                } else {
                    _historyList.value = emptyList()
                }
            } catch (e: Exception) {
                _historyList.value = emptyList()
            }
        }
    }
    
    /**
     * 解析历史记录内容
     */
    private fun parseHistoryContent(content: String): List<HistoryItem> {
        val history = mutableListOf<HistoryItem>()
        val sections = content.split(SEPARATOR)
        
        for (section in sections) {
            if (section.trim().isEmpty()) continue
            
            try {
                val selector = extractLineValue(section, "命令：")
                val message = extractLineValue(section, "消息：")
                val javaCommand = extractLineValue(section, "Java版：")
                val bedrockCommand = extractLineValue(section, "基岩版：")
                val timeText = extractLineValue(section, "时间：")
                
                if (selector != null && message != null && javaCommand != null && bedrockCommand != null && timeText != null) {
                    val timestamp = parseTimestamp(timeText)
                    history.add(HistoryItem(selector, message, javaCommand, bedrockCommand, timestamp))
                }
            } catch (e: Exception) {
                // 跳过解析失败的记录
            }
        }
        
        return history
    }
    
    /**
     * 从内容中提取指定行的值
     */
    private fun extractLineValue(content: String, prefix: String): String? {
        val lines = content.lines()
        for (line in lines) {
            if (line.trim().startsWith(prefix)) {
                return line.substring(prefix.length).trim()
            }
        }
        return null
    }
    
    /**
     * 解析时间戳
     */
    private fun parseTimestamp(timeText: String): Long {
        return try {
            dateFormat.parse(timeText)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    
    /**
     * 保存历史记录
     */
    suspend fun saveHistory(historyList: List<HistoryItem>) {
        withContext(Dispatchers.IO) {
            try {
                val content = buildHistoryContentInternal(historyList)
                val file = getHistoryFile()
                file.writeText(content)
                _historyList.value = historyList
                
                saveConfig()
            } catch (e: Exception) {
                // 保存失败
            }
        }
    }
    
    /**
     * 构建历史记录内容
     */
    private fun buildHistoryContentInternal(historyList: List<HistoryItem>): String {
        val content = StringBuilder()
        
        for (item in historyList) {
            content.append(SEPARATOR).append("\n")
            content.append("命令：").append(item.selector).append("\n")
            content.append("消息：").append(item.message).append("\n")
            content.append("Java版：").append(item.javaCommand).append("\n")
            content.append("基岩版：").append(item.bedrockCommand).append("\n")
            content.append("时间：").append(dateFormat.format(Date(item.timestamp))).append("\n")
            content.append(SEPARATOR).append("\n")
        }
        
        return content.toString()
    }
    
    /**
     * 添加历史记录
     */
    suspend fun addHistory(item: HistoryItem) {
        val currentList = _historyList.value.toMutableList()
        currentList.add(0, item) // 添加到开头
        saveHistory(currentList)
    }
    
    /**
     * 删除历史记录
     */
    suspend fun deleteHistory(item: HistoryItem) {
        val currentList = _historyList.value.toMutableList()
        currentList.remove(item)
        saveHistory(currentList)
    }
    
    /**
     * 清空历史记录
     */
    suspend fun clearHistory() {
        saveHistory(emptyList())
    }
    
    /**
     * 搜索历史记录
     */
    suspend fun searchHistory(query: String): List<HistoryItem> {
        return _historyList.value.filter { item ->
            item.selector.contains(query, ignoreCase = true) ||
            item.message.contains(query, ignoreCase = true)
        }
    }
    
    /**
     * 设置存储目录URI
     */
    suspend fun setStorageUri(uri: String) {
        _storageUri.value = uri
        saveConfig()
        loadHistory()
    }
    
    /**
     * 设置存储文件名
     */
    suspend fun setStorageFilename(filename: String) {
        _storageFilename.value = filename
        saveConfig()
        loadHistory()
    }
    
    /**
     * 清除存储设置
     */
    suspend fun clearStorageSettings() {
        _storageUri.value = null
        _storageFilename.value = DEFAULT_FILENAME
        saveConfig()
        loadHistory()
    }
    
    /**
     * 构建历史记录内容
     * 这个方法生成历史记录的文本内容，由调用者负责写入文件
     */
    suspend fun buildHistoryContent(historyList: List<HistoryItem>): String {
        return withContext(Dispatchers.IO) {
            buildHistoryContentInternal(historyList)
        }
    }
}
