package com.tellraw.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    val timestamp: Long,
    /** 内部带标记的组件文本（Base64 后写入历史文件）；空 = 旧记录。 */
    val markers: String = ""
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
        private const val TAG = "HistoryRepository"
        private const val DEFAULT_FILENAME = "TellrawCommand.txt"
        private const val CONFIG_FILENAME = "tellraw_config.json"
        private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
        
        // 特殊符号：用于标记历史记录的开始和结束
        // \u200B = 零宽度空格（Zero Width Space）- 不可见
        // \u200C = 零宽度非连接符（Zero Width Non-Joiner）- 不可见
        private const val START_MARKER = "\u200B"
        private const val END_MARKER = "\u200C"
    }
    
    /** SimpleDateFormat 不是线程安全的；每次新建，避免并发读写历史时格式化出错。 */
    private fun formatTimestamp(timestamp: Long): String =
        SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(Date(timestamp))
    
    /** 历史文件的读-改-写需要串行，避免“删除时正好在追加”导致丢记录。 */
    private val fileMutex = Mutex()
    
    // 存储目录路径（用户通过文件管理器选择）
    private var _storageUri: MutableStateFlow<String?> = MutableStateFlow(null)
    val storageUri: Flow<String?> = _storageUri.asStateFlow()
    
    // 存储文件名
    private var _storageFilename: MutableStateFlow<String> = MutableStateFlow(DEFAULT_FILENAME)
    val storageFilename: Flow<String> = _storageFilename.asStateFlow()
    
    // 历史记录列表缓存
    private val _historyList = MutableStateFlow<List<HistoryItem>>(emptyList())
    val historyList: Flow<List<HistoryItem>> = _historyList.asStateFlow()
    
    // 配置文件最后修改时间缓存
    private var _configFileLastModified = 0L
    
    /**
     * 初始化配置
     */
    suspend fun init() {
        loadConfig()
        loadHistory()
    }
    
    /**
     * 加载配置
     * 只有当配置文件被修改时才重新读取，避免不必要的文件 I/O
     */
    suspend fun loadConfig() {
        withContext(Dispatchers.IO) {
            try {
                val configFile = File(context.filesDir, CONFIG_FILENAME)
                if (configFile.exists()) {
                    val lastModified = configFile.lastModified()
                    // 只有当配置文件被修改时才重新加载
                    if (lastModified > _configFileLastModified) {
                        val json = configFile.readText()
                        val storageUri = extractJsonValue(json, "history_storage_uri")
                        val storageFilename = extractJsonValue(json, "history_storage_filename")

                        _storageUri.value = storageUri?.takeIf { it.isNotEmpty() }
                        _storageFilename.value = storageFilename?.takeIf { it.isNotEmpty() } ?: DEFAULT_FILENAME
                        _configFileLastModified = lastModified
                    }
                }
            } catch (e: Exception) {
                // 加载失败，使用默认值
            }
        }
    }
    
    /**
     * 保存配置。
     *
     * 注意：本仓库与 [SettingsRepository] 共用 `tellraw_config.json`（历史存储位置本来就是设置的一部分）。
     * 旧实现**整份重写**、只写这两个键 -> 一旦调用就会把别的设置全清掉（"清空全部历史"会触发它，
     * 导致其余设置在下次启动悄悄回默认）。现在改为**只增改这两个键、保留文件里其它键**。
     */
    suspend fun saveConfig() {
        withContext(Dispatchers.IO) {
            try {
                val configFile = File(context.filesDir, CONFIG_FILENAME)
                val existing = if (configFile.exists()) configFile.readText() else ""
                configFile.writeText(mergeConfigJson(existing))
            } catch (e: Exception) {
                // 保存失败
            }
        }
    }

    /** 把历史存储两个键并入现有 JSON（不动其它键）。文件为空/损坏时按新文件生成。 */
    private fun mergeConfigJson(existing: String): String {
        val keys = listOf(
            "history_storage_uri" to (_storageUri.value ?: ""),
            "history_storage_filename" to _storageFilename.value
        )
        fun freshJson(): String =
            "{\n" + keys.joinToString(",\n") { "  \"${it.first}\": \"${it.second}\"" } + "\n}"

        if (existing.isBlank()) return freshJson()
        var out = existing
        for ((k, v) in keys) {
            val re = Regex("\"${Regex.escape(k)}\"\\s*:\\s*\"[^\"]*\"")
            out = when {
                re.containsMatchIn(out) -> re.replace(out) { "\"$k\": \"$v\"" }
                else -> {
                    val close = out.lastIndexOf('}')
                    if (close < 0) return freshJson()
                    val head = out.substring(0, close).trimEnd()
                    val sep = if (head.endsWith("{")) "\n" else ",\n"
                    head + sep + "  \"$k\": \"$v\"\n" + out.substring(close)
                }
            }
        }
        return out
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
     * 每次调用都会从配置文件重新加载存储路径，确保与 SettingsRepository 同步
     * 优先使用用户选择的目录，如果文件不存在则创建它
     * 如果用户没有选择目录或没有权限，返回沙盒目录的文件
     */
    private suspend fun getHistoryFile(): File? {
        // 每次获取文件时都重新加载配置，确保存储路径是最新的
        loadConfig()
        val uri = _storageUri.value
        val filename = _storageFilename.value
        
        return if (uri != null && uri.isNotEmpty()) {
            // 用户选择了存储目录
            val externalFile = File(uri, filename)
            if (!externalFile.exists()) {
                try {
                    // 文件不存在，创建它
                    externalFile.createNewFile()
                    externalFile
                } catch (e: Exception) {
                    // 创建失败，返回沙盒文件
                    val sandboxFile = File(context.filesDir, filename)
                    if (!sandboxFile.exists()) {
                        try {
                            sandboxFile.createNewFile()
                        } catch (e: Exception) {
                            // 沙盒文件也创建失败
                            return null
                        }
                    }
                    sandboxFile
                }
            } else {
                // 文件已存在，检查是否有写权限
                if (externalFile.canWrite()) {
                    externalFile
                } else {
                    // 没有写权限，返回沙盒文件
                    val sandboxFile = File(context.filesDir, filename)
                    if (!sandboxFile.exists()) {
                        try {
                            sandboxFile.createNewFile()
                        } catch (e: Exception) {
                            // 沙盒文件也创建失败
                            return null
                        }
                    }
                    sandboxFile
                }
            }
        } else {
            // 未选择存储目录，使用沙盒文件
            val sandboxFile = File(context.filesDir, filename)
            if (!sandboxFile.exists()) {
                try {
                    sandboxFile.createNewFile()
                } catch (e: Exception) {
                    // 沙盒文件创建失败
                    return null
                }
            }
            sandboxFile
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
     * 格式：\u200B时间:年:月:日 时:分:秒
     *  输入文本:输入的文本
     *  JAVA版输出:JAVA版的指令
     *  基岩版输出:基岩版的指令\u200C
     */
    internal fun parseHistoryContent(content: String): List<HistoryItem> {
        val history = mutableListOf<HistoryItem>()
        
        // 只把"标记 + 时间:"当记录头；消息正文里出现单个特殊符号时不会被切成两段
        val sections = content.split(Regex("(?=${START_MARKER}时间:)"))
        
        for (section in sections) {
            if (section.trim().isEmpty()) continue
            
            try {
                // 只去掉记录末尾的结束标记，保留正文里可能出现的同名符号
                val sectionContent = section.substringBeforeLast(END_MARKER, section)
                
                // 使用前缀匹配来解析各个字段
                var timestamp = System.currentTimeMillis()
                var message = ""
                var javaCommand = ""
                var bedrockCommand = ""
                var markers = ""
                
                // 按行分割，但保留换行符
                val lines = sectionContent.split("\n")
                var currentField = ""
                var currentContent = mutableListOf<String>()
                
                for (line in lines) {
                    val trimmedLine = line.trim()
                    when {
                        trimmedLine.startsWith("时间:") -> {
                            // 保存上一个字段
                            when (currentField) {
                                "输入文本" -> message = currentContent.joinToString("\n").trim()
                                "JAVA版输出" -> javaCommand = currentContent.joinToString("\n").trim()
                                "基岩版输出" -> bedrockCommand = currentContent.joinToString("\n").trim()
                            }
                            // 开始新字段
                            currentField = "时间"
                            currentContent = mutableListOf(trimmedLine.substringAfter("时间:"))
                        }
                        trimmedLine.startsWith("输入文本:") -> {
                            // 保存上一个字段
                            when (currentField) {
                                "时间" -> timestamp = parseTimestamp(currentContent.joinToString(""))
                                "JAVA版输出" -> javaCommand = currentContent.joinToString("\n").trim()
                                "基岩版输出" -> bedrockCommand = currentContent.joinToString("\n").trim()
                            }
                            // 开始新字段
                            currentField = "输入文本"
                            currentContent = mutableListOf(trimmedLine.substringAfter("输入文本:"))
                        }
                        trimmedLine.startsWith("JAVA版输出:") -> {
                            // 保存上一个字段
                            when (currentField) {
                                "时间" -> timestamp = parseTimestamp(currentContent.joinToString(""))
                                "输入文本" -> message = currentContent.joinToString("\n").trim()
                                "基岩版输出" -> bedrockCommand = currentContent.joinToString("\n").trim()
                            }
                            // 开始新字段
                            currentField = "JAVA版输出"
                            currentContent = mutableListOf(trimmedLine.substringAfter("JAVA版输出:"))
                        }
                        trimmedLine.startsWith("基岩版输出:") -> {
                            // 保存上一个字段
                            when (currentField) {
                                "时间" -> timestamp = parseTimestamp(currentContent.joinToString(""))
                                "输入文本" -> message = currentContent.joinToString("\n").trim()
                                "JAVA版输出" -> javaCommand = currentContent.joinToString("\n").trim()
                            }
                            // 开始新字段
                            currentField = "基岩版输出"
                            currentContent = mutableListOf(trimmedLine.substringAfter("基岩版输出:"))
                        }
                        trimmedLine.startsWith("标记文本:") -> {
                            when (currentField) {
                                "时间" -> timestamp = parseTimestamp(currentContent.joinToString(""))
                                "输入文本" -> message = currentContent.joinToString("\n").trim()
                                "JAVA版输出" -> javaCommand = currentContent.joinToString("\n").trim()
                                "基岩版输出" -> bedrockCommand = currentContent.joinToString("\n").trim()
                            }
                            currentField = "标记文本"
                            markers = try {
                                String(
                                    android.util.Base64.decode(
                                        trimmedLine.substringAfter("标记文本:").trim(),
                                        android.util.Base64.NO_WRAP
                                    ),
                                    Charsets.UTF_8
                                )
                            } catch (e: Exception) { "" }
                            currentContent = mutableListOf()
                        }
                        currentField.isNotEmpty() -> {
                            // 添加到当前字段的内容
                            currentContent.add(line)
                        }
                    }
                }
                
                // 保存最后一个字段
                when (currentField) {
                    "时间" -> timestamp = parseTimestamp(currentContent.joinToString(""))
                    "输入文本" -> message = currentContent.joinToString("\n").trim()
                    "JAVA版输出" -> javaCommand = currentContent.joinToString("\n").trim()
                    "基岩版输出" -> bedrockCommand = currentContent.joinToString("\n").trim()
                }
                
                // 检查是否有效
                if (message.isNotEmpty() || javaCommand.isNotEmpty() || bedrockCommand.isNotEmpty()) {
                    // 选择器从Java命令中提取
                    val selector = extractSelectorFromCommand(javaCommand)
                    history.add(HistoryItem(selector, message, javaCommand, bedrockCommand, timestamp, markers))
                }
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
            SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).parse(timeText)?.time
                ?: System.currentTimeMillis()
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
            fileMutex.withLock {
                try {
                    val file = getHistoryFile() ?: return@withLock
                    
                    // 按时间从新到旧排序后一次性写入，避免中途失败留下半截文件
                    val sortedList = historyList.sortedByDescending { it.timestamp }
                    val content = buildString {
                        for (item in sortedList) append(buildSingleHistoryItem(item))
                    }
                    file.writeText(content)
                    
                    _historyList.value = sortedList
                    saveConfig()
                } catch (e: Exception) {
                    // 保存失败
                }
            }
        }
    }
    
    /**
     * 添加历史记录（追加模式）
     */
    suspend fun addHistory(item: HistoryItem) {
        withContext(Dispatchers.IO) {
            fileMutex.withLock {
                try {
                    val file = getHistoryFile() ?: return@withLock
                    
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
    }
    
    /**
     * 构建单个历史记录项的内容
     * 新格式：\u200B时间:年:月:日 时:分:秒
     *  输入文本:输入的文本
     *  JAVA版输出:JAVA版的指令
     *  基岩版输出:基岩版的指令\u200C
     */
    fun buildSingleHistoryItem(item: HistoryItem): String {
        val timeText = formatTimestamp(item.timestamp)
        val markersLine = if (item.markers.isNotBlank()) {
            "\n 标记文本:" + android.util.Base64.encodeToString(
                item.markers.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP
            )
        } else ""
        return "${START_MARKER}时间:$timeText\n 输入文本:${item.message}\n JAVA版输出:${item.javaCommand}\n 基岩版输出:${item.bedrockCommand}$markersLine$END_MARKER\n\n"
    }
    
    /**
     * 删除历史记录（根据特殊符号删除）
     */
    suspend fun deleteHistory(item: HistoryItem) {
        withContext(Dispatchers.IO) {
            fileMutex.withLock {
                try {
                    val file = getHistoryFile() ?: return@withLock
                    if (!file.exists()) return@withLock
                    
                    val timeText = formatTimestamp(item.timestamp)
                    
                    var content = file.readText()
                    val original = content
                    val itemContent = buildSingleHistoryItem(item)
                    
                    // 先精确匹配：只删一条，避免同名记录（同一秒内重复生成）被一起删掉
                    val exactIndex = content.indexOf(itemContent)
                    if (exactIndex >= 0) {
                        content = content.removeRange(exactIndex, exactIndex + itemContent.length)
                    } else {
                        // 文件被手工改过时，用“时间 + 输入文本”定位一条
                        val precise = Regex(
                            "${START_MARKER}时间:${Regex.escape(timeText)}\\n 输入文本:${Regex.escape(item.message)}\\n JAVA版输出:[\\s\\S]*?$END_MARKER"
                        )
                        val preciseMatch = precise.find(content)
                        if (preciseMatch != null) {
                            content = content.removeRange(preciseMatch.range)
                        } else {
                            // 最后手段：只按时间删一条，不再一次性删掉同秒的所有记录
                            val fallback = Regex("${START_MARKER}时间:${Regex.escape(timeText)}[\\s\\S]*?$END_MARKER")
                            val fallbackMatch = fallback.find(content)
                            if (fallbackMatch != null) content = content.removeRange(fallbackMatch.range)
                        }
                    }
                    
                    if (content != original) {
                        // 清理多余的空行
                        content = content.replace(Regex("\n{3,}"), "\n\n")
                        file.writeText(content)
                    }
                    
                    // 重新加载历史记录
                    loadHistory()
                } catch (e: Exception) {
                    // 删除历史记录失败
                }
            }
        }
    }
    
    /**
     * 清空历史记录（只删除应用生成的记录）
     * 根据特殊符号删除，不误伤原有内容
     */
    suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            fileMutex.withLock {
                try {
                    // 只匹配真正的记录头（标记 + "时间:"），避免消息正文里的单个特殊符号把整段吃掉
                    val pattern = Regex("${START_MARKER}时间:[\\s\\S]*?$END_MARKER\\n*")
                    fun cleaned(text: String): String = text
                        .replace(pattern, "")
                        .replace(Regex("\n{3,}"), "\n\n")
                        .trimStart('\n')
                    
                    val file = getHistoryFile()
                    if (file != null && file.exists()) {
                        file.writeText(cleaned(file.readText()))
                        _historyList.value = emptyList()
                        saveConfig()
                    } else {
                        // 没有配置存储目录，检查是否需要清空沙盒文件
                        val sandboxFile = File(context.filesDir, DEFAULT_FILENAME)
                        if (sandboxFile.exists()) {
                            sandboxFile.writeText(cleaned(sandboxFile.readText()))
                            _historyList.value = emptyList()
                        }
                    }
                } catch (e: Exception) {
                    // 清空失败
                }
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
