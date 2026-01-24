package com.tellraw.app.ui.screens

import android.content.Context
import android.content.res.Configuration
import com.tellraw.app.MainActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tellraw.app.R
import com.tellraw.app.data.repository.HistoryItem
import com.tellraw.app.data.remote.GithubRelease
import com.tellraw.app.model.SelectorType
import com.tellraw.app.ui.components.*
import com.tellraw.app.ui.viewmodel.TellrawViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToHelp: () -> Unit,
    viewModel: TellrawViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val activity = context as? MainActivity
    
    val selectorInput by viewModel.selectorInput.collectAsState()
    val messageInput by viewModel.messageInput.collectAsState()
    val javaCommand by viewModel.javaCommand.collectAsState()
    val bedrockCommand by viewModel.bedrockCommand.collectAsState()
    val warnings by viewModel.warnings.collectAsState()
    val selectorType by viewModel.selectorType.collectAsState()
    val useJavaFontStyle by viewModel.useJavaFontStyle.collectAsState()
    val mnMixedMode by viewModel.mnMixedMode.collectAsState()
    val mnCFEnabled by viewModel.mnCFEnabled.collectAsState()
    val showMNDialog by viewModel.showMNDialog.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsState()
    val showDisableCheckDialog by viewModel.showDisableCheckDialog.collectAsState()
    
    // 历史记录存储相关状态
    val showStorageSettingsDialog by viewModel.showStorageSettingsDialog.collectAsState()
    val showFilenameDialog by viewModel.showFilenameDialog.collectAsState()
    val showFileExistsDialog by viewModel.showFileExistsDialog.collectAsState()
    val historyStorageUri by viewModel.historyStorageUri.collectAsState()
    val historyStorageFilename by viewModel.historyStorageFilename.collectAsState()
    val isWritingToFile by viewModel.isWritingToFile.collectAsState()
    val writeFileMessage by viewModel.writeFileMessage.collectAsState()
    
    // 文件选择器相关状态
    val showDirectoryPickerDialog = remember { mutableStateOf(false) }
    
    // 用于跟踪光标位置的状态
    val messageTextFieldValue = remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(messageInput)) }
    
    // 当messageInput变化时，同步更新messageTextFieldValue
    LaunchedEffect(messageInput) {
        if (messageTextFieldValue.value.text != messageInput) {
            messageTextFieldValue.value = androidx.compose.ui.text.input.TextFieldValue(messageInput)
        }
    }
    
    // 历史记录状态
    val commandHistory by viewModel.commandHistory.collectAsState(initial = emptyList())
    val showHistoryDialog = remember { mutableStateOf(false) }
    val showSettingsDialog = remember { mutableStateOf(false) }

    // 设置Context到ViewModel
    LaunchedEffect(context) {
        viewModel.setContext(context)
    }

    // 根据屏幕方向选择布局
    if (isLandscape) {
        LandscapeLayout(
            onNavigateToHelp = onNavigateToHelp,
            selectorInput = selectorInput,
            messageTextFieldValue = messageTextFieldValue,
            javaCommand = javaCommand,
            bedrockCommand = bedrockCommand,
            warnings = warnings,
            selectorType = selectorType,
            isLoading = isLoading,
            showHistoryDialog = showHistoryDialog,
            viewModel = viewModel
        )
    } else {
        PortraitLayout(
            onNavigateToHelp = onNavigateToHelp,
            selectorInput = selectorInput,
            messageTextFieldValue = messageTextFieldValue,
            javaCommand = javaCommand,
            bedrockCommand = bedrockCommand,
            warnings = warnings,
            selectorType = selectorType,
            isLoading = isLoading,
            showHistoryDialog = showHistoryDialog,
            showSettingsDialog = showSettingsDialog,
            viewModel = viewModel
        )
    }

    // 历史记录对话框
    if (showHistoryDialog.value) {
        HistoryDialog(
            historyList = commandHistory,
            onDismiss = { showHistoryDialog.value = false },
            onLoadHistory = { history ->
                viewModel.loadFromHistory(history)
                showHistoryDialog.value = false
            },
            onDeleteHistory = { history ->
                viewModel.deleteHistoryItem(history)
            },
            onClearAll = {
                viewModel.clearAllHistory()
                showHistoryDialog.value = false
            },
            onSearch = { query -> viewModel.searchHistory(query) },
            onShowStorageSettings = { viewModel.showStorageSettings() }
        )
    }

    // §m§n代码处理对话框
    showMNDialog?.let { codeType ->
        MNCodeDialog(
            codeType = codeType,
            onDismiss = { viewModel.dismissMNDialog() },
            onUseJavaFontStyle = { useJava ->
                viewModel.setUseJavaFontStyle(useJava)
                viewModel.dismissMNDialog()
            },
            onMixedModeChoice = { code, choice ->
                viewModel.handleMixedModeChoice(code, choice)
                viewModel.dismissMNDialog()
            },
            mnMixedMode = mnMixedMode
        )
    }
    
    // 设置对话框
    if (showSettingsDialog.value) {
        SettingsDialog(
            useJavaFontStyle = useJavaFontStyle,
            mnMixedMode = mnMixedMode,
            mnCFEnabled = mnCFEnabled,
            onDismiss = { showSettingsDialog.value = false },
            onUseJavaFontStyleChanged = { useJava ->
                viewModel.setUseJavaFontStyle(useJava)
            },
            onMNMixedModeChanged = { mixed ->
                viewModel.setMNMixedMode(mixed)
            },
            onMNCFEnabledChanged = { cfEnabled ->
                viewModel.setMNCFEnabled(cfEnabled)
            }
        )
    }
    
    // 更新对话框
    showUpdateDialog?.let { release ->
        UpdateDialog(
            release = release,
            onDismiss = { viewModel.dismissUpdateDialog() },
            onOpenUrl = { url -> viewModel.openDownloadUrl(url) }
        )
    }
    
    // 禁用版本检查对话框
    if (showDisableCheckDialog) {
        DisableCheckDialog(
            onConfirm = { viewModel.disableVersionCheck() },
            onDismiss = { viewModel.dismissDisableCheckDialog() }
        )
    }
    
    // 历史记录存储设置对话框
    if (showStorageSettingsDialog) {
        HistoryStorageSettingsDialog(
            storageUri = historyStorageUri,
            filename = historyStorageFilename,
            onDismiss = { viewModel.hideStorageSettingsDialog() },
            onSelectDirectory = {
                activity?.checkAndRequestStoragePermission(
                    onGranted = {
                        showDirectoryPickerDialog.value = true
                    },
                    onDenied = {
                        // 权限被拒绝，不显示文件选择器
                    }
                )
            },
            onEditFilename = { viewModel.showFilenameDialog() },
            onClearSettings = { viewModel.clearHistoryStorageSettings() },
            onWriteToFile = {
                viewModel.writeHistoryToFile(context, commandHistory.toList())
            },
            onGrantAllFilesAccess = {
                activity?.requestAllFilesAccessPermission()
            },
            isWriting = isWritingToFile,
            writeMessage = writeFileMessage
        )
    }
    
    // 文件选择器对话框
    if (showDirectoryPickerDialog.value) {
        DirectoryPickerDialog(
            initialPath = historyStorageUri ?: "/storage/emulated/0",
            onDismiss = { showDirectoryPickerDialog.value = false },
            onDirectorySelected = { path ->
                viewModel.setHistoryStorageUri(path)
                showDirectoryPickerDialog.value = false
            }
        )
    }
    
    // 文件名输入对话框
    showFilenameDialog?.let { currentFilename ->
        FilenameInputDialog(
            currentFilename = currentFilename,
            onDismiss = { viewModel.hideFilenameDialog() },
            onConfirm = { filename ->
                viewModel.setHistoryStorageFilename(filename)
                viewModel.hideFilenameDialog()
            }
        )
    }
    
    // 文件已存在对话框
    showFileExistsDialog?.let { filename ->
        FileExistsDialog(
            filename = filename,
            onDismiss = { viewModel.hideFileExistsDialog() },
            onUseExisting = {
                viewModel.appendToExistingFile(context, commandHistory.toList())
                viewModel.hideFileExistsDialog()
            },
            onCustomize = {
                viewModel.showFilenameDialog()
                viewModel.hideFileExistsDialog()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortraitLayout(
    onNavigateToHelp: () -> Unit,
    selectorInput: String,
    messageTextFieldValue: MutableState<androidx.compose.ui.text.input.TextFieldValue>,
    javaCommand: String,
    bedrockCommand: String,
    warnings: List<String>,
    selectorType: SelectorType,
    isLoading: Boolean,
    showHistoryDialog: MutableState<Boolean>,
    showSettingsDialog: MutableState<Boolean>,
    viewModel: TellrawViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 顶部应用栏
        TopAppBar(
            title = { Text(stringResource(R.string.app_title)) },
            actions = {
                IconButton(onClick = { showHistoryDialog.value = true }) {
                    Icon(Icons.Default.History, contentDescription = stringResource(R.string.history))
                }
                IconButton(onClick = { showSettingsDialog.value = true }) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
                }
                IconButton(onClick = onNavigateToHelp) {
                    Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.help))
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
                    text = stringResource(R.string.target_selector),
                    style = MaterialTheme.typography.titleMedium
                )
                
                OutlinedTextField(
                    value = selectorInput,
                    onValueChange = { viewModel.updateSelector(it) },
                    label = { Text(stringResource(R.string.input_selector)) },
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
                        text = stringResource(R.string.detected_selector_type, if (selectorType == SelectorType.JAVA) stringResource(R.string.selector_type_java) else stringResource(R.string.selector_type_bedrock)),
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
                    text = stringResource(R.string.message_input_hint).substring(0, 4),
                    style = MaterialTheme.typography.titleMedium
                )
                
                OutlinedTextField(
                    value = messageTextFieldValue.value,
                    onValueChange = { 
                        messageTextFieldValue.value = it
                        viewModel.updateMessage(it.text)
                    },
                    label = { Text(stringResource(R.string.input_text)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    maxLines = 3
                )
                
                // 颜色代码快速输入
                ColorCodeQuickInput(
                    onCodeSelected = { code ->
                        val currentValue = messageTextFieldValue.value
                        val cursorPosition = currentValue.selection.start
                        val textBeforeCursor = currentValue.text.substring(0, cursorPosition)
                        val textAfterCursor = currentValue.text.substring(cursorPosition)
                        val newText = textBeforeCursor + code + textAfterCursor
                        val newCursorPosition = cursorPosition + code.length
                        
                        val newTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(
                            text = newText,
                            selection = androidx.compose.ui.text.TextRange(newCursorPosition)
                        )
                        
                        messageTextFieldValue.value = newTextFieldValue
                        viewModel.updateMessage(newText)
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
                Text(stringResource(R.string.clear_all))
            }
            
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LandscapeLayout(
    onNavigateToHelp: () -> Unit,
    selectorInput: String,
    messageTextFieldValue: MutableState<androidx.compose.ui.text.input.TextFieldValue>,
    javaCommand: String,
    bedrockCommand: String,
    warnings: List<String>,
    selectorType: SelectorType,
    isLoading: Boolean,
    showHistoryDialog: MutableState<Boolean>,
    viewModel: TellrawViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部应用栏（横屏紧凑版）
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.app_title),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(end = 12.dp)
                )
                IconButton(
                    onClick = { showHistoryDialog.value = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.History, 
                        contentDescription = stringResource(R.string.history),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { showSettingsDialog.value = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onNavigateToHelp,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Help,
                        contentDescription = stringResource(R.string.help),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 横屏左右分栏布局
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 左侧：输入区域
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 选择器输入区域
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.target_selector),
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        OutlinedTextField(
                            value = selectorInput,
                            onValueChange = { viewModel.updateSelector(it) },
                            label = { Text(stringResource(R.string.input_selector)) },
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
                                text = stringResource(R.string.detected_selector_type, if (selectorType == SelectorType.JAVA) stringResource(R.string.selector_type_java) else stringResource(R.string.selector_type_bedrock)),
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
                            text = stringResource(R.string.text_message),
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        OutlinedTextField(
                            value = messageTextFieldValue.value,
                            onValueChange = { 
                                messageTextFieldValue.value = it
                                viewModel.updateMessage(it.text)
                            },
                            label = { Text(stringResource(R.string.input_text)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            maxLines = 5
                        )
                        
                        // 颜色代码快速输入
                        ColorCodeQuickInput(
                            onCodeSelected = { code ->
                                val currentValue = messageTextFieldValue.value
                                val cursorPosition = currentValue.selection.start
                                val textBeforeCursor = currentValue.text.substring(0, cursorPosition)
                                val textAfterCursor = currentValue.text.substring(cursorPosition)
                                val newText = textBeforeCursor + code + textAfterCursor
                                val newCursorPosition = cursorPosition + code.length
                                
                                val newTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(
                                    text = newText,
                                    selection = androidx.compose.ui.text.TextRange(newCursorPosition)
                                )
                                
                                messageTextFieldValue.value = newTextFieldValue
                                viewModel.updateMessage(newText)
                            }
                        )
                    }
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
                        Text(stringResource(R.string.clear_all))
                    }
                    
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            // 右侧：输出区域
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
            }
        }
    }
}