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
        private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
        
        // 特殊符号：用于标记历史记录的开始和结束
        // \u200B = 零宽度空格（Zero Width Space）
        // \u200C = 零宽度非连接符（Zero Width Non-Joiner）
        private const val START_MARKER = " \u200B"
        private const val END_MARKER = " \u200C"
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
     * 只使用用户选择的目录，如果文件不存在则创建它
     * 如果用户没有选择目录，返回null表示无法获取文件
     */
    private fun getHistoryFile(): File? {
        val uri = _storageUri.value
        val filename = _storageFilename.value
        
        return if (uri != null) {
            // 用户选择了存储目录
            val externalFile = File(uri, filename)
            if (!externalFile.exists()) {
                try {
                    // 文件不存在，创建它
                    externalFile.createNewFile()
                } catch (e: Exception) {
                    // 创建失败，返回null
                    return null
                }
            }
            externalFile
        } else {
            // 未选择存储目录，返回null
            null
        }
    }
    
    /**
     * 加载历史记录
     */
    suspend fun loadHistory() {
        withContext(Dispatchers.IO) {
            try {
                val file = getHistoryFile()
                if (file != null && file.exists()) {
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
     * 解析历史记录内容（新格式）
     * 格式：时间:年:月:日 时:分:秒 \u200B
     * 输入文本:输入的文本
     * JAVA版输出:JAVA版的指令
     * 基岩版输出:基岩版的指令 \u200C
     */
    private fun parseHistoryContent(content: String): List<HistoryItem> {
        val history = mutableListOf<HistoryItem>()
        
        // 按开始标记分割
        val sections = content.split(START_MARKER)
        
        for (section in sections) {
            if (section.trim().isEmpty()) continue
            
            try {
                val lines = section.lines().filter { it.isNotEmpty() }
                if (lines.size < 4) continue
                
                // 解析时间行（第一行，格式：时间:年:月:日 时:分:秒）
                val timeLine = lines[0].trim()
                val timestamp = parseTimestamp(timeLine.substringAfter("时间:"))
                
                // 解析输入文本行（格式：输入文本:xxx）
                val message = lines[1].substringAfter("输入文本:").trim()
                
                // 解析Java输出行（格式：JAVA版输出:xxx）
                val javaCommand = lines[2].substringAfter("JAVA版输出:").trim()
                
                // 解析基岩版输出行（格式：基岩版输出:xxx \u200C）
                val bedrockCommand = lines[3].substringAfter("基岩版输出:").replace(END_MARKER, "").trim()
                
                // 选择器从Java命令中提取
                val selector = extractSelectorFromCommand(javaCommand)
                
                history.add(HistoryItem(selector, message, javaCommand, bedrockCommand, timestamp))
            } catch (e: Exception) {
                // 跳过解析失败的记录
            }
        }
        
        // 按时间从新到旧排序
        return history.sortedByDescending { it.timestamp }
    }
    
    /**
     * 从命令中提取选择器
     */
    private fun extractSelectorFromCommand(command: String): String {
        try {
            // 命令格式：tellraw @a {...}
            val parts = command.split(" ", limit = 3)
            if (parts.size >= 2) {
                return parts[1]
            }
        } catch (e: Exception) {
            // 提取失败
        }
        return "@a"
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
     * 保存历史记录（追加模式）
     * 只用于批量保存，单个添加使用addHistory
     */
    suspend fun saveHistory(historyList: List<HistoryItem>) {
        withContext(Dispatchers.IO) {
            try {
                val file = getHistoryFile() ?: return@withContext
                
                // 清空文件并重新写入
                file.writeText("")
                
                // 按时间从新到旧排序并写入
                val sortedList = historyList.sortedByDescending { it.timestamp }
                for (item in sortedList) {
                    val content = buildSingleHistoryItem(item)
                    file.appendText(content)
                }
                
                _historyList.value = sortedList
                saveConfig()
            } catch (e: Exception) {
                // 保存失败
            }
        }
    }
    
    /**
     * 构建历史记录内容（新格式）
     * 格式：时间:年:月:日 时:分:秒 \u200B
     * 输入文本:输入的文本
     * JAVA版输出:JAVA版的指令
     * 基岩版输出:基岩版的指令 \u200C
     */
    private fun buildHistoryContentInternal(historyList: List<HistoryItem>): String {
        val content = StringBuilder()
        
        // 按时间从新到旧排序
        val sortedList = historyList.sortedByDescending { it.timestamp }
        
        for (item in sortedList) {
            val timeText = dateFormat.format(Date(item.timestamp))
            content.append("时间:$timeText$START_MARKER\n")
            content.append("输入文本:").append(item.message).append("\n")
            content.append("JAVA版输出:").append(item.javaCommand).append("\n")
            content.append("基岩版输出:").append(item.bedrockCommand).append(END_MARKER).append("\n\n")
        }
        
        return content.toString()
    }
    
    /**
     * 添加历史记录（追加模式）
     */
    suspend fun addHistory(item: HistoryItem) {
        withContext(Dispatchers.IO) {
            try {
                val file = getHistoryFile() ?: return@withContext
                
                // 如果文件是空的（刚创建），先写入空行
                if (file.length() == 0L) {
                    file.appendText("\n")
                }
                
                val content = buildSingleHistoryItem(item)
                
                // 追加到文件末尾
                file.appendText(content)
                
                // 重新加载历史记录
                loadHistory()
            } catch (e: Exception) {
                // 添加失败
            }
        }
    }
    
    /**
     * 构建单个历史记录项的内容
     * 新格式：时间:年:月:日 时:分:秒 \u200B
     * 输入文本:输入的文本
     * JAVA版输出:JAVA版的指令
     * 基岩版输出:基岩版的指令 \u200C
     */
    fun buildSingleHistoryItem(item: HistoryItem): String {
        val timeText = dateFormat.format(Date(item.timestamp))
        return "时间:$timeText$START_MARKER\n输入文本:${item.message}\nJAVA版输出:${item.javaCommand}\n基岩版输出:${item.bedrockCommand}$END_MARKER\n\n"
    }
    
    /**
     * 删除历史记录（根据特殊符号删除）
     */
    suspend fun deleteHistory(item: HistoryItem) {
        withContext(Dispatchers.IO) {
            try {
                val file = getHistoryFile() ?: return@withContext
                if (!file.exists()) return@withContext
                
                var content = file.readText()
                val itemContent = buildSingleHistoryItem(item)
                
                // 删除匹配的内容
                if (itemContent in content) {
                    content = content.replace(itemContent, "")
                    file.writeText(content)
                }
                
                // 重新加载历史记录
                loadHistory()
            } catch (e: Exception) {
                // 删除失败
            }
        }
    }
    
    /**
     * 清空历史记录（只删除应用生成的记录）
     * 根据特殊符号删除，不误伤原有内容
     */
    suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            try {
                val file = getHistoryFile() ?: return@withContext
                if (!file.exists()) return@withContext
                
                var content = file.readText()
                
                // 删除所有包含开始标记的内容（应用生成的记录）
                // 保留不包含特殊符号的原始内容
                val lines = content.lines().toMutableList()
                val newLines = mutableListOf<String>()
                var skipNext = false
                
                for (i in lines.indices) {
                    if (skipNext) {
                        // 检查是否到达结束标记
                        if (END_MARKER in lines[i]) {
                            skipNext = false
                        }
                        continue
                    }
                    
                    if (START_MARKER in lines[i]) {
                        // 跳过这行及后续内容，直到遇到结束标记
                        skipNext = true
                        continue
                    }
                    
                    newLines.add(lines[i])
                }
                
                file.writeText(newLines.joinToString("\n"))
                
                // 重新加载历史记录
                loadHistory()
            } catch (e: Exception) {
                // 清空失败
            }
        }
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
     * 构建单个历史记录项的内容
     * 这个方法生成单个历史记录的文本内容，由调用者负责写入文件
     */
    suspend fun buildHistoryContent(item: HistoryItem): String {
        return withContext(Dispatchers.IO) {
            buildSingleHistoryItem(item)
        }
    }
}
