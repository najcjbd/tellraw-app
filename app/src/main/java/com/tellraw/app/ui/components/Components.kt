package com.tellraw.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SelectorTypeBadge(type: com.tellraw.app.model.SelectorType) {
    val (text, color) = when (type) {
        com.tellraw.app.model.SelectorType.JAVA -> "Java" to MaterialTheme.colorScheme.primary
        com.tellraw.app.model.SelectorType.BEDROCK -> "基岩" to MaterialTheme.colorScheme.secondary
        com.tellraw.app.model.SelectorType.UNIVERSAL -> "通用" to MaterialTheme.colorScheme.tertiary
    }
    
    SuggestionChip(
        onClick = { },
        label = { Text(text) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = color.copy(alpha = 0.2f),
            labelColor = color
        )
    )
}

@Composable
fun ColorCodeQuickInput(
    onCodeSelected: (String) -> Unit
) {
    val colorCodes = listOf(
        "§0" to "黑色", "§1" to "深蓝", "§2" to "深绿", "§3" to "深青",
        "§4" to "深红", "§5" to "深紫", "§6" to "金色", "§7" to "灰色",
        "§8" to "深灰", "§9" to "蓝色", "§a" to "绿色", "§b" to "青色",
        "§c" to "红色", "§d" to "粉色", "§e" to "黄色", "§f" to "白色",
        "§l" to "粗体", "§m" to "删除", "§n" to "下划", "§o" to "斜体",
        "§k" to "混乱", "§r" to "重置"
    )
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(colorCodes) { (code, name) ->
            FilterChip(
                onClick = { onCodeSelected(code) },
                label = { 
                    Text(
                        text = code + " " + name,
                        maxLines = 1
                    ) 
                },
                selected = false
            )
        }
    }
}

@Composable
fun WarningCard(warnings: List<String>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "转换提醒",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            warnings.forEach { warning ->
                Text(
                    text = "• $warning",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun CommandResults(
    javaCommand: String,
    bedrockCommand: String,
    onCopyJava: () -> Unit,
    onCopyBedrock: () -> Unit,
    onShareJava: () -> Unit,
    onShareBedrock: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "生成的命令",
                style = MaterialTheme.typography.titleMedium
            )
            
            // Java版命令
            CommandDisplay(
                title = "Java版",
                command = javaCommand,
                onCopy = onCopyJava,
                onShare = onShareJava
            )
            
            // 基岩版命令
            CommandDisplay(
                title = "基岩版",
                command = bedrockCommand,
                onCopy = onCopyBedrock,
                onShare = onShareBedrock
            )
        }
    }
}

@Composable
private fun CommandDisplay(
    title: String,
    command: String,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = command,
                onValueChange = { },
                readOnly = true,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace
                )
            )
            
            IconButton(onClick = onCopy) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "复制"
                )
            }
            
            IconButton(onClick = onShare) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "分享"
                )
            }
        }
    }
}

@Composable
fun MNCodeDialog(
    codeType: String?,
    onDismiss: () -> Unit,
    onUseJavaFontStyle: (Boolean) -> Unit,
    onMixedModeChoice: (String, String) -> Unit = { _, _ -> },
    mnMixedMode: Boolean = false
) {
    val codeName = when (codeType) {
        "§m" -> "§m（删除线）"
        "§n" -> "§n（下划线）"
        else -> "§m§n"
    }
    
    var rememberChoice by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = {
            // 点击空白区域取消时，默认使用字体方式
            if (mnMixedMode) {
                onMixedModeChoice(codeType ?: "§m", "font")
            } else {
                onUseJavaFontStyle(true)
            }
            onDismiss()
        },
        title = {
            Text("检测到$codeName 代码")
        },
        text = {
            Column {
                if (mnMixedMode) {
                    Text(
                        text = """检测到$codeName 格式代码，请选择处理方式：

1. 字体方式（格式代码）
2. 颜色代码方式"""
                    )
                } else {
                    Text(
                        text = """检测到$codeName 格式代码，请选择处理方式：

1. Java版使用字体方式，基岩版使用颜色代码方式
2. Java版和基岩版都使用颜色代码方式"""
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberChoice,
                        onCheckedChange = { rememberChoice = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("记住我的选择")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (mnMixedMode) {
                        onMixedModeChoice(codeType ?: "§m", "font")
                    } else {
                        onUseJavaFontStyle(true)
                    }
                }
            ) {
                Text("方式1")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (mnMixedMode) {
                        onMixedModeChoice(codeType ?: "§m", "color")
                    } else {
                        onUseJavaFontStyle(false)
                    }
                }
            ) {
                Text("方式2")
            }
        }
    )
}

