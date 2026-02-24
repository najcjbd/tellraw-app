package com.tellraw.app.util

/**
 * 文本组件工具类
 * 处理文本组件的标记、解析和转换
 */
import android.content.Context
import androidx.annotation.StringRes
import com.tellraw.app.R
import com.tellraw.app.model.SelectorType
import com.tellraw.app.util.SelectorConverter

object TextComponentHelper {
    
    // 特殊标记字符
    const val MARKER_START = '\u0FC8'  // ྈ
    const val MARKER_END = '\u0F34'    // ༴
    
    // 文本组件类型（Java版和基岩版tellraw都支持的组件）
    enum class ComponentType(val key: String, @StringRes val displayNameResId: Int, val hasSubComponent: Boolean = false) {
        TEXT("text", R.string.component_text),
        TRANSLATE("translate", R.string.component_translate, true),
        SCORE("score", R.string.component_score),
        SELECTOR("selector", R.string.component_selector)
    }
    
    // 副组件类型
    enum class SubComponentType(val key: String, @StringRes val displayNameResId: Int, val parentComponent: ComponentType?) {
        WITH("with", R.string.component_with, ComponentType.TRANSLATE),
        SEPARATOR("separator", R.string.component_with, ComponentType.SELECTOR)  // 暂时使用component_with作为displayNameResId
    }
    
    /**
     * 文本组件数据类
     */
    data class TextComponent(
        val type: ComponentType,
        val content: String,
        val subComponents: MutableList<SubComponent> = mutableListOf()
    )
    
    /**
     * 副组件数据类
     */
    data class SubComponent(
        val type: SubComponentType,
        val content: String
    )
    
    /**
     * 将原始文本（带组件标记）解析为组件列表
     * 使用字符串搜索而不是深度计数，避免副组件的MARKER_START干扰
     */
    fun parseTextComponents(text: String): List<TextComponent> {
        val components = mutableListOf<TextComponent>()
        var i = 0
        
        while (i < text.length) {
            // 查找组件开始标记
            if (text[i] == MARKER_START) {
                // 查找组件类型
                val typeEnd = text.indexOf(MARKER_END, i + 1)
                if (typeEnd == -1) {
                    // 没有找到类型结束标记，将标记作为普通文本处理
                    components.add(TextComponent(ComponentType.TEXT, text[i].toString()))
                    i++
                    continue
                }
                
                val typeKey = text.substring(i + 1, typeEnd)
                val type = ComponentType.values().find { it.key == typeKey }
                
                if (type == null) {
                    // 未知的组件类型，将标记作为普通文本处理
                    components.add(TextComponent(ComponentType.TEXT, text.substring(i, typeEnd + 1)))
                    i = typeEnd + 1
                    continue
                }
                
                // 查找组件结束标记（从typeEnd + 1开始搜索，避免误判副组件的MARKER_START）
                // 查找格式：MARKER_END（这是组件的结束标记）
                // 但需要确保不是副组件中的MARKER_END
                var componentEnd = -1
                var searchStart = typeEnd + 1
                var depth = 0
                
                while (searchStart < text.length) {
                    if (text[searchStart] == MARKER_START) {
                        depth++
                    } else if (text[searchStart] == MARKER_END) {
                        if (depth == 0) {
                            componentEnd = searchStart
                            break
                        } else {
                            depth--
                        }
                    }
                    searchStart++
                }
                
                if (componentEnd == -1) {
                    // 没有找到结束标记，将标记作为普通文本处理
                    components.add(TextComponent(ComponentType.TEXT, text.substring(i)))
                    break
                }
                
                // 提取组件内容（不包括标记）
                val componentContent = text.substring(typeEnd + 1, componentEnd)
                val component = parseSingleComponentWithContent(type, componentContent)
                if (component != null) {
                    components.add(component)
                }
                
                i = componentEnd + 1
            } else {
                // 普通文本，寻找下一个组件开始标记
                val nextStart = text.indexOf(MARKER_START, i)
                if (nextStart == -1) {
                    // 没有找到组件标记，剩余全是普通文本
                    if (i < text.length) {
                        components.add(TextComponent(ComponentType.TEXT, text.substring(i)))
                    }
                    break
                } else {
                    // 添加普通文本
                    if (nextStart > i) {
                        components.add(TextComponent(ComponentType.TEXT, text.substring(i, nextStart)))
                    }
                    i = nextStart
                }
            }
        }
        
        return components
    }
    
    /**
     * 解析单个组件（已知组件类型）
     * 使用简单的副组件格式：__type.key__content__
     * 注意：只有以__结束的才是有效的副组件，避免将用户输入的__with__误认为是副组件标记
     */
    private fun parseSingleComponentWithContent(type: ComponentType, content: String): TextComponent? {
        // 查找副组件
        val subComponents = mutableListOf<SubComponent>()
        var searchStart = 0
        
        while (searchStart < content.length) {
            // 查找副组件开始标记：__type.key__
            val subComponentStart = content.indexOf("__", searchStart)
            if (subComponentStart == -1) break
            
            // 查找副组件类型
            val typeStart = subComponentStart + 2  // 跳过__
            val typeEnd = content.indexOf("__", typeStart)
            if (typeEnd == -1) break
            
            val subTypeKey = content.substring(typeStart, typeEnd)
            val subType = SubComponentType.values().find { it.key == subTypeKey }
            if (subType == null) {
                // 不是有效的副组件类型，跳过
                searchStart = typeEnd + 2
                continue
            }
            
            // 查找副组件内容（从typeEnd + 2开始，到下一个__）
            val contentStart = typeEnd + 2
            val contentEnd = content.indexOf("__", contentStart)
            if (contentEnd == -1) {
                // 没有找到结束标记，这不是有效的副组件
                break
            }
            
            val subContent = content.substring(contentStart, contentEnd)
            subComponents.add(SubComponent(subType, subContent))
            
            searchStart = contentEnd + 2  // 跳过结束标记__
        }
        
        // 提取主内容（到第一个副组件或字符串末尾）
        val mainContent = if (subComponents.isEmpty()) {
            content
        } else {
            val firstSubComponent = content.indexOf("__")
            if (firstSubComponent == -1) {
                content
            } else {
                content.substring(0, firstSubComponent)
            }
        }
        
        return TextComponent(type, mainContent, subComponents)
    }
    
