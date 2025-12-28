package com.tellraw.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.format.DateFormat
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
    onDismiss: () -> Unit,
    onUseJavaFontStyle: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("检测到§m§n代码")
        },
        text = {
            Text("""检测到§m§n代码，请选择处理方式：

1. Java版使用字体方式，基岩版使用颜色代码方式
2. Java版和基岩版都使用颜色代码方式""")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onUseJavaFontStyle(true)
                }
            ) {
                Text("方式1")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onUseJavaFontStyle(false)
                }
            ) {
                Text("方式2")
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
    onSearch: (String) -> Unit = {}
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
                if (historyList.isNotEmpty()) {
                    IconButton(
                        onClick = onClearAll
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "清空历史",
                            tint = MaterialTheme.colorScheme.error
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