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
            Text(stringResource(R.string.detected_code, codeName))
        },
        text = {
            Column {
                if (mnMixedMode) {
                    Text(
                        text = stringResource(R.string.code_dialog_message_mixed, codeName)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.code_dialog_message_full, codeName)
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
                    Text(stringResource(R.string.remember_choice))
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
                Text(stringResource(R.string.code_option_1))
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
                Text(stringResource(R.string.code_option_2))
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
            Text(stringResource(R.string.settings_title))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.mn_mixed_mode_config),
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
                            text = stringResource(R.string.mn_mixed_mode),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(R.string.mn_mixed_mode_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (!mnMixedMode) {
                    // 非混合模式时显示字体/颜色选择
                    Text(
                        text = stringResource(R.string.mn_handling_method_title),
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
                                    text = stringResource(R.string.mn_font_style),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = stringResource(R.string.mn_font_style_description),
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
                                    text = stringResource(R.string.mn_color_style),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = stringResource(R.string.mn_color_style_description),
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
                            text = stringResource(R.string.mn_cf_enabled),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(R.string.mn_cf_enabled_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
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
    var searchQuery by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.command_history))
                IconButton(
                    onClick = onShowStorageSettings
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings_title)
                    )
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
                    label = { Text(stringResource(R.string.search_history)) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { 
                                searchQuery = ""
                                onSearch("")
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_history))
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
                                text = if (searchQuery.isEmpty()) stringResource(R.string.no_history) else stringResource(R.string.no_search_results),
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
                Text(stringResource(R.string.close))
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
    onExportConfig: () -> Unit = {},
    onImportConfig: () -> Unit = {},
    isWriting: Boolean = false,
    writeMessage: String? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.storage_settings))
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
                    text = stringResource(R.string.storage_location),
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
                                text = stringResource(R.string.storage_directory),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = if (storageUri != null) stringResource(R.string.storage_directory_selected) else stringResource(R.string.storage_directory_not_set),
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
                                contentDescription = stringResource(R.string.select_directory)
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
                                text = stringResource(R.string.storage_filename),
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
                                contentDescription = stringResource(R.string.set_filename)
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
                        Text(stringResource(R.string.clear_storage_settings))
                    }
                }
                
                // 配置管理按钮
                if (storageUri != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onExportConfig,
                            modifier = Modifier.weight(1f),
                            enabled = !isWriting
                        ) {
                            Text(stringResource(R.string.export_config))
                        }
                        OutlinedButton(
                            onClick = onImportConfig,
                            modifier = Modifier.weight(1f),
                            enabled = !isWriting
                        ) {
                            Text(stringResource(R.string.import_config))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
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
                    Text(if (isWriting) stringResource(R.string.writing) else stringResource(R.string.write_to_file))
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
            Text(stringResource(R.string.set_filename))
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.enter_filename),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = filename,
                    onValueChange = { filename = it },
                    label = { Text(stringResource(R.string.storage_filename)) },
                    placeholder = { Text(stringResource(R.string.filename_placeholder)) },
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
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
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
            Text(stringResource(R.string.file_exists))
        },
        text = {
            Text(
                text = "文件 \"$filename\" 已存在。\n\n是否使用此文件追加历史记录？\n\n如果选择\"否\"，您需要自定义一个新的文件名。",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onUseExisting) {
                Text(stringResource(R.string.use_existing_file))
            }
        },
        dismissButton = {
            TextButton(onClick = onCustomize) {
                Text(stringResource(R.string.customize_filename))
            }
        }
    )
}