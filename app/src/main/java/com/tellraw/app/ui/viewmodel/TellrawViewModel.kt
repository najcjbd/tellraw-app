package com.tellraw.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tellraw.app.model.SelectorType
import com.tellraw.app.model.TellrawCommand
import com.tellraw.app.util.SelectorConverter
import com.tellraw.app.util.TextFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TellrawViewModel @Inject constructor() : ViewModel() {
    
    private val _selectorInput = MutableStateFlow("")
    val selectorInput: StateFlow<String> = _selectorInput.asStateFlow()
    
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
            
            val allWarnings = mutableListOf<String>()
            
            // 检测选择器类型（与Python版本的detect_selector_type函数逻辑一致）
            val detectedType = SelectorConverter.detectSelectorType(selector)
            
            // 转换选择器
            val conversionResult = when (detectedType) {
                SelectorType.JAVA -> {
                    // Java版输入：Java版输出使用原始输入，基岩版输出需要转换
                    val result = SelectorConverter.convertJavaToBedrock(selector)
                    allWarnings.addAll(result.bedrockReminders)
                    selector to result.bedrockSelector
                }
                SelectorType.BEDROCK -> {
                    // 基岩版输入：基岩版输出使用原始输入，Java版输出需要转换
                    val result = SelectorConverter.convertBedrockToJava(selector)
                    allWarnings.addAll(result.javaReminders)
                    result.javaSelector to selector
                }
                SelectorType.UNIVERSAL -> {
                    selector to selector
                }
            }
            
            // 确定m_n_handling参数（与Python版本一致）
            val mNHandling = if (_useJavaFontStyle.value) "font" else "color"
            
            // 过滤Java版参数，移除基岩版特有的参数（完全不支持）
            val (javaSelectorFiltered, javaRemovedParams) = SelectorConverter.filterSelectorParameters(conversionResult.first, SelectorType.JAVA)
            
            // 过滤基岩版参数，移除Java版特有的参数（完全不支持）
            val (bedrockSelectorFiltered, bedrockRemovedParams) = SelectorConverter.filterSelectorParameters(conversionResult.second, SelectorType.BEDROCK)
            
            // 合并所有Java版提醒信息并去重
            val allJavaReminders = mutableListOf<String>()
            allJavaReminders.addAll(javaRemovedParams)
            
            // 分类并去重处理Java版提醒
            val javaNonReminders = allJavaReminders.filter { 
                !it.startsWith("Java版") && !it.startsWith("基岩版") && 
                !"nbt参数" in it && !"参数已转换为" in it && !"已转换为" in it 
            }.distinct()
            
            val javaSpecificReminders = allJavaReminders.filter { 
                "参数已转换为" in it || "nbt参数" in it || 
                "已转换为" in it || (it.startsWith("Java版") || it.startsWith("基岩版"))
            }.distinct()
            
            // 合并所有基岩版提醒信息并去重
            val allBedrockReminders = mutableListOf<String>()
            allBedrockReminders.addAll(bedrockRemovedParams)
            
            // 分类并去重处理基岩版提醒
            val bedrockNonReminders = allBedrockReminders.filter { 
                !it.startsWith("Java版") && !it.startsWith("基岩版") && 
                !"nbt参数" in it && !"参数已转换为" in it && !"已转换为" in it 
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
        val bedrockJson = TextFormatter.convertToBedrockJson(message, mNHandling == "font")
        return "tellraw $filteredSelector $bedrockJson"
    }
    
    fun copyToClipboard(text: String) {
        // 这里需要实现复制到剪贴板的功能
        // 在实际应用中，需要使用Android的ClipboardManager
    }
    
    fun shareCommand(command: String) {
        // 这里需要实现分享功能
        // 在实际应用中，需要使用Android的分享Intent
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
}