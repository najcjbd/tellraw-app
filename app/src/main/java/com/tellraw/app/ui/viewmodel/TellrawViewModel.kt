package com.tellraw.app.ui.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tellraw.app.MainActivity
import com.tellraw.app.R
import com.tellraw.app.data.repository.HistoryItem
import com.tellraw.app.data.repository.HistoryRepository
import com.tellraw.app.data.repository.SettingsRepository
import com.tellraw.app.data.repository.VersionCheckRepository
import com.tellraw.app.data.remote.GithubRelease
import com.tellraw.app.model.SelectorType
import com.tellraw.app.model.TellrawCommand
import com.tellraw.app.util.SelectorConverter
import com.tellraw.app.util.TextFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TellrawViewModel @Inject constructor(
    private val versionCheckRepository: VersionCheckRepository,
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
    @Suppress("StaticFieldLeak")
    @ApplicationContext private val applicationContext: Context  // 注入Application Context，不会造成内存泄漏
) : ViewModel() {
    
    private val _selectorInput = MutableStateFlow("")
    val selectorInput: StateFlow<String> = _selectorInput.asStateFlow()
    
    /**
     * 初始化版本检查和加载设置
     */
    fun initialize() {
        initializeVersionCheck()
        viewModelScope.launch {
            // 先从JSON文件加载配置
            settingsRepository.loadConfig()
            // 然后加载设置
            loadSettings()
            // 加载历史记录
            historyRepository.init()
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
    
    private val _javaBedrockMixedMode = MutableStateFlow(false)
    val javaBedrockMixedMode: StateFlow<Boolean> = _javaBedrockMixedMode.asStateFlow()
    
    /**
     * 加载用户设置
     */
    private suspend fun loadSettings() {
        val mnHandlingMode = settingsRepository.getMNHandlingMode()
        val mixedMode = settingsRepository.getMNMixedMode()
        val cfEnabled = settingsRepository.getMNCFEnabled()
        val javaBedrockMixedMode = settingsRepository.getJavaBedrockMixedMode()
        
        // 检查历史记录存储设置，如果配置了存储位置但没有权限，则清空设置
        val historyStorageUri = settingsRepository.getHistoryStorageUri()
        if (historyStorageUri != null && !hasStoragePermission()) {
            settingsRepository.setHistoryStorageUri("")
            _historyStorageUri.value = null
        }
        
        // 处理配置文件中的互斥逻辑
        // 三个选项是互斥的：混合模式、选择§m/§n的处理、§m/§n_c/f
        // 当检测到两个及以上开启时，默认关闭其他的，只开启"选择§m/§n的处理"
        val enabledCount = listOf(mixedMode, cfEnabled, true).count { it }
        
        if (enabledCount >= 2) {
            // 两个及以上开启，只保留"选择§m/§n的处理"
            settingsRepository.setMNMixedMode(false)
            settingsRepository.setMNCFEnabled(false)
            _mnMixedMode.value = false
            _mnCFEnabled.value = false
            // 保留原有的mn_handling_mode
            _useJavaFontStyle.value = mnHandlingMode
        } else if (mixedMode) {
            // 只开启混合模式
            _mnMixedMode.value = true
            _mnCFEnabled.value = false
            _useJavaFontStyle.value = true // 默认值，但UI中不显示
        } else if (cfEnabled) {
            // 只开启§m/§n_c/f
            _mnMixedMode.value = false
            _mnCFEnabled.value = true
            _useJavaFontStyle.value = true // 默认值，但UI中不显示
        } else {
            // 都没开启时，使用原有的mn_handling_mode
            _mnMixedMode.value = false
            _mnCFEnabled.value = false
            _useJavaFontStyle.value = mnHandlingMode
        }
        
        // 加载JAVA/基岩混合模式
        _javaBedrockMixedMode.value = javaBedrockMixedMode
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
            ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
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
    
    // frontendMessage中§m/§n的位置映射（混合模式下使用）
    // 记录每个§m/§n在frontendMessage中的位置和对应的backendMessage中的位置和类型
    private data class MNMapping(
        val frontendPos: Int,          // 在frontendMessage中的位置
        val backendPos: Int,           // 在backendMessage中的位置
        val type: String,              // "m" 或 "n"
        val mode: String               // "f" (font) 或 "c" (color)
    )
    private var mnMappings = mutableListOf<MNMapping>()
    
    // 版本检查相关状态
    private val _showUpdateDialog = MutableStateFlow<GithubRelease?>(null)
    val showUpdateDialog: StateFlow<GithubRelease?> = _showUpdateDialog.asStateFlow()
    
    private val _showDisableCheckDialog = MutableStateFlow(false)
    val showDisableCheckDialog: StateFlow<Boolean> = _showDisableCheckDialog.asStateFlow()
    
    private val _showJavaBedrockMixedModeWarningDialog = MutableStateFlow(false)
    val showJavaBedrockMixedModeWarningDialog: StateFlow<Boolean> = _showJavaBedrockMixedModeWarningDialog.asStateFlow()
    
    fun showJavaBedrockMixedModeWarningDialog() {
        _showJavaBedrockMixedModeWarningDialog.value = true
    }

    fun dismissJavaBedrockMixedModeWarningDialog() {
        _showJavaBedrockMixedModeWarningDialog.value = false
    }
    
    // 历史记录相关状态
    val commandHistory: Flow<List<HistoryItem>> = historyRepository.historyList
    
    // 搜索结果状态
    private val _searchResults = MutableStateFlow<List<HistoryItem>>(emptyList())
    val searchResults: StateFlow<List<HistoryItem>> = _searchResults.asStateFlow()
    
    // 历史记录存储相关状态
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
        
        // 更新计数器为实际数量（修复删除时计数器不准确的问题）
        _mCodeCount.value = currentMCount
        _nCodeCount.value = currentNCount
    }
    
    /**
     * 更新后台消息（混合模式下将§m/§n转换为§m_f/§m_c/§n_f/§n_c）
     * 使用位置映射来保留用户的选择
     */
    private fun updateBackendMessage(frontendMessage: String) {
        // 构建新的backendMessage
        val newBackend = StringBuilder()
        val newMappings = mutableListOf<MNMapping>()
        
        var frontendIndex = 0
        var backendIndex = 0
        
        while (frontendIndex < frontendMessage.length) {
            // 检查是否是§m或§n
            if (frontendIndex + 1 < frontendMessage.length && 
                (frontendMessage[frontendIndex] == '§') &&
                (frontendMessage[frontendIndex + 1] == 'm' || frontendMessage[frontendIndex + 1] == 'n')) {
                
                val code = frontendMessage.substring(frontendIndex, frontendIndex + 2)
                val type = if (code == "§m") "m" else "n"
                
                // 检查是否在旧映射中存在
                val oldMapping = mnMappings.find { it.frontendPos == frontendIndex }
                
                if (oldMapping != null) {
                    // 保留旧的选择
                    val backendCode = if (oldMapping.type == "m") {
                        "§m_${oldMapping.mode}"
                    } else {
                        "§n_${oldMapping.mode}"
                    }
                    newBackend.append(backendCode)
                    newMappings.add(MNMapping(
                        frontendPos = frontendIndex,
                        backendPos = backendIndex,
                        type = type,
                        mode = oldMapping.mode
                    ))
                    backendIndex += 4
                } else {
                    // 新增的§m/§n，默认使用字体方式
                    val backendCode = if (type == "m") "§m_f" else "§n_f"
                    newBackend.append(backendCode)
                    newMappings.add(MNMapping(
                        frontendPos = frontendIndex,
                        backendPos = backendIndex,
                        type = type,
                        mode = "f"
                    ))
                    backendIndex += 4
                }
                
                frontendIndex += 2
            } else {
                // 普通字符
                newBackend.append(frontendMessage[frontendIndex])
                frontendIndex++
                backendIndex++
            }
        }
        
        backendMessageContent = newBackend.toString()
        mnMappings = newMappings
    }
    
    /**
     * 处理混合模式下的§m/§n选择
     * @param codeType §m或§n
     * @param choice "font"表示方式一（Java版用字体模式，基岩版用颜色模式），"color"表示方式二（两版都用颜色模式）
     */
    fun handleMixedModeChoice(codeType: String, choice: String) {
        if (_mnMixedMode.value) {
            val type = if (codeType == "§m") "m" else "n"
            
            if (choice == "font") {
                // 方式一：Java版用字体模式（§m_f/§n_f），基岩版用颜色模式（§m/§n）
                // 不需要修改后台消息，保持§m_f/§n_f
                // 在生成命令时，Java版会使用§m_f/§n_f（字体方式），基岩版会将§m_f/§n_f转换为§m/§n（颜色代码）
                return
            } else if (choice == "color") {
                // 方式二：两版都用颜色模式（§m_c/§n_c）
                // 找到第一个mode为"f"的映射并更新
                val mappingIndex = mnMappings.indexOfFirst { it.type == type && it.mode == "f" }
                if (mappingIndex != -1) {
                    val mapping = mnMappings[mappingIndex]
                    // 更新映射表
                    mnMappings[mappingIndex] = mapping.copy(mode = "c")
                    // 更新backendMessageContent
                    val backendCode = if (type == "m") "§m_c" else "§n_c"
                    backendMessageContent = backendMessageContent.substring(0, mapping.backendPos) + 
                                             backendCode + 
                                             backendMessageContent.substring(mapping.backendPos + 4)
                }
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
            // 自动保存配置到JSON文件
            settingsRepository.saveConfig()
        }
        generateCommands()
    }
    
    fun setMNMixedMode(enabled: Boolean) {
        _mnMixedMode.value = enabled
        // 保存设置到数据库
        viewModelScope.launch {
            settingsRepository.setMNMixedMode(enabled)
            // 如果开启混合模式，自动关闭§m/§n_c/f
            if (enabled) {
                settingsRepository.setMNCFEnabled(false)
                _mnCFEnabled.value = false
                // 清空mn_handling_mode（设置为默认值）
                settingsRepository.setMNHandlingMode(true)
                _useJavaFontStyle.value = true
            }
            // 自动保存配置到JSON文件
            settingsRepository.saveConfig()
        }
        generateCommands()
    }
    
    fun setMNCFEnabled(enabled: Boolean) {
        _mnCFEnabled.value = enabled
        // 保存设置到数据库
        viewModelScope.launch {
            settingsRepository.setMNCFEnabled(enabled)
            // 如果开启§m/§n_c/f，自动关闭混合模式
            if (enabled) {
                settingsRepository.setMNMixedMode(false)
                _mnMixedMode.value = false
                // 清空mn_handling_mode（设置为默认值）
                settingsRepository.setMNHandlingMode(true)
                _useJavaFontStyle.value = true
            }
            // 自动保存配置到JSON文件
            settingsRepository.saveConfig()
        }
        generateCommands()
    }
    
    fun setJavaBedrockMixedMode(enabled: Boolean) {
        if (enabled) {
            // 开启混合模式时显示警告对话框
            showJavaBedrockMixedModeWarningDialog()
        } else {
            // 直接关闭混合模式
            _javaBedrockMixedMode.value = enabled
            // 关闭混合模式时，使用新合并逻辑
            SelectorConverter.setMergeLogicMode(false)
            viewModelScope.launch {
                settingsRepository.setJavaBedrockMixedMode(enabled)
                settingsRepository.saveConfig()
            }
            generateCommands()
        }
    }

    /**
     * 确认开启JAVA/基岩混合模式（在警告对话框中点击确定后调用）
     */
    fun confirmJavaBedrockMixedMode() {
        _javaBedrockMixedMode.value = true
        // 开启混合模式时，使用源代码合并逻辑
        SelectorConverter.setMergeLogicMode(true)
        viewModelScope.launch {
            settingsRepository.setJavaBedrockMixedMode(true)
            settingsRepository.saveConfig()
        }
        dismissJavaBedrockMixedModeWarningDialog()
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
        val javaSelectorConversion = SelectorConverter.convertBedrockToJava(selector, applicationContext)
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
        val (javaSelectorFiltered, javaRemovedParams) = SelectorConverter.filterSelectorParameters(javaSelectorFinal, SelectorType.JAVA, applicationContext)

        // 过滤基岩版参数，移除Java版特有的参数（完全不支持）
        val (bedrockSelectorFiltered, bedrockRemovedParams) = SelectorConverter.filterSelectorParameters(bedrockSelectorFinal, SelectorType.BEDROCK, applicationContext)
        
        // 合并所有Java版提醒信息并去重
        val allJavaReminders = mutableListOf<String>()
        allJavaReminders.addAll(javaGamemodeReminders)
        allJavaReminders.addAll(javaRemovedParams)
        if (wasConverted) allJavaReminders.addAll(javaSelectorConversion.javaReminders)
        
        // 合并所有基岩版提醒信息并去重
        val allBedrockReminders = mutableListOf<String>()
        allBedrockReminders.addAll(bedrockGamemodeReminders)
        allBedrockReminders.addAll(bedrockRemovedParams)
        
        // 检测§m_f/§n_f，添加基岩版转换警告
        // 但在混合模式下不显示此警告
        val isMixedMode = mNHandling == "font" && !mnCFEnabled
        if (!isMixedMode && ("§m_f" in message || "§n_f" in message)) {
            applicationContext.let { ctx ->
                allWarnings.add(ctx.getString(R.string.bedrock_font_to_color_warning))
            }
        }
        
        // 生成Java版命令
        val javaJson = TextFormatter.convertToJavaJson(message, mNHandling, mnCFEnabled)
        val javaCommand = "tellraw $javaSelectorFiltered $javaJson"
        
        // 生成基岩版命令
        val bedrockJson = TextFormatter.convertToBedrockJson(message, mNHandling, mnCFEnabled)
        val bedrockCommand = "tellraw $bedrockSelectorFiltered $bedrockJson"
        
        // 处理提醒信息显示
        // 分类并去重处理Java版提醒
        val javaNonReminders = allJavaReminders.filter {
            !it.startsWith("Java版") && !it.startsWith("基岩版")
        }.distinct()

        val javaSpecificReminders = allJavaReminders.filter {
            it.startsWith("Java版") || it.startsWith("基岩版")
        }.distinct()

        // 分类并去重处理基岩版提醒
        val bedrockNonReminders = allBedrockReminders.filter {
            !it.startsWith("Java版") && !it.startsWith("基岩版")
        }.distinct()

        val bedrockSpecificReminders = allBedrockReminders.filter {
            it.startsWith("Java版") || it.startsWith("基岩版")
        }.distinct()

        // 添加Java版参数剔除提醒
        if (javaNonReminders.isNotEmpty()) {
            applicationContext.let { ctx ->
                allWarnings.add(ctx.getString(R.string.note_java_unsupported_params, javaNonReminders.joinToString(", ")))
            }
        }

        // 添加Java版特殊提醒
        javaSpecificReminders.forEach { reminder ->
            applicationContext.let { ctx ->
                allWarnings.add(ctx.getString(R.string.note_java_specific, reminder))
            }
        }

        // 添加基岩版参数剔除提醒
        if (bedrockNonReminders.isNotEmpty()) {
            applicationContext.let { ctx ->
                allWarnings.add(ctx.getString(R.string.note_bedrock_unsupported_params, bedrockNonReminders.joinToString(", ")))
            }
        }

        // 添加基岩版特殊提醒
        bedrockSpecificReminders.forEach { reminder ->
            applicationContext.let { ctx ->
                allWarnings.add(ctx.getString(R.string.note_java_specific, reminder))
            }
        }
        
        // 添加选择器转换提醒
        if (wasConverted && originalSelector != null && convertedSelector != null) {
            applicationContext.let { ctx ->
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
                
                // 检查是否启用了JAVA/基岩混合模式
                val (javaSelector, bedrockSelector) = if (_javaBedrockMixedMode.value) {
                    val mixedReminders = mutableListOf<String>()
                    val (javaOut, bedrockOut) = SelectorConverter.convertForMixedMode(selector, applicationContext, mixedReminders)
                    javaOut to bedrockOut
                } else {
                    selector to selector
                }
                
                // 生成Java版命令
                val (javaFilteredSelector, _) = SelectorConverter.filterSelectorParameters(javaSelector, SelectorType.JAVA, applicationContext)
                val javaJson = TextFormatter.convertToJavaJson(messageToUse, mNHandling, _mnCFEnabled.value)
                val javaCommand = "tellraw $javaFilteredSelector $javaJson"
                
                // 生成基岩版命令
                val (bedrockFilteredSelector, _) = SelectorConverter.filterSelectorParameters(bedrockSelector, SelectorType.BEDROCK, applicationContext)
                val bedrockJson = TextFormatter.convertToBedrockJson(messageToUse, mNHandling, _mnCFEnabled.value)
                val bedrockCommand = "tellraw $bedrockFilteredSelector $bedrockJson"
                
                _javaCommand.value = javaCommand
                _bedrockCommand.value = bedrockCommand
                _warnings.value = emptyList()  // TODO: 可以在这里添加警告信息
            } catch (e: IllegalArgumentException) {
                // 处理未知§组合的异常
                _javaCommand.value = ""
                _bedrockCommand.value = ""
                _warnings.value = listOf(e.message ?: applicationContext.getString(R.string.command_generation_error))
            } catch (e: Exception) {
                // 处理其他可能的异常，防止卡死
                _javaCommand.value = ""
                _bedrockCommand.value = ""
                _warnings.value = listOf(applicationContext.getString(R.string.command_generation_error_with_message, e.message ?: ""))
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
        val (filteredSelector, _) = SelectorConverter.filterSelectorParameters(selector, SelectorType.JAVA, applicationContext)

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
        val (filteredSelector, _) = SelectorConverter.filterSelectorParameters(selector, SelectorType.BEDROCK, applicationContext)
        
        // 转换文本为基岩版JSON格式
        val bedrockJson = TextFormatter.convertToBedrockJson(message, mNHandling, mnCFEnabled)
        return "tellraw $filteredSelector $bedrockJson"
    }
    
    fun copyToClipboard(text: String) {
        val clipboard = applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Tellraw Command", text)
        clipboard.setPrimaryClip(clip)
    }

    fun shareCommand(command: String) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, command)
            putExtra(Intent.EXTRA_SUBJECT, applicationContext.getString(R.string.share_subject))
        }
        applicationContext.startActivity(Intent.createChooser(shareIntent, applicationContext.getString(R.string.share_tellraw_command)))
    }

    fun clearAll() {
        val selector = _selectorInput.value
        val message = _messageInput.value
        val javaCommand = _javaCommand.value
        val bedrockCommand = _bedrockCommand.value
        
        // 如果有内容，先保存到历史记录
        if (selector.isNotEmpty() || message.isNotEmpty()) {
            viewModelScope.launch {
                historyRepository.addHistory(
                    HistoryItem(
                        selector = selector,
                        message = message,
                        javaCommand = javaCommand,
                        bedrockCommand = bedrockCommand,
                        timestamp = System.currentTimeMillis()
                    )
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
        
        // 重置后台消息和映射表
        backendMessageContent = ""
        mnMappings.clear()
        
        // 重置对话框状态
        _showMNDialog.value = null
    }
    
    // 历史记录管理函数
    
    /**
     * 删除指定的历史记录
     */
    fun deleteHistoryItem(history: HistoryItem) {
        viewModelScope.launch {
            historyRepository.deleteHistory(history)
        }
    }
    
    /**
     * 清空所有历史记录
     */
    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
        }
    }
    
    /**
     * 从历史记录加载命令
     */
    fun loadFromHistory(history: HistoryItem) {
        _selectorInput.value = history.selector
        _messageInput.value = history.message
        _javaCommand.value = history.javaCommand
        _bedrockCommand.value = history.bedrockCommand
        detectSelectorType()
        
        // 重置§m和§n计数器
        _mCodeCount.value = 0
        _nCodeCount.value = 0
        lastMessageContent = history.message
        
        // 在混合模式下，重新构建映射表
        if (_mnMixedMode.value) {
            backendMessageContent = ""
            mnMappings.clear()
            updateBackendMessage(history.message)
        } else {
            backendMessageContent = history.message
            mnMappings.clear()
        }
        
        // 重置对话框状态
        _showMNDialog.value = null
    }
    
    // 搜索历史记录
    fun searchHistory(query: String) {
        viewModelScope.launch {
            val results = historyRepository.searchHistory(query)
            _searchResults.value = results
        }
    }
    
    // 版本检查相关方法
    
    /**
     * 初始化版本检查
     */
    private fun initializeVersionCheck() {
        applicationContext.let { ctx ->
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
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        applicationContext.startActivity(intent)
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
    
    // 历史记录存储相关方法
    
    /**
     * 加载历史记录存储设置
     */
    private suspend fun loadHistoryStorageSettings() {
        _historyStorageUri.value = settingsRepository.getHistoryStorageUri()
        // 清理文件名：移除引号和其他非法字符
        val rawFilename = settingsRepository.getHistoryStorageFilename()
        val cleanFilename = rawFilename.trim().replace("\"", "").replace("/", "").replace("\\", "")
        _historyStorageFilename.value = cleanFilename
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
     * 设置历史记录存储URI
     * 检查文件是否存在并处理：有文件提示"文件已存在"，无文件则创建
     */
    fun setHistoryStorageUri(uri: String) {
        viewModelScope.launch {
            settingsRepository.setHistoryStorageUri(uri)
            _historyStorageUri.value = uri
            
            val filename = _historyStorageFilename.value
            val file = java.io.File(uri, filename)
            
            if (file.exists()) {
                // 文件已存在，在文件开头添加空行（如果是空的则添加）
                try {
                    val currentContent = file.readText()
                    if (currentContent.isNotEmpty() && !currentContent.startsWith("\n")) {
                        file.writeText("\n" + currentContent)
                    }
                    _writeFileMessage.value = "文件已存在，将使用该文件存储历史记录"
                } catch (e: Exception) {
                    _writeFileMessage.value = "文件已存在，但无法读取内容，将直接使用该文件存储历史记录"
                }
            } else {
                // 文件不存在，创建它
                try {
                    file.createNewFile()
                    _writeFileMessage.value = "成功使用该目录，文件已创建"
                } catch (e: Exception) {
                    _writeFileMessage.value = "创建文件失败：${e.message}，将使用应用沙盒存储"
                }
            }
            
            // 重新加载历史记录
            loadHistoryStorageSettings()
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
     * 检查文件是否存在并处理
     */
    fun setHistoryStorageFilename(filename: String) {
        viewModelScope.launch {
            // 清理文件名：移除引号和其他非法字符
            val cleanFilename = filename.trim().replace("\"", "").replace("/", "").replace("\\", "")
            settingsRepository.setHistoryStorageFilename(cleanFilename)
            _historyStorageFilename.value = cleanFilename
            
            val storagePath = _historyStorageUri.value
            if (storagePath != null) {
                val file = java.io.File(storagePath, cleanFilename)
                
                if (file.exists()) {
                    // 文件已存在
                    _writeFileMessage.value = "文件已存在，将使用该文件存储历史记录"
                } else {
                    // 文件不存在，创建它
                    try {
                        file.createNewFile()
                        _writeFileMessage.value = "成功使用该目录，文件已创建"
                    } catch (e: Exception) {
                        _writeFileMessage.value = "创建文件失败：${e.message}，将使用应用沙盒存储"
                    }
                }
            }
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
     * 选择/创建文件
     * 检查文件是否存在：如果文件存在，提示"文件已存在"；如果文件不存在，创建文件并提示"成功使用该目录"
     */
    fun writeHistoryToFile(context: Context) {
        val activity = context as? MainActivity

        // 主动申请权限
        activity?.requestAllFilesAccessIfNeeded(
            onGranted = {
                doSelectOrCreateFile(context)
            },
            onDenied = {
                _writeFileMessage.value = context.getString(R.string.permission_denied)
                _isWritingToFile.value = false
            }
        )
    }
    
    /**
     * 实际执行选择/创建文件
     */
    private fun doSelectOrCreateFile(context: Context) {
        viewModelScope.launch {
            _isWritingToFile.value = true
            _writeFileMessage.value = null
            
            try {
                val storagePath = settingsRepository.getHistoryStorageUri()
                val filename = settingsRepository.getHistoryStorageFilename()
                
                if (storagePath == null) {
                    // 未选择文件夹
                    _writeFileMessage.value = "请先选择存储目录"
                    _isWritingToFile.value = false
                    return@launch
                }
                
                // 使用文件管理器选择的目录
                val file = java.io.File(storagePath, filename)
                
                // 检查文件是否存在
                if (file.exists()) {
                    // 文件已存在
                    _writeFileMessage.value = context.getString(R.string.file_exists_hint)
                } else {
                    // 文件不存在，创建它
                    try {
                        file.createNewFile()
                        _writeFileMessage.value = context.getString(R.string.file_created_successfully)
                    } catch (e: Exception) {
                        _writeFileMessage.value = context.getString(R.string.file_create_failed) + ": ${e.message}"
                    }
                }
            } catch (e: Exception) {
                _writeFileMessage.value = context.getString(R.string.create_file_failed) + ": ${e.message}"
            } finally {
                _isWritingToFile.value = false
            }
        }
    }
    
    /**
     * 追加写入到文件
     */
    private fun appendToFile(file: java.io.File, historyList: List<HistoryItem>) {
        for (item in historyList) {
            val content = historyRepository.buildSingleHistoryItem(item)
            file.appendText(content)
        }
    }
    
    /**
     * 创建新文件并写入
     */
    fun createNewFile(context: Context, newFilename: String, historyList: List<HistoryItem>) {
        viewModelScope.launch {
            _isWritingToFile.value = true
            _writeFileMessage.value = null
            
            try {
                val storagePath = settingsRepository.getHistoryStorageUri()
                
                if (storagePath == null) {
                    writeToFileInSandbox(context, newFilename, historyList)
                    return@launch
                }
                
                val file = java.io.File(storagePath, newFilename)
                
                // 如果新文件已存在，追加写入
                if (file.exists()) {
                    appendToFile(file, historyList)
                } else {
                    // 创建新文件并写入
                    file.createNewFile()
                    appendToFile(file, historyList)
                }
                
                // 更新文件名设置
                settingsRepository.setHistoryStorageFilename(newFilename)
                _historyStorageFilename.value = newFilename
                
                _writeFileMessage.value = context.getString(R.string.write_success, historyList.size)
            } catch (e: Exception) {
                _writeFileMessage.value = context.getString(R.string.create_file_failed) + ": ${e.message}"
            } finally {
                _isWritingToFile.value = false
                hideFileExistsDialog()
            }
        }
    }
    
    /**
     * 将历史记录写入应用沙盒（追加模式）
     */
    private suspend fun writeToFileInSandbox(context: Context, filename: String, historyList: List<HistoryItem>) {
        try {
            val file = java.io.File(context.filesDir, filename)
            
            // 确保文件存在
            if (!file.exists()) {
                file.createNewFile()
            }
            
            // 追加写入历史记录
            for (item in historyList) {
                val content = historyRepository.buildHistoryContent(item)
                file.appendText(content)
            }
            
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
    fun appendToExistingFile(context: Context, historyList: List<HistoryItem>) {
        viewModelScope.launch {
            _isWritingToFile.value = true
            _writeFileMessage.value = null
            
            try {
                val storagePath = settingsRepository.getHistoryStorageUri()
                val filename = settingsRepository.getHistoryStorageFilename()
                
                if (storagePath == null) {
                    // 未选择文件夹，在沙盒中追加
                    appendToFileInSandbox(context, filename, historyList)
                    return@launch
                }
                
                // 使用文件管理器选择的目录
                val file = java.io.File(storagePath, filename)
                
                // 生成历史记录内容
                val content = StringBuilder()
                for (item in historyList) {
                    content.append(historyRepository.buildHistoryContent(item))
                }
                
                // 追加写入文件
                file.appendText(content.toString())
                
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
    private suspend fun appendToFileInSandbox(context: Context, filename: String, historyList: List<HistoryItem>) {
        try {
            val file = java.io.File(context.filesDir, filename)
            
            // 生成历史记录内容
            val content = StringBuilder()
            for (item in historyList) {
                content.append(historyRepository.buildHistoryContent(item))
            }
            
            // 追加写入文件
            file.appendText(content.toString())
            
            _writeFileMessage.value = context.getString(R.string.append_success, historyList.size)
        } catch (e: Exception) {
            _writeFileMessage.value = context.getString(R.string.append_file_failed) + ": ${e.message}"
        } finally {
            _isWritingToFile.value = false
        }
    }
}