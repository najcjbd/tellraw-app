package com.tellraw.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val CONFIG_FILENAME = "tellraw_config.json"
        private const val KEY_MN_HANDLING_MODE = "mn_handling_mode"
        private const val KEY_MN_MIXED_MODE = "mn_mixed_mode"
        private const val KEY_MN_CF_ENABLED = "mn_cf_enabled"
        private const val KEY_JAVA_BEDROCK_MIXED_MODE = "java_bedrock_mixed_mode"
        private const val KEY_HISTORY_STORAGE_URI = "history_storage_uri"
        private const val KEY_HISTORY_STORAGE_FILENAME = "history_storage_filename"
        private const val VALUE_MODE_FONT = "font"
        private const val VALUE_MODE_COLOR = "color"
        private const val DEFAULT_HISTORY_FILENAME = "TellrawCommand.txt"
    }
    
    // §m§n处理模式
    private val _mnHandlingMode = MutableStateFlow(true) // 默认使用字体方式
    val mnHandlingMode: Flow<Boolean> = _mnHandlingMode.asStateFlow()
    
    // 混合模式开关
    private val _mnMixedMode = MutableStateFlow(false)
    val mnMixedMode: Flow<Boolean> = _mnMixedMode.asStateFlow()
    
    // §m/§n_c/f开关
    private val _mnCFEnabled = MutableStateFlow(false)
    val mnCFEnabled: Flow<Boolean> = _mnCFEnabled.asStateFlow()
    
    // JAVA/基岩混合模式开关
    private val _javaBedrockMixedMode = MutableStateFlow(false)
    val javaBedrockMixedMode: Flow<Boolean> = _javaBedrockMixedMode.asStateFlow()
    
    // 历史记录存储目录URI
    private val _historyStorageUri = MutableStateFlow<String?>(null)
    val historyStorageUri: Flow<String?> = _historyStorageUri.asStateFlow()
    
    // 历史记录存储文件名
    private val _historyStorageFilename = MutableStateFlow(DEFAULT_HISTORY_FILENAME)
    val historyStorageFilename: Flow<String> = _historyStorageFilename.asStateFlow()
    
    /**
     * 初始化配置
     */
    suspend fun init() {
        loadConfig()
    }
    
    /**
     * 加载配置
     * @return 加载后的设置值对象
     */
    suspend fun loadConfig(): LoadedSettings {
        return withContext(Dispatchers.IO) {
            try {
                val configFile = File(context.filesDir, CONFIG_FILENAME)
                if (!configFile.exists()) {
                    return@withContext LoadedSettings(
                        mnHandlingMode = _mnHandlingMode.value,
                        mnMixedMode = _mnMixedMode.value,
                        mnCFEnabled = _mnCFEnabled.value,
                        javaBedrockMixedMode = _javaBedrockMixedMode.value,
                        historyStorageUri = _historyStorageUri.value,
                        historyStorageFilename = _historyStorageFilename.value
                    )
                }
                
                val json = configFile.readText()
                importSettingsFromJson(json)
                
                // 返回加载后的设置值
                LoadedSettings(
                    mnHandlingMode = _mnHandlingMode.value,
                    mnMixedMode = _mnMixedMode.value,
                    mnCFEnabled = _mnCFEnabled.value,
                    javaBedrockMixedMode = _javaBedrockMixedMode.value,
                    historyStorageUri = _historyStorageUri.value,
                    historyStorageFilename = _historyStorageFilename.value
                )
            } catch (e: Exception) {
                // 加载失败，返回当前值
                LoadedSettings(
                    mnHandlingMode = _mnHandlingMode.value,
                    mnMixedMode = _mnMixedMode.value,
                    mnCFEnabled = _mnCFEnabled.value,
                    javaBedrockMixedMode = _javaBedrockMixedMode.value,
                    historyStorageUri = _historyStorageUri.value,
                    historyStorageFilename = _historyStorageFilename.value
                )
            }
        }
    }
    
    /**
     * 加载后的设置值
     */
    data class LoadedSettings(
        val mnHandlingMode: Boolean,
        val mnMixedMode: Boolean,
        val mnCFEnabled: Boolean,
        val javaBedrockMixedMode: Boolean,
        val historyStorageUri: String?,
        val historyStorageFilename: String
    )
    
    /**
     * 保存配置
     */
    suspend fun saveConfig(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val json = exportSettingsAsJson()
                val configFile = File(context.filesDir, CONFIG_FILENAME)
                configFile.writeText(json)
                true
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * 从 JSON 字符串导入配置
     */
    private suspend fun importSettingsFromJson(jsonString: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val mnHandlingMode = extractJsonValue(jsonString, KEY_MN_HANDLING_MODE) ?: VALUE_MODE_FONT
                val mnMixedMode = extractJsonValue(jsonString, KEY_MN_MIXED_MODE) == "true"
                val mnCfEnabled = extractJsonValue(jsonString, KEY_MN_CF_ENABLED) == "true"
                val javaBedrockMixedMode = extractJsonValue(jsonString, KEY_JAVA_BEDROCK_MIXED_MODE) == "true"
                val historyStorageUri = extractJsonValue(jsonString, KEY_HISTORY_STORAGE_URI) ?: ""
                val rawFilename = extractJsonValue(jsonString, KEY_HISTORY_STORAGE_FILENAME) ?: DEFAULT_HISTORY_FILENAME
                val historyStorageFilename = rawFilename.trim().replace("\"", "").replace("/", "").replace("\\", "")
                
                _mnHandlingMode.value = mnHandlingMode == VALUE_MODE_FONT
                _mnMixedMode.value = mnMixedMode
                _mnCFEnabled.value = mnCfEnabled
                _javaBedrockMixedMode.value = javaBedrockMixedMode
                _historyStorageUri.value = historyStorageUri.takeIf { it.isNotEmpty() }
                _historyStorageFilename.value = historyStorageFilename
                
                true
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * 导出配置为 JSON 字符串
     */
    private fun exportSettingsAsJson(): String {
        val mnHandlingMode = if (_mnHandlingMode.value) VALUE_MODE_FONT else VALUE_MODE_COLOR
        val mnMixedMode = _mnMixedMode.value
        val mnCfEnabled = _mnCFEnabled.value
        val javaBedrockMixedMode = _javaBedrockMixedMode.value
        val historyStorageUri = _historyStorageUri.value ?: ""
        val historyStorageFilename = _historyStorageFilename.value
        
        return """
            {
              "$KEY_MN_HANDLING_MODE": "$mnHandlingMode",
              "$KEY_MN_MIXED_MODE": $mnMixedMode,
              "$KEY_MN_CF_ENABLED": $mnCfEnabled,
              "$KEY_JAVA_BEDROCK_MIXED_MODE": $javaBedrockMixedMode,
              "$KEY_HISTORY_STORAGE_URI": "$historyStorageUri",
              "$KEY_HISTORY_STORAGE_FILENAME": "$historyStorageFilename"
            }
        """.trimIndent()
    }
    
    /**
     * 从 JSON 字符串中提取值
     */
    private fun extractJsonValue(jsonString: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"?([^\"]*)\"?".toRegex()
        val match = pattern.find(jsonString)
        return match?.groupValues?.get(1)?.trim()
    }
    
    // §m§n处理模式相关方法
    
    /**
     * 获取§m§n处理模式
     * @return true表示使用字体方式，false表示使用颜色代码方式
     */
    suspend fun getMNHandlingMode(): Boolean {
        return _mnHandlingMode.value
    }
    
    /**
     * 设置§m§n处理模式
     * @param useFontMode true表示使用字体方式，false表示使用颜色代码方式
     */
    suspend fun setMNHandlingMode(useFontMode: Boolean) {
        _mnHandlingMode.value = useFontMode
        saveConfig()
    }
    
    /**
     * 获取混合模式开关
     * @return true表示混合模式开启，false表示关闭
     */
    suspend fun getMNMixedMode(): Boolean {
        return _mnMixedMode.value
    }
    
    /**
     * 设置混合模式开关
     * @param enabled true表示开启混合模式，false表示关闭
     */
    suspend fun setMNMixedMode(enabled: Boolean) {
        _mnMixedMode.value = enabled
        saveConfig()
    }
    
    /**
     * 获取§m/§n_c/f设置
     * @return true表示启用_c/_f后缀，false表示禁用
     */
    suspend fun getMNCFEnabled(): Boolean {
        return _mnCFEnabled.value
    }
    
    /**
     * 设置§m/§n_c/f开关
     * @param enabled true表示启用_c/_f后缀，false表示禁用
     */
    suspend fun setMNCFEnabled(enabled: Boolean) {
        _mnCFEnabled.value = enabled
        saveConfig()
    }
    
    /**
     * 获取JAVA/基岩混合模式开关
     * @return true表示开启混合模式，false表示关闭
     */
    suspend fun getJavaBedrockMixedMode(): Boolean {
        return _javaBedrockMixedMode.value
    }
    
    /**
     * 设置JAVA/基岩混合模式开关
     * @param enabled true表示开启混合模式，false表示关闭
     */
    suspend fun setJavaBedrockMixedMode(enabled: Boolean) {
        _javaBedrockMixedMode.value = enabled
        saveConfig()
    }
    
    // 历史记录存储相关方法
    
    /**
     * 获取历史记录存储目录URI
     */
    suspend fun getHistoryStorageUri(): String? {
        return _historyStorageUri.value
    }
    
    /**
     * 设置历史记录存储目录URI
     */
    suspend fun setHistoryStorageUri(uri: String) {
        _historyStorageUri.value = uri.takeIf { it.isNotEmpty() }
        saveConfig()
    }
    
    /**
     * 获取历史记录存储文件名
     */
    suspend fun getHistoryStorageFilename(): String {
        return _historyStorageFilename.value
    }
    
    /**
     * 设置历史记录存储文件名
     */
    suspend fun setHistoryStorageFilename(filename: String) {
        val cleanFilename = filename.trim().replace("\"", "").replace("/", "").replace("\\", "")
        _historyStorageFilename.value = cleanFilename.takeIf { it.isNotEmpty() } ?: DEFAULT_HISTORY_FILENAME
        saveConfig()
    }
    
    /**
     * 清除历史记录存储设置
     */
    suspend fun clearHistoryStorageSettings() {
        _historyStorageUri.value = null
        _historyStorageFilename.value = DEFAULT_HISTORY_FILENAME
        saveConfig()
    }
}