@Composable
fun SettingsDialog(
    useJavaFontStyle: Boolean,
    mnMixedMode: Boolean,
    mnCFEnabled: Boolean,
    onDismiss: () -> Unit,
    onUseJavaFontStyleChanged: (Boolean) -> Unit,
    onMNMixedModeChanged: (Boolean) -> Unit,
    onMNCFEnabledChanged: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("设置")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "§m§n混合模式配置",
                    style = MaterialTheme.typography.titleMedium
                )
                
                // 混合模式开关
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = mnMixedMode,
                        onCheckedChange = onMNMixedModeChanged
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "混合模式",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "未选择时默认使用字体方式",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (!mnMixedMode) {
                    // 非混合模式时显示字体/颜色选择
                    Text(
                        text = "选择§m（删除线）和§n（下划线）代码的处理方式：",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = useJavaFontStyle,
                                onClick = { onUseJavaFontStyleChanged(true) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "字体方式",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Java版使用删除线/下划线，基岩版使用颜色代码",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !useJavaFontStyle,
                                onClick = { onUseJavaFontStyleChanged(false) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "颜色代码方式",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Java版和基岩版都使用颜色代码",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // §m/§n_c/f设置
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = mnCFEnabled,
                        onCheckedChange = onMNCFEnabledChanged
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "启用§m/§n_c/f",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "使用§m_f/§m_c/§n_f/§n_c指定处理方式",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}

@Composable
fun HistoryDialog(
    historyList: List<com.tellraw.app.data.local.CommandHistory>,
    onDismiss: () -> Unit,
    onLoadHistory: (com.tellraw.app.data.local.CommandHistory) -> Unit,
    onDeleteHistory: (com.tellraw.app.data.local.CommandHistory) -> Unit,
    onClearAll: () -> Unit,
    onSearch: (String) -> Unit = {},
    onShowStorageSettings: () -> Unit = {}
) {
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("命令历史记录")
                Row {
                    IconButton(
                        onClick = onClearAll
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "清空历史",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    IconButton(
                        onClick = onShowStorageSettings
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "存储设置"
                        )
                    }
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 搜索框
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        onSearch(it)
                    },
                    label = { Text("搜索历史记录") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { 
                                searchQuery = ""
                                onSearch("")
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "清除搜索")
                            }
                        }
                    }
                )
                
                if (historyList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isEmpty()) "暂无历史记录" else "没有找到匹配的历史记录",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(historyList) { history ->
                            HistoryItem(
                                history = history,
                                onLoad = { onLoadHistory(history) },
                                onDelete = { onDeleteHistory(history) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun HistoryItem(
    history: com.tellraw.app.data.local.CommandHistory,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val timeText = dateFormat.format(Date(history.timestamp))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "选择器: ${history.selector}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                    Text(
                        text = "消息: ${history.message.take(30)}${if (history.message.length > 30) "..." else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
                
                Row {
                    IconButton(
                        onClick = onLoad,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "加载",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryStorageSettingsDialog(
    storageUri: String?,
    filename: String,
    onDismiss: () -> Unit,
    onSelectDirectory: () -> Unit,
    onEditFilename: () -> Unit,
    onClearSettings: () -> Unit,
    onWriteToFile: () -> Unit,
    isWriting: Boolean = false,
    writeMessage: String? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("历史记录存储设置")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "存储历史记录的位置",
                    style = MaterialTheme.typography.titleMedium
                )
                
                // 存储目录
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "存储目录",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = if (storageUri != null) "已选择存储目录" else "未设置存储目录",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (storageUri != null) 
                                    MaterialTheme.colorScheme.onSurface 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onSelectDirectory) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "选择目录"
                            )
                        }
                    }
                }
                
                // 文件名
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "文件名",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = filename,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = onEditFilename) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "编辑文件名"
                            )
                        }
                    }
                }
                
                // 写入状态消息
                if (writeMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (writeMessage.contains("成功"))
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = writeMessage,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (writeMessage.contains("成功"))
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                
                // 清除设置按钮
                if (storageUri != null) {
                    OutlinedButton(
                        onClick = onClearSettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("清除存储设置")
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("取消")
                }
                Button(
                    onClick = onWriteToFile,
                    enabled = storageUri != null && !isWriting
                ) {
                    if (isWriting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isWriting) "写入中..." else "写入文件")
                }
            }
        }
    )
}

@Composable
fun FilenameInputDialog(
    currentFilename: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var filename by remember { mutableStateOf(currentFilename) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("设置文件名")
        },
        text = {
            Column {
                Text(
                    text = "请输入历史记录存储的文件名：",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = filename,
                    onValueChange = { filename = it },
                    label = { Text("文件名") },
                    placeholder = { Text("TellrawCommand.txt") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (filename.isNotBlank()) {
                        onConfirm(filename.trim())
                    }
                },
                enabled = filename.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun FileExistsDialog(
    filename: String,
    onDismiss: () -> Unit,
    onUseExisting: () -> Unit,
    onCustomize: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("文件已存在")
        },
        text = {
            Text(
                text = "文件 \"$filename\" 已存在。\n\n是否使用此文件追加历史记录？\n\n如果选择\"否\"，您需要自定义一个新的文件名。",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onUseExisting) {
                Text("是，使用此文件")
            }
        },
        dismissButton = {
            TextButton(onClick = onCustomize) {
                Text("否，自定义文件名")
            }
        }
    )
}