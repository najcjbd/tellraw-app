package com.tellraw.app.util

/**
 * 文本组件工具类
 * 处理文本组件的标记、解析和转换
 */
import android.content.Context
import androidx.annotation.StringRes
import com.tellraw.app.R
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
        WITH("with", R.string.component_with, ComponentType.TRANSLATE)
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
     */
    fun parseTextComponents(text: String): List<TextComponent> {
        val components = mutableListOf<TextComponent>()
        var i = 0
        
        while (i < text.length) {
            // 查找组件开始标记
            if (i + 1 < text.length && text[i] == MARKER_START) {
                // 查找对应的结束标记
                val endPos = findMatchingEndMarker(text, i)
                if (endPos != -1) {
                    // 提取组件内容
                    val componentContent = text.substring(i + 1, endPos)
                    val component = parseSingleComponent(componentContent)
                    if (component != null) {
                        components.add(component)
                    }
                    i = endPos + 1
                } else {
                    // 没有找到结束标记，将标记作为普通文本处理
                    components.add(TextComponent(ComponentType.TEXT, text[i].toString()))
                    i++
                }
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
     * 查找匹配的结束标记
     */
    private fun findMatchingEndMarker(text: String, startPos: Int): Int {
        var depth = 1
        var i = startPos + 1
        
        while (i < text.length && depth > 0) {
            if (text[i] == MARKER_START) {
                depth++
            } else if (text[i] == MARKER_END) {
                depth--
            }
            i++
        }
        
        return if (depth == 0) i - 1 else -1
    }
    
    /**
     * 解析单个组件
     */
    private fun parseSingleComponent(content: String): TextComponent? {
        // 查找组件类型和内容
        val parts = content.split(MARKER_END.toString()).toMutableList()
        if (parts.size < 2) return null
        
        val typeKey = parts[0]
        val type = ComponentType.values().find { it.key == typeKey } ?: return null
        
        // 主内容
        val mainContent = if (parts.size >= 2) parts[1] else ""
        
        // 解析副组件
        val subComponents = mutableListOf<SubComponent>()
        var i = 2
        while (i < parts.size) {
            if (i + 1 < parts.size) {
                val subTypeKey = parts[i]
                val subType = SubComponentType.values().find { it.key == subTypeKey }
                if (subType != null) {
                    subComponents.add(SubComponent(subType, parts[i + 1]))
                    i += 2
                } else {
                    i++
                }
            } else {
                i++
            }
        }
        
        return TextComponent(type, mainContent, subComponents)
    }
    
    /**
     将组件列表转换为标记文本
     */
    fun componentsToText(components: List<TextComponent>): String {
        return components.joinToString("") { component ->
            val subComponentText = component.subComponents.joinToString("") { sub ->
                "$MARKER_END${sub.type.key}$MARKER_END${sub.content}$MARKER_END${sub.type.key}"
            }
            "$MARKER_START${component.type.key}$MARKER_END${component.content}$subComponentText$MARKER_END${component.type.key}"
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
        if (currentComponent == null || currentComponent == ComponentType.TEXT) {
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
        
        // 如果光标在所有组件之后，创建新组件
        if (targetComponentIndex == -1) {
            val result = StringBuilder()
            components.forEach { result.append(componentToText(it)) }
            result.append(componentToText(TextComponent(currentComponent, textToInsert)))
            return result.toString()
        }
        
        // 光标在某个组件内
        val targetComponent = components[targetComponentIndex]
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
        
        if (components.size == 1 && components[0].type == ComponentType.TEXT && components[0].subComponents.isEmpty()) {
            // 单个纯文本组件
            val plainText = components[0].content
            val formatMap = processSectionCodesToJson(plainText, mNHandling, mnCFEnabled)
            if (formatMap.isEmpty()) {
                return """{"text":"$plainText"}"""
            } else {
                val result = formatMap.toMutableMap()
                result["text"] = plainText
                return mapToJson(result)
            }
        }
        
        val mainComponent = components[0]
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
                    val withComponents = mainComponent.subComponents
                        .filter { it.type == SubComponentType.WITH }
                        .map { sub ->
                            // 处理副组件内容中的§代码
                            val subFormatMap = processSectionCodesToJson(sub.content, mNHandling, mnCFEnabled)
                            if (subFormatMap.isEmpty()) {
                                sub.content
                            } else {
                                val mutableMap = subFormatMap.toMutableMap()
                                mutableMap["text"] = sub.content
                                mutableMap
                            }
                        }
                    if (withComponents.isNotEmpty()) {
                        result["with"] = withComponents
                    }
                }
            }
            ComponentType.SCORE -> {
                // 记分板分数格式：{"score":{"name":"...","objective":"..."}}
                // 内容格式：name:objective，支持用逗号分隔多个
                // 转义规则：如果name或objective包含逗号，用'\,'表示
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
                    
                    // 添加剩余的score组件到extra
                    if (scoreEntries.size > 1) {
                        val extraScores = scoreEntries.drop(1).map { (name, objective) ->
                            mapOf<String, Any>("score" to mapOf("name" to name, "objective" to objective))
                        }
                        
                        // 如果已有extra，合并
                        val existingExtra = result["extra"] as? List<Map<String, Any>> ?: emptyList()
                        result["extra"] = existingExtra + extraScores
                    }
                }
            }
ComponentType.SELECTOR -> {
                // 选择器：完整调用选择器转换逻辑
                val selectorString = mainComponent.content
                val (selectorEntries, separatorEntries) = parseSelectorContent(selectorString)
                
                if (selectorEntries.isEmpty()) {
                    // 空内容，作为纯文本处理
                    result["text"] = mainComponent.content
                } else {
                    // 对每个selector调用完整的转换逻辑
                    val convertedSelectors = selectorEntries.mapIndexed { index, selector ->
                        if (context != null) {
                            // 使用convertForMixedMode获取Java版和基岩版结果
                            val reminders = mutableListOf<String>()
                            val (javaSelector, bedrockSelector) = SelectorConverter.convertForMixedMode(selector, context, reminders)
                            javaSelector  // Java版使用Java版结果
                        } else {
                            // 没有Context，保持原样
                            selector
                        }
                    }
                    
                    result["selector"] = convertedSelectors[0]
                    
                    // 如果有separator，添加到主selector
                    if (separatorEntries.isNotEmpty() && separatorEntries[0] != null) {
                        result["separator"] = mapOf("text" to separatorEntries[0]!!)
                    }
                    
                    // 添加剩余的selector组件到extra
                    if (convertedSelectors.size > 1) {
                        val extraSelectors = mutableListOf<Map<String, Any>>()
                        for (i in 1 until convertedSelectors.size) {
                            val selectorMap = mutableMapOf<String, Any>("selector" to convertedSelectors[i])
                            // 如果有separator，添加到该selector
                            if (separatorEntries.size > i && separatorEntries[i] != null) {
                                selectorMap["separator"] = mapOf("text" to separatorEntries[i]!!)
                            }
                            extraSelectors.add(selectorMap)
                        }
                        
                        // 如果已有extra，合并
                        val existingExtra = result["extra"] as? List<Map<String, Any>> ?: emptyList()
                        result["extra"] = existingExtra + extraSelectors
                    }
                }
            }
        }
        
        // 添加extra（如果有子组件）
        if (components.size > 1) {
            val extra = components.subList(1, components.size).map { sub ->
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
                            val withComponents = sub.subComponents
                                .filter { it.type == SubComponentType.WITH }
                                .map { with ->
                                    val subFormatMap = processSectionCodesToJson(with.content, mNHandling, mnCFEnabled)
                                    if (subFormatMap.isEmpty()) {
                                        with.content
                                    } else {
                                        val mutableMap = subFormatMap.toMutableMap()
                                        mutableMap["text"] = with.content
                                        mutableMap
                                    }
                                }
                            if (withComponents.isNotEmpty()) {
                                subMap["with"] = withComponents
                            }
                        }
                    }
                    ComponentType.SCORE -> {
                        val scoreEntries = parseScoreContent(sub.content)
                        
                        if (scoreEntries.isEmpty()) {
                            subMap["text"] = sub.content
                        } else {
                            val (firstName, firstObjective) = scoreEntries[0]
                            subMap["score"] = mapOf(
                                "name" to firstName,
                                "objective" to firstObjective
                            )
                            
                            // 多个score组件，使用extra
                            if (scoreEntries.size > 1) {
                                val extraScores = scoreEntries.drop(1).map { (name, objective) ->
                                    mapOf<String, Any>("score" to mapOf("name" to name, "objective" to objective))
                                }
                                
                                // 将extraScores添加到extra列表中（稍后合并）
                                subMap["__extra_scores__"] = extraScores
                            }
                        }
                    }
                    ComponentType.SELECTOR -> {
                        // SELECTOR组件支持多个选择器（用逗号分隔），完整调用选择器转换逻辑
                        val selectorString = sub.content
                        val (selectorEntries, separatorEntries) = parseSelectorContent(selectorString)
                        
                        if (selectorEntries.isEmpty()) {
                            subMap["text"] = sub.content
                        } else {
                            // 对每个selector调用完整的转换逻辑
                            val convertedSelectors = selectorEntries.mapIndexed { index, selector ->
                                if (context != null) {
                                    // 使用convertForMixedMode获取Java版和基岩版结果
                                    val reminders = mutableListOf<String>()
                                    val (javaSelector, bedrockSelector) = SelectorConverter.convertForMixedMode(selector, context, reminders)
                                    javaSelector  // Java版使用Java版结果
                                } else {
                                    // 没有Context，保持原样
                                    selector
                                }
                            }
                            
                            subMap["selector"] = convertedSelectors[0]
                            
                            // 如果有separator，添加到主selector
                            if (separatorEntries.isNotEmpty() && separatorEntries[0] != null) {
                                subMap["separator"] = mapOf("text" to separatorEntries[0]!!)
                            }
                            
                            // 多个selector组件，使用extra
                            if (convertedSelectors.size > 1) {
                                val extraSelectors = mutableListOf<Map<String, Any>>()
                                for (i in 1 until convertedSelectors.size) {
                                    val selectorMap = mutableMapOf<String, Any>("selector" to convertedSelectors[i])
                                    // 如果有separator，添加到该selector
                                    if (separatorEntries.size > i && separatorEntries[i] != null) {
                                        selectorMap["separator"] = mapOf("text" to separatorEntries[i]!!)
                                    }
                                    extraSelectors.add(selectorMap)
                                }
                                
                                // 将extraSelectors添加到extra列表中（稍后合并）
                                subMap["__extra_selectors__"] = extraSelectors
                            }
                        }
                    }
                }
                subMap
            }
            result["extra"] = extra
        }
        
        // 处理__extra_scores__，展开到extra中
        val extraList = result["extra"] as? MutableList<Map<String, Any>> ?: mutableListOf()
        val itemsToRemove = mutableListOf<Int>()
        
        for (i in extraList.indices) {
            val item = extraList[i].toMutableMap()
            val extraScores = item.remove("__extra_scores__") as? List<Map<String, Any>>
            if (extraScores != null) {
                extraList[i] = item  // 更新item，移除__extra_scores__
                itemsToRemove.add(i)
                // 在当前位置插入额外的score组件
                extraList.addAll(i + 1, extraScores)
            }
            
            val extraSelectors = item.remove("__extra_selectors__") as? List<Map<String, Any>>
            if (extraSelectors != null) {
                extraList[i] = item  // 更新item，移除__extra_selectors__
                itemsToRemove.add(i)
                // 在当前位置插入额外的selector组件
                extraList.addAll(i + 1, extraSelectors)
            }
        }
        
        // 删除已处理的项
        itemsToRemove.sortedDescending().forEach { extraList.removeAt(it) }
        
        if (extraList.isNotEmpty()) {
            result["extra"] = extraList
        } else {
            result.remove("extra")
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
        
        val rawtext = components.map { component ->
            val item = mutableMapOf<String, Any>()
            
            when (component.type) {
                ComponentType.TEXT -> {
                    // 处理§代码
                    item["text"] = processSectionCodesToBedrock(component.content, mNHandling, mnCFEnabled)
                }
                ComponentType.TRANSLATE -> {
                    item["translate"] = component.content
                    if (component.subComponents.isNotEmpty()) {
                        val withRawtext = component.subComponents
                            .filter { it.type == SubComponentType.WITH }
                            .map { with ->
                                mapOf<String, Any>("text" to processSectionCodesToBedrock(with.content, mNHandling, mnCFEnabled))
                            }
                        if (withRawtext.isNotEmpty()) {
                            item["with"] = mapOf("rawtext" to withRawtext)
                        }
                    }
                }
                ComponentType.SCORE -> {
                    // 记分板分数格式：{"score":{"name":"...","objective":"..."}}
                    // 支持多个name:objective，用逗号分隔
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
                        
                        // 添加剩余的score组件
                        if (scoreEntries.size > 1) {
                            val extraScores = scoreEntries.drop(1).map { (name, objective) ->
                                mapOf<String, Any>("score" to mapOf("name" to name, "objective" to objective))
                            }
                            
                            // 将extraScores添加到rawtext中（在函数返回前处理）
                            item["__extra_scores__"] = extraScores
                        }
                    }
                }
ComponentType.SELECTOR -> {
                    // 选择器：完整调用选择器转换逻辑（基岩版不支持separator）
                    val selectorString = component.content
                    val (selectorEntries, separatorEntries) = parseSelectorContent(selectorString)
                    
                    if (selectorEntries.isEmpty()) {
                        // 空内容，作为纯文本处理
                        item["text"] = processSectionCodesToBedrock(component.content, mNHandling, mnCFEnabled)
                    } else {
                        // 检测是否有separator参数
                        if (separatorEntries.any { it != null }) {
                            warnings?.add(context?.getString(R.string.bedrock_separator_not_supported) ?: "基岩版不支持separator参数，已忽略所有sep:定义")
                        }
                        
                        // 对每个selector调用完整的转换逻辑
                        val convertedSelectors = selectorEntries.map { selector ->
                            if (context != null) {
                                // 使用convertForMixedMode获取Java版和基岩版结果
                                val reminders = mutableListOf<String>()
                                val (javaSelector, bedrockSelector) = SelectorConverter.convertForMixedMode(selector, context, reminders)
                                bedrockSelector  // 基岩版使用基岩版结果
                            } else {
                                // 没有Context，保持原样
                                selector
                            }
                        }
                        
                        item["selector"] = convertedSelectors[0]
                        
                        // 添加剩余的selector组件
                        if (convertedSelectors.size > 1) {
                            val extraSelectors = convertedSelectors.drop(1).map { selector ->
                                mapOf<String, Any>("selector" to selector)
                            }
                            
                            // 将extraSelectors添加到rawtext中（在函数返回前处理）
                            item["__extra_selectors__"] = extraSelectors
                        }
                        
                        // 基岩版不支持separator参数，忽略所有sep:定义
                    }
                }
            }
            
            item
        }
        
        // 处理__extra_scores__和__extra_selectors__，展开成多个rawtext项
        val expandedRawtext = mutableListOf<Map<String, Any>>()
        for (item in rawtext) {
            expandedRawtext.add(item)
            val extraScores = item.remove("__extra_scores__") as? List<Map<String, Any>>
            if (extraScores != null) {
                expandedRawtext.addAll(extraScores)
            }
            val extraSelectors = item.remove("__extra_selectors__") as? List<Map<String, Any>>
            if (extraSelectors != null) {
                expandedRawtext.addAll(extraSelectors)
            }
        }
        
        return """{"rawtext":${listToJson(expandedRawtext)}}"""
    }
    
    /**
     * 解析score组件内容
     * 格式：name:objective（支持用逗号分隔多个）
     * 转义规则：如果name或objective包含逗号，用'\,'表示
     */
    private fun parseScoreContent(content: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        var currentEntry = StringBuilder()
        var i = 0
        
        while (i < content.length) {
            if (content[i] == ',' && i + 1 < content.length && content[i + 1] == ',') {
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
     * 解析selector组件内容
     * 格式：selector（支持用逗号分隔多个）
     * 特殊语法：sep:分隔符（用于指定前面一个selector的分隔符，必须完整匹配sep:）
     * 转义规则：如果selector包含逗号，用'\,'表示
     * @return Pair<选择器列表, 分隔符列表>（分隔符列表与选择器列表一一对应，无分隔符则为null）
     */
    private fun parseSelectorContent(content: String): Pair<List<String>, List<String?>> {
        val selectors = mutableListOf<String>()
        val separators = mutableListOf<String?>()
        var currentEntry = StringBuilder()
        var i = 0
        
        while (i < content.length) {
            if (content[i] == ',' && i + 1 < content.length && content[i + 1] == ',') {
                // 转义的逗号，添加单个逗号
                currentEntry.append(',')
                i += 2
            } else if (content[i] == ',') {
                // 分隔符，处理当前条目
                val entry = currentEntry.toString().trim()
                if (entry.isNotEmpty()) {
                    if (entry == "sep:" || entry.startsWith("sep:")) {
                        // 这是分隔符定义（必须以sep:开头）
                        val separatorValue = entry.substring(4).trim()
                        // 将分隔符应用到前一个selector
                        if (selectors.isNotEmpty()) {
                            // 更新最后一个selector的分隔符
                            if (separators.size < selectors.size) {
                                // 补齐分隔符列表
                                while (separators.size < selectors.size - 1) {
                                    separators.add(null)
                                }
                                separators.add(separatorValue.ifEmpty { "," })
                            } else {
                                // 替换最后一个分隔符（多个sep:时以最后一个为准）
                                separators[separators.size - 1] = separatorValue.ifEmpty { "," }
                            }
                        }
                    } else {
                        // 这是普通selector
                        selectors.add(entry)
                        separators.add(null)  // 默认无分隔符
                    }
                }
                currentEntry.clear()
                i++
            } else {
                currentEntry.append(content[i])
                i++
            }
        }
        
        // 处理最后一个条目
        val lastEntry = currentEntry.toString().trim()
        if (lastEntry.isNotEmpty()) {
            if (lastEntry == "sep:" || lastEntry.startsWith("sep:")) {
                // 这是分隔符定义
                val separatorValue = lastEntry.substring(4).trim()
                // 将分隔符应用到前一个selector
                if (selectors.isNotEmpty()) {
                    if (separators.size < selectors.size) {
                        while (separators.size < selectors.size - 1) {
                            separators.add(null)
                        }
                        separators.add(separatorValue.ifEmpty { "," })
                    } else {
                        separators[separators.size - 1] = separatorValue.ifEmpty { "," }
                    }
                }
            } else {
                // 这是普通selector
                selectors.add(lastEntry)
                separators.add(null)
            }
        }
        
        return Pair(selectors, separators)
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
            is Map<*, *> -> mapToJson(value as Map<String, Any>)
            is List<*> -> listToJson(value as List<Any>)
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
        
        // 提取纯文本内容
        return components.joinToString("") { component ->
            // 主内容
            val mainContent = component.content
            // 副组件内容
            val subContent = component.subComponents.joinToString("") { it.content }
            mainContent + subContent
        }
    }
}
