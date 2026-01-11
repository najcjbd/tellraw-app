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
    
    // 存储权限请求器
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            onPermissionGranted?.invoke()
        }
    }
    
    // SAF目录选择器
    private val directoryPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            // 获取持久化权限
            contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            // 将URI传递给ViewModel
            onDirectorySelected?.invoke(uri.toString())
        }
    }
    
    // 目录选择回调
    private var onDirectorySelected: ((String) -> Unit)? = null
    
    // SAF文件创建器
    private val fileCreatorLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) {
            // 获取持久化权限
            contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            // TODO: 将URI传递给ViewModel
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    fun checkAndRequestStoragePermission(onGranted: () -> Unit) {
        if (hasStoragePermission()) {
            onGranted()
        } else {
            onPermissionGranted = onGranted
            requestStoragePermission()
        }
    }
    
    /**
     * 检查是否有存储权限
     */
    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 不需要传统存储权限，SAF可以直接使用
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
    
    /**
     * 启动目录选择器
     */
    fun launchDirectoryPicker(onSelected: (String) -> Unit) {
        onDirectorySelected = onSelected
        directoryPickerLauncher.launch(null)
    }
    
    /**
     * 启动文件创建器
     */
    fun launchFileCreator(filename: String) {
        fileCreatorLauncher.launch(filename)
    }
    
    /**
     * 启动文件创建器
     */
    fun launchFileCreator(filename: String) {
        fileCreatorLauncher.launch(filename)
    }
}