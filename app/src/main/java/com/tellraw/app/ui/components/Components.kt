package com.tellraw.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tellraw.app.R
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SelectorTypeBadge(type: com.tellraw.app.model.SelectorType) {
    val (text, color) = when (type) {
        com.tellraw.app.model.SelectorType.JAVA -> stringResource(R.string.java_edition_short) to MaterialTheme.colorScheme.primary
        com.tellraw.app.model.SelectorType.BEDROCK -> stringResource(R.string.selector_type_bedrock_short) to MaterialTheme.colorScheme.secondary
        com.tellraw.app.model.SelectorType.UNIVERSAL -> stringResource(R.string.selector_type_universal_short) to MaterialTheme.colorScheme.tertiary
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
        "§0" to stringResource(R.string.color_code_black),
        "§1" to stringResource(R.string.color_code_dark_blue),
        "§2" to stringResource(R.string.color_code_dark_green),
        "§3" to stringResource(R.string.color_code_dark_aqua),
        "§4" to stringResource(R.string.color_code_dark_red),
        "§5" to stringResource(R.string.color_code_dark_purple),
        "§6" to stringResource(R.string.color_code_gold),
        "§7" to stringResource(R.string.color_code_gray),
        "§8" to stringResource(R.string.color_code_dark_gray),
        "§9" to stringResource(R.string.color_code_blue),
        "§a" to stringResource(R.string.color_code_green),
        "§b" to stringResource(R.string.color_code_aqua),
        "§c" to stringResource(R.string.color_code_red),
        "§d" to stringResource(R.string.color_code_light_purple),
        "§e" to stringResource(R.string.color_code_yellow),
        "§f" to stringResource(R.string.color_code_white),
        "§l" to stringResource(R.string.format_code_bold),
        "§m" to stringResource(R.string.format_code_strikethrough),
        "§n" to stringResource(R.string.format_code_underline),
        "§o" to stringResource(R.string.format_code_italic),
        "§k" to stringResource(R.string.format_code_obfuscated),
        "§r" to stringResource(R.string.format_code_reset)
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
                text = stringResource(R.string.conversion_warnings),
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
                text = stringResource(R.string.generated_commands),
                style = MaterialTheme.typography.titleMedium
            )
            
            // Java版命令
            CommandDisplay(
                title = stringResource(R.string.command_title_java),
                command = javaCommand,
                onCopy = onCopyJava,
                onShare = onShareJava
            )
            
            // 基岩版命令
            CommandDisplay(
                title = stringResource(R.string.command_title_bedrock),
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
                    contentDescription = stringResource(R.string.action_copy)
                )
            }
            
            IconButton(onClick = onShare) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = stringResource(R.string.action_share)
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
        "§m" -> stringResource(R.string.code_strikethrough)
        "§n" -> stringResource(R.string.code_underline)
        else -> "§m§n"
    }
    
    var rememberChoice by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = {
            // 点击空白区域取消时，默认使用方式一
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
                    // 混合模式：显示方式一和方式二
                    Text(
                        text = stringResource(R.string.code_dialog_message_mixed, codeName)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.code_option_1_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.code_option_2_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
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
        dismissButton = {
            TextButton(
                onClick = {
                    if (mnMixedMode) {
                        // 方式一：Java版用字体模式，基岩版用颜色模式
                        onMixedModeChoice(codeType ?: "§m", "font")
                    } else {
                        onUseJavaFontStyle(true)
                    }
                }
            ) {
                Text(stringResource(R.string.code_option_1))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (mnMixedMode) {
                        // 方式二：两版都用颜色模式
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
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    if (isLandscape) {
        // 横屏：侧边栏布局
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(350.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 顶部栏 - 减小高度和字体
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.settings_title),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.close),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                
                // 设置内容
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.mn_mixed_mode_config),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    // 混合模式开关
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = mnMixedMode,
                            onCheckedChange = { enabled ->
                                // 开启混合模式时，自动关闭§m/§n_c/f和字体/颜色选择
                                if (enabled) {
                                    onMNCFEnabledChanged(false)
                                }
                                onMNMixedModeChanged(enabled)
                            },
                            enabled = !mnCFEnabled
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.mn_mixed_mode),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (mnCFEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.mn_mixed_mode_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // 只有当混合模式和§m/§n_c/f都关闭时，才显示字体/颜色选择
                    if (!mnMixedMode && !mnCFEnabled) {
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
                                    onClick = { 
                                        // 选择字体模式时，保持混合模式和§m/§n_c/f关闭
                                        onUseJavaFontStyleChanged(true)
                                    }
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
                                    onClick = { 
                                        // 选择颜色模式时，保持混合模式和§m/§n_c/f关闭
                                        onUseJavaFontStyleChanged(false)
                                    }
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
                            onCheckedChange = { enabled ->
                                // 开启§m/§n_c/f时，自动关闭混合模式
                                if (enabled) {
                                    onMNMixedModeChanged(false)
                                }
                                onMNCFEnabledChanged(enabled)
                            },
                            enabled = !mnMixedMode
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.mn_cf_enabled),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (mnMixedMode) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.mn_cf_enabled_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // 底部操作栏
                Surface(
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                }
            }
        }
    } else {
        // 竖屏：对话框布局
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
                            onCheckedChange = { enabled ->
                                // 开启混合模式时，自动关闭§m/§n_c/f和字体/颜色选择
                                if (enabled) {
                                    onMNCFEnabledChanged(false)
                                }
                                onMNMixedModeChanged(enabled)
                            },
                            enabled = !mnCFEnabled
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.mn_mixed_mode),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (mnCFEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.mn_mixed_mode_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // 只有当混合模式和§m/§n_c/f都关闭时，才显示字体/颜色选择
                    if (!mnMixedMode && !mnCFEnabled) {
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
                                    onClick = { 
                                        // 选择字体模式时，保持混合模式和§m/§n_c/f关闭
                                        onUseJavaFontStyleChanged(true)
                                    }
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
                                    onClick = { 
                                        // 选择颜色模式时，保持混合模式和§m/§n_c/f关闭
                                        onUseJavaFontStyleChanged(false)
                                    }
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
                            onCheckedChange = { enabled ->
                                // 开启§m/§n_c/f时，自动关闭混合模式
                                if (enabled) {
                                    onMNMixedModeChanged(false)
                                }
                                onMNCFEnabledChanged(enabled)
                            },
                            enabled = !mnMixedMode
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.mn_cf_enabled),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (mnMixedMode) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
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
}

@Composable
fun HistoryDialog(
    historyList: List<com.tellraw.app.data.repository.HistoryItem>,
    onDismiss: () -> Unit,
    onLoadHistory: (com.tellraw.app.data.repository.HistoryItem) -> Unit,
    onDeleteHistory: (com.tellraw.app.data.repository.HistoryItem) -> Unit,
    onClearAll: () -> Unit = {},
    onSearch: (String) -> Unit = {},
    onShowStorageSettings: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    if (isLandscape) {
        // 横屏：侧边栏布局
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(450.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // 顶部栏 - 减小高度和字体
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.command_history),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Row {
                            IconButton(
                                onClick = onShowStorageSettings,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = stringResource(R.string.settings_title),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.close),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                
                // 搜索框 - 减小高度
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        onSearch(it)
                    },
                    label = { Text(stringResource(R.string.search_history)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    singleLine = true,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { 
                                searchQuery = ""
                                onSearch("")
                            }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_history), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                )
                
                // 历史记录列表 - 增大宽度
                if (historyList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
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
                
                // 底部操作栏 - 减小高度
                Surface(
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (historyList.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    onClearAll()
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(stringResource(R.string.clear_history), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(stringResource(R.string.close), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    } else {
        // 竖屏：对话框布局
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (historyList.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                onClearAll()
                                onDismiss()
                            }
                        ) {
                            Text(stringResource(R.string.clear_history))
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        )
    }
}

@Composable
private fun HistoryItem(
    history: com.tellraw.app.data.repository.HistoryItem,
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
                        text = stringResource(R.string.history_item_selector, history.selector),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                    Text(
                        text = stringResource(R.string.history_item_message, 
                            history.message.take(30) + if (history.message.length > 30) "..." else ""),
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
                            contentDescription = stringResource(R.string.action_load),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.action_delete),
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
    onGrantAllFilesAccess: () -> Unit = {},
    isWriting: Boolean = false,
    writeMessage: String? = null
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    if (isLandscape) {
        // 横屏：侧边栏布局
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(350.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 顶部栏 - 减小高度和字体
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.storage_settings),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.close),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                
                // 设置内容
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.storage_location),
                        style = MaterialTheme.typography.bodyLarge
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
                    
                    // 提示信息
                    if (storageUri == null) {
                        Text(
                            text = stringResource(R.string.sandbox_storage_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // 写入状态消息
                    if (writeMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (writeMessage.contains(stringResource(R.string.success)))
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = writeMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (writeMessage.contains(stringResource(R.string.success)))
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onErrorContainer
                                )
                                // 如果消息包含"权限"，显示授予权限按钮
                                if (writeMessage.contains("权限") && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = onGrantAllFilesAccess,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(stringResource(R.string.grant_all_files_access))
                                    }
                                }
                            }
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
                }
                
                // 底部操作栏
                Surface(
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = onWriteToFile,
                            enabled = !isWriting,
                            modifier = Modifier.weight(1f)
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
            }
        }
    } else {
        // 竖屏：对话框布局
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
                    
                    // 提示信息
                    if (storageUri == null) {
                        Text(
                            text = stringResource(R.string.sandbox_storage_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // 写入状态消息
                    if (writeMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (writeMessage.contains(stringResource(R.string.success)))
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = writeMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (writeMessage.contains(stringResource(R.string.success)))
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onErrorContainer
                                )
                                // 如果消息包含"权限"，显示授予权限按钮
                                if (writeMessage.contains("权限") && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = onGrantAllFilesAccess,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(stringResource(R.string.grant_all_files_access))
                                    }
                                }
                            }
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
                        enabled = !isWriting
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
                    val finalFilename = if (filename.isBlank()) {
                        "TellrawCommand.txt"
                    } else {
                        // 清理文件名：移除引号和其他非法字符
                        filename.trim().replace("\"", "").replace("/", "").replace("\\", "")
                    }
                    onConfirm(finalFilename)
                }
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
    onAppendToExisting: () -> Unit,
    onCustomize: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.file_exists))
        },
        text = {
            Text(
                text = stringResource(R.string.file_exists_message, filename),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onAppendToExisting) {
                Text(stringResource(R.string.append_to_file))
            }
        },
        dismissButton = {
            TextButton(onClick = onCustomize) {
                Text(stringResource(R.string.customize_filename))
            }
        }
    )
}