    /**
     * 将组件列表转换为标记文本
     * 格式：MARKER_START + type.key + MARKER_END + content + MARKER_END
     * 副组件格式：__type.key__content__
     * 这样parseTextComponents可以直接使用字符串搜索，而不是深度计数
     */
    fun componentsToText(components: List<TextComponent>): String {
        return components.joinToString("") { component ->
            val subComponentText = component.subComponents.joinToString("") { sub ->
                "__${sub.type.key}__${sub.content}__"
            }
            "$MARKER_START${component.type.key}$MARKER_END${component.content}$subComponentText$MARKER_END"
        }
    }
    
    /**
     * 将单个组件转换为标记文本
     */
    fun componentToText(component: TextComponent): String {
        return componentsToText(listOf(component))
    }
    
    /**
     * 将组件内容中的§代码转换为Java版JSON属性
     */
    private fun processSectionCodesToJson(content: String, mNHandling: String, mnCFEnabled: Boolean): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        val tokens = tokenizeText(content)
        
        for (token in tokens) {
            val (tokenType, tokenValue) = token
            
            when (tokenType) {
                "color" -> {
                    if (tokenValue in JAVA_COLORS) {
                        result["color"] = JAVA_COLORS[tokenValue]!!
                    }
                }
                "format" -> {
                    when (tokenValue) {
                        "k" -> result["obfuscated"] = true
                        "l" -> result["bold"] = true
                        "m" -> {
                            if (mNHandling == "font") {
                                result["strikethrough"] = true
                            } else {
                                result["color"] = "dark_red"
                            }
                        }
                        "n" -> {
                            if (mNHandling == "font") {
                                result["underlined"] = true
                            } else {
                                result["color"] = "red"
                            }
                        }
                        "o" -> result["italic"] = true
                        "r" -> {
                            // 重置当前样式
                            result.clear()
                        }
                    }
                }
            }
        }
        
