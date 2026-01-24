package com.tellraw.app

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.tellraw.app.ui.navigation.TellrawNavigation
import com.tellraw.app.ui.theme.TellrawGeneratorTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    // 权限请求回调
    private var onPermissionGranted: (() -> Unit)? = null
    private var onPermissionDenied: (() -> Unit)? = null
    
    // 存储权限请求器
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            onPermissionGranted?.invoke()
        } else {
            onPermissionDenied?.invoke()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 隐藏刘海屏区域（横屏）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = 
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        
        setContent {
            TellrawGeneratorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TellrawNavigation()
                }
            }
        }
    }
    
    /**
     * 检查并请求存储权限
     */
    fun checkAndRequestStoragePermission(
        onGranted: () -> Unit,
        onDenied: () -> Unit = {}
    ) {
        if (hasStoragePermission()) {
            onGranted()
        } else {
            onPermissionGranted = onGranted
            onPermissionDenied = onDenied
            requestStoragePermission()
        }
    }
    
    /**
     * 主动申请所有文件访问权限（Android 11+）
     * 在需要写入外部文件时调用
     */
    fun requestAllFilesAccessIfNeeded(onGranted: () -> Unit, onDenied: () -> Unit = {}) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!hasAllFilesAccessPermission()) {
                onPermissionGranted = onGranted
                onPermissionDenied = onDenied
                requestAllFilesAccessPermission()
            } else {
                onGranted()
            }
        } else {
            // Android 10及以下，检查传统存储权限
            if (!hasStoragePermission()) {
                onPermissionGranted = onGranted
                onPermissionDenied = onDenied
                requestStoragePermission()
            } else {
                onGranted()
            }
        }
    }
    
    /**
     * 主动申请存储权限
     * 在需要读取外部文件时调用
     */
    fun requestStoragePermissionIfNeeded(onGranted: () -> Unit, onDenied: () -> Unit = {}) {
        if (!hasStoragePermission()) {
            onPermissionGranted = onGranted
            onPermissionDenied = onDenied
            requestStoragePermission()
        } else {
            onGranted()
        }
    }

    /**
     * 检查是否有所有文件访问权限（MANAGE_EXTERNAL_STORAGE）
     */
    fun hasAllFilesAccessPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            false
        }
    }

    /**
     * 请求所有文件访问权限（跳转到系统设置）
     */
    fun requestAllFilesAccessPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = android.content.Intent(
                    android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } catch (e: Exception) {
                // 某些设备可能不支持这个 Intent，尝试通用设置
                val intent = android.content.Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }
    }
    
    /**
     * 检查是否有存储权限
     */
    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 不需要传统存储权限
            true
        } else {
            // Android 10及以下需要检查存储权限
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * 请求存储权限
     */
    private fun requestStoragePermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 不需要请求传统存储权限
            emptyArray()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12 不需要传统存储权限
            emptyArray()
        } else {
            // Android 10及以下需要请求存储权限
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        
        if (permissions.isNotEmpty()) {
            storagePermissionLauncher.launch(permissions)
        }
    }
}