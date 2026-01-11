package com.tellraw.app.data.repository

import com.tellraw.app.data.local.AppSettings
import com.tellraw.app.data.local.AppSettingsDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val appSettingsDao: AppSettingsDao
) {
    
    companion object {
        private const val KEY_MN_HANDLING_MODE = "mn_handling_mode"
        private const val KEY_MN_MIXED_MODE = "mn_mixed_mode"
        private const val KEY_MN_CF_ENABLED = "mn_cf_enabled"
        private const val KEY_HISTORY_STORAGE_URI = "history_storage_uri"
        private const val KEY_HISTORY_STORAGE_FILENAME = "history_storage_filename"
        private const val VALUE_MODE_FONT = "font"
        private const val VALUE_MODE_COLOR = "color"
    }
    
    /**
     * 获取§m§n处理模式
     * @return true表示使用字体方式，false表示使用颜色代码方式
     */
    suspend fun getMNHandlingMode(): Boolean {
        val settings = appSettingsDao.getByKey(KEY_MN_HANDLING_MODE)
        return settings?.value == VALUE_MODE_FONT
    }
    
    /**
     * 设置§m§n处理模式
     * @param useFontMode true表示使用字体方式，false表示使用颜色代码方式
     */
    suspend fun setMNHandlingMode(useFontMode: Boolean) {
        val value = if (useFontMode) VALUE_MODE_FONT else VALUE_MODE_COLOR
        appSettingsDao.insert(AppSettings(KEY_MN_HANDLING_MODE, value))
    }
    
    /**
     * 获取§m§n处理模式的Flow
     */
    fun getMNHandlingModeFlow(): Flow<Boolean> {
        return appSettingsDao.getAll().map { settings ->
            val setting = settings.find { it.key == KEY_MN_HANDLING_MODE }
            setting?.value == VALUE_MODE_FONT
        }
    }
    
    /**
     * 获取混合模式开关
     * @return true表示混合模式开启，false表示关闭
     */
    suspend fun getMNMixedMode(): Boolean {
        val settings = appSettingsDao.getByKey(KEY_MN_MIXED_MODE)
        return settings?.value == "true"
    }
    
    /**
     * 设置混合模式开关
     * @param enabled true表示开启混合模式，false表示关闭
     */
    suspend fun setMNMixedMode(enabled: Boolean) {
        appSettingsDao.insert(AppSettings(KEY_MN_MIXED_MODE, enabled.toString()))
    }
    
    /**
     * 获取混合模式开关的Flow
     */
    fun getMNMixedModeFlow(): Flow<Boolean> {
        return appSettingsDao.getAll().map { settings ->
            val setting = settings.find { it.key == KEY_MN_MIXED_MODE }
            setting?.value == "true"
        }
    }
    
    /**
     * 获取§m/§n_c/f设置
     * @return true表示启用_c/_f后缀，false表示禁用
     */
    suspend fun getMNCFEnabled(): Boolean {
        val settings = appSettingsDao.getByKey(KEY_MN_CF_ENABLED)
        return settings?.value == "true"
    }
    
    /**
     * 设置§m/§n_c/f开关
     * @param enabled true表示启用_c/_f后缀，false表示禁用
     */
    suspend fun setMNCFEnabled(enabled: Boolean) {
        appSettingsDao.insert(AppSettings(KEY_MN_CF_ENABLED, enabled.toString()))
    }
    
    /**
     * 获取§m/§n_c/f开关的Flow
     */
    fun getMNCFEnabledFlow(): Flow<Boolean> {
        return appSettingsDao.getAll().map { settings ->
            val setting = settings.find { it.key == KEY_MN_CF_ENABLED }
            setting?.value == "true"
        }
    }
    
    /**
     * 保存通用设置
     */
    suspend fun saveSetting(key: String, value: String) {
        appSettingsDao.insert(AppSettings(key, value))
    }
    
    /**
     * 获取通用设置
     */
    suspend fun getSetting(key: String, defaultValue: String = ""): String {
        val settings = appSettingsDao.getByKey(key)
        return settings?.value ?: defaultValue
    }
    
    /**
     * 获取通用设置的Flow
     */
    fun getSettingFlow(key: String, defaultValue: String = ""): Flow<String> {
        return appSettingsDao.getAll().map { settings ->
            val setting = settings.find { it.key == key }
            setting?.value ?: defaultValue
        }
    }
    
    // 历史记录存储相关方法
    
    /**
     * 获取历史记录存储目录URI
     */
    suspend fun getHistoryStorageUri(): String? {
        val settings = appSettingsDao.getByKey(KEY_HISTORY_STORAGE_URI)
        return settings?.value.takeIf { it.isNotEmpty() }
    }
    
    /**
     * 设置历史记录存储目录URI
     */
    suspend fun setHistoryStorageUri(uri: String) {
        appSettingsDao.insert(AppSettings(KEY_HISTORY_STORAGE_URI, uri))
    }
    
    /**
     * 获取历史记录存储目录URI的Flow
     */
    fun getHistoryStorageUriFlow(): Flow<String?> {
        return appSettingsDao.getAll().map { settings ->
            val setting = settings.find { it.key == KEY_HISTORY_STORAGE_URI }
            setting?.value?.takeIf { it.isNotEmpty() }
        }
    }
    
    /**
     * 获取历史记录存储文件名
     */
    suspend fun getHistoryStorageFilename(): String {
        val settings = appSettingsDao.getByKey(KEY_HISTORY_STORAGE_FILENAME)
        return settings?.value.takeIf { it.isNotEmpty() } ?: "TellrawCommand.txt"
    }
    
    /**
     * 设置历史记录存储文件名
     */
    suspend fun setHistoryStorageFilename(filename: String) {
        appSettingsDao.insert(AppSettings(KEY_HISTORY_STORAGE_FILENAME, filename))
    }
    
    /**
     * 获取历史记录存储文件名的Flow
     */
    fun getHistoryStorageFilenameFlow(): Flow<String> {
        return appSettingsDao.getAll().map { settings ->
            val setting = settings.find { it.key == KEY_HISTORY_STORAGE_FILENAME }
            setting?.value?.takeIf { it.isNotEmpty() } ?: "TellrawCommand.txt"
        }
    }
}