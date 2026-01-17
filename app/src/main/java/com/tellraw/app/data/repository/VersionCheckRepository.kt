package com.tellraw.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.tellraw.app.data.remote.GithubApiService
import com.tellraw.app.data.remote.GithubRelease
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VersionCheckRepository @Inject constructor(
    private val githubApiService: GithubApiService,
    private val context: Context
) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("version_check_prefs", Context.MODE_PRIVATE)
    
    private val _updateAvailable = MutableStateFlow<GithubRelease?>(null)
    val updateAvailable: Flow<GithubRelease?> = _updateAvailable.asStateFlow()
    
    companion object {
        private const val KEY_CURRENT_VERSION = "current_version"
        private const val KEY_DISABLE_CHECKS = "disable_version_checks"
        private const val KEY_LAST_CHECK_TIME = "last_check_time"
        private const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000 // 24小时
    }
    
    /**
     * 检查是否有新版本
     */
    suspend fun checkForUpdates(): Result<GithubRelease?> {
        // 检查是否禁用了版本检查
        if (isVersionCheckDisabled()) {
            return Result.success(null)
        }
        
        // 检查是否需要检查（避免频繁检查）
        if (!shouldCheckForUpdates()) {
            return Result.success(null)
        }
        
        return try {
            val latestRelease = githubApiService.getLatestRelease()
            val currentVersion = getCurrentVersion()
            
            // 保存最后检查时间
            saveLastCheckTime()
            
            // 如果是新版本，触发更新通知
            if (isNewVersion(currentVersion, latestRelease.tag_name)) {
                _updateAvailable.value = latestRelease
                Result.success(latestRelease)
            } else {
                Result.success(null)
            }
        } catch (e: IOException) {
            Result.failure(Exception(context.getString(R.string.network_error, e.message ?: "")))
        } catch (e: HttpException) {
            Result.failure(Exception(context.getString(R.string.server_error, e.message ?: "")))
        }
    }
    
    /**
     * 禁用版本检查
     */
    fun disableVersionCheck() {
        sharedPreferences.edit()
            .putBoolean(KEY_DISABLE_CHECKS, true)
            .apply()
    }
    
    /**
     * 启用版本检查
     */
    fun enableVersionCheck() {
        sharedPreferences.edit()
            .putBoolean(KEY_DISABLE_CHECKS, false)
            .apply()
    }
    
    /**
     * 检查版本检查是否被禁用
     */
    fun isVersionCheckDisabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_DISABLE_CHECKS, false)
    }
    
    /**
     * 保存当前版本
     */
    fun saveCurrentVersion(version: String) {
        sharedPreferences.edit()
            .putString(KEY_CURRENT_VERSION, version)
            .apply()
    }
    
    /**
     * 获取当前版本
     */
    fun getCurrentVersion(): String {
        return sharedPreferences.getString(KEY_CURRENT_VERSION, "1.0.0") ?: "1.0.0"
    }
    
    /**
     * 清除更新通知
     */
    fun clearUpdateNotification() {
        _updateAvailable.value = null
    }
    
    private fun shouldCheckForUpdates(): Boolean {
        val lastCheckTime = sharedPreferences.getLong(KEY_LAST_CHECK_TIME, 0)
        val currentTime = System.currentTimeMillis()
        return currentTime - lastCheckTime > CHECK_INTERVAL_MS
    }
    
    private fun saveLastCheckTime() {
        sharedPreferences.edit()
            .putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis())
            .apply()
    }
    
    /**
     * 比较版本号，判断是否为新版本
     * 使用语义化版本比较
     */
    private fun isNewVersion(currentVersion: String, latestVersion: String): Boolean {
        try {
            val current = parseVersion(currentVersion.removePrefix("v"))
            val latest = parseVersion(latestVersion.removePrefix("v"))
            
            // 比较主版本号
            if (latest.major > current.major) return true
            if (latest.major < current.major) return false
            
            // 比较次版本号
            if (latest.minor > current.minor) return true
            if (latest.minor < current.minor) return false
            
            // 比较修订号
            return latest.patch > current.patch
        } catch (e: Exception) {
            // 如果版本号格式不正确，使用字符串比较
            return latestVersion != currentVersion
        }
    }
    
    private fun parseVersion(version: String): Version {
        val parts = version.split(".")
        return Version(
            major = parts.getOrNull(0)?.toIntOrNull() ?: 0,
            minor = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        )
    }
    
    private data class Version(
        val major: Int,
        val minor: Int,
        val patch: Int
    )
}