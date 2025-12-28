package com.tellraw.app.ui.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tellraw.app.data.local.CommandHistory
import com.tellraw.app.data.remote.GithubRelease
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
    private val versionCheckRepository: VersionCheckRepository
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
        // 设置Context后初始化版本检查
        initializeVersionCheck()
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
    
    private val _showMNDialog = MutableStateFlow(false)
    val showMNDialog: StateFlow<Boolean> = _showMNDialog.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
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
    
    fun updateSelector(selector: String) {
        _selectorInput.value = selector
        detectSelectorType()
        generateCommands()
    }
    
    fun updateMessage(message: String) {
        _messageInput.value = message
        
        // 检测是否包含§m§n代码
        if (TextFormatter.containsMNCodes(message)) {
            _showMNDialog.value = true
        } else {
            generateCommands()
        }
    }
    
    fun setUseJavaFontStyle(useJavaFont: Boolean) {
        _useJavaFontStyle.value = useJavaFont
        generateCommands()
    }
    
    fun dismissMNDialog() {
        _showMNDialog.value = false
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
        mNHandling: String = "none"
    ): TellrawCommand {
        val allWarnings = mutableListOf<String>()
        
        // 检测选择器类型（与Python版本的detect_selector_type函数逻辑一致）
        val detectedType = SelectorConverter.detectSelectorType(selector)
        
        // 将基岩版选择器转换为Java版（如果需要）
        val javaSelectorConversion = SelectorConverter.convertBedrockToJava(selector)
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
        val (javaSelectorFiltered, javaRemovedParams) = SelectorConverter.filterSelectorParameters(javaSelectorConversion.javaSelector, SelectorType.JAVA)
        
        // 过滤基岩版参数，移除Java版特有的参数（完全不支持）
        val (bedrockSelectorFiltered, bedrockRemovedParams) = SelectorConverter.filterSelectorParameters(javaSelectorConversion.bedrockSelector, SelectorType.BEDROCK)
        
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
        val javaJson = TextFormatter.convertToJavaJson(message, mNHandling)
        val javaCommand = "tellraw $javaSelectorFiltered $javaJson"
        
        // 生成基岩版命令
        val bedrockJson = TextFormatter.convertToBedrock(message, mNHandling)
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
            allWarnings.add("注意: Java版不支持以下参数，已从Java版命令中移除: ${javaNonReminders.joinToString(", ")}")
        }
        
        // 添加Java版特殊提醒
        javaSpecificReminders.forEach { reminder ->
            allWarnings.add("注意: $reminder")
        }
        
        // 添加基岩版参数剔除提醒
        if (bedrockNonReminders.isNotEmpty()) {
            allWarnings.add("注意: 基岩版不支持以下参数，已从基岩版命令中移除: ${bedrockNonReminders.joinToString(", ")}")
        }
        
        // 添加基岩版特殊提醒
        bedrockSpecificReminders.forEach { reminder ->
            allWarnings.add("注意: $reminder")
        }
        
        // 添加选择器转换提醒
        if (wasConverted && originalSelector != null && convertedSelector != null) {
            allWarnings.add("注意: 基岩版选择器 $originalSelector 在Java版中不支持，已转换为 $convertedSelector")
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
            
            // 确定m_n_handling参数（与Python版本一致）
            val mNHandling = if (_useJavaFontStyle.value) "font" else "color"
            
            // 使用统一的命令生成函数
            val result = generateTellrawCommands(selector, message, mNHandling)
            
            _javaCommand.value = result.javaCommand
            _bedrockCommand.value = result.bedrockCommand
            _warnings.value = result.warnings
            _isLoading.value = false
            
            // 保存到历史记录
            viewModelScope.launch {
                tellrawRepository.saveCommandToHistory(
                    selector = selector,
                    message = message,
                    javaCommand = result.javaCommand,
                    bedrockCommand = result.bedrockCommand
                )
            }
            
            // 生成tellraw命令（与Python版本的generate_tellraw_commands函数逻辑一致）
            val javaCommand = generateJavaCommand(
                selector = javaSelectorFiltered,
                message = message,
                mNHandling = mNHandling
            )
            
            val bedrockCommand = generateBedrockCommand(
                selector = bedrockSelectorFiltered,
                message = message,
                mNHandling = mNHandling
            )
            
            _javaCommand.value = javaCommand
            _bedrockCommand.value = bedrockCommand
            _warnings.value = allWarnings
            _isLoading.value = false
        }
    }
    
    /**
     * 生成Java版tellraw命令，与Python版本一致
     */
    private fun generateJavaCommand(
        selector: String,
        message: String,
        mNHandling: String
    ): String {
        // 过滤Java版不支持的参数
        val (filteredSelector, _) = SelectorConverter.filterSelectorParameters(selector, SelectorType.JAVA)
        
        // 转换文本为Java版JSON格式
        val javaJson = TextFormatter.convertToJavaJson(message, mNHandling)
        return "tellraw $filteredSelector $javaJson"
    }
    
    /**
     * 生成基岩版tellraw命令，与Python版本一致
     */
    private fun generateBedrockCommand(
        selector: String,
        message: String,
        mNHandling: String
    ): String {
        // 过滤基岩版不支持的参数
        val (filteredSelector, _) = SelectorConverter.filterSelectorParameters(selector, SelectorType.BEDROCK)
        
        // 转换文本为基岩版JSON格式
        val bedrockJson = TextFormatter.convertToBedrockJson(message, mNHandling)
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
            ctx.startActivity(Intent.createChooser(shareIntent, "分享Tellraw命令"))
        }
    }
    
    fun clearAll() {
        _selectorInput.value = ""
        _messageInput.value = ""
        _javaCommand.value = ""
        _bedrockCommand.value = ""
        _warnings.value = emptyList()
        _selectorType.value = SelectorType.UNIVERSAL
        _useJavaFontStyle.value = true
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
}