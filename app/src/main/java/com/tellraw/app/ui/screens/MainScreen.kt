package com.tellraw.app.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tellraw.app.data.remote.GithubRelease
import com.tellraw.app.model.SelectorType
import com.tellraw.app.ui.components.*
import com.tellraw.app.ui.viewmodel.TellrawViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToHelp: () -> Unit,
    viewModel: TellrawViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val selectorInput by viewModel.selectorInput.collectAsState()
    val messageInput by viewModel.messageInput.collectAsState()
    val javaCommand by viewModel.javaCommand.collectAsState()
    val bedrockCommand by viewModel.bedrockCommand.collectAsState()
    val warnings by viewModel.warnings.collectAsState()
    val selectorType by viewModel.selectorType.collectAsState()
    val useJavaFontStyle by viewModel.useJavaFontStyle.collectAsState()
    val showMNDialog by viewModel.showMNDialog.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsState()
    val showDisableCheckDialog by viewModel.showDisableCheckDialog.collectAsState()
    
    // 设置Context到ViewModel
    LaunchedEffect(context) {
        viewModel.setContext(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 顶部应用栏
        TopAppBar(
            title = { Text("Tellraw命令生成器") },
            actions = {
                IconButton(onClick = onNavigateToHelp) {
                    Icon(Icons.Default.Help, contentDescription = "帮助")
                }
            }
        )

        // 选择器输入区域
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "目标选择器",
                    style = MaterialTheme.typography.titleMedium
                )
                
                OutlinedTextField(
                    value = selectorInput,
                    onValueChange = { viewModel.updateSelector(it) },
                    label = { Text("输入选择器 (如: @a, @p, @e[type=player])") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    trailingIcon = {
                        if (selectorType != SelectorType.UNIVERSAL) {
                            SelectorTypeBadge(type = selectorType)
                        }
                    }
                )
                
                // 选择器类型提示
                if (selectorType != SelectorType.UNIVERSAL) {
                    Text(
                        text = "检测到${if (selectorType == SelectorType.JAVA) "Java版" else "基岩版"}选择器",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 消息输入区域
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "文本消息",
                    style = MaterialTheme.typography.titleMedium
                )
                
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { viewModel.updateMessage(it) },
                    label = { Text("输入消息文本") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    maxLines = 3
                )
                
                // 颜色代码快速输入
                ColorCodeQuickInput(
                    onCodeSelected = { code ->
                        val currentText = messageInput
                        viewModel.updateMessage(currentText + code)
                    }
                )
            }
        }

        // 警告信息
        warnings.forEach { warning ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = warning,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // 命令结果
        if (javaCommand.isNotEmpty() || bedrockCommand.isNotEmpty()) {
            CommandResults(
                javaCommand = javaCommand,
                bedrockCommand = bedrockCommand,
                onCopyJava = { viewModel.copyToClipboard(javaCommand) },
                onCopyBedrock = { viewModel.copyToClipboard(bedrockCommand) },
                onShareJava = { viewModel.shareCommand(javaCommand) },
                onShareBedrock = { viewModel.shareCommand(bedrockCommand) }
            )
        }

        // 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.clearAll() },
                modifier = Modifier.weight(1f)
            ) {
                Text("清空")
            }
            
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }

    // §m§n代码处理对话框
    if (showMNDialog) {
        MNCodeDialog(
            onDismiss = { viewModel.dismissMNDialog() },
            onUseJavaFontStyle = { useJava ->
                viewModel.setUseJavaFontStyle(useJava)
                viewModel.dismissMNDialog()
            }
        )
    }
    
    // 更新对话框
    showUpdateDialog?.let { release ->
        UpdateDialog(
            release = release,
            onDismiss = { viewModel.dismissUpdateDialog() },
            onOpenUrl = { url -> viewModel.openDownloadUrl(url) },
            onDisableChecks = { viewModel.showDisableCheckDialog() }
        )
    }
    
    // 禁用版本检查对话框
    if (showDisableCheckDialog) {
        DisableCheckDialog(
            onConfirm = { viewModel.disableVersionCheck() },
            onDismiss = { viewModel.dismissDisableCheckDialog() }
        )
    }
}