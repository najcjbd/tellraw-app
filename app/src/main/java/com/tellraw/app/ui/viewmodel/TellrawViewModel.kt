package com.tellraw.app.ui.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tellraw.app.R
import com.tellraw.app.data.local.CommandHistory
import com.tellraw.app.data.remote.GithubRelease
import com.tellraw.app.data.repository.SettingsRepository
import com.tellraw.app.data.repository.TellrawRepository
import com.tellraw.app.data.repository.VersionCheckRepository
import com.tellraw.app.model.SelectorType
import com.tellraw.app.model.TellrawCommand
import com.tellraw.app.util.SelectorConverter
import com.tellraw.app.util.TextFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TellrawViewModel @Inject constructor(
    private val tellrawRepository: TellrawRepository,
    private val versionCheckRepository: VersionCheckRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _selectorInput = MutableStateFlow("")
    val selectorInput: StateFlow<String> = _selectorInput.asStateFlow()
    
    // 用于存储Context，在UI层设置
    private var context: Context? = null
    
    /**
     * 设置Context，由UI层在创建ViewModel后调用
     */
    fun setContext(context: Context) {
        this.context = context
        // 设置Context后初始化版本检查和加载设置
        initializeVersionCheck()
        viewModelScope.launch {
            loadSettings()
        }
    }
    
    private val _messageInput = MutableStateFlow("")
    val messageInput: StateFlow<String> = _messageInput.asStateFlow()
    
    private val _javaCommand = MutableStateFlow("")
    val javaCommand: StateFlow<String> = _javaCommand.asStateFlow()
    
    private val _bedrockCommand = MutableStateFlow("")
    val bedrockCommand: StateFlow<String> = _bedrockCommand.asStateFlow()
    
    private val _warnings = MutableStateFlow<List<String>>(emptyList())
    val warnings: StateFlow<List<String>> = _warnings.asStateFlow()
    
    private val _selectorType = MutableStateFlow(SelectorType.UNIVERSAL)
    val selectorType: StateFlow<SelectorType> = _selectorType.asStateFlow()
    
    private val _useJavaFontStyle = MutableStateFlow(true)
    val useJavaFontStyle: StateFlow<Boolean> = _useJavaFontStyle.asStateFlow()
    
    private val _mnMixedMode = MutableStateFlow(false)
    val mnMixedMode: StateFlow<Boolean> = _mnMixedMode.asStateFlow()
    
    private val _mnCFEnabled = MutableStateFlow(false)
    val mnCFEnabled: StateFlow<Boolean> = _mnCFEnabled.asStateFlow()
    
    /**
     * 加载用户设置
     */
    private suspend fun loadSettings() {
        val mnHandlingMode = settingsRepository.getMNHandlingMode()
        val mixedMode = settingsRepository.getMNMixedMode()
        val cfEnabled = settingsRepository.getMNCFEnabled()
        
        // 检查历史记录存储设置，如果配置了存储位置但没有权限，则清空设置
        val historyStorageUri = settingsRepository.getHistoryStorageUri()
        if (historyStorageUri != null && !hasStoragePermission()) {
            settingsRepository.setHistoryStorageUri("")
            _historyStorageUri.value = null
        }
        
        // 处理配置文件中的互斥逻辑
        if (mixedMode && cfEnabled) {
            // 同时开启混合模式和§m/§n_c/f，都改为false，保持原有的mn_handling_mode
            settingsRepository.setMNMixedMode(false)
            settingsRepository.setMNCFEnabled(false)
            _mnMixedMode.value = false
            _mnCFEnabled.value = false
            _useJavaFontStyle.value = mnHandlingMode
        } else if (mixedMode || cfEnabled) {
            // 有一个开启时，mn_handling_mode为空（不显示选项）
            _mnMixedMode.value = mixedMode
            _mnCFEnabled.value = cfEnabled
            _useJavaFontStyle.value = true // 默认值，但UI中不显示
        } else {
            // 都没开启时，使用原有的mn_handling_mode
            _mnMixedMode.value = false
            _mnCFEnabled.value = false
            _useJavaFontStyle.value = mnHandlingMode
        }
    }
    
    /**
     * 检查是否有存储权限
     */
    private fun hasStoragePermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Android 11+ 不需要传统存储权限
            true
        } else {
            // Android 10及以下需要检查存储权限
            context?.let { ctx ->
                ContextCompat.checkSelfPermission(
                    ctx,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    ctx,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } ?: false
        }
    }
    
    private val _showMNDialog = MutableStateFlow<String?>(null)
    val showMNDialog: StateFlow<String?> = _showMNDialog.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 跟踪§m和§n的使用次数
    private val _mCodeCount = MutableStateFlow(0)
    val mCodeCount: StateFlow<Int> = _mCodeCount.asStateFlow()
    
    private val _nCodeCount = MutableStateFlow(0)
    val nCodeCount: StateFlow<Int> = _nCodeCount.asStateFlow()
    
    // 上一次的消息内容，用于检测新增的§m和§n
    private var lastMessageContent = ""
    
    // 后台存储的消息（混合模式下转换为§m_f/§m_c/§n_f/§n_c）
    private var backendMessageContent = ""
    
    // 版本检查相关状态
    private val _showUpdateDialog = MutableStateFlow<GithubRelease?>(null)
    val showUpdateDialog: StateFlow<GithubRelease?> = _showUpdateDialog.asStateFlow()
    
    private val _showDisableCheckDialog = MutableStateFlow(false)
    val showDisableCheckDialog: StateFlow<Boolean> = _showDisableCheckDialog.asStateFlow()
    
    // 历史记录相关状态
    val commandHistory: Flow<List<CommandHistory>> = tellrawRepository.getCommandHistory()
    
    // 搜索结果状态
    private val _searchResults = MutableStateFlow<List<CommandHistory>>(emptyList())
    val searchResults: StateFlow<List<CommandHistory>> = _searchResults.asStateFlow()
    
    // SAF相关状态
    private val _historyStorageUri = MutableStateFlow<String?>(null)
    val historyStorageUri: StateFlow<String?> = _historyStorageUri.asStateFlow()
    
    private val _historyStorageFilename = MutableStateFlow("TellrawCommand.txt")
    val historyStorageFilename: StateFlow<String> = _historyStorageFilename.asStateFlow()
    
    private val _showStorageSettingsDialog = MutableStateFlow(false)
    val showStorageSettingsDialog: StateFlow<Boolean> = _showStorageSettingsDialog.asStateFlow()
    
    private val _showFilenameDialog = MutableStateFlow<String?>(null)
    val showFilenameDialog: StateFlow<String?> = _showFilenameDialog.asStateFlow()
    
    private val _showFileExistsDialog = MutableStateFlow<String?>(null)
    val showFileExistsDialog: StateFlow<String?> = _showFileExistsDialog.asStateFlow()
    
    private val _isWritingToFile = MutableStateFlow(false)
    val isWritingToFile: StateFlow<Boolean> = _isWritingToFile.asStateFlow()
    
    private val _writeFileMessage = MutableStateFlow<String?>(null)
    val writeFileMessage: StateFlow<String?> = _writeFileMessage.asStateFlow()
    
    fun updateSelector(selector: String) {
        _selectorInput.value = selector
        detectSelectorType()
        generateCommands()
    }
    
    fun updateMessage(message: String) {
        _messageInput.value = message
        
        // 检测新增的§m和§n代码
        detectAndCountMNCodes(message)
        
        // 始终生成命令，不等待对话框关闭
        generateCommands()
    }
    
    /**
     * 检测并计数§m和§n代码的使用
     */
    private fun detectAndCountMNCodes(message: String) {
        // 如果启用了_c/_f后缀，不检测普通的§m§n
        if (_mnCFEnabled.value) {
            // 更新上一次的消息内容
            lastMessageContent = message
            backendMessageContent = message
            return
        }
        
        // 统计当前消息中所有的§m和§n数量
        val currentMCount = countOccurrences(message, "§m")
        val currentNCount = countOccurrences(message, "§n")
        
        // 检测新增的§m代码
        val newMCount = currentMCount - countOccurrences(lastMessageContent, "§m")
        if (newMCount > 0) {
            _mCodeCount.value += newMCount
            // 只在混合模式下弹出对话框
            if (_mnMixedMode.value) {
                _showMNDialog.value = "§m"
            }
        }
        
        // 检测新增的§n代码
        val newNCount = currentNCount - countOccurrences(lastMessageContent, "§n")
        if (newNCount > 0) {
            _nCodeCount.value += newNCount
            // 只在混合模式下弹出对话框
            if (_mnMixedMode.value) {
                _showMNDialog.value = "§n"
            }
        }
        
        // 更新上一次的消息内容
        lastMessageContent = message
        
        // 在混合模式下，更新后台存储
        if (_mnMixedMode.value) {
            updateBackendMessage(message)
        } else {
            backendMessageContent = message
        }
    }
    
    /**
     * 更新后台消息（混合模式下将§m/§n转换为§m_f/§m_c/§n_f/§n_c）
     */
    private fun updateBackendMessage(frontendMessage: String) {
        // 将§m/§n转换为§m_f/§m_c/§n_f/§n_c
        // 这里暂时全部转换为§m_f/§n_f（字体方式），用户选择后会更新
        var result = frontendMessage
        result = result.replace("§m", "§m_f")
        result = result.replace("§n", "§n_f")
        backendMessageContent = result
    }
    
    /**
     * 处理混合模式下的§m/§n选择
     * @param codeType §m或§n
     * @param choice "font"或"color"
     */
    fun handleMixedModeChoice(codeType: String, choice: String) {
        if (_mnMixedMode.value) {
            // 找到第一个未选择的§m_f/§n_f并更新为§m_c/§n_c
            val targetCode = if (codeType == "§m") "§m_f" else "§n_f"
            val replacementCode = if (choice == "color") {
                if (codeType == "§m") "§m_c" else "§n_c"
            } else {
                targetCode
            }
            
            if (choice == "font") {
                // 选择字体方式，不需要修改
                return
            }
            
            // 找到第一个§m_f/§n_f并替换为§m_c/§n_c
            val index = backendMessageContent.indexOf(targetCode)
            if (index != -1) {
                backendMessageContent = backendMessageContent.substring(0, index) + replacementCode + backendMessageContent.substring(index + 3)
            }
        }
    }
    
    /**
     * 计算字符串中某个子串的出现次数
     */
    private fun countOccurrences(text: String, substring: String): Int {
        var count = 0
        var index = 0
        while (index < text.length) {
            val foundIndex = text.indexOf(substring, index)
            if (foundIndex != -1) {
                count++
                index = foundIndex + substring.length
            } else {
                break
            }
        }
        return count
    }
    
    fun setUseJavaFontStyle(useJavaFont: Boolean) {
        _useJavaFontStyle.value = useJavaFont
        // 保存设置到数据库
        viewModelScope.launch {
            settingsRepository.setMNHandlingMode(useJavaFont)
        }
        generateCommands()
    }
    
    fun setMNMixedMode(enabled: Boolean) {
        _mnMixedMode.value = enabled
        // 保存设置到数据库
        viewModelScope.launch {
            settingsRepository.setMNMixedMode(enabled)
            // 如果开启混合模式，清空mn_handling_mode（设置为默认值）
            if (enabled) {
                settingsRepository.setMNHandlingMode(true)
                _useJavaFontStyle.value = true
            }
        }
        generateCommands()
    }
    
    fun setMNCFEnabled(enabled: Boolean) {
        _mnCFEnabled.value = enabled
        // 保存设置到数据库
        viewModelScope.launch {
            settingsRepository.setMNCFEnabled(enabled)
            // 如果启用了_c/_f后缀，自动关闭混合模式
            if (enabled && _mnMixedMode.value) {
                settingsRepository.setMNMixedMode(false)
                _mnMixedMode.value = false
            }
            // 如果开启§m/§n_c/f，清空mn_handling_mode（设置为默认值）
            if (enabled) {
                settingsRepository.setMNHandlingMode(true)
                _useJavaFontStyle.value = true
            }
        }
        generateCommands()
    }
    
    fun dismissMNDialog() {
        _showMNDialog.value = null
        generateCommands()
    }
    
    private fun detectSelectorType() {
        val selector = _selectorInput.value
        if (selector.isNotEmpty()) {
            _selectorType.value = SelectorConverter.detectSelectorType(selector)
        }
    }
    
    /**
     * 生成tellraw命令 - 与Python版本的generate_tellraw_commands函数逻辑完全一致
     */
    private fun generateTellrawCommands(
        selector: String, 
        message: String, 
        mNHandling: String = "none",
        mnCFEnabled: Boolean = false
    ): TellrawCommand {
        val allWarnings = mutableListOf<String>()
        
        // 检测选择器类型（与Python版本的detect_selector_type函数逻辑一致）
        val detectedType = SelectorConverter.detectSelectorType(selector)
        
        // 将基岩版选择器转换为Java版（如果需要）
        val javaSelectorConversion = SelectorConverter.convertBedrockToJava(selector, context ?: return TellrawCommand("", "", emptyList()))
        val wasConverted = javaSelectorConversion.wasConverted
        val originalSelector = if (wasConverted) selector else null
        val convertedSelector = if (wasConverted) javaSelectorConversion.javaSelector else null
        
        // 转换gamemode/m参数并收集提醒
        val (javaSelectorConverted, bedrockSelectorConverted, javaGamemodeReminders, bedrockGamemodeReminders) = 
            javaSelectorConversion
        
        // 根据原始选择器类型选择转换后的选择器作为基础
        val (javaSelectorFinal, bedrockSelectorFinal) = when (detectedType) {
            SelectorType.BEDROCK -> {
                // 对于基岩版输入：
                // 基岩版输出直接使用原始输入
                // Java版输出需要将基岩版参数转换为Java版
                val javaFinal = if (wasConverted) javaSelectorConversion.javaSelector else bedrockSelectorConverted
                Pair(javaFinal, selector)
            }
            else -> {
                // 对于Java版输入：
                // Java版输出直接使用原始输入
                // 基岩版输出需要将Java版参数转换为基岩版
                Pair(selector, javaSelectorConverted)
            }
        }

        // 过滤Java版参数，移除基岩版特有的参数（完全不支持）
        val (javaSelectorFiltered, javaRemovedParams) = SelectorConverter.filterSelectorParameters(javaSelectorFinal, SelectorType.JAVA, context ?: return TellrawCommand("", "", emptyList()))

        // 过滤基岩版参数，移除Java版特有的参数（完全不支持）
        val (bedrockSelectorFiltered, bedrockRemovedParams) = SelectorConverter.filterSelectorParameters(bedrockSelectorFinal, SelectorType.BEDROCK, context ?: return TellrawCommand("", "", emptyList()))
        
        // 合并所有Java版提醒信息并去重
        val allJavaReminders = mutableListOf<String>()
        allJavaReminders.addAll(javaGamemodeReminders)
        allJavaReminders.addAll(javaRemovedParams)
        if (wasConverted) allJavaReminders.addAll(javaSelectorConversion.javaReminders)
        
        // 合并所有基岩版提醒信息并去重
        val allBedrockReminders = mutableListOf<String>()
        allBedrockReminders.addAll(bedrockGamemodeReminders)
        allBedrockReminders.addAll(bedrockRemovedParams)
        
        // 生成Java版命令
        val javaJson = TextFormatter.convertToJavaJson(message, mNHandling, mnCFEnabled)
        val javaCommand = "tellraw $javaSelectorFiltered $javaJson"
        
        // 生成基岩版命令
        val bedrockJson = TextFormatter.convertToBedrockJson(message, mNHandling, mnCFEnabled)
        val bedrockCommand = "tellraw $bedrockSelectorFiltered $bedrockJson"
        
        // 处理提醒信息显示
        // 分类并去重处理Java版提醒
        val javaNonReminders = allJavaReminders.filter { 
            !it.startsWith("Java版") && !it.startsWith("基岩版") && 
            "nbt参数" !in it && "参数已转换为" !in it && "已转换为" !in it 
        }.distinct()
        
        val javaSpecificReminders = allJavaReminders.filter { 
            "参数已转换为" in it || "nbt参数" in it || 
            "已转换为" in it || (it.startsWith("Java版") || it.startsWith("基岩版"))
        }.distinct()
        
        // 分类并去重处理基岩版提醒
        val bedrockNonReminders = allBedrockReminders.filter { 
            !it.startsWith("Java版") && !it.startsWith("基岩版") && 
            "nbt参数" !in it && "参数已转换为" !in it && "已转换为" !in it 
        }.distinct()
        
        val bedrockSpecificReminders = allBedrockReminders.filter { 
            "参数已转换为" in it || "nbt参数" in it || 
            "已转换为" in it || (it.startsWith("Java版") || it.startsWith("基岩版"))
        }.distinct()
        
        // 添加Java版参数剔除提醒
        if (javaNonReminders.isNotEmpty()) {
            context?.let { ctx ->
                allWarnings.add(ctx.getString(R.string.note_java_unsupported_params, javaNonReminders.joinToString(", ")))
            }
        }
        
        // 添加Java版特殊提醒
        javaSpecificReminders.forEach { reminder ->
            context?.let { ctx ->
                allWarnings.add(ctx.getString(R.string.note_java_specific, reminder))
            }
        }
        
        // 添加基岩版参数剔除提醒
        if (bedrockNonReminders.isNotEmpty()) {
            context?.let { ctx ->
                allWarnings.add(ctx.getString(R.string.note_bedrock_unsupported_params, bedrockNonReminders.joinToString(", ")))
            }
        }
        
        // 添加基岩版特殊提醒
        bedrockSpecificReminders.forEach { reminder ->
            context?.let { ctx ->
                allWarnings.add(ctx.getString(R.string.note_java_specific, reminder))
            }
        }
        
        // 添加选择器转换提醒
        if (wasConverted && originalSelector != null && convertedSelector != null) {
            context?.let { ctx ->
                allWarnings.add(ctx.getString(R.string.note_bedrock_selector_converted, originalSelector, convertedSelector))
            }
        }
        
        return TellrawCommand(javaCommand, bedrockCommand, allWarnings)
    }

    private fun generateCommands() {
        viewModelScope.launch {
            _isLoading.value = true
            
            val selector = _selectorInput.value
            val message = _messageInput.value
            
            if (selector.isEmpty() || message.isEmpty()) {
                _javaCommand.value = ""
                _bedrockCommand.value = ""
                _warnings.value = emptyList()
                _isLoading.value = false
                return@launch
            }
            
            try {
                // 确定要使用的消息（混合模式下使用后台存储）
                val messageToUse = if (_mnMixedMode.value) backendMessageContent else message
                
                // 确定m_n_handling参数
                val mNHandling = when {
                    _mnMixedMode.value -> "font"  // 混合模式下使用font模式，因为已经转换为§m_f/§m_c/§n_f/§n_c
                    _useJavaFontStyle.value -> "font"
                    else -> "color"
                }
                
                // 使用统一的命令生成函数
                val result = generateTellrawCommands(selector, messageToUse, mNHandling, true)
                
                _javaCommand.value = result.javaCommand
                _bedrockCommand.value = result.bedrockCommand
                _warnings.value = result.warnings
            } catch (e: IllegalArgumentException) {
                // 处理未知§组合的异常
                _javaCommand.value = ""
                _bedrockCommand.value = ""
                _warnings.value = listOf(e.message ?: context?.getString(R.string.command_generation_error) ?: "Command generation error")
            } catch (e: Exception) {
                // 处理其他可能的异常，防止卡死
                _javaCommand.value = ""
                _bedrockCommand.value = ""
                _warnings.value = listOf(context?.getString(R.string.command_generation_error_with_message, e.message ?: "") ?: "Command generation error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 生成Java版tellraw命令，与Python版本一致
     */
    private fun generateJavaCommand(
        selector: String,
        message: String,
        mNHandling: String,
        mnCFEnabled: Boolean = false
    ): String {
        // 过滤Java版不支持的参数
        val (filteredSelector, _) = SelectorConverter.filterSelectorParameters(selector, SelectorType.JAVA, context ?: return "")
        
        // 转换文本为Java版JSON格式
        val javaJson = TextFormatter.convertToJavaJson(message, mNHandling, mnCFEnabled)
        return "tellraw $filteredSelector $javaJson"
    }
    
    /**
     * 生成基岩版tellraw命令，与Python版本一致
     */
    private fun generateBedrockCommand(
        selector: String,
        message: String,
        mNHandling: String,
        mnCFEnabled: Boolean = false
    ): String {
        // 过滤基岩版不支持的参数
        val (filteredSelector, _) = SelectorConverter.filterSelectorParameters(selector, SelectorType.BEDROCK, context ?: return "")
        
        // 转换文本为基岩版JSON格式
        val bedrockJson = TextFormatter.convertToBedrockJson(message, mNHandling, mnCFEnabled)
        return "tellraw $filteredSelector $bedrockJson"
    }
    
    fun copyToClipboard(text: String) {
        context?.let { ctx ->
            val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Tellraw Command", text)
            clipboard.setPrimaryClip(clip)
        }
    }
    
    fun shareCommand(command: String) {
        context?.let { ctx ->
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, command)
                putExtra(Intent.EXTRA_SUBJECT, "Minecraft Tellraw Command")
            }
            ctx.startActivity(Intent.createChooser(shareIntent, ctx.getString(R.string.share_tellraw_command)))
        }
    }
    
    fun clearAll() {
        val selector = _selectorInput.value
        val message = _messageInput.value
        val javaCommand = _javaCommand.value
        val bedrockCommand = _bedrockCommand.value
        
        // 如果有内容，先保存到历史记录
        if (selector.isNotEmpty() || message.isNotEmpty()) {
            viewModelScope.launch {
                tellrawRepository.saveCommandToHistory(
                    selector = selector,
                    message = message,
                    javaCommand = javaCommand,
                    bedrockCommand = bedrockCommand
                )
            }
        }
        
        _selectorInput.value = ""
        _messageInput.value = ""
        _javaCommand.value = ""
        _bedrockCommand.value = ""
        _warnings.value = emptyList()
        _selectorType.value = SelectorType.UNIVERSAL
        _useJavaFontStyle.value = true
        
        // 重置§m和§n计数器
        _mCodeCount.value = 0
        _nCodeCount.value = 0
        lastMessageContent = ""
        
        // 重置对话框状态
        _showMNDialog.value = null
    }
    
    // 历史记录管理函数
    
    /**
     * 删除指定的历史记录
     */
    fun deleteHistoryItem(history: CommandHistory) {
        viewModelScope.launch {
            tellrawRepository.deleteCommandHistory(history)
        }
    }
    
    /**
     * 清空所有历史记录
     */
    fun clearAllHistory() {
        viewModelScope.launch {
            // 在清空历史记录前，先保存当前输入的内容
            val selector = _selectorInput.value
            val message = _messageInput.value
            val javaCommand = _javaCommand.value
            val bedrockCommand = _bedrockCommand.value
            
            // 如果有内容，先保存到历史记录
            if (selector.isNotEmpty() || message.isNotEmpty()) {
                tellrawRepository.saveCommandToHistory(
                    selector = selector,
                    message = message,
                    javaCommand = javaCommand,
                    bedrockCommand = bedrockCommand
                )
            }
            
            tellrawRepository.clearAllHistory()
        }
    }
    
    /**
     * 从历史记录加载命令
     */
    fun loadFromHistory(history: CommandHistory) {
        _selectorInput.value = history.selector
        _messageInput.value = history.message
        _javaCommand.value = history.javaCommand
        _bedrockCommand.value = history.bedrockCommand
        detectSelectorType()
        
        // 重置§m和§n计数器
        _mCodeCount.value = 0
        _nCodeCount.value = 0
        lastMessageContent = history.message
        
        // 重置对话框状态
        _showMNDialog.value = null
    }
    
    // 搜索历史记录
    fun searchHistory(query: String) {
        viewModelScope.launch {
            tellrawRepository.searchHistory(query).collect { results ->
                _searchResults.value = results
            }
        }
    }
    
    // 根据ID获取历史记录
    suspend fun getHistoryById(id: Long): CommandHistory? {
        return tellrawRepository.getHistoryById(id)
    }
    
    // 版本检查相关方法
    
    /**
     * 初始化版本检查
     */
    private fun initializeVersionCheck() {
        context?.let { ctx ->
            // 保存当前版本（从BuildConfig获取）
            try {
                val version = ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
                versionCheckRepository.saveCurrentVersion(version)
            } catch (e: Exception) {
                // 如果获取版本失败，使用默认值
                versionCheckRepository.saveCurrentVersion("1.0.0")
            }
            
            // 检查更新
            checkForUpdates()
        }
    }
    
    /**
     * 检查更新
     */
    fun checkForUpdates() {
        viewModelScope.launch {
            val result = versionCheckRepository.checkForUpdates()
            result.onSuccess { release ->
                if (release != null) {
                    _showUpdateDialog.value = release
                }
            }
        }
    }
    
    /**
     * 打开下载链接
     */
    fun openDownloadUrl(url: String) {
        context?.let { ctx ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            ctx.startActivity(intent)
        }
    }
    
    /**
     * 关闭更新对话框
     */
    fun dismissUpdateDialog() {
        _showUpdateDialog.value = null
    }
    
    /**
     * 显示禁用版本检查对话框
     */
    fun showDisableCheckDialog() {
        _showDisableCheckDialog.value = true
    }
    
    /**
     * 关闭禁用版本检查对话框
     */
    fun dismissDisableCheckDialog() {
        _showDisableCheckDialog.value = false
    }
    
    /**
     * 禁用版本检查
     */
    fun disableVersionCheck() {
        versionCheckRepository.disableVersionCheck()
        dismissUpdateDialog()
        dismissDisableCheckDialog()
    }
    
    /**
     * 获取版本检查状态
     */
    fun isVersionCheckDisabled(): Boolean {
        return versionCheckRepository.isVersionCheckDisabled()
    }
    
    // SAF相关方法
    
    /**
     * 加载历史记录存储设置
     */
    private suspend fun loadHistoryStorageSettings() {
        _historyStorageUri.value = settingsRepository.getHistoryStorageUri()
        _historyStorageFilename.value = settingsRepository.getHistoryStorageFilename()
    }
    
    /**
     * 显示存储设置对话框
     */
    fun showStorageSettings() {
        viewModelScope.launch {
            loadHistoryStorageSettings()
            _showStorageSettingsDialog.value = true
        }
    }
    
    /**
     * 隐藏存储设置对话框
     */
    fun hideStorageSettingsDialog() {
        _showStorageSettingsDialog.value = false
    }
    
    /**
     * 设置历史记录存储目录URI
     */
    fun setHistoryStorageUri(uri: String) {
        viewModelScope.launch {
            settingsRepository.setHistoryStorageUri(uri)
            _historyStorageUri.value = uri
        }
    }
    
    /**
     * 显示文件名对话框
     */
    fun showFilenameDialog() {
        _showFilenameDialog.value = _historyStorageFilename.value
    }
    
    /**
     * 隐藏文件名对话框
     */
    fun hideFilenameDialog() {
        _showFilenameDialog.value = null
    }
    
    /**
     * 设置历史记录存储文件名
     */
    fun setHistoryStorageFilename(filename: String) {
        viewModelScope.launch {
            settingsRepository.setHistoryStorageFilename(filename)
            _historyStorageFilename.value = filename
        }
    }
    
    /**
     * 显示文件已存在对话框
     */
    fun showFileExistsDialog(filename: String) {
        _showFileExistsDialog.value = filename
    }
    
    /**
     * 隐藏文件已存在对话框
     */
    fun hideFileExistsDialog() {
        _showFileExistsDialog.value = null
    }
    
    /**
     * 清除历史记录存储设置
     */
    fun clearHistoryStorageSettings() {
        viewModelScope.launch {
            settingsRepository.setHistoryStorageUri("")
            settingsRepository.setHistoryStorageFilename("TellrawCommand.txt")
            _historyStorageUri.value = null
            _historyStorageFilename.value = "TellrawCommand.txt"
        }
    }
    
    /**
     * 将历史记录写入文件
     */
    fun writeHistoryToFile(context: Context, historyList: List<CommandHistory>) {
        viewModelScope.launch {
            _isWritingToFile.value = true
            _writeFileMessage.value = null
            
            try {
                val uriString = settingsRepository.getHistoryStorageUri()
                val filename = settingsRepository.getHistoryStorageFilename()
                
                if (uriString == null) {
                    // 未选择文件夹，存储在Android/data沙盒中
                    writeToFileInSandbox(context, filename, historyList)
                    return@launch
                }
                
                val uri = Uri.parse(uriString)
                val contentResolver = context.contentResolver
                
                // 检查文件是否存在
                val existingUri = findFileInDirectory(contentResolver, uri, filename)
                
                if (existingUri != null) {
                    // 文件已存在，询问用户是否使用现有文件
                    _showFileExistsDialog.value = filename
                    _isWritingToFile.value = false
                    return@launch
                }
                
                // 文件不存在，直接创建新文件
                val fileUri = createFileInDirectory(contentResolver, uri, filename)
                
                if (fileUri == null) {
                    _writeFileMessage.value = context.getString(R.string.create_file_failed)
                    _isWritingToFile.value = false
                    return@launch
                }
                
                // 生成历史记录内容
                val content = buildHistoryContent(historyList)
                
                // 写入文件
                contentResolver.openOutputStream(fileUri, "w")?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                }
                
                _writeFileMessage.value = context.getString(R.string.write_success, historyList.size)
            } catch (e: Exception) {
                _writeFileMessage.value = context.getString(R.string.create_file_failed) + ": ${e.message}"
            } finally {
                _isWritingToFile.value = false
            }
        }
    }
    
    /**
     * 将历史记录写入应用沙盒
     */
    private suspend fun writeToFileInSandbox(context: Context, filename: String, historyList: List<CommandHistory>) {
        try {
            val file = java.io.File(context.filesDir, filename)
            
            // 生成历史记录内容
            val content = buildHistoryContent(historyList)
            
            // 写入文件
            file.writeText(content)
            
            _writeFileMessage.value = context.getString(R.string.write_success, historyList.size)
        } catch (e: Exception) {
            _writeFileMessage.value = context.getString(R.string.create_file_failed) + ": ${e.message}"
        } finally {
            _isWritingToFile.value = false
        }
    }
    
    /**
     * 使用现有文件追加历史记录
     */
    fun appendToExistingFile(context: Context, historyList: List<CommandHistory>) {
        viewModelScope.launch {
            _isWritingToFile.value = true
            _writeFileMessage.value = null
            
            try {
                val uriString = settingsRepository.getHistoryStorageUri()
                val filename = settingsRepository.getHistoryStorageFilename()
                
                if (uriString == null) {
                    // 未选择文件夹，在沙盒中追加
                    appendToFileInSandbox(context, filename, historyList)
                    return@launch
                }
                
                val uri = Uri.parse(uriString)
                val contentResolver = context.contentResolver
                
                // 查找文件
                val fileUri = findFileInDirectory(contentResolver, uri, filename)
                
                if (fileUri == null) {
                    _writeFileMessage.value = context.getString(R.string.file_not_found)
                    _isWritingToFile.value = false
                    return@launch
                }
                
                // 生成历史记录内容
                val content = buildHistoryContent(historyList)
                
                // 追加写入文件
                contentResolver.openOutputStream(fileUri, "wa")?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                }
                
                _writeFileMessage.value = context.getString(R.string.append_success, historyList.size)
            } catch (e: Exception) {
                _writeFileMessage.value = context.getString(R.string.append_file_failed) + ": ${e.message}"
            } finally {
                _isWritingToFile.value = false
            }
        }
    }
    
    /**
     * 在应用沙盒中追加历史记录
     */
    private suspend fun appendToFileInSandbox(context: Context, filename: String, historyList: List<CommandHistory>) {
        try {
            val file = java.io.File(context.filesDir, filename)
            
            // 生成历史记录内容
            val content = buildHistoryContent(historyList)
            
            // 追加写入文件
            file.appendText(content)
            
            _writeFileMessage.value = context.getString(R.string.append_success, historyList.size)
        } catch (e: Exception) {
            _writeFileMessage.value = context.getString(R.string.append_file_failed) + ": ${e.message}"
        } finally {
            _isWritingToFile.value = false
        }
    }
    
    /**
     * 在目录中查找文件
     */
    private suspend fun findFileInDirectory(
        contentResolver: ContentResolver,
        directoryUri: Uri,
        filename: String
    ): Uri? {
        return try {
            val docId = DocumentsContract.getDocumentId(directoryUri)
            if (docId.isNullOrEmpty()) {
                android.util.Log.e("TellrawViewModel", "Invalid directory URI")
                return null
            }
            
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                directoryUri,
                docId
            )
            
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            )
            
            contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val docIdIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                
                if (docIdIndex == -1 || nameIndex == -1) {
                    android.util.Log.e("TellrawViewModel", "Invalid cursor indices")
                    return@use null
                }
                
                while (cursor.moveToNext()) {
                    val currentDocId = cursor.getString(docIdIndex)
                    val name = cursor.getString(nameIndex)
                    
                    if (name == filename && currentDocId != null) {
                        return DocumentsContract.buildDocumentUriUsingTree(directoryUri, currentDocId)
                    }
                }
            }
            
            null
        } catch (e: SecurityException) {
            android.util.Log.e("TellrawViewModel", "SecurityException when finding file: ${e.message}")
            null
        } catch (e: IllegalArgumentException) {
            android.util.Log.e("TellrawViewModel", "IllegalArgumentException when finding file: ${e.message}")
            null
        } catch (e: Exception) {
            android.util.Log.e("TellrawViewModel", "Exception when finding file: ${e.message}", e)
            null
        }
    }
    
    /**
     * 在目录中创建文件
     */
    private suspend fun createFileInDirectory(
        contentResolver: ContentResolver,
        directoryUri: Uri,
        filename: String
    ): Uri? {
        return try {
            // 检查目录URI是否有效
            val docId = DocumentsContract.getDocumentId(directoryUri)
            if (docId.isNullOrEmpty()) {
                android.util.Log.e("TellrawViewModel", "Invalid directory URI")
                return null
            }

            // 尝试创建文件
            val fileUri = DocumentsContract.createDocument(
                contentResolver,
                directoryUri,
                "text/plain",
                filename
            )
            
            if (fileUri == null) {
                android.util.Log.e("TellrawViewModel", "Failed to create document: $filename")
            }
            
            fileUri
        } catch (e: SecurityException) {
            android.util.Log.e("TellrawViewModel", "SecurityException when creating file: ${e.message}")
            null
        } catch (e: IllegalArgumentException) {
            android.util.Log.e("TellrawViewModel", "IllegalArgumentException when creating file: ${e.message}")
            null
        } catch (e: Exception) {
            android.util.Log.e("TellrawViewModel", "Exception when creating file: ${e.message}", e)
            null
        }
    }
    
    /**
     * 构建历史记录内容
     */
    private fun buildHistoryContent(historyList: List<CommandHistory>): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        
        return buildString {
            historyList.forEach { history ->
                val timeText = dateFormat.format(java.util.Date(history.timestamp))
                context?.let { ctx ->
                    append(ctx.getString(R.string.history_command, "${history.selector} ${history.message}") + "\n")
                    append(ctx.getString(R.string.history_java_command, history.javaCommand) + "\n")
                    append(ctx.getString(R.string.history_bedrock_command, history.bedrockCommand) + "\n")
                    append(ctx.getString(R.string.history_time, timeText) + "\n")
                }
                append("========================================\n")
            }
        }
    }
    
    }