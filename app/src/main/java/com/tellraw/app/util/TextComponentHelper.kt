package com.tellraw.app.util

/**
 * 文本组件工具类
 * 处理文本组件的标记、解析和转换
 */
import android.content.Context
import androidx.annotation.StringRes
import com.tellraw.app.R
import com.google.gson.JsonElement
import com.google.gson.JsonParser
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
        SELECTOR("selector", R.string.component_selector, true)
    }
    
    // 副组件类型
    enum class SubComponentType(val key: String, @StringRes val displayNameResId: Int, val parentComponent: ComponentType?) {
        WITH("with", R.string.component_with, ComponentType.TRANSLATE),
        WITH_JSON("with_json", R.string.component_with_json, ComponentType.TRANSLATE),
        FALLBACK("fallback", R.string.component_fallback, ComponentType.TRANSLATE),
        SEPARATOR("separator", R.string.component_separator, ComponentType.SELECTOR)
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
     * 标记格式：content + 副组件 + MARKER_START + type.key + MARKER_END
     * 即类型标记是它前面那段内容的后缀，副组件写在内容之后、类型标记之前
     */
    // ---------------- 标记符转义 ----------------
    // 用户输入里的 \、_、ྈ、༴ 会先转义，解析时再还原；
    // 这样玩家输入的敏感符号不会被当成组件/副组件标记。

    private fun escapeContent(value: String): String =
        value.replace("\\", "\\\\")
            .replace("_", "\\_")
            .replace(MARKER_START.toString(), "\\" + MARKER_START)
            .replace(MARKER_END.toString(), "\\" + MARKER_END)

    private fun unescapeContent(value: String): String {
        val sb = StringBuilder(value.length)
        var i = 0
        while (i < value.length) {
            if (value[i] == '\\' && i + 1 < value.length) {
                sb.append(value[i + 1]); i += 2
            } else {
                sb.append(value[i]); i++
            }
        }
        return sb.toString()
    }

    private fun isEscaped(text: String, index: Int): Boolean {
        var count = 0
        var i = index - 1
        while (i >= 0 && text[i] == '\\') { count++; i-- }
        return count % 2 == 1
    }

    private fun indexOfUnescaped(text: String, needle: String, start: Int): Int {
        var i = start
        while (i <= text.length - needle.length) {
            if (text.startsWith(needle, i) && !isEscaped(text, i)) return i
            i++
        }
        return -1
    }

    fun parseTextComponents(text: String): List<TextComponent> {
        // 没有组件标记：原样作为一个 TEXT 组件（不能 unescape，否则玩家输入的 \ 会消失）
        if (MARKER_START !in text) {
            return if (text.isEmpty()) emptyList() else listOf(TextComponent(ComponentType.TEXT, text))
        }
        val components = mutableListOf<TextComponent>()
        var i = 0
        var componentStart = 0  // 追踪当前组件内容的开始位置
        
        while (i < text.length) {
            if (text[i] == '\\') { i += 2; continue }
            // 查找组件开始标记（转义的不算）
            if (text[i] == MARKER_START && !isEscaped(text, i)) {
                // 查找组件类型
                val typeEnd = indexOfUnescaped(text, MARKER_END.toString(), i + 1)
                
                if (typeEnd == -1) {
                    // 没有找到类型结束标记，把标记之后的全部当普通文本处理
                    components.add(TextComponent(ComponentType.TEXT, unescapeContent(text.substring(componentStart))))
                    // 必须把 componentStart 推到底：否则结尾的"处理剩余普通文本"会把同一段再加一遍
                    // （实测 @aྈselector༴abྈ -> extra 里 abྈ 出现两次）
                    componentStart = text.length
                    break
                }
                
                val typeKey = text.substring(i + 1, typeEnd)
                val type = ComponentType.values().find { it.key == typeKey }
                
                if (type == null) {
                    // 未知的组件类型，将标记作为普通文本处理
                    components.add(TextComponent(ComponentType.TEXT, unescapeContent(text.substring(componentStart, typeEnd + 1))))
                    componentStart = typeEnd + 1
                    i = typeEnd + 1
                    continue
                }
                
                // 提取组件内容（从componentStart到i的文本），再从中解析出副组件
                val componentContent = text.substring(componentStart, i)
                val component = parseSingleComponentWithContent(type, componentContent)
                    ?: TextComponent(type, unescapeContent(componentContent))
                components.add(component)
                
                // 更新位置
                componentStart = typeEnd + 1
                i = typeEnd + 1
            } else {
                i++
            }
        }
        
        // 处理剩余的普通文本
        if (componentStart < text.length) {
            val remainingText = text.substring(componentStart)
            components.add(TextComponent(ComponentType.TEXT, unescapeContent(remainingText)))
        }
        
        return components
    }
    
    /**
     * 解析单个组件（已知组件类型）
     * 副组件格式：__type.key__content__，写在主内容之后
     * 只有"类型合法 + 父组件匹配 + 以__结束"的才算有效副组件，
     * 避免把用户输入里的 __with__ 之类误认成副组件标记
     */
    private fun parseSingleComponentWithContent(type: ComponentType, content: String): TextComponent? {
        // 查找副组件
        val subComponents = mutableListOf<SubComponent>()
        var mainContentEnd = content.length  // 主内容的结束位置（遇到第一个有效副组件时前移）
        var searchStart = 0
        
        while (searchStart < content.length) {
            // 查找副组件开始标记：__type.key__（转义的 __ 不算）
            val subComponentStart = indexOfUnescaped(content, "__", searchStart)
            if (subComponentStart == -1) break
            
            // 查找副组件类型
            val typeStart = subComponentStart + 2  // 跳过__
            val typeEnd = indexOfUnescaped(content, "__", typeStart)
            if (typeEnd == -1) break
            
            val subTypeKey = content.substring(typeStart, typeEnd)
            // 副组件类型必须与父组件匹配：WITH只属于translate，SEPARATOR只属于selector
            val subType = SubComponentType.values().find { it.key == subTypeKey && it.parentComponent == type }
            if (subType == null) {
                // 不是有效的副组件类型，跳过
                searchStart = typeEnd + 2
                continue
            }
            
            // 查找副组件内容（从typeEnd + 2开始，到下一个__）
            val contentStart = typeEnd + 2
            val contentEnd = indexOfUnescaped(content, "__", contentStart)
            if (contentEnd == -1) {
                // 没有找到结束标记，这不是有效的副组件
                break
            }
            
            if (subComponents.isEmpty()) {
                // 主内容到第一个有效副组件为止
                mainContentEnd = subComponentStart
            }
            val subContent = unescapeContent(content.substring(contentStart, contentEnd))
            subComponents.add(SubComponent(subType, subContent))
            
            searchStart = contentEnd + 2  // 跳过结束标记__
        }
        
        // 主内容：到第一个有效副组件为止（没有副组件时就是整个内容）
        val mainContent = unescapeContent(content.substring(0, mainContentEnd))
        
        return TextComponent(type, mainContent, subComponents)
    }
    
    /**
     * 将组件列表转换为标记文本
     * 格式：content + 副组件 + MARKER_START + type.key + MARKER_END
     * 副组件格式：__type.key__content__
     * 即类型标记是该组件内容的后缀
     */
    fun componentsToText(components: List<TextComponent>): String {
        return components.joinToString("") { component ->
            val subComponentText = component.subComponents.joinToString("") { sub ->
                "__${sub.type.key}__${escapeContent(sub.content)}__"
            }
            "${escapeContent(component.content)}$subComponentText$MARKER_START${component.type.key}$MARKER_END"
        }
    }
    
    /**
     * 前台纯文本被增删后，按旧组件结构重新切分内容（结构保持不变）。
     * 只用于存在非 TEXT 组件的情况；截断时副组件同样按绝对位置截取。
     */
    fun remapComponentContents(
        oldComponents: List<TextComponent>,
        frontendMessage: String
    ): List<TextComponent> {
        val result = mutableListOf<TextComponent>()
        val newLength = frontendMessage.length
        var currentPos = 0

        for (oldComponent in oldComponents) {
            val componentLength =
                oldComponent.content.length + oldComponent.subComponents.sumOf { it.content.length }

            if (currentPos + componentLength <= newLength) {
                val newContent =
                    frontendMessage.substring(currentPos, currentPos + oldComponent.content.length)
                val newSubComponents = oldComponent.subComponents.mapIndexed { index, oldSub ->
                    val offset = oldComponent.content.length +
                        oldComponent.subComponents.take(index).sumOf { it.content.length }
                    val subContent = frontendMessage.substring(
                        currentPos + offset, currentPos + offset + oldSub.content.length
                    )
                    SubComponent(oldSub.type, subContent)
                }.toMutableList()

                result.add(TextComponent(oldComponent.type, newContent, newSubComponents))
                currentPos += newContent.length + newSubComponents.sumOf { it.content.length }
            } else {
                val remainingLength = newLength - currentPos
                if (remainingLength > 0) {
                    val newContent = frontendMessage.substring(
                        currentPos, currentPos + minOf(oldComponent.content.length, remainingLength)
                    )
                    val newSubComponents = mutableListOf<SubComponent>()
                    // 副组件的绝对起点 = 当前组件起点 + 旧主内容长度
                    var subOffset = currentPos + oldComponent.content.length
                    for (oldSub in oldComponent.subComponents) {
                        if (subOffset < newLength) {
                            val remainingSubLength = newLength - subOffset
                            val subContent = frontendMessage.substring(
                                subOffset, subOffset + minOf(oldSub.content.length, remainingSubLength)
                            )
                            newSubComponents.add(SubComponent(oldSub.type, subContent))
                            subOffset += oldSub.content.length
                        }
                    }

                    result.add(TextComponent(oldComponent.type, newContent, newSubComponents))
                    currentPos += newContent.length + newSubComponents.sumOf { it.content.length }
                }
            }
        }

        if (currentPos < newLength) {
            result.add(TextComponent(ComponentType.TEXT, frontendMessage.substring(currentPos)))
        }
        // 内容被删空的组件不应继续留在标记里（与 deleteComponentMarkers 的清理口径一致）
        return result.filterNot { it.content.isEmpty() && it.subComponents.isEmpty() }
    }

    /** 前台文本的一处编辑（公共前后缀之外的替换区间）。 */
    data class TextEdit(val start: Int, val removedLength: Int, val insertedText: String)

    fun diffTextEdit(oldText: String, newText: String): TextEdit {
        var prefix = 0
        val maxPrefix = minOf(oldText.length, newText.length)
        while (prefix < maxPrefix && oldText[prefix] == newText[prefix]) prefix++
        var suffix = 0
        val maxSuffix = minOf(oldText.length - prefix, newText.length - prefix)
        while (suffix < maxSuffix &&
            oldText[oldText.length - 1 - suffix] == newText[newText.length - 1 - suffix]
        ) suffix++
        val removed = oldText.length - prefix - suffix
        val inserted = newText.substring(prefix, newText.length - suffix)
        return TextEdit(prefix, removed, inserted)
    }

    /**
     * 按"这次实际改了哪一段"来重切内容：
     * - 单点插入落在某个内容段内/边界时，插到该段（边界优先归前一段）；
     * - 替换/删除完全落在同一段内时，只改这一段。
     * 跨段编辑返回 null，由调用方回退到长度重切。
     */
    fun remapComponentContentsForEdit(
        oldComponents: List<TextComponent>,
        oldPlainText: String,
        newPlainText: String
    ): List<TextComponent>? {
        if (oldComponents.isEmpty()) return null
        val expected = oldComponents.joinToString("") { c ->
            c.content + c.subComponents.joinToString("") { it.content }
        }
        if (expected != oldPlainText) return null

        data class Seg(val compIndex: Int, val subIndex: Int, val start: Int, val end: Int)

        val segments = mutableListOf<Seg>()
        var pos = 0
        oldComponents.forEachIndexed { ci, c ->
            val mainEnd = pos + c.content.length
            segments.add(Seg(ci, -1, pos, mainEnd))
            pos = mainEnd
            c.subComponents.forEachIndexed { si, sub ->
                val end = pos + sub.content.length
                segments.add(Seg(ci, si, pos, end))
                pos = end
            }
        }

        val edit = diffTextEdit(oldPlainText, newPlainText)
        val seg: Seg = if (edit.removedLength == 0) {
            val insertAt = edit.start
            segments.lastOrNull { it.start < insertAt && insertAt <= it.end }
                ?: segments.firstOrNull { insertAt >= it.start && insertAt <= it.end }
                ?: return null
        } else {
            val end = edit.start + edit.removedLength
            segments.firstOrNull { edit.start >= it.start && end <= it.end } ?: return null
        }

        fun applyToText(old: String, segStart: Int): String {
            val rel = edit.start - segStart
            return old.substring(0, rel) + edit.insertedText + old.substring(rel + edit.removedLength)
        }

        val updated = oldComponents.toMutableList()
        val c = updated[seg.compIndex]
        if (seg.subIndex < 0) {
            updated[seg.compIndex] = c.copy(content = applyToText(c.content, seg.start))
        } else {
            val subs = c.subComponents.toMutableList()
            val oldSub = subs[seg.subIndex]
            subs[seg.subIndex] = SubComponent(oldSub.type, applyToText(oldSub.content, seg.start))
            updated[seg.compIndex] = c.copy(subComponents = subs)
        }
        // 删除后变空的组件直接移除，避免留下空 translate/score/selector
        return updated.filterNot { it.content.isEmpty() && it.subComponents.isEmpty() }
    }

    /**
     * 在"纯文本位置"插入一个完整组件；如果位置落在 TEXT 组件内部，会把它拆成前后两段。
     * 位置按 content + 子组件内容的长度计算（标记符不参与）。
     */
    fun insertComponentAt(originalText: String, insertPosition: Int, component: TextComponent): String {
        val components = parseTextComponents(originalText).toMutableList()
        if (components.isEmpty()) return componentToText(component)
        var pos = 0
        for (i in components.indices) {
            val c = components[i]
            val len = c.content.length + c.subComponents.sumOf { it.content.length }
            if (insertPosition in pos..(pos + len)) {
                val offset = insertPosition - pos
                if (c.type == ComponentType.TEXT && component.type == ComponentType.TEXT &&
                    offset in 0..c.content.length
                ) {
                    // TEXT 插 TEXT：合并，避免每敲一个字就多一个组件
                    components[i] = c.copy(
                        content = c.content.substring(0, offset) + component.content + c.content.substring(offset)
                    )
                } else if (c.type == ComponentType.TEXT && offset in 0..c.content.length) {
                    val before = c.content.substring(0, offset)
                    val after = c.content.substring(offset)
                    val repl = mutableListOf<TextComponent>()
                    if (before.isNotEmpty()) repl.add(TextComponent(ComponentType.TEXT, before))
                    repl.add(component)
                    if (after.isNotEmpty()) repl.add(TextComponent(ComponentType.TEXT, after))
                    components.removeAt(i)
                    components.addAll(i, repl)
                } else if (offset == 0) {
                    // 光标在该组件起点：插在它前面
                    components.add(i, component)
                } else {
                    components.add(i + 1, component)
                }
                return componentsToText(components)
            }
            pos += len
        }
        components.add(component)
        return componentsToText(components)
    }

    /**
     * 将单个组件转换为标记文本
     */
    fun componentToText(component: TextComponent): String {
        return componentsToText(listOf(component))
    }

    /**
     * 光标所在位置的 translate 组件下标（没有返回 null）。
     * 位置按 content + 子组件内容长度计算；区间含两端，光标停在 translate 末尾也能命中。
     */
    fun findTranslateIndexAt(components: List<TextComponent>, position: Int): Int? {
        if (position < 0) return null
        var pos = 0
        for ((index, component) in components.withIndex()) {
            val len = component.content.length + component.subComponents.sumOf { it.content.length }
            if (component.type == ComponentType.TRANSLATE && position in pos..(pos + len)) {
                return index
            }
            pos += len
        }
        return null
    }

    /**
     * 把构造器的结果写回标记文本：
     * - 光标命中 translate -> 替换键、fallback 与 with；
     * - 没有 translate -> 在光标处新建（复用 [insertComponentAt] 的拆分规则）。
     *
     * [argsJson] 会先规范成紧凑 JSON 数组；空数组表示不写 with 子组件。
     */
    fun applyTranslateWithAt(
        markers: String,
        targetPosition: Int,
        key: String,
        fallback: String,
        argsJson: String
    ): String {
        require(key.isNotBlank()) { "translate key is blank" }
        val normalized = WithArgCodec.normalizeArgsJson(argsJson)
        val hasArgs = normalized != "[]"
        val components = parseTextComponents(markers).toMutableList()
        val index = findTranslateIndexAt(components, targetPosition)

        if (index == null) {
            return insertComponentAt(
                markers,
                targetPosition,
                buildTranslateComponent(key, fallback, normalized.takeIf { hasArgs })
            )
        }

        components[index] = components[index].copy(
            content = key,
            subComponents = buildTranslateSubComponents(fallback, normalized.takeIf { hasArgs })
        )
        return componentsToText(components)
    }

    private fun buildTranslateComponent(
        key: String,
        fallback: String,
        argsJson: String?
    ): TextComponent = TextComponent(
        ComponentType.TRANSLATE,
        key,
        buildTranslateSubComponents(fallback, argsJson)
    )

    private fun buildTranslateSubComponents(
        fallback: String,
        argsJson: String?
    ): MutableList<SubComponent> {
        val subs = mutableListOf<SubComponent>()
        if (fallback.isNotEmpty()) subs.add(SubComponent(SubComponentType.FALLBACK, fallback))
        if (argsJson != null) subs.add(SubComponent(SubComponentType.WITH_JSON, argsJson))
        return subs
    }
    
    /**
     * 将组件内容中的§代码转换为Java版JSON属性
     */
    private fun processSectionCodesToJson(content: String, mNHandling: String, mnCFEnabled: Boolean, bedrockColorToRgb: Boolean = true): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        val tokens = tokenizeText(content, bedrockColorToRgb)
        
        for (token in tokens) {
            val (tokenType, tokenValue) = token
            
            when (tokenType) {
                "color" -> {
                    // 基岩独有颜色（§g..§v）-> 精确 RGB
                    if (bedrockColorToRgb && tokenValue.length == 1 && tokenValue[0] in VersionDiff.BEDROCK_ONLY_COLOR_RGB) {
                        result["color"] = VersionDiff.BEDROCK_ONLY_COLOR_RGB.getValue(tokenValue[0])
                    } else if (tokenValue in JAVA_COLORS) {
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
                                result["color"] = if (bedrockColorToRgb) VersionDiff.BEDROCK_MN_COLOR_RGB.getValue('m') else "dark_red"
                            }
                        }
                        "n" -> {
                            if (mNHandling == "font") {
                                result["underlined"] = true
                            } else {
                                result["color"] = if (bedrockColorToRgb) VersionDiff.BEDROCK_MN_COLOR_RGB.getValue('n') else "red"
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
    private fun tokenizeText(text: String, bedrockColorToRgb: Boolean = true): List<Pair<String, String>> {
        val tokens = mutableListOf<Pair<String, String>>()
        // bedrockColorToRgb 时把基岩独有颜色码（§g..§v）也当"颜色"对待，供后面写精确 RGB；
        // 否则维持原样（当普通文本）。
        val knownColorCodes = setOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f") +
            if (bedrockColorToRgb) VersionDiff.BEDROCK_ONLY_COLOR_RGB.keys.map { it.toString() } else emptyList()
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
     * position 是去掉标记后的纯文本下标，与输入框里的光标位置一致
     */
    fun getComponentTypeAtPosition(text: String, position: Int): ComponentType? {
        if (position < 0) return null
        
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
        
        return null
    }
    
    /**
     * 获取指定位置的文本组件内容
     * position 是去掉标记后的纯文本下标，与输入框里的光标位置一致
     * @return 组件的完整内容字符串（包括副组件内容）
     */
    fun getComponentContentAtPosition(text: String, position: Int): String? {
        if (position < 0) return null
        
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
        
        return null
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
        
        // 没有选中组件 / 选中 TEXT：插入一个 TEXT 组件（内部会转义敏感符号）
        if (currentComponent == null || currentComponent == ComponentType.TEXT) {
            return insertComponentAt(
                originalText, insertPosition, TextComponent(ComponentType.TEXT, textToInsert)
            )
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
            val mainLen = targetComponent.content.length
            val replacement: TextComponent
            if (offsetInTargetComponent <= mainLen) {
                val newContent = targetComponent.content.substring(0, offsetInTargetComponent) +
                    textToInsert +
                    targetComponent.content.substring(offsetInTargetComponent)
                replacement = targetComponent.copy(content = newContent)
            } else {
                // 光标落在副组件内容里：插入到对应的副组件，而不是按主内容长度截 substring（会越界崩溃）
                val subs = targetComponent.subComponents.toMutableList()
                var remaining = offsetInTargetComponent - mainLen
                var inserted = false
                for (i in subs.indices) {
                    if (remaining <= subs[i].content.length) {
                        val c = subs[i].content
                        subs[i] = SubComponent(
                            subs[i].type,
                            c.substring(0, remaining) + textToInsert + c.substring(remaining)
                        )
                        inserted = true
                        break
                    }
                    remaining -= subs[i].content.length
                }
                replacement = if (inserted) {
                    targetComponent.copy(subComponents = subs)
                } else {
                    // 兜底：追加到最后一个副组件（或主内容）末尾
                    if (subs.isNotEmpty()) {
                        val last = subs.last()
                        subs[subs.lastIndex] = SubComponent(last.type, last.content + textToInsert)
                        targetComponent.copy(subComponents = subs)
                    } else {
                        targetComponent.copy(content = targetComponent.content + textToInsert)
                    }
                }
            }
            
            val result = StringBuilder()
            for (i in 0 until targetComponentIndex) {
                result.append(componentToText(components[i]))
            }
            result.append(componentToText(replacement))
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
    context: Context? = null,
    warnings: MutableList<String>? = null,
    separatorAsTextComponent: Boolean = false,
    nbtSyntax: VersionDiff.NbtSyntax = VersionDiff.NbtSyntax.MODERN,
    bedrockColorToRgb: Boolean = true,
    target: com.tellraw.app.model.VersionSpec? = null
): String {
        if (components.isEmpty()) return "{}"

        // 统一展开：expandComponents 现在是幂等的（会保留组件上已有的副组件），
        // 所以不再需要"条目数 ≤ 1 就算已展开"这种靠猜的启发式。
        // 那个启发式会让同一个空 score 组件在"直接转换"与"先展开"两条路上给出不同结果
        // （空内容时 0 ≤ 1 成立 -> 跳过展开 -> {"name":"","objective":""}；
        //  先展开则被改写成 ":" -> {"name":"*","objective":"*"}）。
        val expandedComponents = expandComponents(components)

        // 检查是否所有组件都是TEXT组件
        val allTextComponents = expandedComponents.all { it.type == ComponentType.TEXT && it.subComponents.isEmpty() }
        if (allTextComponents) {
            // 所有组件都是TEXT组件，合并内容并使用原有的text文本处理逻辑
            val combinedText = expandedComponents.joinToString("") { it.content }
            return TextFormatter.convertToJavaJson(combinedText, mNHandling, mnCFEnabled, context, nbtSyntax = nbtSyntax, bedrockColorToRgb = bedrockColorToRgb, target = target)
        }

        if (expandedComponents.size == 1 && expandedComponents[0].type == ComponentType.TEXT && expandedComponents[0].subComponents.isEmpty()) {
            // 单个纯文本组件（这个分支现在不会被触发，因为上面已经处理了所有TEXT组件的情况）
            val plainText = expandedComponents[0].content
            val formatMap = processSectionCodesToJson(plainText, mNHandling, mnCFEnabled, bedrockColorToRgb)
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
                val formatMap = processSectionCodesToJson(mainComponent.content, mNHandling, mnCFEnabled, bedrockColorToRgb)
                result.putAll(formatMap)
            }
            ComponentType.TRANSLATE -> {
                result["translate"] = mainComponent.content
                mainComponent.subComponents.find { it.type == SubComponentType.FALLBACK }?.let {
                    result["fallback"] = it.content
                }
                if (mainComponent.subComponents.isNotEmpty()) {
                    val withSubComponent = withSubComponent(mainComponent, warnings)
                    if (withSubComponent != null) {
                        val jsonMode = withSubComponent.type == SubComponentType.WITH_JSON
                        val items = parseWithItems(withSubComponent.content, jsonMode)
                        if (jsonMode && items.isEmpty() && withSubComponent.content.isNotBlank()) {
                            warnings?.add("with(JSON) 解析失败，已忽略该 with")
                        }
                        val withComponents = items.mapNotNull { item ->
                            if (item.json != null) {
                                item.json
                            } else {
                                val param = item.raw
                                val subFormatMap = processSectionCodesToJson(param, mNHandling, mnCFEnabled, bedrockColorToRgb)
                                if (subFormatMap.isEmpty()) param
                                else {
                                    val mutableMap = subFormatMap.toMutableMap()
                                    mutableMap["text"] = param
                                    mutableMap
                                }
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
                    // 空内容按通配符处理，与 expandComponents 把空score改写成":"后的结果保持一致
                    result["score"] = mapOf(
                        "name" to "*",
                        "objective" to "*"
                    )
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
                        val (javaSelector, _) = SelectorConverter.convertForMixedMode(selectorEntries[0], context, reminders, nbtSyntax)
                        // 混合模式没转换时用原选择器；无论哪条路都再按目标版本过滤一次
                        // （越界参数丢弃 + 提醒、@n 改写）。
                        val baseSelector = if (javaSelector == selectorEntries[0]) selectorEntries[0] else javaSelector
                        val (filteredSelector, _, filterReminders) = SelectorConverter.filterSelectorParameters(
                            baseSelector,
                            SelectorType.JAVA,
                            context,
                            nbtSyntax,
                            target
                        )
                        reminders.addAll(filterReminders)
                        // 组件内选择器产生的提醒交给调用方（原来直接丢弃，导致"提醒用户"全都提醒不了）
                        warnings?.addAll(reminders)
                        filteredSelector
                    } else {
                        // 没有Context，保持原样
                        selectorEntries[0]
                    }

                    result["selector"] = convertedSelector

                    // 从副组件中提取separator
                    val separatorSubComponent = mainComponent.subComponents.find { it.type == SubComponentType.SEPARATOR }
                    if (separatorSubComponent != null) {
                        result["separator"] = separatorJsonValue(separatorSubComponent.content, separatorAsTextComponent)
                    } else if (separatorEntries.isNotEmpty() && separatorEntries[0] != null) {
                        // 如果没有副组件中的separator，使用parseSelectorContent返回的separator
                        result["separator"] = separatorJsonValue(separatorEntries[0]!!, separatorAsTextComponent)
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
                        val formatMap = processSectionCodesToJson(sub.content, mNHandling, mnCFEnabled, bedrockColorToRgb)
                        subMap.putAll(formatMap)
                    }
                    ComponentType.TRANSLATE -> {
                        subMap["translate"] = sub.content
                        sub.subComponents.find { it.type == SubComponentType.FALLBACK }?.let {
                            subMap["fallback"] = it.content
                        }
                        if (sub.subComponents.isNotEmpty()) {
                            val withSubComponent = withSubComponent(sub, warnings)
                            if (withSubComponent != null) {
                                val jsonMode = withSubComponent.type == SubComponentType.WITH_JSON
                                val items = parseWithItems(withSubComponent.content, jsonMode)
                                if (jsonMode && items.isEmpty() && withSubComponent.content.isNotBlank()) {
                                    warnings?.add("with(JSON) 解析失败，已忽略该 with")
                                }
                                val withComponents = items.mapNotNull { item ->
                                    if (item.json != null) {
                                        item.json
                                    } else {
                                        val param = item.raw
                                        val subFormatMap = processSectionCodesToJson(param, mNHandling, mnCFEnabled, bedrockColorToRgb)
                                        if (subFormatMap.isEmpty()) param
                                        else {
                                            val mutableMap = subFormatMap.toMutableMap()
                                            mutableMap["text"] = param
                                            mutableMap
                                        }
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
                            // 空内容按通配符处理，与 mainComponent 保持一致
                            subMap["score"] = mapOf(
                                "name" to "*",
                                "objective" to "*"
                            )
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
                            // 空内容，保持为selector字段（与mainComponent一致）
                            subMap["selector"] = ""
                        } else {
                            // 对selector调用完整的转换逻辑
                            val convertedSelector = if (context != null) {
                                // 使用convertForMixedMode获取Java版和基岩版结果
                                val reminders = mutableListOf<String>()
                                val (javaSelector, _) = SelectorConverter.convertForMixedMode(selectorEntries[0], context, reminders, nbtSyntax)
                                // 无论混合模式是否转换，都再按目标版本过滤一次（越界参数丢弃 + 提醒）。
                                val baseSelector = if (javaSelector == selectorEntries[0]) selectorEntries[0] else javaSelector
                                val (filteredSelector, _, filterReminders) = SelectorConverter.filterSelectorParameters(
                                    baseSelector,
                                    SelectorType.JAVA,
                                    context,
                                    nbtSyntax,
                                    target
                                )
                                reminders.addAll(filterReminders)
                                // 组件内选择器产生的提醒交给调用方（原来直接丢弃）
                                warnings?.addAll(reminders)
                                filteredSelector
                            } else {
                                // 没有Context，保持原样
                                selectorEntries[0]
                            }

                            subMap["selector"] = convertedSelector

                            // 从副组件中提取separator
                            val separatorSubComponent = sub.subComponents.find { it.type == SubComponentType.SEPARATOR }
                            if (separatorSubComponent != null) {
                                subMap["separator"] = separatorJsonValue(separatorSubComponent.content, separatorAsTextComponent)
                            } else if (separatorEntries.isNotEmpty() && separatorEntries[0] != null) {
                                // 如果没有副组件中的separator，使用parseSelectorContent返回的separator
                                subMap["separator"] = separatorJsonValue(separatorEntries[0]!!, separatorAsTextComponent)
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
    warnings: MutableList<String>? = null,
    nbtSyntax: VersionDiff.NbtSyntax = VersionDiff.NbtSyntax.MODERN,
    target: com.tellraw.app.model.VersionSpec? = null
): String {
        if (components.isEmpty()) return """{"rawtext":[]}"""

        // 展开组件，将score和selector组件中的多个条目分割成独立的文本组件
        val expandedComponents = expandComponents(components)

        // 检查是否所有组件都是TEXT组件
        val allTextComponents = expandedComponents.all { it.type == ComponentType.TEXT && it.subComponents.isEmpty() }
        if (allTextComponents) {
            // 所有组件都是TEXT组件，合并内容并使用原有的text文本处理逻辑
            val combinedText = expandedComponents.joinToString("") { it.content }
            return TextFormatter.convertToBedrockJson(combinedText, mNHandling, mnCFEnabled, nbtSyntax = nbtSyntax, target = target)
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
                    if (component.subComponents.any { it.type == SubComponentType.FALLBACK }) {
                        warnings?.add("基岩版不支持 translate 的 fallback，已忽略")
                    }
                    if (component.subComponents.isNotEmpty()) {
                        val withSubComponent = withSubComponent(component, warnings)
                        if (withSubComponent != null) {
                            val jsonMode = withSubComponent.type == SubComponentType.WITH_JSON
                            // with：纯文本参数 -> text；JSON 嵌套组件 -> 基岩 rawtext 对象列表
                            // （基岩 rawtext 的 with 支持 {"rawtext":[...]}，可含嵌套 translate）
                            val items = parseWithItems(withSubComponent.content, jsonMode)
                            if (jsonMode && items.isEmpty() && withSubComponent.content.isNotBlank()) {
                                warnings?.add("with(JSON) 解析失败，已忽略该 with")
                            }
                            val withRawtext = mutableListOf<Any>()
                            for (item in items) {
                                if (item.json != null) {
                                    jsonWithToBedrock(item.json, warnings, withRawtext)
                                } else {
                                    withRawtext.add(
                                        mapOf<String, Any>(
                                            "text" to processSectionCodesToBedrock(item.raw, mNHandling, mnCFEnabled)
                                        )
                                    )
                                }
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
                        // 空内容按通配符处理，与 Java 版保持一致
                        item["score"] = mapOf(
                            "name" to "*",
                            "objective" to "*"
                        )
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
                        // 检测是否有separator
                        // 注意：selector组件在这里已经被展开，separator变成了SEPARATOR子组件，
                        // 内容里不再有 ,'sep': 字样，所以必须同时看子组件（否则这条提醒永远不会触发）
                        if (separatorEntries.any { it != null } ||
                            component.subComponents.any { it.type == SubComponentType.SEPARATOR }
                        ) {
                            warnings?.add(context?.getString(R.string.bedrock_separator_not_supported) ?: "基岩版不支持separator参数，已忽略所有sep:定义")
                        }

                        // 对selector调用完整的转换逻辑
                        val convertedSelector = if (context != null) {
                            // 使用convertForMixedMode获取Java版和基岩版结果
                            val reminders = mutableListOf<String>()
                            val (_, bedrockSelector) = SelectorConverter.convertForMixedMode(selectorEntries[0], context, reminders, nbtSyntax)
                            // 无论混合模式是否转换，都再按目标版本过滤一次（越界参数丢弃 + 提醒、@n 改写）。
                            val baseSelector = if (bedrockSelector == selectorEntries[0]) selectorEntries[0] else bedrockSelector
                            val (filteredSelector, _, filterReminders) = SelectorConverter.filterSelectorParameters(
                                baseSelector,
                                SelectorType.BEDROCK,
                                context,
                                nbtSyntax,
                                target
                            )
                            reminders.addAll(filterReminders)
                            // 组件内选择器产生的提醒交给调用方（原来直接丢弃，导致"删除并提醒"全都提醒不了）
                            warnings?.addAll(reminders)
                            filteredSelector
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
     * 特殊语法：,'sep':值（用于指定前面所有@选择器的分隔符）
     *   带单引号是字面文本：,'sep':'kk' 的值是 kk；,'sep':'air' 就是文本 air
     *   不带单引号才是关键字：,'sep':air 让前面的第一个@忽略所有separator
     * 转义规则：selector里方括号 [...] 内的逗号属于该选择器内部；也可以写 \, 表示一个转义逗号
     * @return Pair<选择器列表, 分隔符列表>（分隔符列表与选择器列表一一对应，无分隔符则为null）
     */
    private fun parseSelectorContent(rawContent: String): Pair<List<String>, List<String?>> {
        // 先把"不该当作条目分隔符"的逗号（方括号内、\, 转义）换成等长的占位字符，
        // 后面的扫描逻辑因此可以保持原样；字符串长度不变，位置索引依然对齐
        val content = maskSeparatorCommas(rawContent)

        // 第一步：提取所有sep:定义和它们的位置
        // 规则：,'sep':作为一个整体，在selector文本组件有最高优先级
        // ,'sep':后面的文本一直到下一个,(不包含,)为一个整体，或者是如果后面没有,时，那么就是一直到结束的文本的separator
        val sepDefinitions = mutableListOf<Triple<Int, String, Int>>()  // (原始位置索引, 分隔符, selector索引)
        // 哪些 sep:定义是不带引号的 air（关键字：让前面的第一个@忽略所有separator）
        val airKeywordPositions = mutableSetOf<Int>()
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
                val rawSeparatorValue = if (nextCommaIndex != -1) {
                    remainingText.substring(0, nextCommaIndex).trim()
                } else {
                    remainingText.trim()
                }
                // 外面的单引号表示"引号里是字面文本"：,'sep':'kk' -> kk，,'sep':'air' -> 文本 air
                // 不带单引号才是关键字：,'sep':air
                val isQuotedLiteral = rawSeparatorValue.length >= 2 &&
                    rawSeparatorValue.first() == '\'' && rawSeparatorValue.last() == '\''
                val separatorValue = if (isQuotedLiteral) {
                    // 引号里出现两个连续单引号表示一个字面单引号：
                    // ,'sep':'''' -> 分隔符就是 ' ；,'sep':'a''b' -> a'b
                    rawSeparatorValue.substring(1, rawSeparatorValue.length - 1).replace("''", "'")
                } else {
                    rawSeparatorValue
                }
                if (!isQuotedLiteral && separatorValue == "air") {
                    airKeywordPositions.add(sepStart)
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
        
        // 第二步：提取selector条目（不包括sep:定义），同时记录每个selector的原始位置
        
                // 规则：选择器的读取是从@一直到后面一个@(不包含后面一个@)，或者是如果后面没有@了那就表明一直到这个文本组件的结束或者一直到后面的,(不包含,)
        
                // 规则：如果同一selector文本组件第一个@前面有文本，那就第一个@前面的文本作为独立的selector组件参数(第一个@前面的'sep':直接被忽略)
        
                // 规则：,'sep':作为一个整体，在selector文本组件有最高优先级，读取selector时必须跳过sep:定义
        
                val selectors = mutableListOf<String>()
        
                val selectorPositions = mutableListOf<Int>()  // 每个selector的原始位置
        
                var startIndex = -1
                var lastEntryEnd = 0   // 上一个已消费条目（含分隔符）之后的位置，用于识别"无@条目"
        
                var lastAtPos = -1
        
                i = 0
        
                
        
                // 跳过sep:定义查找下一个@
        
                fun skipSepDefinitions(startIndex: Int): Int {
        
                    var idx = startIndex
        
                    while (idx < content.length) {
        
                        if (content.substring(idx).startsWith(",'sep':")) {
        
                            // 跳过,'sep':'
        
                            idx += 7  // ,'sep':的长度
        
                            // 找到sep:定义后面的下一个逗号（跳过sep:定义的值）
                            val nextCommaIndex = content.indexOf(',', idx)
                            if (nextCommaIndex != -1) {
                                // 找到下一个逗号，跳过它（因为这个逗号是sep:定义的一部分）
                                idx = nextCommaIndex + 1
                            } else {
                                // 没有找到逗号，到达文本末尾
                                idx = content.length
                            }
        
                        } else {
        
                            break
        
                        }
        
                    }
        
                    return idx
        
                }
        
                
        
                while (i < content.length) {
        
                    // 跳过sep:定义
        
                    if (content.substring(i).startsWith(",'sep':")) {
        
                        i = skipSepDefinitions(i)
        
                        continue
        
                    }
        
                    
        
                    if (content[i] == '@') {
        
                        if (startIndex == -1) {
        
                            // 第一个@，记录起点
        
                            // 检查前面是否有非@文本（第一个@前面的'sep':直接被忽略）
        
                            if (i > 0 && lastAtPos == -1) {
        
                                // 前面有非@文本，添加为selector
        
                                // 但是需要跳过sep:定义
        
                                // 从 lastEntryEnd 开始取：被"无@条目"逻辑消费过的部分不能再算一次
                                // （否则 mknbt,@p 会把 mknbt 加两遍）
                                var textBeforeAt = content.substring(lastEntryEnd, i)
        
                                // 移除sep:定义
                                
                                                                while (textBeforeAt.contains(",'sep':")) {
                                                                    val sepStart = textBeforeAt.indexOf(",'sep':")        
                                    if (sepStart == -1) break
        
                                    val sepEnd = sepStart + 7
        
                                    val remainingText = textBeforeAt.substring(sepEnd)
        
                                    val nextCommaIndex = remainingText.indexOf(',')
        
                                    if (nextCommaIndex != -1) {
        
                                        textBeforeAt = textBeforeAt.substring(0, sepStart) + textBeforeAt.substring(sepEnd + nextCommaIndex + 1)
        
                                    } else {
        
                                        textBeforeAt = textBeforeAt.substring(0, sepStart)
        
                                    }
        
                                }
        
                                // 去掉尾随逗号：mknbt,@p 会切出 "mknbt,"，逗号不属于选择器名
                                // （同文件另外两条路径都做了这件事，这里原先漏了）
                                if (textBeforeAt.endsWith(",")) {
                                    textBeforeAt = textBeforeAt.substring(0, textBeforeAt.length - 1)
                                }
                                if (textBeforeAt.isNotEmpty()) {
        
                                    selectors.add(textBeforeAt)
        
                                    selectorPositions.add(0)  // 第一个selector的位置是0
        
                                }
        
                            }
        
                            startIndex = i
        
                        } else {
        
                            // 找到下一个@，提取从startIndex到i-1的selector
        
                            var selector = content.substring(startIndex, i)
        
                            // 检查selector是否包含sep:定义，如果包含，只取sep:定义之前的部分
                            val sepStart = selector.indexOf(",'sep':")
                            if (sepStart != -1) {
                                selector = selector.substring(0, sepStart)
                            }
        
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
        
                            // 检查selector是否包含sep:定义，如果包含，只取sep:定义之前的部分
                            val sepStart = selector.indexOf(",'sep':")
                            if (sepStart != -1) {
                                selector = selector.substring(0, sepStart)
                            }
                            
                            // 检查selector是否以逗号结尾，如果是，则去掉末尾的逗号
                            if (selector.endsWith(",")) {
                                selector = selector.substring(0, selector.length - 1)
                            }
        
                            selectors.add(selector)
                            selectorPositions.add(startIndex)
                            startIndex = -1
                            lastEntryEnd = skipSepDefinitions(i + 1)   // 这段（含分隔符）已消费掉

                            // 跳过sep:定义
                            i = lastEntryEnd - 1  // -1 因为循环末尾会i++
                        } else {
                    // 先看"这个逗号之前"有没有一段无@文本（@a,mknbt,@p 里的 mknbt）。
                    // 原实现只向后（前瞻）看，导致夹在中间的无@条目被整段丢掉 —— 违反意见.txt:21-25
                    val bareBefore = content.substring(lastEntryEnd, i).trim()
                    if (bareBefore.isNotEmpty()) {
                        selectors.add(bareBefore)
                        selectorPositions.add(lastEntryEnd)
                        lastEntryEnd = i + 1
                        i++
                        continue
                    }

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
            
            // 检查selector是否包含sep:定义，如果包含，只取sep:定义之前的部分
            val sepStart = selector.indexOf(",'sep':")
            if (sepStart != -1) {
                selector = selector.substring(0, sepStart)
            }
            
            // 检查selector是否以逗号结尾，如果是，则去掉末尾的逗号
            if (selector.endsWith(",")) {
                selector = selector.substring(0, selector.length - 1)
            }
            
            selectors.add(selector)
            selectorPositions.add(startIndex)
        } else if (lastAtPos == -1 && content.isNotEmpty()) {
            // 没有@：整个内容作为一个选择器。
            // 但 ,'sep':… 必须先剥掉 —— separator 只修饰@选择器（意见.txt:32），
            // 否则会生成 {"selector":"mknbt,'sep':kk"} 这种把语法漏进选择器的结果
            var onlySelector = content
            val onlySepIndex = onlySelector.indexOf(",'sep':")
            if (onlySepIndex != -1) {
                onlySelector = onlySelector.substring(0, onlySepIndex)
            }
            if (onlySelector.endsWith(",")) {
                onlySelector = onlySelector.substring(0, onlySelector.length - 1)
            }
            if (onlySelector.isNotEmpty()) {
                selectors.add(onlySelector)
                selectorPositions.add(0)
            }
        } else if (startIndex == -1 && lastAtPos != -1) {
            // @ 之后、最后一个逗号之后剩下的无@条目（例如 @a,mknbt 里的 mknbt）
            var bareTail = content.substring(lastEntryEnd).trim()
            val tailSepIndex = bareTail.indexOf(",'sep':")
            if (tailSepIndex != -1) {
                bareTail = bareTail.substring(0, tailSepIndex)
            }
            if (bareTail.endsWith(",")) {
                bareTail = bareTail.substring(0, bareTail.length - 1)
            }
            if (bareTail.isNotEmpty()) {
                selectors.add(bareTail)
                selectorPositions.add(lastEntryEnd)
            }
        }
        
        // 第三步：为每个sep:定义找到对应的selector索引
        // 规则：sep:定义修饰它前面的所有@选择器，直到遇到没有被separator修饰的@选择器
        // 限制：
        // 1. separator只能修饰@选择器（mknbt、uuid等非@选择器忽略separator）
        // 2. 第一个@前面的sep:定义直接被忽略
        // 3. 'sep':'air'的特殊情况优先级最高：前面的第一个@选择器会自动忽略所有separator参数
        
        // 为每个sep:定义找到对应的selector索引
        // sepIndex应该是sepPos前面的最后一个selector的索引
        // 这样循环从sepIndex开始，修饰sepPos前面的所有@选择器
        for (sepIdx in sepDefinitions.indices) {
            val sepPos = sepDefinitions[sepIdx].first
            // 找到sepPos前面的最后一个selector
            var lastSelectorBeforeSep = -1
            for (selectorIdx in selectorPositions.indices) {
                if (selectorPositions[selectorIdx] < sepPos) {
                    lastSelectorBeforeSep = selectorIdx
                } else {
                    break
                }
            }
            sepDefinitions[sepIdx] = Triple(sepPos, sepDefinitions[sepIdx].second, lastSelectorBeforeSep)
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
        
        // 用于记录哪些selector被哪些separator修饰
        val selectorToSep = mutableMapOf<Int, Int>()  // selector索引 -> 修饰它的sep索引
        
        // 先处理'sep':'air'，标记被影响的选择器
        for ((sepPos, separatorValue, sepIndex) in sepDefinitions) {
            if (sepIndex == -1) continue
            if (sepPos !in airKeywordPositions) continue
            
            // 找到sep:定义前面的第一个@选择器
            // 注意：sepIndex 本身就是"该 sep 前面最后一个 selector"，也就是"它前面的第一个@"，
            // 所以起点必须是 sepIndex；原来写 sepIndex-1 会保护到左边那个选择器（意见.txt:36-37）
            var targetIndex = -1
            for (index in sepIndex downTo 0) {
                if (selectors[index].startsWith("@")) {
                    targetIndex = index
                    break
                }
            }
            if (targetIndex != -1) {
                // 标记这个@选择器为忽略所有separator
                airIgnoredSelectors.add(targetIndex)
            }
        }
        
        // 处理sep:定义（跳过'sep':'air'和被'sep':'air'影响的选择器）
        for ((sepPos, separatorValue, sepIndex) in sepDefinitions) {
            if (sepIndex == -1) continue
            if (sepPos in airKeywordPositions) continue  // 关键字air不是分隔符，已经在上面单独处理过了
            
            // 检查sep:定义是否在第一个@选择器之前
            if (firstAtSelectorIndex != -1 && sepIndex < firstAtSelectorIndex) {
                // 第一个@前面的sep:定义直接被忽略
                continue
            }
            
            // 从sepIndex开始往前查找，修饰所有@选择器
            // 规则：separator会修饰他前面所有@选择器，直到遇到没有被separator修饰的@选择器
            // 注意：被'sep':'air'影响的选择器会被跳过，但不影响其他separator的修饰逻辑
            // 注意：遇到非@选择器（如mknbt、uuid等）时停止查找
            for (index in sepIndex downTo 0) {
                if (!selectors[index].startsWith("@")) {
                    // 遇到非@选择器，停止查找
                    break
                }
                
                // 这是一个@选择器
                
                // 如果被'sep':'air'影响，跳过这个选择器，但不停止查找
                if (airIgnoredSelectors.contains(index)) {
                    continue
                }
                
                if (!selectorToSep.containsKey(index)) {
                    // 这个@选择器还没有被修饰，应用separator
                    separators[index] = separatorValue
                    selectorToSep[index] = sepIndex
                } else {
                    // 这个@选择器已经被其他separator修饰了，停止查找
                    // （被'sep':'air'影响的选择器不会在这里停止，因为上面已经continue了）
                    break
                }
            }
        }
        
        return Pair(
            selectors.map { unmaskSeparatorCommas(it) },
            separators.map { value -> value?.let { unmaskSeparatorCommas(it) } }
        )
    }

    // "不该当作条目分隔符"的逗号，在扫描期间使用的占位字符
    private const val COMMA_PLACEHOLDER = '\uE000'

    /**
     * 把"不该当作条目分隔符"的逗号换成占位字符，配合 unmaskSeparatorCommas 还原
     * 规则：方括号 [...] 内的逗号属于该选择器内部；\, 是转义逗号
     */
    private fun maskSeparatorCommas(content: String): String {
        if (!content.contains('[') && !content.contains("\\,")) return content

        val chars = content.toCharArray()
        var depth = 0
        var i = 0
        while (i < chars.size) {
            when {
                chars[i] == '\\' && i + 1 < chars.size && chars[i + 1] == ',' -> {
                    chars[i + 1] = COMMA_PLACEHOLDER
                    i += 2
                }
                chars[i] == '[' -> {
                    depth++
                    i++
                }
                chars[i] == ']' -> {
                    if (depth > 0) depth--
                    i++
                }
                chars[i] == ',' && depth > 0 -> {
                    chars[i] = COMMA_PLACEHOLDER
                    i++
                }
                else -> i++
            }
        }
        return String(chars)
    }

    /**
     * 还原占位字符：方括号内的逗号还原成逗号，\, 转义还原成逗号（去掉反斜杠）
     */
    private fun unmaskSeparatorCommas(value: String): String {
        if (!value.contains(COMMA_PLACEHOLDER)) return value
        return value.replace(COMMA_PLACEHOLDER, ',').replace("\\,", ",")
    }

    /**
     * 解析translate组件的with参数
     * @return 参数列表
     */
    /** 一个 with 项：普通文本（raw）或嵌套 JSON 文本组件。 */
    data class WithItem(val raw: String = "", val json: JsonElement? = null) {
        fun plainText(): String = if (json != null) jsonPlainText(json) else raw
    }

    /**
     * 解析 translate 的 with：
     *  - `[...]` / `{...}` -> JSON 文本组件（保留样式/嵌套）；
     *  - 其他 -> 逗号分隔的纯文本（支持 `\,` 转义）。
     */
    fun parseWithItems(content: String, jsonMode: Boolean = false): List<WithItem> {
        val t = content.trim()
        if (t.isEmpty()) return emptyList()
        if (jsonMode) {
            try {
                val el = JsonParser.parseString(t)
                if (el.isJsonArray) return el.asJsonArray.map { WithItem(json = it) }
                if (el.isJsonObject) return listOf(WithItem(json = el))
            } catch (e: Exception) {
                return emptyList()   // 明确选了 JSON 但解析失败：由调用方提醒
            }
            return emptyList()
        }
        return parseTranslateWithContent(content).map { WithItem(raw = it) }
    }

    /** 取 translate 的 with 子组件：优先 with_json；两者同时存在时按 JSON 并提醒。 */
    fun withSubComponent(component: TextComponent, warnings: MutableList<String>? = null): SubComponent? {
        val json = component.subComponents.firstOrNull { it.type == SubComponentType.WITH_JSON }
        val plain = component.subComponents.firstOrNull { it.type == SubComponentType.WITH }
        if (json != null && plain != null) {
            warnings?.add("translate 同时有 with 和 with(JSON)，已按 with(JSON) 处理")
        }
        return json ?: plain
    }

    fun isJsonWith(component: TextComponent): Boolean =
        component.subComponents.any { it.type == SubComponentType.WITH_JSON }

    /**
     * 在 with（纯文本）与 with(JSON) 之间切换时继承内容：
     *  - 文本 -> JSON：每个参数包成 {"text": ...}；
     *  - JSON -> 文本：取纯文本，逗号写成 \, 防止被当分隔符；
     *  - JSON 的样式在降级为纯文本时会丢失（这是纯文本模式的必然结果）。
     * 多次切换对纯文本内容是稳定的；带样式的组件切回纯文本后无法恢复样式。
     */
    fun migrateWithMode(component: TextComponent, toJson: Boolean): TextComponent {
        val subComponents = component.subComponents.toMutableList()
        val plain = subComponents.firstOrNull { it.type == SubComponentType.WITH }
        val json = subComponents.firstOrNull { it.type == SubComponentType.WITH_JSON }
        if (toJson) {
            if (json == null && plain != null) {
                val args = if (plain.content.isEmpty()) emptyList()
                    else parseTranslateWithContent(plain.content)
                val arr = args.map { mapOf("text" to it) }
                subComponents.remove(plain)
                subComponents.add(
                    SubComponent(SubComponentType.WITH_JSON, com.google.gson.Gson().toJson(arr))
                )
            } else if (json != null) {
                subComponents.remove(plain)
            }
        } else {
            if (plain == null && json != null) {
                val args = parseWithItems(json.content, jsonMode = true).map { item ->
                    item.plainText().replace(",", "\\,")
                }
                subComponents.remove(json)
                subComponents.add(SubComponent(SubComponentType.WITH, args.joinToString(",")))
            } else if (plain != null) {
                subComponents.remove(json)
            }
        }
        return component.copy(subComponents = subComponents)
    }

    /** 公开给网格预览：translate 的 with 参数（嵌套组件取纯文本）。 */
    fun parseTranslateArgs(content: String, jsonMode: Boolean = false): List<String> =
        parseWithItems(content, jsonMode).map { it.plainText() }

    private fun jsonPlainText(el: JsonElement): String {
        if (el.isJsonPrimitive) return el.asString
        if (el.isJsonArray) return el.asJsonArray.joinToString("") { jsonPlainText(it) }
        if (!el.isJsonObject) return ""
        val o = el.asJsonObject
        val sb = StringBuilder()
        when {
            o.has("text") && o.get("text").isJsonPrimitive -> sb.append(o.get("text").asString)
            o.has("translate") && o.get("translate").isJsonPrimitive -> sb.append(o.get("translate").asString)
            o.has("selector") && o.get("selector").isJsonPrimitive -> sb.append(o.get("selector").asString)
            o.has("score") && o.get("score").isJsonObject -> {
                val sc = o.getAsJsonObject("score")
                sb.append(sc.get("name")?.takeIf { it.isJsonPrimitive }?.asString ?: "")
                sb.append(":")
                sb.append(sc.get("objective")?.takeIf { it.isJsonPrimitive }?.asString ?: "")
            }
        }
        o.get("extra")?.takeIf { it.isJsonArray }?.let { ex ->
            for (e in ex.asJsonArray) sb.append(jsonPlainText(e))
        }
        return sb.toString()
    }

    /** 把 Java JSON 组件转成基岩 rawtext 项（with 对象形式）。样式字段暂未完全保留。 */
    private fun jsonWithToBedrock(
        el: JsonElement,
        warnings: MutableList<String>?,
        out: MutableList<Any>
    ) {
        when {
            el.isJsonPrimitive -> out.add(mapOf("text" to el.asString))
            el.isJsonArray -> el.asJsonArray.forEach { jsonWithToBedrock(it, warnings, out) }
            el.isJsonObject -> {
                val o = el.asJsonObject
                val styleKeys = listOf(
                    "color", "bold", "italic", "underlined", "strikethrough", "obfuscated",
                    "font", "clickEvent", "hoverEvent", "insertion"
                )
                val styleHit = styleKeys.firstOrNull { o.has(it) }
                if (styleHit != null) {
                    warnings?.add("基岩 with 的嵌套组件样式暂未完全保留（$styleHit）")
                }
                when {
                    o.has("text") -> {
                        val t = o.get("text")
                        if (t.isJsonPrimitive) {
                            out.add(mapOf("text" to t.asString))
                        } else {
                            warnings?.add("基岩 with 里的 text 不是字符串，已按 JSON 文本转换")
                            out.add(mapOf("text" to t.toString()))
                        }
                    }
                    o.has("translate") -> {
                        val keyEl = o.get("translate")
                        val key = if (keyEl.isJsonPrimitive) keyEl.asString else {
                            warnings?.add("基岩 with 里的 translate 键不是字符串，已按 JSON 文本转换")
                            keyEl.toString()
                        }
                        val m = mutableMapOf<String, Any>("translate" to key)
                        o.get("with")?.let { m["with"] = bedrockWithValue(it, warnings) }
                        out.add(m)
                    }
                    o.has("selector") -> {
                        val selEl = o.get("selector")
                        val sel = if (selEl.isJsonPrimitive) selEl.asString else {
                            warnings?.add("基岩 with 里的 selector 不是字符串，已按 JSON 文本转换")
                            selEl.toString()
                        }
                        out.add(mapOf("selector" to sel))
                    }
                    o.has("score") && o.get("score").isJsonObject -> {
                        val sc = o.getAsJsonObject("score")
                        out.add(
                            mapOf(
                                "score" to mapOf(
                                    "name" to (sc.get("name")?.takeIf { it.isJsonPrimitive }?.asString ?: "*"),
                                    "objective" to (sc.get("objective")?.takeIf { it.isJsonPrimitive }?.asString ?: "*")
                                )
                            )
                        )
                    }
                }
                o.get("extra")?.let { extra -> jsonWithToBedrock(extra, warnings, out) }
            }
        }
    }

    private fun bedrockWithValue(el: JsonElement, warnings: MutableList<String>?): Any {
        if (el.isJsonArray) {
            val arr = el.asJsonArray
            if (arr.all { it.isJsonPrimitive }) return arr.map { it.asString }
            val list = mutableListOf<Any>()
            jsonWithToBedrock(el, warnings, list)
            return mapOf("rawtext" to list)
        }
        if (el.isJsonObject) {
            val list = mutableListOf<Any>()
            jsonWithToBedrock(el, warnings, list)
            return mapOf("rawtext" to list)
        }
        return if (el.isJsonPrimitive) el.asString else ""
    }

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
    fun expandComponents(components: List<TextComponent>): List<TextComponent> {
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
                        // 空内容，保持为score组件（与SELECTOR一致）
                        expanded.add(TextComponent(ComponentType.SCORE, ":"))
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
                        // 空内容，保留为selector组件（内容为空字符串），副组件一并保留
                        expanded.add(TextComponent(ComponentType.SELECTOR, "", component.subComponents))
                    } else {
                        // 每个selector条目都作为独立的selector组件
                        for ((index, selector) in selectorEntries.withIndex()) {
                            val subComponents = mutableListOf<SubComponent>()
                            // 保留组件上已有的副组件（例如界面上设好的 separator）。
                            // 只有单条目时才有意义——多条目时无法判断该挂到哪一个上。
                            // 有了这个，expandComponents 才是幂等的：对"已展开"的组件再展开一次
                            // 不会把 separator 丢掉。
                            if (selectorEntries.size == 1) {
                                component.subComponents.forEach { existing ->
                                    if (subComponents.none { it.type == existing.type }) {
                                        subComponents.add(existing)
                                    }
                                }
                            }
                            // 添加separator作为副组件
                            val sep = if (index < separatorEntries.size) separatorEntries[index] else null
                            if (sep != null && subComponents.none { it.type == SubComponentType.SEPARATOR }) {
                                subComponents.add(SubComponent(SubComponentType.SEPARATOR, sep))
                            }
                            val newComponent = TextComponent(ComponentType.SELECTOR, selector, subComponents)
                            expanded.add(newComponent)
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
            is String -> "\"${escapeJsonString(value)}\""
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
            is JsonElement -> value.toString()
            else -> "\"${escapeJsonString(value.toString())}\""
        }
    }

    /**
     * separator 的输出形态：
     * 默认输出纯字符串；开关打开时输出文本组件 {"text": "..."}，方便玩家手动替换该组件（如加颜色）
     */
    private fun separatorJsonValue(content: String, asTextComponent: Boolean): Any =
        if (asTextComponent) mapOf("text" to content) else content

    /**
     * 按JSON规范转义字符串内容（反斜杠、双引号、控制字符）
     * 只转义反斜杠是不够的：结尾的反斜杠会把闭合引号吃掉，产出非法JSON
     */
    private fun escapeJsonString(value: String): String {
        val builder = StringBuilder(value.length + 8)
        for (ch in value) {
            when (ch) {
                '"' -> builder.append("\\\"")
                '\\' -> builder.append("\\\\")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                else -> if (ch < ' ') {
                    builder.append("\\u").append(ch.code.toString(16).padStart(4, '0'))
                } else {
                    builder.append(ch)
                }
            }
        }
        return builder.toString()
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