/**
 * 文件夹选择器对话框（替代SAF）
 * 使用传统的文件管理器界面，只允许选择文件夹
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryPickerDialog(
    initialPath: String = "/storage/emulated/0",
    onDismiss: () -> Unit,
    onDirectorySelected: (String) -> Unit
) {
    var currentPath by remember { mutableStateOf(initialPath) }
    var directories by remember { mutableStateOf<List<String>>(emptyList()) }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    // 加载当前目录的文件夹列表
    LaunchedEffect(currentPath) {
        try {
            val file = java.io.File(currentPath)
            if (file.exists() && file.isDirectory) {
                directories = file.listFiles()
                    ?.filter { it.isDirectory }
                    ?.map { it.name }
                    ?.sorted()
                    ?: emptyList()
            } else {
                directories = emptyList()
            }
        } catch (e: Exception) {
            directories = emptyList()
        }
    }
    
    if (isLandscape) {
        // 横屏布局：侧边栏布局
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(400.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 标题栏 - 减小高度
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.select_directory),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.cancel),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                
                HorizontalDivider()
                
                // 当前路径显示
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.selected_directory, currentPath),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2
                        )
                    }
                }
                
                // 文件夹列表
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // 返回上一级按钮
                        if (currentPath != "/") {
                            item {
                                androidx.compose.material3.ListItem(
                                    headlineContent = { Text(stringResource(R.string.parent_directory), style = MaterialTheme.typography.bodySmall) },
                                    leadingContent = {
                                        Icon(
                                            Icons.Default.Folder,
                                            contentDescription = null
                                        )
                                    },
                                    modifier = Modifier.clickable {
                                        val parentFile = java.io.File(currentPath).parentFile
                                        if (parentFile != null) {
                                            currentPath = parentFile.absolutePath
                                        }
                                    }
                                )
                            }
                        }
                        
                        // 文件夹列表
                        items(directories) { dirName ->
                            androidx.compose.material3.ListItem(
                                headlineContent = { Text(dirName) },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    currentPath = "$currentPath/$dirName"
                                }
                            )
                        }
                        
                        // 空状态
                        if (directories.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.no_history),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                
                // 操作按钮 - 移到右边，长方形设计
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .height(36.dp)
                            .width(80.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text(stringResource(R.string.cancel), style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onDirectorySelected(currentPath) },
                        modifier = Modifier
                            .height(36.dp)
                            .width(100.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text(stringResource(R.string.select_directory), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    } else {
        // 竖屏布局：上下分栏
        androidx.compose.material3.BasicAlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                // 当前路径显示
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
                        Text(
                            text = stringResource(R.string.selected_directory, currentPath),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 文件夹列表
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 返回上一级按钮
                        if (currentPath != "/") {
                            item {
                                androidx.compose.material3.ListItem(
                                    headlineContent = { Text(stringResource(R.string.parent_directory)) },
                                    leadingContent = {
                                        Icon(
                                            Icons.Default.Folder,
                                            contentDescription = null
                                        )
                                    },
                                    modifier = Modifier.clickable {
                                        val parentFile = java.io.File(currentPath).parentFile
                                        if (parentFile != null) {
                                            currentPath = parentFile.absolutePath
                                        }
                                    }
                                )
                            }
                        }
                        
                        // 文件夹列表
                        items(directories) { dirName ->
                            androidx.compose.material3.ListItem(
                                headlineContent = { Text(dirName) },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    currentPath = "$currentPath/$dirName"
                                }
                            )
                        }
                        
                        // 空状态
                        if (directories.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.no_history),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 操作按钮 - 移到右边，长方形设计
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .height(36.dp)
                            .width(80.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text(stringResource(R.string.cancel), style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onDirectorySelected(currentPath) },
                        modifier = Modifier
                            .height(36.dp)
                            .width(100.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text(stringResource(R.string.select_directory), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}