        return result
    }
    
    /**
     * 将组件内容中的§代码转换为基岩版文本
     */
    private fun processSectionCodesToBedrock(content: String, mNHandling: String, mnCFEnabled: Boolean): String {
        var processedText = content
        
        // 基岩版中，§m/§n始终作为颜色代码处理
        processedText = processedText.replace("§m_f", "§m").replace("§m_c", "§m")
        processedText = processedText.replace("§n_f", "§n").replace("§n_c", "§n")
        
        return processedText
    }
    
    /**
     * 简单的文本分词（提取颜色和格式代码）
     */
    private fun tokenizeText(text: String): List<Pair<String, String>> {
        val tokens = mutableListOf<Pair<String, String>>()
        val knownColorCodes = setOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f")
        val knownFormatCodes = setOf("k", "l", "m", "n", "o", "r")
        
        var i = 0
        var currentType = "text"
        var currentValue = StringBuilder()
        
        while (i < text.length) {
            if (i + 1 < text.length && text[i] == '§') {
                // 保存之前的文本
                if (currentValue.isNotEmpty()) {
                    tokens.add(Pair(currentType, currentValue.toString()))
                    currentValue.clear()
                }
                
                // 处理颜色或格式代码
                val code = text.substring(i + 1, i + 2)
                if (code in knownColorCodes) {
                    currentType = "color"
                    currentValue.append(code)
                } else if (code in knownFormatCodes) {
                    currentType = "format"
                    currentValue.append(code)
                } else {
                    // 未知代码，作为普通文本
                    currentType = "text"
                    currentValue.append(text[i])
                    currentValue.append(code)
                }
                
                i += 2
            } else {
                // 普通文本
                if (currentType == "text") {
                    currentValue.append(text[i])
                } else {
                    // 保存之前的代码
                    if (currentValue.isNotEmpty()) {
                        tokens.add(Pair(currentType, currentValue.toString()))
                        currentValue.clear()
                    }
                    currentType = "text"
                    currentValue.append(text[i])
                }
                i++
            }
        }
        
        // 保存最后一个token
        if (currentValue.isNotEmpty()) {
            tokens.add(Pair(currentType, currentValue.toString()))
        }
        
        return tokens
    }
    
    /**
     * Java版颜色代码映射
     */
    private val JAVA_COLORS = mapOf(
        "0" to "black",
        "1" to "dark_blue",
        "2" to "dark_green",
        "3" to "dark_aqua",
        "4" to "dark_red",
        "5" to "dark_purple",
        "6" to "gold",
        "7" to "gray",
        "8" to "dark_gray",
        "9" to "blue",
        "a" to "green",
        "b" to "aqua",
        "c" to "red",
        "d" to "light_purple",
        "e" to "yellow",
        "f" to "white"
    )
    
    /**
     * 获取指定位置的文本组件类型
     */
    fun getComponentTypeAtPosition(text: String, position: Int): ComponentType? {
        if (position < 0 || position >= text.length) return null
        
        // 跳过标记字符
        if (text[position] == MARKER_START || text[position] == MARKER_END) {
            return null
        }
        
        val components = parseTextComponents(text)
        var currentPos = 0
        
        for (component in components) {
            // 计算组件内容长度（不包括标记）
            val contentLength = component.content.length + 
                                component.subComponents.sumOf { it.content.length }
            
            if (position >= currentPos && position < currentPos + contentLength) {
                return component.type
            }
            
            currentPos += contentLength
        }
        
        return ComponentType.TEXT
    }
    
    /**
     * 获取指定位置的文本组件内容
     * @return 组件的完整内容字符串（包括副组件内容）
     */
    fun getComponentContentAtPosition(text: String, position: Int): String? {
        if (position < 0 || position >= text.length) return null
        
        // 跳过标记字符
        if (text[position] == MARKER_START || text[position] == MARKER_END) {
            return null
        }
        
        val components = parseTextComponents(text)
        var currentPos = 0
        
        for (component in components) {
            // 计算组件内容长度（不包括标记）
            val contentLength = component.content.length + 
                                component.subComponents.sumOf { it.content.length }
            
            if (position >= currentPos && position < currentPos + contentLength) {
                // 返回组件的完整内容（包括副组件）
                return buildString {
                    append(component.content)
                    component.subComponents.forEach { sub ->
                        append(sub.content)
                    }
                }
            }
            
            currentPos += contentLength
        }
        
        return text  // 如果没有找到组件，返回整个文本
    }
    
    /**
     * 在指定位置插入文本，根据当前组件状态应用组件标记
     * 实现逻辑：新文本会"汇入"到光标位置的组件中，直到遇到下一个组件标记
     */
    fun insertTextWithComponent(
        originalText: String,
        insertPosition: Int,
        textToInsert: String,
        currentComponent: ComponentType?
    ): String {
        if (textToInsert.isEmpty()) return originalText
        
        // 如果没有选中组件，直接插入（默认text组件）
        if (currentComponent == null) {
            return originalText.substring(0, insertPosition) + textToInsert + originalText.substring(insertPosition)
        }
        
        // 如果选中了TEXT组件，直接插入文本（不创建新的text组件标记）
        // 这样连续输入的字符会被合并到一个TEXT组件中
        if (currentComponent == ComponentType.TEXT) {
            return originalText.substring(0, insertPosition) + textToInsert + originalText.substring(insertPosition)
        }
        
        // 解析原始文本
        val components = parseTextComponents(originalText)
        
        // 如果没有组件，创建新组件
        if (components.isEmpty()) {
            return componentToText(TextComponent(currentComponent, textToInsert))
        }
        
        // 找到光标位置所在的组件
        var currentPos = 0
        var targetComponentIndex = -1
        var offsetInTargetComponent = 0
        
        for (i in components.indices) {
            val component = components[i]
            val componentTextLength = component.content.length + 
                                       component.subComponents.sumOf { it.content.length }
            
            if (insertPosition >= currentPos && insertPosition < currentPos + componentTextLength) {
                targetComponentIndex = i
                offsetInTargetComponent = insertPosition - currentPos
                break
            }
            
            currentPos += componentTextLength
        }
        
        // 如果光标在所有组件之后，检查是否可以与最后一个组件合并
        if (targetComponentIndex == -1) {
            val lastComponent = components.lastOrNull()
            if (lastComponent != null && lastComponent.type == currentComponent && lastComponent.subComponents.isEmpty()) {
                // 与最后一个组件合并
                val mergedContent = lastComponent.content + textToInsert
                val mergedComponent = TextComponent(currentComponent, mergedContent, lastComponent.subComponents)
                val result = StringBuilder()
                for (i in 0 until components.size - 1) {
                    result.append(componentToText(components[i]))
                }
                result.append(componentToText(mergedComponent))
                return result.toString()
            } else {
                // 创建新组件
                val result = StringBuilder()
                components.forEach { result.append(componentToText(it)) }
                result.append(componentToText(TextComponent(currentComponent, textToInsert)))
                return result.toString()
            }
        }
        
        // 光标在某个组件内
        val targetComponent = components[targetComponentIndex]
        
        // 如果目标组件类型与当前选中组件类型相同，直接合并内容
        if (targetComponent.type == currentComponent) {
            val newContent = targetComponent.content.substring(0, offsetInTargetComponent) + 
                           textToInsert + 
                           targetComponent.content.substring(offsetInTargetComponent)
            val mergedComponent = TextComponent(currentComponent, newContent, targetComponent.subComponents)
            
            val result = StringBuilder()
            for (i in 0 until targetComponentIndex) {
                result.append(componentToText(components[i]))
            }
            result.append(componentToText(mergedComponent))
            for (i in targetComponentIndex + 1 until components.size) {
                result.append(componentToText(components[i]))
            }
            return result.toString()
        }
        
        // 类型不同或有副组件，使用原有逻辑
        val result = StringBuilder()
        
        // 添加前面的组件
        for (i in 0 until targetComponentIndex) {
            result.append(componentToText(components[i]))
        }
        
        // 检查是否在组件内容范围内
        if (offsetInTargetComponent < targetComponent.content.length) {
            // 在主组件内容内插入
            val newContent = targetComponent.content.substring(0, offsetInTargetComponent) + 
                           textToInsert + 
                           targetComponent.content.substring(offsetInTargetComponent)
            result.append(componentToText(TextComponent(targetComponent.type, newContent, targetComponent.subComponents)))
        } else {
            // 在副组件内或之后，需要计算在哪个副组件中
            var remainingOffset = offsetInTargetComponent - targetComponent.content.length
            val updatedSubComponents = targetComponent.subComponents.toMutableList()
            var insertedInSub = false
            
            for (i in updatedSubComponents.indices) {
                if (remainingOffset < updatedSubComponents[i].content.length) {
                    // 在该副组件内插入
                    val newSubContent = updatedSubComponents[i].content.substring(0, remainingOffset) + 
                                       textToInsert + 
                                       updatedSubComponents[i].content.substring(remainingOffset)
                    updatedSubComponents[i] = SubComponent(
                        updatedSubComponents[i].type,
                        newSubContent
                    )
                    insertedInSub = true
                    break
                }
                remainingOffset -= updatedSubComponents[i].content.length
            }
            
            if (!insertedInSub) {
                // 在所有副组件之后，添加到主组件内容末尾
                val newContent = targetComponent.content + textToInsert
                result.append(componentToText(TextComponent(targetComponent.type, newContent, targetComponent.subComponents)))
            } else {
                result.append(componentToText(TextComponent(targetComponent.type, targetComponent.content, updatedSubComponents)))
            }
        }
        
        // 添加后面的组件
        for (i in targetComponentIndex + 1 until components.size) {
            result.append(componentToText(components[i]))
        }
        
        return result.toString()
    }
    
    /**
     * 转换为Java版JSON
     */
    fun convertToJavaJson(
    components: List<TextComponent>,
    mNHandling: String = "font",
    mnCFEnabled: Boolean = false,
    context: Context? = null
): String {
        if (components.isEmpty()) return "{}"

        // 展开组件，将score和selector组件中的多个条目分割成独立的文本组件
        val expandedComponents = expandComponents(components)

        // 检查是否所有组件都是TEXT组件
        val allTextComponents = expandedComponents.all { it.type == ComponentType.TEXT && it.subComponents.isEmpty() }
        if (allTextComponents) {
            // 所有组件都是TEXT组件，合并内容并使用原有的text文本处理逻辑
            val combinedText = expandedComponents.joinToString("") { it.content }
            return TextFormatter.convertToJavaJson(combinedText, mNHandling, mnCFEnabled, context)
        }

        if (expandedComponents.size == 1 && expandedComponents[0].type == ComponentType.TEXT && expandedComponents[0].subComponents.isEmpty()) {
            // 单个纯文本组件（这个分支现在不会被触发，因为上面已经处理了所有TEXT组件的情况）
            val plainText = expandedComponents[0].content
            val formatMap = processSectionCodesToJson(plainText, mNHandling, mnCFEnabled)
            if (formatMap.isEmpty()) {
                return """{"text":"$plainText"}"""
            } else {
                val result = formatMap.toMutableMap()
                result["text"] = plainText
                return mapToJson(result)
            }
        }

        val mainComponent = expandedComponents[0]
        val result = mutableMapOf<String, Any>()

        when (mainComponent.type) {
            ComponentType.TEXT -> {
                result["text"] = mainComponent.content
                // 处理文本中的§代码
                val formatMap = processSectionCodesToJson(mainComponent.content, mNHandling, mnCFEnabled)
                result.putAll(formatMap)
            }
            ComponentType.TRANSLATE -> {
                result["translate"] = mainComponent.content
                if (mainComponent.subComponents.isNotEmpty()) {
                    val withSubComponent = mainComponent.subComponents.find { it.type == SubComponentType.WITH }
                    if (withSubComponent != null) {
                        // 解析with参数（用逗号分隔，支持\,转义）
                        val withParams = parseTranslateWithContent(withSubComponent.content)
                        // 过滤空参数并处理§代码
                        val withComponents = withParams.filter { it.isNotEmpty() }.map { param ->
                            val subFormatMap = processSectionCodesToJson(param, mNHandling, mnCFEnabled)
                            if (subFormatMap.isEmpty()) {
                                param
                            } else {
                                val mutableMap = subFormatMap.toMutableMap()
                                mutableMap["text"] = param
                                mutableMap
                            }
                        }
                        if (withComponents.isNotEmpty()) {
                            result["with"] = withComponents
                        }
                    }
                }
            }
            ComponentType.SCORE -> {
                // 记分板分数格式：{"score":{"name":"...","objective":"..."}}
                // 内容格式：name:objective（已展开，每个组件只包含一个条目）
                val scoreEntries = parseScoreContent(mainComponent.content)

                if (scoreEntries.isEmpty()) {
                    // 空内容，作为纯文本处理
                    result["text"] = mainComponent.content
                } else {
                    val (firstName, firstObjective) = scoreEntries[0]
                    result["score"] = mapOf(
                        "name" to firstName,
                        "objective" to firstObjective
                    )
                }
            }
            ComponentType.SELECTOR -> {
                // 选择器：完整调用选择器转换逻辑（已展开，每个组件只包含一个条目）
                val selectorString = mainComponent.content
                val (selectorEntries, separatorEntries) = parseSelectorContent(selectorString)

                if (selectorEntries.isEmpty()) {
                    // 空内容，作为空selector处理
                    result["selector"] = ""
                } else {
                    // 对selector调用完整的转换逻辑
                    val convertedSelector = if (context != null) {
                        // 使用convertForMixedMode获取Java版和基岩版结果
                        val reminders = mutableListOf<String>()
                        val (javaSelector, bedrockSelector) = SelectorConverter.convertForMixedMode(selectorEntries[0], context, reminders)
                        
                        // 如果convertForMixedMode没有进行任何转换（返回的selector和输入的selector相同），
                        // 说明selector只包含一种版本的特有参数，需要调用filterSelectorParameters来处理转换
                        if (javaSelector == selectorEntries[0]) {
                            // 没有进行任何转换，调用filterSelectorParameters来处理基岩版到Java版的参数转换
                            val (filteredSelector, _, _) = SelectorConverter.filterSelectorParameters(
                                selectorEntries[0],
                                SelectorType.JAVA,
                                context
                            )
                            filteredSelector
                        } else {
                            // 已经进行了转换，使用convertForMixedMode的结果
                            javaSelector
                        }
                    } else {
                        // 没有Context，保持原样
                        selectorEntries[0]
                    }

                    result["selector"] = convertedSelector

                    // 从副组件中提取separator
                    val separatorSubComponent = mainComponent.subComponents.find { it.type == SubComponentType.SEPARATOR }
                    if (separatorSubComponent != null) {
                        result["separator"] = mapOf("text" to separatorSubComponent.content)
                    } else if (separatorEntries.isNotEmpty() && separatorEntries[0] != null) {
                        // 如果没有副组件中的separator，使用parseSelectorContent返回的separator
                        result["separator"] = mapOf("text" to separatorEntries[0]!!)
                    }
                }
            }
        }

        // 添加extra（如果有子组件）
        if (expandedComponents.size > 1) {
            val extra = expandedComponents.subList(1, expandedComponents.size).map { sub ->
                val subMap = mutableMapOf<String, Any>()
                when (sub.type) {
                    ComponentType.TEXT -> {
                        subMap["text"] = sub.content
                        val formatMap = processSectionCodesToJson(sub.content, mNHandling, mnCFEnabled)
                        subMap.putAll(formatMap)
                    }
                    ComponentType.TRANSLATE -> {
                        subMap["translate"] = sub.content
                        if (sub.subComponents.isNotEmpty()) {
                            val withSubComponent = sub.subComponents.find { it.type == SubComponentType.WITH }
                            if (withSubComponent != null) {
                                // 解析with参数（用逗号分隔，支持\,转义）
                                val withParams = parseTranslateWithContent(withSubComponent.content)
                                // 过滤空参数并处理§代码
                                val withComponents = withParams.filter { it.isNotEmpty() }.map { param ->
                                    val subFormatMap = processSectionCodesToJson(param, mNHandling, mnCFEnabled)
                                    if (subFormatMap.isEmpty()) {
                                        param
                                    } else {
                                        val mutableMap = subFormatMap.toMutableMap()
                                        mutableMap["text"] = param
                                        mutableMap
                                    }
                                }
                                if (withComponents.isNotEmpty()) {
                                    subMap["with"] = withComponents
                                }
                            }
                        }
                    }
                    ComponentType.SCORE -> {
                        // SCORE组件已展开，每个组件只包含一个条目
                        val scoreEntries = parseScoreContent(sub.content)

                        if (scoreEntries.isEmpty()) {
                            subMap["text"] = sub.content
                        } else {
                            val (firstName, firstObjective) = scoreEntries[0]
                            subMap["score"] = mapOf(
                                "name" to firstName,
                                "objective" to firstObjective
                            )
                        }
                    }
                    ComponentType.SELECTOR -> {
                        // SELECTOR组件已展开，每个组件只包含一个条目
                        val selectorString = sub.content
                        val (selectorEntries, separatorEntries) = parseSelectorContent(selectorString)

                        if (selectorEntries.isEmpty()) {
                            subMap["text"] = sub.content
                        } else {
                            // 对selector调用完整的转换逻辑
                            val convertedSelector = if (context != null) {
                                // 使用convertForMixedMode获取Java版和基岩版结果
                                val reminders = mutableListOf<String>()
                                val (javaSelector, bedrockSelector) = SelectorConverter.convertForMixedMode(selectorEntries[0], context, reminders)
                                
                                // 如果convertForMixedMode没有进行任何转换（返回的selector和输入的selector相同），
                                // 说明selector只包含一种版本的特有参数，需要调用filterSelectorParameters来处理转换
                                if (javaSelector == selectorEntries[0]) {
                                    // 没有进行任何转换，调用filterSelectorParameters来处理基岩版到Java版的参数转换
                                    val (filteredSelector, _, _) = SelectorConverter.filterSelectorParameters(
                                        selectorEntries[0],
                                        SelectorType.JAVA,
                                        context
                                    )
                                    filteredSelector
                                } else {
                                    // 已经进行了转换，使用convertForMixedMode的结果
                                    javaSelector
                                }
                            } else {
                                // 没有Context，保持原样
                                selectorEntries[0]
                            }

                            subMap["selector"] = convertedSelector

                            // 从副组件中提取separator
                            val separatorSubComponent = sub.subComponents.find { it.type == SubComponentType.SEPARATOR }
                            if (separatorSubComponent != null) {
                                subMap["separator"] = separatorSubComponent.content
                            } else if (separatorEntries.isNotEmpty() && separatorEntries[0] != null) {
                                // 如果没有副组件中的separator，使用parseSelectorContent返回的separator
                                subMap["separator"] = separatorEntries[0]!!
                            }
                        }
                    }
                }
                subMap
            }
            result["extra"] = extra
        }

        return mapToJson(result)
    }
    
    /**
     * 转换为基岩版rawtext
     */
    fun convertToBedrockJson(
    components: List<TextComponent>, 
    mNHandling: String = "font", 
    mnCFEnabled: Boolean = false,
    context: Context? = null,
    warnings: MutableList<String>? = null
): String {
        if (components.isEmpty()) return """{"rawtext":[]}"""

        // 展开组件，将score和selector组件中的多个条目分割成独立的文本组件
        val expandedComponents = expandComponents(components)

        // 检查是否所有组件都是TEXT组件
        val allTextComponents = expandedComponents.all { it.type == ComponentType.TEXT && it.subComponents.isEmpty() }
        if (allTextComponents) {
            // 所有组件都是TEXT组件，合并内容并使用原有的text文本处理逻辑
            val combinedText = expandedComponents.joinToString("") { it.content }
            return TextFormatter.convertToBedrockJson(combinedText, mNHandling, mnCFEnabled)
        }

        val rawtext = expandedComponents.map { component ->
            val item = mutableMapOf<String, Any>()

            when (component.type) {
                ComponentType.TEXT -> {
                    // 处理§代码
                    item["text"] = processSectionCodesToBedrock(component.content, mNHandling, mnCFEnabled)
                }
                ComponentType.TRANSLATE -> {
                    item["translate"] = component.content
                    if (component.subComponents.isNotEmpty()) {
                        val withSubComponent = component.subComponents.find { it.type == SubComponentType.WITH }
                        if (withSubComponent != null) {
                            // 解析with参数（用逗号分隔，支持\,转义）
                            val withParams = parseTranslateWithContent(withSubComponent.content)
                            // 过滤空参数并处理§代码
                            val withRawtext = withParams.filter { it.isNotEmpty() }.map { param ->
                                mapOf<String, Any>("text" to processSectionCodesToBedrock(param, mNHandling, mnCFEnabled))
                            }
                            if (withRawtext.isNotEmpty()) {
                                item["with"] = mapOf("rawtext" to withRawtext)
                            }
                        }
                    }
                }
                ComponentType.SCORE -> {
                    // SCORE组件已展开，每个组件只包含一个条目
                    val scoreEntries = parseScoreContent(component.content)

                    if (scoreEntries.isEmpty()) {
                        // 空内容，作为纯文本处理
                        item["text"] = processSectionCodesToBedrock(component.content, mNHandling, mnCFEnabled)
                    } else {
                        val (firstName, firstObjective) = scoreEntries[0]
                        item["score"] = mapOf(
                            "name" to firstName,
                            "objective" to firstObjective
                        )
                    }
                }
                ComponentType.SELECTOR -> {
                    // SELECTOR组件已展开，每个组件只包含一个条目
                    val selectorString = component.content
                    val (selectorEntries, separatorEntries) = parseSelectorContent(selectorString)

                    if (selectorEntries.isEmpty()) {
                        // 空内容，作为空selector处理
                        item["selector"] = ""
                    } else {
                        // 检测是否有separator参数
                        if (separatorEntries.any { it != null }) {
                            warnings?.add(context?.getString(R.string.bedrock_separator_not_supported) ?: "基岩版不支持separator参数，已忽略所有sep:定义")
                        }

                        // 对selector调用完整的转换逻辑
                        val convertedSelector = if (context != null) {
                            // 使用convertForMixedMode获取Java版和基岩版结果
                            val reminders = mutableListOf<String>()
                            val (javaSelector, bedrockSelector) = SelectorConverter.convertForMixedMode(selectorEntries[0], context, reminders)
                            
                            // 如果convertForMixedMode没有进行任何转换（返回的selector和输入的selector相同），
                            // 说明selector只包含一种版本的特有参数，需要调用filterSelectorParameters来处理转换
                            if (bedrockSelector == selectorEntries[0]) {
                                // 没有进行任何转换，调用filterSelectorParameters来处理Java版到基岩版的参数转换
                            val (filteredSelector, _, _) = SelectorConverter.filterSelectorParameters(
                                selectorEntries[0],
                                SelectorType.BEDROCK,
                                context
                            )
                            filteredSelector
                            } else {
                                // 已经进行了转换，使用convertForMixedMode的结果
                                bedrockSelector
                            }
                        } else {
                            // 没有Context，保持原样
                            selectorEntries[0]
                        }

                        item["selector"] = convertedSelector

                        // 基岩版不支持separator参数，忽略所有sep:定义
                    }
                }
            }

            item
        }

        return """{"rawtext":${listToJson(rawtext)}}"""
    }

    /**
     * 解析score组件内容
     * 格式：name:objective（支持用逗号分隔多个）
     * 转义规则：如果name或objective包含逗号，用\,表示
     */
    private fun parseScoreContent(content: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        var currentEntry = StringBuilder()
        var i = 0
        
        while (i < content.length) {
            if (content[i] == '\\' && i + 1 < content.length && content[i + 1] == ',') {
                // 转义的逗号，添加单个逗号
                currentEntry.append(',')
                i += 2
            } else if (content[i] == ',') {
                // 分隔符，处理当前条目
                if (currentEntry.isNotEmpty()) {
                    result.add(parseSingleScoreEntry(currentEntry.toString()))
                }
                currentEntry.clear()
                i++
            } else {
                currentEntry.append(content[i])
                i++
            }
        }
        
        // 处理最后一个条目
        if (currentEntry.isNotEmpty()) {
            result.add(parseSingleScoreEntry(currentEntry.toString()))
        }
        
        return result
        
            }
        
            
        
            /**
        
             * 解析单个score条目
        
             * 格式：name:objective
        
             */
        
            private fun parseSingleScoreEntry(entry: String): Pair<String, String> {
        
                val parts = entry.split(":", limit = 2)
        
                val name = when {
        
                    parts[0].isEmpty() && parts.size == 2 -> "*"  // 只有:objective
        
                    parts[0].isEmpty() -> "*"  // 只有:
        
                    else -> parts[0]
        
                }
        
                val objective = when {
        
                    parts.size == 2 && parts[1].isEmpty() -> "*"  // 只有name:
        
                    parts.size == 2 -> parts[1]  // 正常格式
        
                    else -> "*"  // 只有name或格式错误
        
                }
        
                return Pair(name, objective)
        
            }
        
        
        
            /**
        
             * 解析selector组件内容
     * 格式：selector（支持用逗号分隔多个）
     * 特殊语法：sep:分隔符（用于指定前面一个selector的分隔符，必须完整匹配sep:）
     * 转义规则：如果selector包含逗号，用\,表示
     * @return Pair<选择器列表, 分隔符列表>（分隔符列表与选择器列表一一对应，无分隔符则为null）
     */
    /**
     * 解析selector组件内容
     * 格式：selector（支持用逗号分隔多个）
     * 特殊语法：,'sep':分隔符（用于指定前面所有@选择器的分隔符）
     * 转义规则：如果selector包含逗号，用\,表示
     * @return Pair<选择器列表, 分隔符列表>（分隔符列表与选择器列表一一对应，无分隔符则为null）
     */
    private fun parseSelectorContent(content: String): Pair<List<String>, List<String?>> {
        // 第一步：提取所有sep:定义和它们的位置
        // 规则：,'sep':作为一个整体，在selector文本组件有最高优先级
        // ,'sep':后面的文本一直到下一个,(不包含,)为一个整体，或者是如果后面没有,时，那么就是一直到结束的文本的separator
        val sepDefinitions = mutableListOf<Triple<Int, String, Int>>()  // (原始位置索引, 分隔符, selector索引)
        var i = 0
        
        while (i < content.length) {
            if (content.substring(i).startsWith(",'sep':")) {
                // 找到sep:定义
                val sepStart = i
                var sepEnd = i + 7  // ,'sep':的长度
                
                // 提取separator值
                // 规则：,'sep':后面的文本一直到下一个,(不包含,)为一个整体，或者是如果后面没有,时，那么就是一直到结束的文本
                val remainingText = content.substring(sepEnd)
                val nextCommaIndex = remainingText.indexOf(',')
                val separatorValue = if (nextCommaIndex != -1) {
                    remainingText.substring(0, nextCommaIndex).trim()
                } else {
                    remainingText.trim()
                }
                
                sepDefinitions.add(Triple(sepStart, separatorValue.ifEmpty { "," }, -1))  // selector索引暂时设为-1
                
                // 跳过sep:定义和separator值
                if (nextCommaIndex != -1) {
                    i = sepEnd + nextCommaIndex
                } else {
                    i = content.length
                }
            } else {
                i++
            }
        }
        
        // 第二步：提取selector条目（保留sep:定义，因为sep:定义应该保留在selector字符串中）
        // 规则：选择器的读取是从@一直到后面一个@(不包含后面一个@)，或者是如果后面没有@了那就表明一直到这个文本组件的结束或者一直到后面的,(不包含,)
        // 规则：如果同一selector文本组件第一个@前面有文本，那就第一个@前面的文本作为独立的selector组件参数(第一个@前面的'sep':直接被忽略)
        val selectors = mutableListOf<String>()
        val selectorPositions = mutableListOf<Int>()  // 每个selector的原始位置
        var startIndex = -1
        var lastAtPos = -1
        i = 0
        
        while (i < content.length) {
            // 注意：不再跳过sep:定义，因为sep:定义应该保留在selector字符串中
            
            if (content[i] == '@') {
                if (startIndex == -1) {
                    // 第一个@，记录起点
                    // 检查前面是否有非@文本
                    if (i > 0 && lastAtPos == -1) {
                        // 前面有非@文本，添加为selector
                        val precedingText = content.substring(0, i)
                        if (precedingText.isNotEmpty()) {
                            selectors.add(precedingText)
                            selectorPositions.add(0)  // 第一个selector的位置是0
                        }
                    }
                    startIndex = i
                } else {
                    // 找到下一个@，提取从startIndex到i-1的selector
                    var selector = content.substring(startIndex, i)
                    
                    // 检查selector是否以逗号结尾，如果是，则去掉末尾的逗号
                    if (selector.endsWith(",")) {
                        selector = selector.substring(0, selector.length - 1)
                    }
                    
                    selectors.add(selector)
                    selectorPositions.add(startIndex)
                    startIndex = i
                }
                lastAtPos = i
                i++
            } else if (content[i] == ',') {
                // 逗号，处理当前条目
                if (startIndex != -1) {
                    var selector = content.substring(startIndex, i)
                    
                    // 检查selector是否以逗号结尾，如果是，则去掉末尾的逗号
                    if (selector.endsWith(",")) {
                        selector = selector.substring(0, selector.length - 1)
                    }
                    
                    selectors.add(selector)
                    selectorPositions.add(startIndex)
                    startIndex = -1
                } else {
                    // 规则：如果有,相隔并且无@，就作为一个无@文本组件参数
                    // 检查逗号后面是否有非@文本
                    var nextIndex = i + 1
                    var foundAt = false
                    var foundComma = false
                    var textEndIndex = i + 1
                    
                    while (nextIndex < content.length) {
                        // 跳过sep:定义
                        if (content.substring(nextIndex).startsWith(",'sep':")) {
                            val sepEnd = nextIndex + 7
                            val remainingText = content.substring(sepEnd)
                            val nextCommaIndex = remainingText.indexOf(',')
                            if (nextCommaIndex != -1) {
                                nextIndex = sepEnd + nextCommaIndex + 1
                            } else {
                                nextIndex = content.length
                            }
                            continue
                        }
                        
                        if (content[nextIndex] == '@') {
                            // 后面有@，停止
                            foundAt = true
                            break
                        } else if (content[nextIndex] == ',') {
                            // 后面有逗号，将逗号之间的文本作为无@文本组件参数
                            foundComma = true
                            textEndIndex = nextIndex
                            break
                        } else {
                            nextIndex++
                        }
                    }
                    
                    // 如果找到了逗号，将逗号之间的文本作为无@文本组件参数
                    if (foundComma && !foundAt) {
                        val textBetweenCommas = content.substring(i + 1, textEndIndex).trim()
                        if (textBetweenCommas.isNotEmpty()) {
                            selectors.add(textBetweenCommas)
                            selectorPositions.add(i + 1)
                        }
                        i = textEndIndex
                        continue
                    }
                }
                i++
            } else {
                i++
            }
        }
        
        // 处理最后一个@到末尾的selector
        if (startIndex != -1) {
            var selector = content.substring(startIndex)
            
            // 检查selector是否以逗号结尾，如果是，则去掉末尾的逗号
            if (selector.endsWith(",")) {
                selector = selector.substring(0, selector.length - 1)
            }
            
            selectors.add(selector)
            selectorPositions.add(startIndex)
        } else if (lastAtPos == -1 && content.isNotEmpty()) {
            // 没有@，将整个内容作为selector
            selectors.add(content)
            selectorPositions.add(0)
        }
        
        // 第三步：为每个sep:定义找到对应的selector索引
        // 规则：sep:定义修饰它前面的所有@选择器，直到遇到没有被separator修饰的@选择器
        // 限制：
        // 1. separator只能修饰@选择器（mknbt、uuid等非@选择器忽略separator）
        // 2. 第一个@前面的sep:定义直接被忽略
        // 3. 'sep':'air'的特殊情况优先级最高：前面的第一个@选择器会自动忽略所有separator参数
        
        // 为每个sep:定义找到对应的selector索引
        // sep:定义应该放在它前面的第一个selector之后
        for (sepIdx in sepDefinitions.indices) {
            val sepPos = sepDefinitions[sepIdx].first
            // 找到sepPos前面最近的selector
            var bestSelectorIndex = -1
            for (selectorIdx in selectorPositions.indices) {
                if (selectorPositions[selectorIdx] < sepPos) {
                    bestSelectorIndex = selectorIdx
                } else {
                    break
                }
            }
            sepDefinitions[sepIdx] = Triple(sepPos, sepDefinitions[sepIdx].second, bestSelectorIndex)
        }
        
        // 初始化separators列表，全部为null
        val separators = mutableListOf<String?>()
        for (idx in selectors.indices) {
            separators.add(null)
        }
        
        // 找到第一个@选择器的位置
        var firstAtSelectorIndex = -1
        for (idx in selectors.indices) {
            if (selectors[idx].startsWith("@")) {
                firstAtSelectorIndex = idx
                break
            }
        }
        
        // 标记哪些@选择器被'sep':'air'影响（忽略所有separator参数）
        val airIgnoredSelectors = mutableSetOf<Int>()
        
        // 先处理所有sep:定义，记录哪些selector被哪些separator修饰
        val sepToSelectors = mutableMapOf<Int, MutableList<Int>>()  // sep索引 -> 修饰的selector索引列表
        val selectorToSep = mutableMapOf<Int, Int>()  // selector索引 -> 修饰它的sep索引
        
        // 处理sep:定义
        for ((sepPos, separatorValue, sepIndex) in sepDefinitions) {
            if (sepIndex == -1) continue
            
            // 检查sep:定义是否在第一个@选择器之前
            if (firstAtSelectorIndex != -1 && sepIndex < firstAtSelectorIndex) {
                // 第一个@前面的sep:定义直接被忽略
                continue
            }
            
            // 从sepIndex开始往前查找，修饰所有@选择器
            for (index in sepIndex - 1 downTo 0) {
                if (selectors[index].startsWith("@")) {
                    // 这是一个@选择器
                    if (!selectorToSep.containsKey(index)) {
                        // 这个@选择器还没有被修饰，应用separator
                        separators[index] = separatorValue
                        selectorToSep[index] = sepIndex
                        sepToSelectors.getOrPut(sepIndex) { mutableListOf() }.add(index)
                    } else {
                        // 这个@选择器已经被修饰了，停止
                        break
                    }
                }
            }
        }
        
        // 处理'sep':'air'的特殊情况
        for ((sepPos, separatorValue, sepIndex) in sepDefinitions) {
            if (sepIndex == -1) continue
            if (separatorValue != "air") continue
            
            // 找到sep:定义前面的第一个@选择器
            var targetIndex = -1
            for (index in sepIndex - 1 downTo 0) {
                if (selectors[index].startsWith("@")) {
                    targetIndex = index
                    break
                }
            }
            if (targetIndex != -1) {
                // 清除这个@选择器的separator
                separators[targetIndex] = null
                // 移除selector到sep的映射
                selectorToSep.remove(targetIndex)
                // 从sep到selectors的映射中移除
                sepToSelectors[sepIndex]?.remove(targetIndex)
            }
        }
        
        return Pair(selectors, separators)
    }
    
    /**
     * 解析translate组件的with参数
     * @return 参数列表
     */
    private fun parseTranslateWithContent(content: String): List<String> {
        val result = mutableListOf<String>()
        var currentEntry = StringBuilder()
        var i = 0

        while (i < content.length) {
            if (content[i] == '\\' && i + 1 < content.length && content[i + 1] == ',') {
                // 转义的逗号，添加单个逗号
                currentEntry.append(',')
                i += 2
            } else if (content[i] == ',') {
                // 分隔符，处理当前条目
                result.add(currentEntry.toString())
                currentEntry.clear()
                i++
            } else {
                currentEntry.append(content[i])
                i++
            }
        }

        // 处理最后一个条目
        result.add(currentEntry.toString())

        return result
    }

    /**
     * 展开组件列表，将score和selector组件中的多个条目分割成独立的文本组件
     * 这样每个score或selector条目都会成为独立的文本组件，而不是放在同一个组件的extra中
     */
    private fun expandComponents(components: List<TextComponent>): List<TextComponent> {
        val expanded = mutableListOf<TextComponent>()

        for (component in components) {
            when (component.type) {
                ComponentType.TEXT -> {
                    // TEXT组件直接使用原有的text文本处理逻辑
                    // 将TEXT组件的内容提取出来，作为纯文本处理
                    expanded.add(TextComponent(ComponentType.TEXT, component.content))
                }
                ComponentType.SCORE -> {
                    val scoreEntries = parseScoreContent(component.content)
                    if (scoreEntries.isEmpty()) {
                        // 空内容，作为纯文本处理
                        expanded.add(TextComponent(ComponentType.TEXT, component.content))
                    } else {
                        // 每个score条目都作为独立的score组件
                        for ((name, objective) in scoreEntries) {
                            expanded.add(TextComponent(ComponentType.SCORE, "$name:$objective"))
                        }
                    }
                }
                ComponentType.SELECTOR -> {
                    val (selectorEntries, separatorEntries) = parseSelectorContent(component.content)
                    if (selectorEntries.isEmpty()) {
                        // 空内容，保留为selector组件（内容为空字符串）
                        expanded.add(TextComponent(ComponentType.SELECTOR, ""))
                    } else {
                        // 每个selector条目都作为独立的selector组件
                        for ((index, selector) in selectorEntries.withIndex()) {
                            // 添加separator作为副组件
                            val subComponents = mutableListOf<SubComponent>()
                            if (index < separatorEntries.size && separatorEntries[index] != null) {
                                subComponents.add(SubComponent(SubComponentType.SEPARATOR, separatorEntries[index]!!))
                            }
                            expanded.add(TextComponent(ComponentType.SELECTOR, selector, subComponents))
                        }
                    }
                }
                else -> {
                    // 其他组件类型保持不变
                    expanded.add(component)
                }
            }
        }

        return expanded
    }
    private fun mapToJson(map: Map<String, Any>): String {
        val entries = map.entries.joinToString(",") { (key, value) ->
            "\"$key\":${valueToJson(value)}"
        }
        return "{$entries}"
    }
    
    /**
     * 简化的List转JSON
     */
    private fun listToJson(list: List<Any>): String {
        val items = list.joinToString(",") { valueToJson(it) }
        return "[$items]"
    }
    
    /**
     * 值转JSON
     */
    private fun valueToJson(value: Any): String {
        return when (value) {
            is String -> "\"${value.replace("\"", "\\\"")}\""
            is Number -> value.toString()
            is Boolean -> value.toString()
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                mapToJson(value as Map<String, Any>)
            }
            is List<*> -> {
                @Suppress("UNCHECKED_CAST")
                listToJson(value as List<Any>)
            }
            else -> "\"$value\""
        }
    }
    
    /**
     * 移除所有组件标记，返回纯文本
     */
    fun stripComponentMarkers(text: String): String {
        // 如果没有标记符，直接返回
        if (!text.contains(MARKER_START) || !text.contains(MARKER_END)) {
            return text
        }
        
        // 解析组件列表
        val components = parseTextComponents(text)
        
        // 提取纯文本内容（包括副组件内容）
        return components.joinToString("") { component ->
            // 主内容
            val mainContent = component.content
            // 副组件内容（跳过__type.key__content__标记，只提取内容）
            val subContent = component.subComponents.joinToString("") { it.content }
            mainContent + subContent
        }
    }
}
