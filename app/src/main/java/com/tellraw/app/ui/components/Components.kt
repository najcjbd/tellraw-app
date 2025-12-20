package com.tellraw.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

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
                        text = "$code $name",
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
            Text("检测到§m§n代码，请选择处理方式：

1. Java版使用字体方式，基岩版使用颜色代码方式
2. Java版和基岩版都使用颜色代码方式")
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